package org.folio.rmapi;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import io.vertx.core.Vertx;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.folio.cache.VertxCache;
import org.folio.holdingsiq.model.Configuration;
import org.folio.holdingsiq.model.CustomerResources;
import org.folio.holdingsiq.model.Title;
import org.folio.holdingsiq.model.Titles;
import org.folio.holdingsiq.service.impl.TitlesHoldingsIQServiceImpl;
import org.folio.rmapi.cache.TitleCacheKey;
import org.folio.util.FutureUtils;

public class TitlesServiceImpl extends TitlesHoldingsIQServiceImpl {

  private static final Logger LOG = LogManager.getLogger(TitlesServiceImpl.class);

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

  private CompletableFuture<Title> retrieveTitleWithCache(long titleId) {
    var cacheKey = buildTitleCacheKey(titleId);
    return titleCache.getValueOrLoad(cacheKey, () -> super.retrieveTitle(titleId));
  }

  public CompletableFuture<Titles> retrieveTitles(List<Long> titleIds) {
    Set<CompletableFuture<Title>> futures = titleIds.stream()
      .map(id -> retrieveTitle(id, true))
      .collect(Collectors.toSet());
    return FutureUtils.allOfSucceeded(futures, throwable -> LOG.warn(throwable.getMessage(), throwable))
      .thenApply(this::mapToTitles);
  }

  public void updateCache(Title title) {
    var cacheKey = buildTitleCacheKey(title.getTitleId());
    mergeCustomerResources(cacheKey, title);
    titleCache.putValue(cacheKey, title);
  }

  private void mergeCustomerResources(TitleCacheKey cacheKey, Title title) {
    List<CustomerResources> updatedCustomerResources = title.getCustomerResourcesList();
    List<CustomerResources> customerResources = titleCache.getValue(cacheKey).getCustomerResourcesList();

    if (!updatedCustomerResources.isEmpty() && !Objects.isNull(customerResources)) {
      var updatedCustomerResourceId = updatedCustomerResources.get(0).getPackageId();
      var oldCustomerResources = customerResources.stream()
        .filter(resource -> !Objects.equals(resource.getPackageId(), updatedCustomerResourceId))
        .collect(Collectors.toList());
      updatedCustomerResources.addAll(oldCustomerResources);
    }
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

  private TitleCacheKey buildTitleCacheKey(long titleId) {
    return TitleCacheKey.builder()
      .titleId(titleId)
      .rmapiConfiguration(configuration)
      .tenant(tenantId)
      .build();
  }
}
