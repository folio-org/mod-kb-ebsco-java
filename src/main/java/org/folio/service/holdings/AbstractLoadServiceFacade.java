package org.folio.service.holdings;

import static org.folio.repository.holdings.LoadStatus.COMPLETED;
import static org.folio.repository.holdings.LoadStatus.IN_PROGRESS;
import static org.folio.service.holdings.HoldingConstants.HOLDINGS_SERVICE_ADDRESS;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiPredicate;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.internal.util.Producer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import org.folio.holdingsiq.service.LoadService;
import org.folio.holdingsiq.service.impl.LoadServiceImpl;
import org.folio.repository.holdings.LoadStatus;
import org.folio.service.holdings.message.LoadFailedMessage;
import org.folio.service.holdings.message.LoadHoldingsMessage;
import org.folio.service.holdings.message.SnapshotCreatedMessage;
import org.folio.service.holdings.message.SnapshotFailedMessage;

@Component
public abstract class AbstractLoadServiceFacade implements LoadServiceFacade {
  public static final DateTimeFormatter HOLDINGS_STATUS_TIME_FORMATTER = new DateTimeFormatterBuilder()
    .parseCaseInsensitive()
    .append(DateTimeFormatter.ISO_LOCAL_DATE)
    .appendLiteral(' ')
    .append(DateTimeFormatter.ISO_LOCAL_TIME)
    .toFormatter();
  protected static final int MAX_COUNT = 5000;
  private static final Logger logger = LoggerFactory.getLogger(AbstractLoadServiceFacade.class);
  protected final HoldingsService holdingsService;
  protected final int loadPageRetries;
  protected final int loadPageDelay;
  private long statusRetryDelay;
  private int statusRetryCount;
  private int snapshotRefreshPeriod;
  private Vertx vertx;

  public AbstractLoadServiceFacade(@Value("${holdings.status.check.delay}") long statusRetryDelay,
                                   @Value("${holdings.status.retry.count}") int statusRetryCount,
                                   @Value("${holdings.page.retry.delay}") int loadPageRetryDelay,
                                   @Value("${holdings.snapshot.refresh.period}") int snapshotRefreshPeriod,
                                   @Value("${holdings.page.retry.count}") int loadPageRetryCount,
                                   Vertx vertx) {
    this.loadPageDelay = loadPageRetryDelay;
    this.loadPageRetries = loadPageRetryCount;
    this.statusRetryDelay = statusRetryDelay;
    this.statusRetryCount = statusRetryCount;
    this.snapshotRefreshPeriod = snapshotRefreshPeriod;
    this.vertx = vertx;
    this.holdingsService = HoldingsService.createProxy(vertx, HOLDINGS_SERVICE_ADDRESS);
  }

  @Override
  public void createSnapshot(ConfigurationMessage message) {
    LoadServiceImpl loadingService = new LoadServiceImpl(message.getConfiguration(), vertx);
    CompletableFuture.completedFuture(null)
      .thenCompose(o -> populateHoldingsIfNecessary(loadingService))
      .thenAccept(status -> holdingsService.snapshotCreated(new SnapshotCreatedMessage(message.getConfiguration(), status.getTransactionId(),
        status.getTotalCount(), getRequestCount(status.getTotalCount(), getMaxPageSize()), message.getTenantId())))
      .whenComplete((o, throwable) -> {
        if (throwable != null) {
          logger.error("Failed to create snapshot", throwable);
          holdingsService.snapshotFailed(
            new SnapshotFailedMessage(message.getConfiguration(), throwable.getMessage(), message.getTenantId()));
        }
      });
  }

