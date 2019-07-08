package org.folio.service.holdings;

import static org.folio.common.ListUtils.mapItems;
import static org.folio.repository.holdings.LoadStatus.COMPLETED;
import static org.folio.repository.holdings.LoadStatus.IN_PROGRESS;
import static org.folio.repository.holdings.status.HoldingsLoadingStatusFactory.getStatusCompleted;
import static org.folio.repository.holdings.status.HoldingsLoadingStatusFactory.getStatusLoadingHoldings;
import static org.folio.repository.holdings.status.HoldingsLoadingStatusFactory.getStatusPopulatingStagingArea;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import org.folio.holdingsiq.model.Holding;
import org.folio.holdingsiq.model.HoldingsLoadStatus;
import org.folio.repository.holdings.HoldingInfoInDB;
import org.folio.repository.holdings.HoldingsRepository;
import org.folio.repository.holdings.LoadStatus;
import org.folio.repository.holdings.status.HoldingsStatusRepository;
import org.folio.repository.resources.ResourceInfoInDB;
import org.folio.rest.jaxrs.model.HoldingsLoadingStatus;
import org.folio.rest.util.template.RMAPITemplateContext;

@Component
public class HoldingsServiceImpl implements HoldingsService {

  private static final int MAX_COUNT = 5000;
  private static final Logger logger = LoggerFactory.getLogger(HoldingsServiceImpl.class);
  private Vertx vertx;
  private long delay;
  private int retryCount;
  private HoldingsRepository holdingsRepository;
  private HoldingsStatusRepository holdingsStatusRepository;

  @Autowired
  public HoldingsServiceImpl(Vertx vertx, HoldingsRepository holdingsRepository,
                             HoldingsStatusRepository holdingsStatusRepository,
                             @Value("${holdings.status.check.delay}") long delay,
                             @Value("${holdings.status.retry.count}") int retryCount) {
    this.vertx = vertx;
    this.holdingsRepository = holdingsRepository;
    this.holdingsStatusRepository = holdingsStatusRepository;
    this.delay = delay;
    this.retryCount = retryCount;
  }

  public CompletableFuture<Void> loadHoldings(RMAPITemplateContext context) {
    return populateHoldings(context)
      .thenCompose(isSuccessful -> waitForCompleteStatus(context, retryCount))
      .thenCompose(loadStatus -> loadHoldings(context, loadStatus.getTotalCount()));
  }

  private CompletableFuture<Void> updateStatus(HoldingsLoadingStatus status, String tenantId) {
    return holdingsStatusRepository.update(status, tenantId);
  }

  @Override
  public CompletableFuture<List<HoldingInfoInDB>> getHoldingsByIds(List<ResourceInfoInDB> resourcesResult, String tenant) {
    return holdingsRepository.findAllById(getTitleIdsAsList(resourcesResult), tenant);
  }

  private List<String> getTitleIdsAsList(List<ResourceInfoInDB> resources){
    return mapItems(resources, dbResource -> dbResource.getId().getProviderIdPart() + "-"
      + dbResource.getId().getPackageIdPart() + "-" + dbResource.getId().getTitleIdPart());
  }

  private CompletableFuture<Void> populateHoldings(RMAPITemplateContext context) {
    return getLoadingStatus(context).thenCompose(loadStatus -> {
      final LoadStatus other = LoadStatus.fromValue(loadStatus.getStatus());
      if (IN_PROGRESS.equals(other)) {
        return CompletableFuture.completedFuture(null);
      } else {
        logger.info("Start populating holdings to stage environment.");
        return context.getLoadingService().populateHoldings();
      }
    }).thenAccept(o -> updateStatus(getStatusPopulatingStagingArea(), context.getOkapiData().getTenant()));
  }

  public CompletableFuture<HoldingsLoadStatus> waitForCompleteStatus(RMAPITemplateContext context,  int retryCount) {
    CompletableFuture<HoldingsLoadStatus> future = new CompletableFuture<>();
    waitForCompleteStatus(context, retryCount, future);
    return future;
  }

  public void waitForCompleteStatus(RMAPITemplateContext context, int retries, CompletableFuture<HoldingsLoadStatus> future) {
    vertx.setTimer(delay, timerId -> getLoadingStatus(context)
      .thenAccept(loadStatus -> {
        final LoadStatus status = LoadStatus.fromValue(loadStatus.getStatus());
        logger.info("Getting status of stage snapshot: {}.", status);
        if (COMPLETED.equals(status)) {
          future.complete(loadStatus);
        } else if (IN_PROGRESS.equals(status)) {
          if (retries <= 0) {
            throw new IllegalStateException("Failed to get status with status response:" + loadStatus);
          }
          waitForCompleteStatus(context, retries - 1, future);
        } else {
          future.completeExceptionally(new IllegalStateException("Failed to get status with status response:" + loadStatus));
        }
      }).exceptionally(throwable -> {
        future.completeExceptionally(throwable);
        return null;
      }));
  }

  public CompletableFuture<Void> loadHoldings(RMAPITemplateContext context, Integer totalCount) {
    final String tenantId = context.getOkapiData().getTenant();
    CompletableFuture<Void> future = CompletableFuture.completedFuture(null);
    final Instant updatedAt = Instant.now();
    for (int iteration = 1; iteration < getRequestCount(totalCount) + 1; iteration++) {
      int finalIteration = iteration;
      future = future
        .thenCompose(o -> context.getLoadingService().loadHoldings(MAX_COUNT, finalIteration))
        .thenCompose(holding -> saveHoldings(holding.getHoldingsList(), updatedAt, tenantId))
        .thenCompose(o -> updateStatus(getStatusLoadingHoldings(totalCount,
          calculateImportedCount(finalIteration, totalCount)), tenantId));
    }
    future = future
      .thenCompose(o -> holdingsRepository.deleteByTimeStamp(updatedAt, tenantId))
      .thenCompose(o -> updateStatus(getStatusCompleted(totalCount), tenantId));
    return future;
  }

  private int calculateImportedCount(int iteration, int totalCount) {
    final int amount = iteration * MAX_COUNT;
    return amount >= totalCount ? totalCount : amount;
  }

  /**
   * Defines an amount of request needed to load all holdings from the staged area
   *
   * @param totalCount - total records count
   * @return number of requests
   *
   */
  private int getRequestCount(Integer totalCount) {
    final int quotient = totalCount / MAX_COUNT;
    final int remainder = totalCount % MAX_COUNT;
    return remainder == 0 ? quotient : quotient + 1;
  }

  private CompletableFuture<HoldingsLoadStatus> getLoadingStatus(RMAPITemplateContext context) {
    return context.getLoadingService().getLoadingStatus();
  }

  private CompletableFuture<Void> saveHoldings(List<Holding> holdings, Instant updatedAt, String tenantId) {
    Set<HoldingInfoInDB> dbHoldings = holdings.stream()
      .filter(distinctByKey(this::getHoldingsId))
      .map(holding -> new HoldingInfoInDB(
        holding.getTitleId(),
        holding.getPackageId(),
        holding.getVendorId(),
        holding.getPublicationTitle(),
        holding.getPublisherName(),
        holding.getResourceType()
      ))
      .collect(Collectors.toSet());
    logger.info("Saving holdings to database.");
    return holdingsRepository.saveAll(dbHoldings, updatedAt, tenantId);
  }

  private  <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
    Set<Object> seen = ConcurrentHashMap.newKeySet();
    return t -> seen.add(keyExtractor.apply(t));
  }

  private String getHoldingsId(Holding holding) {
    return holding.getVendorId() + "-" + holding.getPackageId() + "-" + holding.getTitleId();
  }
}
