package org.folio.rmapi;

import static java.util.concurrent.CompletableFuture.completedFuture;

import io.vertx.core.Vertx;
import jakarta.validation.ValidationException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import lombok.extern.log4j.Log4j2;
import org.folio.cache.VertxCache;
import org.folio.holdingsiq.model.Configuration;
import org.folio.holdingsiq.model.PackageData;
import org.folio.holdingsiq.model.PackageId;
import org.folio.holdingsiq.model.Resource;
import org.folio.holdingsiq.model.ResourceId;
import org.folio.holdingsiq.model.Resources;
import org.folio.holdingsiq.service.PackagesHoldingsIQService;
import org.folio.holdingsiq.service.exception.ResourceNotFoundException;
import org.folio.holdingsiq.service.impl.ResourcesHoldingsIQServiceImpl;
import org.folio.rest.util.IdParser;
import org.folio.rmapi.cache.ResourceCacheKey;
import org.folio.rmapi.result.ResourceBulkResult;
import org.folio.rmapi.result.ResourceResult;
import org.folio.rmapi.result.VendorResult;
import org.folio.util.FutureUtils;

@Log4j2
public class ResourcesServiceImpl extends ResourcesHoldingsIQServiceImpl {
  private static final String INCLUDE_PROVIDER_VALUE = "provider";
  private static final String INCLUDE_PACKAGE_VALUE = "package";
  private static final String INCLUDE_RELATIONSHIPS_VALUE = "relationships";

  private final ProvidersServiceImpl providerService;
  private final PackagesHoldingsIQService packagesService;
  private final VertxCache<ResourceCacheKey, Resource> resourceCache;
  private final Configuration configuration;
  private final String tenantId;

  public ResourcesServiceImpl(Configuration config, Vertx vertx, String tenantId,
                              ProvidersServiceImpl providerService, PackagesHoldingsIQService packagesService,
                              VertxCache<ResourceCacheKey, Resource> resourceCache) {
    super(config, vertx);
    this.providerService = providerService;
    this.packagesService = packagesService;
    this.configuration = config;
    this.tenantId = tenantId;
    this.resourceCache = resourceCache;
  }

  @Override
  public CompletableFuture<Resource> retrieveResource(ResourceId resourceId) {
    return super.retrieveResource(resourceId).thenApply(resource -> verifyProvider(resource, resourceId));
  }

  public CompletableFuture<ResourceResult> retrieveResource(ResourceId resourceId, List<String> includes) {
    return retrieveResource(resourceId, includes, false);
  }

  public CompletableFuture<ResourceResult> retrieveResource(ResourceId resourceId, List<String> includes,
                                                            boolean useCache) {

    CompletableFuture<Resource> resourceFuture;
    if (includes.contains(INCLUDE_RELATIONSHIPS_VALUE)) {
      resourceFuture = retrieveResourceWithRelationships(resourceId);
    } else {
      resourceFuture = useCache ? retrieveResourceWithCache(resourceId) : retrieveResource(resourceId);
    }
    var vendorFuture = retrieveVendor(resourceId, includes);
    var packageFuture = retrievePackage(resourceId, includes);
    boolean includeTitle = includes.contains("title");

    return CompletableFuture.allOf(resourceFuture, vendorFuture, packageFuture)
      .thenCompose(o ->
        completedFuture(
          new ResourceResult(resourceFuture.join(), vendorFuture.join().getVendor(), packageFuture.join(),
            includeTitle)));
  }

  @Override
  public CompletableFuture<Resource> retrieveResourceWithRelationships(ResourceId resourceId) {
    return super.retrieveResourceWithRelationships(resourceId)
      .thenApply(resource -> verifyProvider(resource, resourceId));
  }

  /**
   * HoldingsIQ v2 addresses a resource by package and title ids alone, so a mismatched provider id would otherwise
   * be ignored.
   */
  private Resource verifyProvider(Resource resource, ResourceId resourceId) {
    var vendorId = resource.getVendorId();
    if (vendorId != null && vendorId != resourceId.providerIdPart()) {
      throw new ResourceNotFoundException(
        "Resource " + resourceId + " not found for provider " + resourceId.providerIdPart(),
        404, "Not Found", null, null);
    }
    return resource;
  }

  public CompletableFuture<Resources> retrieveResources(List<ResourceId> resourceIds) {
    return retrieveResources(resourceIds, Collections.emptyList());
  }

  public CompletableFuture<Resources> retrieveResources(List<ResourceId> resourceIds, List<String> includes) {
    Set<CompletableFuture<ResourceResult>> futures = resourceIds.stream()
      .map(id -> retrieveResource(id, includes, true))
      .collect(Collectors.toSet());

    return FutureUtils.allOfSucceeded(futures, throwable -> log.warn(throwable.getMessage(), throwable))
      .thenApply(results -> Resources.builder().titles(sortByTitleName(results)).build());
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
              failed.add(resourceId.providerIdPart() + "-" + resourceId.packageIdPart() + "-"
                         + resourceId.titleIdPart());
            }
          }))
      .collect(Collectors.toSet());

    return FutureUtils.allOfSucceeded(futures, throwable -> log.warn(throwable.getMessage(), throwable))
      .thenApply(results -> new ResourceBulkResult(sortByTitleName(results), failed));
  }

  private CompletableFuture<PackageData> retrievePackage(ResourceId resourceId,
                                                             List<String> includes) {
    if (includes.contains(INCLUDE_PACKAGE_VALUE)) {
      PackageId id = new PackageId(resourceId.providerIdPart(), resourceId.packageIdPart());
      return packagesService.retrievePackage(id.packageIdPart());
    }
    return completedFuture(null);
  }

  private CompletableFuture<VendorResult> retrieveVendor(ResourceId resourceId,
                                                         List<String> includes) {
    if (includes.contains(INCLUDE_PROVIDER_VALUE)) {
      return providerService.retrieveProvider(resourceId.providerIdPart(), "");
    }
    return completedFuture(new VendorResult(null, null));
  }

  private CompletableFuture<Resource> retrieveResourceWithCache(ResourceId resourceId) {
    ResourceCacheKey cacheKey = ResourceCacheKey.builder()
      .resourceId(resourceId)
      .tenant(tenantId)
      .rmapiConfiguration(configuration)
      .build();
    return resourceCache.getValueOrLoad(cacheKey, () -> retrieveResource(resourceId));
  }

  private List<Resource> sortByTitleName(List<ResourceResult> results) {
    return results.stream()
      .map(ResourceResult::getResource)
      .sorted(Comparator.comparing(Resource::getTitleName))
      .toList();
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
