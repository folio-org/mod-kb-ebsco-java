package org.folio.service.packages;

import static java.util.concurrent.CompletableFuture.completedFuture;

import java.util.concurrent.CompletableFuture;
import org.folio.cache.VertxCache;
import org.folio.config.cache.VendorIdCacheKey;
import org.folio.rest.util.template.RmApiTemplateContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Resolves (and caches) the vendor id used to identify custom packages, hiding the
 * {@link VertxCache} wiring from {@link PackageService}.
 */
@Service
public class CustomProviderIdService {

  private final VertxCache<VendorIdCacheKey, Integer> vendorIdCache;

  public CustomProviderIdService(@Qualifier("vendorIdCache") VertxCache<VendorIdCacheKey, Integer> vendorIdCache) {
    this.vendorIdCache = vendorIdCache;
  }

  public CompletableFuture<Integer> getCustomProviderId(RmApiTemplateContext context) {
    var cacheKey = VendorIdCacheKey.builder()
      .tenant(context.getRequestContext().getTenant())
      .rmapiConfiguration(context.getConfiguration())
      .build();
    var cachedId = vendorIdCache.getValue(cacheKey);
    if (cachedId != null) {
      return completedFuture(cachedId);
    }
    return context.getProvidersService().getVendorId()
      .thenCompose(id -> {
        vendorIdCache.putValue(cacheKey, id);
        return completedFuture(id);
      });
  }
}
