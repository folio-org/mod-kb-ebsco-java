package org.folio.rmapi;

import static java.util.concurrent.CompletableFuture.completedFuture;

import java.util.concurrent.CompletableFuture;

import io.vertx.core.Vertx;

import org.folio.cache.VertxCache;
import org.folio.holdingsiq.model.Configuration;
import org.folio.holdingsiq.model.Packages;
import org.folio.holdingsiq.model.VendorById;
import org.folio.holdingsiq.service.HoldingsIQService;
import org.folio.holdingsiq.service.PackagesHoldingsIQService;
import org.folio.holdingsiq.service.impl.ProviderHoldingsIQServiceImpl;
import org.folio.rmapi.cache.VendorCacheKey;
import org.folio.rmapi.result.VendorResult;

public class ProvidersServiceImpl extends ProviderHoldingsIQServiceImpl {

  private static final String INCLUDE_PACKAGES_VALUE = "packages";

  private PackagesHoldingsIQService packagesService;
  private VertxCache<VendorCacheKey, VendorById> vendorCache;
  private Configuration configuration;
  private String tenantId;

  public ProvidersServiceImpl(Configuration config, Vertx vertx, String tenantId, HoldingsIQService holdingsService, VertxCache<VendorCacheKey, VendorById> vendorCache ) {
    super(config, vertx, holdingsService);
    this.configuration = config;
    this.tenantId = tenantId;
    this.vendorCache = vendorCache;
  }

  public void setPackagesService(PackagesHoldingsIQService packagesService) {
    this.packagesService = packagesService;
  }

  public CompletableFuture<VendorResult> retrieveProvider(long id, String include) {
    return retrieveProvider(id, include, false);
  }

  public CompletableFuture<VendorResult> retrieveProvider(long id, String include, boolean useCache) {

    CompletableFuture<VendorById> vendorFuture;
    CompletableFuture<Packages> packagesFuture;
    if (useCache) {
      vendorFuture = retrieveProviderWithCache(id);
    } else {
      vendorFuture = super.retrieveProvider(id);
    }
    if (INCLUDE_PACKAGES_VALUE.equalsIgnoreCase(include)) {
      packagesFuture = packagesService.retrievePackages(id);
    } else {
      packagesFuture = completedFuture(null);
    }
    return CompletableFuture.allOf(vendorFuture, packagesFuture)
      .thenCompose(o ->
        completedFuture(new VendorResult(vendorFuture.join(), packagesFuture.join())));
  }

  private CompletableFuture<VendorById> retrieveProviderWithCache(long id) {
    VendorCacheKey cacheKey = VendorCacheKey.builder()
      .vendorId(String.valueOf(id))
      .rmapiConfiguration(configuration)
      .tenant(tenantId)
      .build();
    VendorById cachedVendor = vendorCache.getValue(cacheKey);
    if (cachedVendor != null) {
      return CompletableFuture.completedFuture(cachedVendor);
    } else {
      return retrieveProvider(id)
        .thenCompose(vendorById -> {
          vendorCache.putValue(cacheKey, vendorById);
          return CompletableFuture.completedFuture(vendorById);
        });
    }
  }

}
