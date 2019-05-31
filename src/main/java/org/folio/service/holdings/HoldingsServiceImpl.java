package org.folio.service.holdings;

import static org.folio.repository.holdings.LoadStatus.COMPLETED;
import static org.folio.repository.holdings.LoadStatus.IN_PROGRESS;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import org.folio.holdingsiq.model.Holding;
import org.folio.holdingsiq.model.HoldingsLoadStatus;
import org.folio.repository.holdings.DbHolding;
import org.folio.repository.holdings.HoldingsRepository;
import org.folio.repository.holdings.LoadStatus;
import org.folio.rest.util.template.RMAPITemplateContext;

@Component
public class HoldingsServiceImpl implements HoldingsService {

  private static final int MAX_COUNT = 5000;
  private static final Logger logger = LoggerFactory.getLogger(HoldingsServiceImpl.class);
  private Vertx vertx;
  private long delay;
  private int retryCount;
  private HoldingsRepository holdingsRepository;

  @Autowired
  public HoldingsServiceImpl(Vertx vertx, HoldingsRepository holdingsRepository,
                             @Value("${holdings.status.check.delay}") long delay,
                             @Value("${holdings.status.retry.count}") int retryCount) {
    this.vertx = vertx;
    this.holdingsRepository = holdingsRepository;
    this.delay = delay;
    this.retryCount = retryCount;
  }

  public CompletableFuture<Void> loadHoldings(RMAPITemplateContext context, String tenantId) {
    return populateHoldings(context)
      .thenCompose(isSuccessful -> waitForCompleteStatus(context, retryCount))
      .thenCompose(loadStatus -> holdingsRepository.removeHoldings(tenantId)
        .thenCompose(o -> loadHoldings(context, loadStatus.getTotalCount(), tenantId))
      );
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
    });
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

  public CompletableFuture<Void> loadHoldings(RMAPITemplateContext context, Integer totalCount, String tenantId) {

    CompletableFuture<Void> future = CompletableFuture.completedFuture(null);
    final int totalRequestCount = getRequestCount(totalCount);
    for (int iteration = 1; iteration < totalRequestCount + 1; iteration++) {
      int finalIteration = iteration;
      future = future
        .thenCompose(o -> context.getLoadingService().loadHoldings(MAX_COUNT, finalIteration))
        .thenCompose(holding -> saveHolding(holding.getHoldingsList(), tenantId));
    }
    return future;
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

  private CompletableFuture<Void> saveHolding(List<Holding> holdings, String tenantId) {
    List<DbHolding> dbHoldings = holdings.stream()
      .map(holding -> new DbHolding(
        holding.getTitleId(),
        holding.getPackageId(),
        holding.getVendorId(),
        holding.getPublicationTitle(),
        holding.getPublisherName(),
        holding.getResourceType()
      ))
      .collect(Collectors.toList());
    logger.info("Saving holdings to database.");
    return holdingsRepository.saveHoldings(dbHoldings, tenantId);
  }
}
