package org.folio.rmapi;

import static java.util.concurrent.CompletableFuture.completedFuture;

import static org.folio.common.FutureUtils.allOfSucceeded;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

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
import org.folio.rmapi.cache.PackageCacheKey;
import org.folio.rmapi.result.PackageResult;
import org.folio.rmapi.result.VendorResult;

public class PackageServiceImpl extends PackagesHoldingsIQServiceImpl {

  private static final String INCLUDE_PROVIDER_VALUE = "provider";
  private static final String INCLUDE_RESOURCES_VALUE = "resources";
  private static final Logger LOG = LoggerFactory.getLogger(PackageServiceImpl.class);

  private ProvidersServiceImpl providerService;
  private TitlesHoldingsIQService titlesService;
  private VertxCache<PackageCacheKey, PackageByIdData> packageCache;
  private Configuration configuration;
  private String tenantId;

  public PackageServiceImpl(Configuration config, Vertx vertx, String tenantId, ProvidersServiceImpl providerService,
                            TitlesHoldingsIQService titlesService, VertxCache<PackageCacheKey, PackageByIdData> packageCache) {
    super(config, vertx);
    this.providerService = providerService;
    this.titlesService = titlesService;
    this.packageCache = packageCache;
    this.tenantId = tenantId;
    this.configuration = config;
  }

  public CompletableFuture<PackageResult> retrievePackage(PackageId packageId, List<String> includedObjects) {
    return retrievePackage(packageId, includedObjects, false);
  }

  public CompletableFuture<Packages> retrievePackages(List<PackageId> packageIds) {
    Set<CompletableFuture<PackageResult>> futures = packageIds.stream()
      .map(id -> retrievePackage(id, Collections.emptyList(), true))
      .collect(Collectors.toSet());
    return allOfSucceeded(futures, throwable -> LOG.warn(throwable.getMessage(), throwable))
      .thenApply(this::mapToPackages);
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

  public CompletableFuture<PackageResult> retrievePackage(PackageId packageId, List<String> includedObjects, boolean useCache) {
    CompletableFuture<PackageByIdData> packageFuture;
    if(useCache){
      packageFuture = retrievePackageWithCache(packageId);
    }else{
      packageFuture = retrievePackage(packageId);
    }

    CompletableFuture<Titles> titlesFuture;
    if (includedObjects.contains(INCLUDE_RESOURCES_VALUE)) {
      titlesFuture = titlesService.retrieveTitles(packageId.getProviderIdPart(), packageId.getPackageIdPart(), FilterQuery.builder().build(),
        Sort.NAME, 1, 25);
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

  private CompletableFuture<PackageByIdData> retrievePackageWithCache(PackageId packageId) {
    PackageCacheKey cacheKey = PackageCacheKey.builder()
      .packageId(packageId.getProviderIdPart() + "-" + packageId.getPackageIdPart())
      .rmapiConfiguration(configuration)
      .tenant(tenantId)
      .build();
    return packageCache.getValueOrLoad(cacheKey, () -> retrievePackage(packageId));
  }
}
