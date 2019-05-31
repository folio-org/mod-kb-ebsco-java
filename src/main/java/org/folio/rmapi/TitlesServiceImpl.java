package org.folio.rmapi;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.folio.cache.VertxCache;
import org.folio.common.FutureUtils;
import org.folio.holdingsiq.model.Configuration;
import org.folio.holdingsiq.model.Title;
import org.folio.holdingsiq.model.Titles;
import org.folio.holdingsiq.service.impl.TitlesHoldingsIQServiceImpl;
import org.folio.rmapi.cache.TitleCacheKey;

import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class TitlesServiceImpl extends TitlesHoldingsIQServiceImpl {

  private static final Logger LOG = LoggerFactory.getLogger(TitlesServiceImpl.class);

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
    return titleCache.getValueOrLoad(cacheKey, () -> retrieveTitle(titleId));
  }

  public CompletableFuture<Titles> retrieveTitles(List<Long> titleIds) {
    Set<CompletableFuture<Title>> futures = titleIds.stream()
      .map(id -> retrieveTitle(id,true))
      .collect(Collectors.toSet());
    return FutureUtils.allOfSucceeded(futures, throwable -> LOG.warn(throwable.getMessage(), throwable))
      .thenApply(this::mapToTitles);
  }

  private Titles mapToTitles(List<Title> titles) {
    return Titles.builder()
      .titleList(
        titles.stream()
        .sorted(Comparator.comparing(Title::getTitleName))
        .collect(Collectors.toList())
      )
      .build();
  }
}
