package org.folio.service.holdings;

import static org.apache.commons.lang3.math.NumberUtils.INTEGER_ZERO;
import static org.folio.repository.holdings.HoldingsServiceMessagesFactory.getLoadFailedMessage;
import static org.folio.repository.holdings.HoldingsServiceMessagesFactory.getSnapshotCreatedMessage;
import static org.folio.repository.holdings.HoldingsServiceMessagesFactory.getSnapshotFailedMessage;
import static org.folio.repository.holdings.LoadStatus.COMPLETED;
import static org.folio.repository.holdings.LoadStatus.IN_PROGRESS;
import static org.folio.repository.holdings.LoadStatus.NONE;
import static org.folio.service.holdings.HoldingConstants.HOLDINGS_SERVICE_ADDRESS;

import io.vertx.core.Vertx;
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
import java.util.stream.IntStream;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.holdingsiq.service.LoadService;
import org.folio.holdingsiq.service.impl.LoadServiceImpl;
import org.folio.repository.holdings.LoadStatus;
import org.folio.service.holdings.message.ConfigurationMessage;
import org.folio.service.holdings.message.LoadHoldingsMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

@Log4j2
@Component
public abstract class AbstractLoadServiceFacade implements LoadServiceFacade {
  public static final DateTimeFormatter HOLDINGS_STATUS_TIME_FORMATTER = new DateTimeFormatterBuilder()
    .parseCaseInsensitive()
    .append(DateTimeFormatter.ISO_LOCAL_DATE)
    .appendLiteral(' ')
    .append(DateTimeFormatter.ISO_LOCAL_TIME)
    .toFormatter();
  protected final HoldingsService holdingsService;
  protected final int loadPageRetries;
  protected final int loadPageDelay;
  private final int loadPageSizeMin;
  private final int statusRetryCount;
  private final int snapshotRefreshPeriod;
  private final long statusRetryDelay;
  private final Vertx vertx;

  protected AbstractLoadServiceFacade(@Value("${holdings.status.check.delay}") long statusRetryDelay,
                                      @Value("${holdings.status.retry.count}") int statusRetryCount,
                                      @Value("${holdings.page.retry.delay}") int loadPageRetryDelay,
                                      @Value("${holdings.page.retry.count}") int loadPageRetryCount,
                                      @Value("${holdings.page.size.min}") int loadPageSizeMin,
                                      @Value("${holdings.snapshot.refresh.period}") int snapshotRefreshPeriod,
                                      Vertx vertx) {
    this.loadPageSizeMin = loadPageSizeMin;
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
    log.debug("createSnapshot:: by [tenant: {}]", message.getTenantId());

    LoadServiceImpl loadingService = new LoadServiceImpl(message.getConfiguration(), vertx);
    populateHoldingsIfNecessary(loadingService)
      .thenAccept(status -> holdingsService.snapshotCreated(
        getSnapshotCreatedMessage(message, status, getRequestCount(status.getTotalCount(), getMaxPageSize()))))
      .whenComplete((o, throwable) -> {
        if (throwable != null) {
          log.warn("Failed to create snapshot, msg: {}", throwable.getMessage());
          holdingsService.snapshotFailed(getSnapshotFailedMessage(message, throwable));
        }
      });
  }

  @Override
  public void loadHoldings(LoadHoldingsMessage message) {
    log.debug("loadHoldings:: by [tenant: {}]", message.getTenantId());

    LoadServiceImpl loadingService = new LoadServiceImpl(message.getConfiguration(), vertx);
    loadHoldings(message, loadingService)
      .whenComplete((result, throwable) -> {
        if (throwable != null) {
          log.warn("Failed to load holdings, msg: {}", throwable.getMessage());
          holdingsService.loadingFailed(getLoadFailedMessage(message, throwable));
        }
      });
  }

  /**
   * Starts process of loading holdings data using existing snapshot.
   */
  protected abstract CompletableFuture<Void> loadHoldings(LoadHoldingsMessage message, LoadService loadingService);

