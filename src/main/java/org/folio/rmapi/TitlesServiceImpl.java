package org.folio.rmapi;

import io.vertx.core.Vertx;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import lombok.extern.log4j.Log4j2;
import org.folio.cache.VertxCache;
import org.folio.holdingsiq.model.Configuration;
import org.folio.holdingsiq.model.Title;
import org.folio.holdingsiq.model.Titles;
import org.folio.holdingsiq.service.impl.TitlesHoldingsIQServiceImpl;
import org.folio.rmapi.cache.TitleCacheKey;
import org.folio.util.FutureUtils;

@Log4j2
public class TitlesServiceImpl extends TitlesHoldingsIQServiceImpl {
  private final VertxCache<TitleCacheKey, Title> titleCache;
  private final Configuration configuration;
  private final String tenantId;

  public TitlesServiceImpl(Configuration config, Vertx vertx,
                           String tenantId, VertxCache<TitleCacheKey, Title> titleCache) {
    super(config, vertx);

    this.configuration = config;
    this.tenantId = tenantId;
    this.titleCache = titleCache;
  }

  @Override
  public CompletableFuture<Title> retrieveTitle(long titleId) {
    var titleFuture = super.retrieveTitle(titleId);
    var cacheKey = buildTitleCacheKey(titleId);
    titleFuture.thenAccept(title -> titleCache.putValue(cacheKey, title));
    return titleFuture;
  }

  public CompletableFuture<Title> retrieveTitle(long titleId, boolean useCache) {
    CompletableFuture<Title> titleFuture;
    if (useCache) {
      titleFuture = retrieveTitleWithCache(titleId);
    } else {
      titleFuture = super.retrieveTitle(titleId);
    }
    return titleFuture;
  }

  public CompletableFuture<Titles> retrieveTitles(List<Long> titleIds) {
    Set<CompletableFuture<Title>> futures = titleIds.stream()
      .map(id -> retrieveTitle(id, true))
      .collect(Collectors.toSet());
    return FutureUtils.allOfSucceeded(futures, throwable -> log.warn(throwable.getMessage(), throwable))
      .thenApply(this::mapToTitles);
  }

  public void updateCache(Title title) {
    var cacheKey = buildTitleCacheKey(title.getTitleId());
    Title cachedTitle = titleCache.getValue(cacheKey);
    if (!Objects.isNull(cachedTitle)) {
      mergeCustomerResources(cachedTitle, title);
    }
    titleCache.putValue(cacheKey, title);
  }

  private CompletableFuture<Title> retrieveTitleWithCache(long titleId) {
    var cacheKey = buildTitleCacheKey(titleId);
    return titleCache.getValueOrLoad(cacheKey, () -> super.retrieveTitle(titleId));
  }

  private void mergeCustomerResources(Title cachedTitle, Title title) {
    var updatedCustomerResources = title.getCustomerResourcesList();
    var customerResources = cachedTitle.getCustomerResourcesList();

    if (!updatedCustomerResources.isEmpty()) {
      var updatedCustomerResourceId = updatedCustomerResources.getFirst().getPackageId();
      var oldCustomerResources = customerResources.stream()
        .filter(resource -> !Objects.equals(resource.getPackageId(), updatedCustomerResourceId))
        .toList();
      updatedCustomerResources.addAll(oldCustomerResources);
    }
  }

  private Titles mapToTitles(List<Title> titles) {
    return Titles.builder()
      .titleList(
        titles.stream()
          .sorted(Comparator.comparing(Title::getTitleName))
          .toList()
      )
      .build();
  }

  private TitleCacheKey buildTitleCacheKey(long titleId) {
    return TitleCacheKey.builder()
      .titleId(titleId)
      .rmapiConfiguration(configuration)
      .tenant(tenantId)
      .build();
  }
}
