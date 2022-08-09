package org.folio.rmapi;

import static java.util.concurrent.CompletableFuture.completedFuture;

import io.vertx.core.Vertx;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import javax.validation.ValidationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.cache.VertxCache;
import org.folio.holdingsiq.model.Configuration;
import org.folio.holdingsiq.model.PackageByIdData;
import org.folio.holdingsiq.model.PackageId;
import org.folio.holdingsiq.model.ResourceId;
import org.folio.holdingsiq.model.Title;
import org.folio.holdingsiq.model.Titles;
import org.folio.holdingsiq.service.PackagesHoldingsIQService;
import org.folio.holdingsiq.service.impl.ResourcesHoldingsIQServiceImpl;
import org.folio.rest.util.IdParser;
import org.folio.rmapi.cache.ResourceCacheKey;
import org.folio.rmapi.result.ResourceBulkResult;
import org.folio.rmapi.result.ResourceResult;
import org.folio.rmapi.result.VendorResult;
import org.folio.util.FutureUtils;

public class ResourcesServiceImpl extends ResourcesHoldingsIQServiceImpl {

  private static final Logger LOG = LogManager.getLogger(ResourcesServiceImpl.class);
  private static final String INCLUDE_PROVIDER_VALUE = "provider";
  private static final String INCLUDE_PACKAGE_VALUE = "package";

  private final ProvidersServiceImpl providerService;
  private final PackagesHoldingsIQService packagesService;
  private final VertxCache<ResourceCacheKey, Title> resourceCache;
  private final Configuration configuration;
  private final String tenantId;

  public ResourcesServiceImpl(Configuration config, Vertx vertx, String tenantId,
                              ProvidersServiceImpl providerService, PackagesHoldingsIQService packagesService,
                              VertxCache<ResourceCacheKey, Title> resourceCache) {
    super(config, vertx);
    this.providerService = providerService;
    this.packagesService = packagesService;
    this.configuration = config;
    this.tenantId = tenantId;
    this.resourceCache = resourceCache;
  }

  public CompletableFuture<ResourceResult> retrieveResource(ResourceId resourceId, List<String> includes) {
    return retrieveResource(resourceId, includes, false);
  }

  public CompletableFuture<ResourceResult> retrieveResource(ResourceId resourceId, List<String> includes,
                                                            boolean useCache) {
    CompletableFuture<Title> titleFuture;
    CompletableFuture<PackageByIdData> packageFuture;
    CompletableFuture<VendorResult> vendorFuture;

    if (useCache) {
      titleFuture = retrieveResourceWithCache(resourceId, tenantId, configuration);
    } else {
      titleFuture = super.retrieveResource(resourceId);
    }
    if (includes.contains(INCLUDE_PROVIDER_VALUE)) {
      vendorFuture = providerService.retrieveProvider(resourceId.getProviderIdPart(), "");
    } else {
      vendorFuture = completedFuture(new VendorResult(null, null));
    }
    if (includes.contains(INCLUDE_PACKAGE_VALUE)) {
      PackageId id = PackageId.builder()
        .providerIdPart(resourceId.getProviderIdPart())
        .packageIdPart(resourceId.getPackageIdPart()).build();
      packageFuture = packagesService.retrievePackage(id);
    } else {
      packageFuture = completedFuture(null);
    }
    boolean includeTitle = includes.contains("title");

    return CompletableFuture.allOf(titleFuture, vendorFuture, packageFuture)
      .thenCompose(o ->
        completedFuture(
          new ResourceResult(titleFuture.join(), vendorFuture.join().getVendor(), packageFuture.join(), includeTitle)));
  }

  public CompletableFuture<Titles> retrieveResources(List<ResourceId> resourceIds) {
    return retrieveResources(resourceIds, Collections.emptyList());
  }

  public CompletableFuture<Titles> retrieveResources(List<ResourceId> resourceIds, List<String> includes) {
    Set<CompletableFuture<ResourceResult>> futures = resourceIds.stream()
      .map(id -> retrieveResource(id, includes, true))
      .collect(Collectors.toSet());

    return FutureUtils.allOfSucceeded(futures, throwable -> LOG.warn(throwable.getMessage(), throwable))
      .thenApply(this::mapToResources);
  }

  public CompletableFuture<ResourceBulkResult> retrieveResourcesBulk(Set<String> resourceBulk) {
    List<String> failed = new ArrayList<>();
    Set<CompletableFuture<ResourceResult>> futures = resourceBulk.stream()
      .map(id -> parseToResourceId(id, failed))
      .filter(Objects::nonNull)
      .map(resourceId ->
        retrieveResource(resourceId, Collections.emptyList(), true)
          .whenComplete((result, throwable) -> {
            if (throwable != null) {
              failed.add(resourceId.getProviderIdPart() + "-" + resourceId.getPackageIdPart() + "-"
                + resourceId.getTitleIdPart());
            }
          }))
      .collect(Collectors.toSet());

    return FutureUtils.allOfSucceeded(futures, throwable -> LOG.warn(throwable.getMessage(), throwable))
      .thenApply(resourceFutures -> mapToResources(resourceFutures, failed));
  }

  private CompletableFuture<Title> retrieveResourceWithCache(ResourceId resourceId, String tenantId,
                                                             Configuration configuration) {
    ResourceCacheKey cacheKey = ResourceCacheKey.builder()
      .resourceId(resourceId)
      .tenant(tenantId)
      .rmapiConfiguration(configuration)
      .build();
    return resourceCache.getValueOrLoad(cacheKey, () -> retrieveResource(resourceId));
  }

  private ResourceBulkResult mapToResources(List<ResourceResult> resourceFutures, List<String> failed) {
    Titles titlesList = mapToResources(resourceFutures);
    return new ResourceBulkResult(titlesList, failed);
  }

  private Titles mapToResources(List<ResourceResult> resourceFutures) {
    List<Title> titlesList = resourceFutures.stream()
      .map(ResourceResult::getTitle)
      .sorted(Comparator.comparing(Title::getTitleName))
      .collect(Collectors.toList());
    return Titles.builder()
      .titleList(titlesList)
      .build();
  }

  private ResourceId parseToResourceId(String resourceId, List<String> failed) {
    try {
      return IdParser.parseResourceId(resourceId);
    } catch (ValidationException exception) {
      failed.add(resourceId);
    }
    return null;
  }
}