  private CompletableFuture<HoldingsStatus> populateHoldingsIfNecessary(LoadService loadingService) {
    return getLastLoadingStatus(loadingService).thenCompose(loadStatus -> {
      if (IN_PROGRESS.equals(loadStatus.getStatus())) {
        return waitForCompleteStatus(statusRetryCount, loadStatus.getTransactionId(), loadingService);
      } else if (snapshotCreatedRecently(loadStatus)) {
        log.info("Snapshot created recently: {}", loadStatus);
        final Integer totalCount = loadStatus.getTotalCount();
        if (INTEGER_ZERO.equals(totalCount)) {
          throw new IllegalStateException("Snapshot created with invalid totalCount: " + loadStatus);
        } else {
          return CompletableFuture.completedFuture(loadStatus);
        }
      } else {
        log.info("Start populating holdings to stage environment.");
        return populateHoldings(loadingService)
          .thenCompose(transactionId -> waitForCompleteStatus(statusRetryCount, transactionId, loadingService));
      }
    });
  }

  private CompletableFuture<HoldingsStatus> waitForCompleteStatus(int retryCount, String transactionId,
                                                                  LoadService loadingService) {
    CompletableFuture<HoldingsStatus> future = new CompletableFuture<>();
    waitForCompleteStatus(retryCount, transactionId, future, loadingService);
    return future;
  }

  private void waitForCompleteStatus(int retries, String transactionId, CompletableFuture<HoldingsStatus> future,
                                     LoadService loadingService) {

    vertx.setTimer(statusRetryDelay, timerId -> getLoadingStatus(loadingService, transactionId)
      .thenAccept(loadStatus -> {
        log.debug("waitForCompleteStatus: stage snapshot [status: {}]", loadStatus.getStatus());
        final LoadStatus status = loadStatus.getStatus();
        if (COMPLETED.equals(status)) {
          final Integer totalCount = loadStatus.getTotalCount();
          if (INTEGER_ZERO.equals(totalCount)) {
            throw new IllegalStateException("Snapshot created with invalid totalCount: " + loadStatus);
          } else {
            future.complete(loadStatus);
          }
        } else if (IN_PROGRESS.equals(status) || NONE.equals(status)) {
          if (retries <= 1) {
            throw new IllegalStateException("Failed to get status with status response: " + loadStatus.getStatus());
          }
          waitForCompleteStatus(retries - 1, transactionId, future, loadingService);
        } else {
          future.completeExceptionally(
            new IllegalStateException("Failed to get status with status response: " + loadStatus));
        }
      }).exceptionally(throwable -> {
        future.completeExceptionally(throwable);
        return null;
      }));
  }

  /**
   * Runs action provided by futureFunction, if future is completed exceptionally then futureFunction
   * will be called again after given delay.
   *
   * @param retries        Amount of times action will be retried
   *                       (e.g. if retries = 2 then futureFunction will be called 2 times)
   * @param delay          delay in milliseconds before action is executed again after failure
   * @param futureFunction provides an asynchronous action
   * @return future returned by
   */
  private <T> CompletableFuture<T> retryOnFailure(int retries, long delay,
                                                  IntFunction<CompletableFuture<T>> futureFunction) {
    CompletableFuture<T> future = new CompletableFuture<>();
    retryOnFailure(retries, delay, future, futureFunction);
    return future;
  }

  private <T> void retryOnFailure(int retries, long delay, CompletableFuture<T> future,
                                  IntFunction<CompletableFuture<T>> futureFunction) {
    doUntilResultMatches(retries, delay, future, futureFunction, (result, ex) -> ex == null);
  }

