package org.folio.rmapi;

import static java.util.concurrent.CompletableFuture.completedFuture;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.folio.cache.VertxCache;
import org.folio.holdingsiq.model.Configuration;
import org.folio.holdingsiq.model.PackageByIdData;
import org.folio.holdingsiq.model.PackageId;
import org.folio.holdingsiq.model.ResourceId;
import org.folio.holdingsiq.model.Title;
import org.folio.holdingsiq.service.PackagesHoldingsIQService;
import org.folio.holdingsiq.service.impl.ResourcesHoldingsIQServiceImpl;
import org.folio.rmapi.cache.ResourceCacheKey;
import org.folio.rmapi.result.ResourceResult;
import org.folio.rmapi.result.VendorResult;

import io.vertx.core.Vertx;

public class ResourcesServiceImpl extends ResourcesHoldingsIQServiceImpl {

  private static final String INCLUDE_PROVIDER_VALUE = "provider";
  private static final String INCLUDE_PACKAGE_VALUE = "package";

  private ProvidersServiceImpl providerService;
  private PackagesHoldingsIQService packagesService;
  private VertxCache<ResourceCacheKey, Title> resourceCache;
  private Configuration configuration;
  private String tenantId;

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

  public CompletableFuture<ResourceResult> retrieveResource(ResourceId resourceId, List<String> includes, boolean useCache) {
    CompletableFuture<Title> titleFuture;
    CompletableFuture<PackageByIdData> packageFuture;
    CompletableFuture<VendorResult> vendorFuture;

    if(useCache){
      titleFuture = retrieveResourceWithCache(resourceId, tenantId, configuration);
    }
    else {
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
        completedFuture(new ResourceResult(titleFuture.join(), vendorFuture.join().getVendor(), packageFuture.join(), includeTitle)));
  }

  private CompletableFuture<Title> retrieveResourceWithCache(ResourceId resourceId, String tenantId, Configuration configuration) {
    ResourceCacheKey cacheKey = ResourceCacheKey.builder()
      .resourceId(resourceId)
      .tenant(tenantId)
      .rmapiConfiguration(configuration)
      .build();
    return resourceCache.getValueOrLoad(cacheKey, () -> retrieveResource(resourceId));
  }
}
