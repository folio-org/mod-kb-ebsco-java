package org.folio.rmapi;

import static java.util.concurrent.CompletableFuture.completedFuture;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import io.vertx.core.Vertx;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.folio.cache.VertxCache;
import org.folio.holdingsiq.model.Configuration;
import org.folio.holdingsiq.model.Packages;
import org.folio.holdingsiq.model.Vendor;
import org.folio.holdingsiq.model.VendorById;
import org.folio.holdingsiq.model.Vendors;
import org.folio.holdingsiq.service.HoldingsIQService;
import org.folio.holdingsiq.service.PackagesHoldingsIQService;
import org.folio.holdingsiq.service.impl.ProviderHoldingsIQServiceImpl;
import org.folio.rmapi.cache.VendorCacheKey;
import org.folio.rmapi.result.VendorResult;
import org.folio.util.FutureUtils;

public class ProvidersServiceImpl extends ProviderHoldingsIQServiceImpl {

  private static final String INCLUDE_PACKAGES_VALUE = "packages";
  private static final Logger LOG = LogManager.getLogger(ProvidersServiceImpl.class);


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

  public CompletableFuture<Vendors> retrieveProviders(List<Long> providerIds) {
    Set<CompletableFuture<VendorResult>> futures = providerIds.stream()
      .map(id -> retrieveProvider(id, "", true))
      .collect(Collectors.toSet());
    return FutureUtils.allOfSucceeded(futures, throwable -> LOG.warn(throwable.getMessage(), throwable))
    .thenApply(this::mapToProviders);
  }

  private Vendors mapToProviders(List<VendorResult> results) {
    List<Vendor> providers = results.stream()
      .map(VendorResult::getVendor)
      .sorted(Comparator.comparing(Vendor::getVendorName))
      .collect(Collectors.toList());
    return Vendors.builder()
      .vendorList(providers)
      .build();
  }

  private CompletableFuture<VendorById> retrieveProviderWithCache(long id) {
    VendorCacheKey cacheKey = VendorCacheKey.builder()
      .vendorId(String.valueOf(id))
      .rmapiConfiguration(configuration)
      .tenant(tenantId)
      .build();
    return vendorCache.getValueOrLoad(cacheKey, () -> retrieveProvider(id));
  }

}