  @Override
  public void loadHoldings(LoadHoldingsMessage message) {
    LoadServiceImpl loadingService = new LoadServiceImpl(message.getConfiguration(), vertx);
    CompletableFuture.completedFuture(null)
      .thenCompose(o -> loadHoldings(message, loadingService))
      .whenComplete((result, throwable) -> {
        if (throwable != null) {
          logger.error("Failed to load holdings", throwable);
          holdingsService.loadingFailed(new LoadFailedMessage(
            message.getConfiguration(), throwable.getMessage(), message.getTenantId(), message.getCurrentTransactionId(), message.getTotalCount(), message.getTotalPages()));
        }
      });
  }

  private CompletableFuture<HoldingsStatus> populateHoldingsIfNecessary(LoadService loadingService) {
    return getLastLoadingStatus(loadingService).thenCompose(loadStatus -> {
      final LoadStatus statusEnum = loadStatus.getStatus();
      if (IN_PROGRESS.equals(statusEnum)) {
        return waitForCompleteStatus(statusRetryCount, loadStatus.getTransactionId(), loadingService);
      }
      else if (snapshotCreatedRecently(loadStatus)) {
        return CompletableFuture.completedFuture(loadStatus);
      } else {
        logger.info("Start populating holdings to stage environment.");
        return populateHoldings(loadingService)
          .thenCompose(transactionId -> waitForCompleteStatus(statusRetryCount, transactionId, loadingService));
      }
    });
  }

  public CompletableFuture<HoldingsStatus> waitForCompleteStatus(int retryCount, String transactionId, LoadService loadingService) {
    CompletableFuture<HoldingsStatus> future = new CompletableFuture<>();
    waitForCompleteStatus(retryCount, transactionId, future, loadingService);
    return future;
  }

  public void waitForCompleteStatus(int retries, String transactionId, CompletableFuture<HoldingsStatus> future, LoadService loadingService) {
    vertx.setTimer(statusRetryDelay, timerId -> getLoadingStatus(loadingService, transactionId)
      .thenAccept(loadStatus -> {
        final LoadStatus status = loadStatus.getStatus();
        logger.info("Getting status of stage snapshot: {}.", status);
        if (COMPLETED.equals(status)) {
          future.complete(loadStatus);
        } else if (IN_PROGRESS.equals(status)) {
          if (retries <= 1) {
            throw new IllegalStateException("Failed to get status with status response:" + loadStatus.getStatus());
          }
          waitForCompleteStatus(retries - 1, transactionId, future, loadingService);
        } else {
          future.completeExceptionally(new IllegalStateException("Failed to get status with status response:" + loadStatus));
        }
      }).exceptionally(throwable -> {
        future.completeExceptionally(throwable);
        return null;
      }));
  }

  /**
   * Runs action provided by futureProducer, if future is completed exceptionally then futureProducer will be called again
   * after given delay.
   *
   * @param retries        Amount of times action will be retried (e.g. if retries = 2 then futureProducer will be called 2 times)
   * @param delay          delay in milliseconds before action is executed again after failure
   * @param futureProducer provides an asynchronous action
   * @return future returned by
   */
  public <T> CompletableFuture<T> retryOnFailure(int retries, long delay, Producer<CompletableFuture<T>> futureProducer) {
    CompletableFuture<T> future = new CompletableFuture<>();
    retryOnFailure(retries, delay, future, futureProducer);
    return future;
  }

  private <T> void retryOnFailure(int retries, long delay, CompletableFuture<T> future, Producer<CompletableFuture<T>> futureProducer) {
    doUntilResultMatches(retries, delay, future, futureProducer, (result, ex) -> ex == null);
  }