  /**
   * Runs action provided by futureFunction, when future completes the result is passed to matcher,
   * if matcher returns false then futureFunction will be called again after delay.
   * if matcher returns true then the result is propagated to returned CompletableFuture
   * if matcher throws exception then exception is propagated to returned CompletableFuture and action is not retried
   *
   * @param retries        Amount of times action will be retried (e.g. if retries = 2 then futureFunction
   *                       will be called 2 times)
   * @param delay          delay in milliseconds before action is executed again after failure
   * @param matcher        predicate that determines if futureFunction should be called again after delay
   * @param futureFunction provides an asynchronous action to be executed
   */
  protected <T> CompletableFuture<T> doUntilResultMatches(int retries, long delay,
                                                          IntFunction<CompletableFuture<T>> futureFunction,
                                                          BiPredicate<T, Throwable> matcher) {
    CompletableFuture<T> future = new CompletableFuture<>();
    doUntilResultMatches(retries, delay, future, futureFunction, matcher);
    return future;
  }

  private <T> void doUntilResultMatches(int retries, long delay, CompletableFuture<T> future,
                                        IntFunction<CompletableFuture<T>> futureFunction,
                                        BiPredicate<T, Throwable> matcher) {
    futureFunction.apply(retries)
      .handle((result, ex) -> {
        if (matcher.test(result, ex)) {
          future.complete(result);
        } else {
          if (retries > 1) {
            vertx.setTimer(delay, timerId -> doUntilResultMatches(retries - 1, delay, future, futureFunction, matcher));
          } else {
            future.completeExceptionally(
              ex != null ? ex : new IllegalStateException("Action failed with result " + result));
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
   * Defines an amount of request needed to load all holdings from the staged area.
   *
   * @param totalCount      - total records count
   * @param maxRequestCount - maximum amount of records per request
   * @return number of requests
   */
  protected int getRequestCount(Integer totalCount, int maxRequestCount) {
    final int quotient = totalCount / maxRequestCount;
    final int remainder = totalCount % maxRequestCount;
    return remainder == 0 ? quotient : quotient + 1;
  }

  private boolean snapshotCreatedRecently(HoldingsStatus status) {
    if (StringUtils.isEmpty(status.getCreated())) {
      return false;
    }

    ZonedDateTime earliestDateConsideredFresh =
      ZonedDateTime.now(ZoneOffset.UTC).minus(snapshotRefreshPeriod, ChronoUnit.MILLIS);
    ZonedDateTime snapshotCreated =
      LocalDateTime.parse(status.getCreated(), HOLDINGS_STATUS_TIME_FORMATTER).atZone(ZoneOffset.UTC);
    return snapshotCreated.isAfter(earliestDateConsideredFresh);
  }

  protected CompletableFuture<Void> loadWithPagination(Integer totalPages,
                                                       IntFunction<CompletableFuture<Void>> pageLoader) {
    CompletableFuture<Void> future = CompletableFuture.completedFuture(null);
    List<Integer> pagesToLoad = IntStream.range(1, totalPages + 1).boxed().toList();
    for (Integer page : pagesToLoad) {
      future = future.thenCompose(
        o -> retryOnFailure(loadPageRetries, loadPageDelay, retries -> calculatePage(pageLoader, page, retries)));
    }
    return future;
  }

  protected CompletableFuture<Void> calculatePage(IntFunction<CompletableFuture<Void>> pageLoader,
                                                  Integer page, Integer retries) {
    if (loadPageRetries > retries) {
      page = Math.max(page / 2, loadPageSizeMin);
    }
    return pageLoader.apply(page);
  }

  /**
   * Starts the process of creating a snapshot of holdings.
   *
   * @return Id if created snapshot has associated id. Null otherwise
   */
  protected abstract CompletableFuture<String> populateHoldings(LoadService loadingService);

  /**
   * Retrieves last loading status.
   *
   * @return loading status of the last created snapshot
   */
  protected abstract CompletableFuture<HoldingsStatus> getLastLoadingStatus(LoadService loadingService);

  /**
   * Retrieves loading status of snapshot with specified transactionId.
   * transactionId can be null if processed snapshot doesn't have id
   *
   * @param transactionId id of snapshot
   * @return status of snapshot
   */
  protected abstract CompletableFuture<HoldingsStatus> getLoadingStatus(LoadService loadingService,
                                                                        @Nullable String transactionId);

  /**
   * Specifies the page size that will be used when loading data from snapshots.
   */
  protected abstract int getMaxPageSize();
}
