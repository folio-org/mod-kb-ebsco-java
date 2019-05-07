package org.folio.rmapi;

import java.util.concurrent.CompletableFuture;

import io.vertx.core.Vertx;

import org.folio.cache.VertxCache;
import org.folio.holdingsiq.model.Configuration;
import org.folio.holdingsiq.model.Title;
import org.folio.holdingsiq.service.impl.TitlesHoldingsIQServiceImpl;
import org.folio.rmapi.cache.TitleCacheKey;

public class TitlesServiceImpl extends TitlesHoldingsIQServiceImpl {

  private VertxCache<TitleCacheKey, Title> titleCache;
  private Configuration configuration;
  private String tenantId;

  public TitlesServiceImpl(Configuration config, Vertx vertx,
                           String tenantId, VertxCache<TitleCacheKey, Title> titleCache ) {
    super(config, vertx);

    this.configuration = config;
    this.tenantId = tenantId;
    this.titleCache = titleCache;
  }

   public CompletableFuture<Title> retrieveTitle(long titleId, boolean useCache) {
    CompletableFuture<Title> titleFuture;
    if(useCache){
      titleFuture = retrieveTitleWithCache(titleId);
    }else{
      titleFuture = super.retrieveTitle(titleId);
    }
    return titleFuture;
  }

  private CompletableFuture<Title> retrieveTitleWithCache(long titleId) {
    TitleCacheKey cacheKey = TitleCacheKey.builder()
      .titleId(titleId)
      .rmapiConfiguration(configuration)
      .tenant(tenantId)
      .build();
    Title cachedTitle = titleCache.getValue(cacheKey);
    if (cachedTitle != null) {
      return CompletableFuture.completedFuture(cachedTitle);
    } else {
      return retrieveTitle(titleId)
        .thenCompose(title -> {
          titleCache.putValue(cacheKey, title);
          return CompletableFuture.completedFuture(title);
        });
    }
  }
}