  /**
   * Runs action provided by futureProducer, when future completes the result is passed to matcher,
   * if matcher returns false then futureProducer will be called again after delay.
   * if matcher returns true then the result is propagated to returned CompletableFuture
   * if matcher throws exception then exception is propagated to returned CompletableFuture and action is not retried
   *
   * @param retries        Amount of times action will be retried (e.g. if retries = 2 then futureProducer will be called 2 times)
   * @param delay          delay in milliseconds before action is executed again after failure
   * @param matcher        predicate that determines if futureProducer should be called again after delay
   * @param futureProducer provides an asynchronous action to be executed
   */
  protected <T> CompletableFuture<T> doUntilResultMatches(int retries, long delay, Producer<CompletableFuture<T>> futureProducer, BiPredicate<T, Throwable> matcher) {
    CompletableFuture<T> future = new CompletableFuture<>();
    doUntilResultMatches(retries, delay, future, futureProducer, matcher);
    return future;
  }
  protected <T> void doUntilResultMatches(int retries, long delay, CompletableFuture<T> future, Producer<CompletableFuture<T>> futureProducer, BiPredicate<T, Throwable> matcher) {
    futureProducer.call()
      .handle((result, ex) -> {
        if(matcher.test(result, ex)){
          future.complete(result);
        }else{
          if (retries > 1) {
            vertx.setTimer(delay, timerId -> doUntilResultMatches(retries - 1, delay, future, futureProducer, matcher)
            );
          }
          else{
            future.completeExceptionally(ex != null ? ex : new IllegalStateException("Action failed with result " +  result));
          }
        }
        return null;
      })
    .exceptionally(ex -> {
      future.completeExceptionally(ex);
      return null;
    });
  }


  /**
   * Defines an amount of request needed to load all holdings from the staged area
   *
   * @param totalCount - total records count
   * @param maxRequestCount - maximum amount of records per request
   * @return number of requests
   */
  protected int getRequestCount(Integer totalCount, int maxRequestCount) {
    final int quotient = totalCount / maxRequestCount;
    final int remainder = totalCount % maxRequestCount;
    return remainder == 0 ? quotient : quotient + 1;
  }

  private boolean snapshotCreatedRecently(HoldingsStatus status) {
    if(StringUtils.isEmpty(status.getCreated())){
      return false;
    }

    ZonedDateTime earliestDateConsideredFresh = ZonedDateTime.now(ZoneOffset.UTC).minus(snapshotRefreshPeriod, ChronoUnit.MILLIS);
    ZonedDateTime snapshotCreated = LocalDateTime.parse(status.getCreated(), HOLDINGS_STATUS_TIME_FORMATTER).atZone(ZoneOffset.UTC);
    return snapshotCreated.isAfter(earliestDateConsideredFresh);
  }

  protected CompletableFuture<Void> loadWithPagination(Integer totalPages, IntFunction<CompletableFuture<Void>> pageLoader) {
    CompletableFuture<Void> future = CompletableFuture.completedFuture(null);
    List<Integer> pagesToLoad = IntStream.range(1, totalPages + 1).boxed().collect(Collectors.toList());
    for (Integer page : pagesToLoad) {
      future = future
        .thenCompose(o -> retryOnFailure(loadPageRetries, loadPageDelay, () -> pageLoader.apply(page)));
    }
    return future;
  }

  /**
   * Starts the process of creating a snapshot of holdings.
   * @return Id if created snapshot has associated id. Null otherwise
   */
  protected abstract CompletableFuture<String> populateHoldings(LoadService loadingService);

  /**
   * Retrieves last loading status
   * @return loading status of the last created snapshot
   */
  protected abstract CompletableFuture<HoldingsStatus> getLastLoadingStatus(LoadService loadingService);

  /**
   * Retrieves loading status of snapshot with specified transactionId.
   * transactionId can be null if processed snapshot doesn't have id
   * @param transactionId id of snapshot
   * @return status of snapshot
   */
  protected abstract CompletableFuture<HoldingsStatus> getLoadingStatus(LoadService loadingService, @Nullable String transactionId);

  /**
   * Starts process of loading holdings data using existing snapshot
   */
  protected abstract CompletableFuture<Void> loadHoldings(LoadHoldingsMessage message, LoadService loadingService);

  /**
   * Specifies the page size that will be used when loading data from snapshots
   */
  protected abstract int getMaxPageSize();
}
