package org.folio.rmapi;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.folio.util.FutureUtils.allOfSucceeded;

import io.vertx.core.Vertx;
import jakarta.validation.ValidationException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.log4j.Log4j2;
import org.folio.cache.VertxCache;
import org.folio.holdingsiq.model.Configuration;
import org.folio.holdingsiq.model.FilterQuery;
import org.folio.holdingsiq.model.PackageByIdData;
import org.folio.holdingsiq.model.PackageData;
import org.folio.holdingsiq.model.PackageId;
import org.folio.holdingsiq.model.Packages;
import org.folio.holdingsiq.model.Sort;
import org.folio.holdingsiq.model.Titles;
import org.folio.holdingsiq.service.TitlesHoldingsIQService;
import org.folio.holdingsiq.service.impl.PackagesHoldingsIQServiceImpl;
import org.folio.properties.common.SearchProperties;
import org.folio.rest.util.IdParser;
import org.folio.rmapi.cache.PackageCacheKey;
import org.folio.rmapi.result.PackageBulkResult;
import org.folio.rmapi.result.PackageResult;
import org.folio.rmapi.result.VendorResult;

@Log4j2
public class PackageServiceImpl extends PackagesHoldingsIQServiceImpl {

  private static final String INCLUDE_PROVIDER_VALUE = "provider";
  private static final String INCLUDE_RESOURCES_VALUE = "resources";
  private final ProvidersServiceImpl providerService;
  private final TitlesHoldingsIQService titlesService;
  private final VertxCache<PackageCacheKey, PackageByIdData> packageCache;
  private final SearchProperties searchProperties;
  private final Configuration configuration;
  private final String tenantId;

  public PackageServiceImpl(Configuration config, Vertx vertx, String tenantId, ProvidersServiceImpl providerService,
                            TitlesHoldingsIQService titlesService,
                            VertxCache<PackageCacheKey, PackageByIdData> packageCache,
                            SearchProperties searchProperties) {
    super(config, vertx);
    this.providerService = providerService;
    this.titlesService = titlesService;
    this.packageCache = packageCache;
    this.tenantId = tenantId;
    this.configuration = config;
    this.searchProperties = searchProperties;
  }

  public CompletableFuture<PackageResult> retrievePackage(PackageId packageId, List<String> includedObjects) {
    return retrievePackage(packageId, includedObjects, false);
  }

  public CompletableFuture<PackageResult> retrievePackage(PackageId packageId, List<String> includedObjects,
                                                          boolean useCache) {
    CompletableFuture<PackageByIdData> packageFuture;
    if (useCache) {
      packageFuture = retrievePackageWithCache(packageId);
    } else {
      packageFuture = retrievePackage(packageId);
    }

    CompletableFuture<Titles> titlesFuture;
    if (includedObjects.contains(INCLUDE_RESOURCES_VALUE)) {
      titlesFuture = titlesService.retrieveTitles(packageId.getProviderIdPart(), packageId.getPackageIdPart(),
        FilterQuery.builder().build(), searchProperties.getTitlesSearchType(), Sort.NAME, 1, 25);
    } else {
      titlesFuture = completedFuture(null);
    }

    CompletableFuture<VendorResult> vendorFuture;
    if (includedObjects.contains(INCLUDE_PROVIDER_VALUE)) {
      vendorFuture = providerService.retrieveProvider(packageId.getProviderIdPart(), null);
    } else {
      vendorFuture = completedFuture(new VendorResult(null, null));
    }

    return CompletableFuture.allOf(packageFuture, titlesFuture, vendorFuture)
      .thenCompose(o ->
        completedFuture(new PackageResult(packageFuture.join(), vendorFuture.join().getVendor(), titlesFuture.join())));
  }

  public CompletableFuture<Packages> retrievePackages(List<PackageId> packageIds) {
    Set<CompletableFuture<PackageResult>> futures = packageIds.stream()
      .map(id -> retrievePackage(id, Collections.emptyList(), true))
      .collect(Collectors.toSet());
    return allOfSucceeded(futures, throwable -> log.warn(throwable.getMessage(), throwable))
      .thenApply(this::mapToPackages);
  }

  public CompletableFuture<PackageBulkResult> retrievePackagesBulk(Set<String> packageIds) {
    Set<CompletableFuture<Result<PackageResult, String>>> futures = new HashSet<>();

    packageIds.forEach(inputId -> {
      try {
        PackageId id = IdParser.parsePackageId(inputId);

        futures.add(retrievePackageForBulk(id));
      } catch (ValidationException e) {
        futures.add(completedFuture(new Failure<>(inputId)));
      }
    });

    return allOfSucceeded(futures, throwable -> log.warn(throwable.getMessage(), throwable))
      .thenApply(this::mapToPackageBulk);
  }

  private PackageBulkResult mapToPackageBulk(List<Result<PackageResult, String>> results) {
    List<PackageResult> pr = new ArrayList<>();
    List<String> failedIds = new ArrayList<>();

    results.forEach(r -> r.accept(pr::add).otherwise(failedIds::add));

    return new PackageBulkResult(mapToPackages(pr), failedIds);
  }

  private CompletableFuture<Result<PackageResult, String>> retrievePackageForBulk(PackageId id) {
    return retrievePackage(id, Collections.emptyList(), true)
      .thenApply(successfulResult())
      .exceptionally(throwable -> {
        log.warn(throwable.getMessage(), throwable);

        return new Failure<>(IdParser.packageIdToString(id));
      });
  }

  private static Function<PackageResult, Result<PackageResult, String>> successfulResult() {
    return Success::new;
  }

  private Packages mapToPackages(List<PackageResult> results) {
    List<PackageData> packages = results.stream()
      .map(PackageResult::getPackageData)
      .sorted(Comparator.comparing(PackageData::getPackageName))
      .collect(Collectors.toList());
    return Packages.builder()
      .packagesList(packages)
      .build();
  }

  private CompletableFuture<PackageByIdData> retrievePackageWithCache(PackageId packageId) {
    PackageCacheKey cacheKey = PackageCacheKey.builder()
      .packageId(IdParser.packageIdToString(packageId))
      .rmapiConfiguration(configuration)
      .tenant(tenantId)
      .build();
    return packageCache.getValueOrLoad(cacheKey, () -> retrievePackage(packageId));
  }

  private interface Result<R, F> {

    boolean successful();

    boolean failed();

    R getResult();

    F getFailure();

    default Result<R, F> accept(Consumer<? super R> consumer) {
      if (successful()) {
        consumer.accept(getResult());
      }
      return this;
    }

    default Result<R, F> otherwise(Consumer<? super F> consumer) {
      if (failed()) {
        consumer.accept(getFailure());
      }
      return this;
    }
  }

  private static class Success<R, F> implements Result<R, F> {

    private final R result;

    Success(R result) {
      this.result = result;
    }

    @Override
    public boolean successful() {
      return true;
    }

    @Override
    public boolean failed() {
      return false;
    }

    @Override
    public R getResult() {
      return result;
    }

    @Override
    public F getFailure() {
      throw new NoSuchElementException("Failure is not present");
    }
  }

  private static class Failure<R, F> implements Result<R, F> {

    private final F failure;

    Failure(F failure) {
      this.failure = failure;
    }

    @Override
    public boolean successful() {
      return false;
    }

    @Override
    public boolean failed() {
      return true;
    }

    @Override
    public R getResult() {
      throw new NoSuchElementException("Result is not present");
    }

    @Override
    public F getFailure() {
      return failure;
    }
  }
}
