package org.folio.service.holdings;

import static org.folio.common.ListUtils.mapItems;
import static org.folio.db.RowSetUtils.toUUID;
import static org.folio.holdingsiq.model.HoldingChangeType.HOLDING_ADDED;
import static org.folio.holdingsiq.model.HoldingChangeType.HOLDING_DELETED;
import static org.folio.holdingsiq.model.HoldingChangeType.HOLDING_UPDATED;
import static org.folio.holdingsiq.model.HoldingChangeType.HOLDING_UPDATED_ADDED_COVERAGE;
import static org.folio.holdingsiq.model.HoldingChangeType.HOLDING_UPDATED_DELETED_COVERAGE;
import static org.folio.repository.holdings.HoldingsServiceMessagesFactory.getLoadHoldingsMessage;
import static org.folio.repository.holdings.status.HoldingsLoadingStatusFactory.getLoadStatusFailed;
import static org.folio.repository.holdings.status.HoldingsLoadingStatusFactory.getStatusCompleted;
import static org.folio.repository.holdings.status.HoldingsLoadingStatusFactory.getStatusLoadingHoldings;
import static org.folio.repository.holdings.status.HoldingsLoadingStatusFactory.getStatusPopulatingStagingArea;
import static org.folio.rest.util.ErrorUtil.createError;
import static org.folio.util.FutureUtils.failedFuture;
import static org.folio.util.FutureUtils.mapVertxFuture;

import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.shareddata.Lock;
import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.internal.util.Producer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import org.folio.holdingsiq.model.Holding;
import org.folio.holdingsiq.model.HoldingChangeType;
import org.folio.holdingsiq.model.HoldingInReport;
import org.folio.repository.holdings.DbHoldingInfo;
import org.folio.repository.holdings.HoldingsId;
import org.folio.repository.holdings.HoldingsRepository;
import org.folio.repository.holdings.status.HoldingsStatusRepository;
import org.folio.repository.holdings.status.retry.RetryStatus;
import org.folio.repository.holdings.status.retry.RetryStatusRepository;
import org.folio.repository.holdings.transaction.TransactionIdRepository;
import org.folio.repository.resources.DbResource;
import org.folio.rest.jaxrs.model.HoldingsLoadingStatus;
import org.folio.rest.jaxrs.model.LoadStatusAttributes;
import org.folio.rest.jaxrs.model.LoadStatusNameEnum;
import org.folio.rest.util.IdParser;
import org.folio.rest.util.template.RMAPITemplateContext;
import org.folio.service.holdings.exception.ProcessInProgressException;
import org.folio.service.holdings.message.ConfigurationMessage;
import org.folio.service.holdings.message.DeltaReportCreatedMessage;
import org.folio.service.holdings.message.DeltaReportMessage;
import org.folio.service.holdings.message.HoldingsMessage;
import org.folio.service.holdings.message.LoadFailedMessage;
import org.folio.service.holdings.message.SnapshotCreatedMessage;
import org.folio.service.holdings.message.SnapshotFailedMessage;

@Component
public class HoldingsServiceImpl implements HoldingsService {

  public static final DateTimeFormatter POSTGRES_TIMESTAMP_FORMATTER = new DateTimeFormatterBuilder()
    .parseCaseInsensitive()
    .append(DateTimeFormatter.ISO_LOCAL_DATE)
    .appendLiteral(' ')
    .append(DateTimeFormatter.ISO_LOCAL_TIME)
    .appendOffset("+HH", "Z")
    .toFormatter();

  private static final Logger logger = LoggerFactory.getLogger(HoldingsServiceImpl.class);
  private static final String START_LOADING_LOCK = "getStatus";
  private static final List<HoldingChangeType> ADDED_OR_UPDATED_CHANGE_TYPES = Arrays.asList(
    HOLDING_ADDED,
    HOLDING_UPDATED,
    HOLDING_UPDATED_ADDED_COVERAGE,
    HOLDING_UPDATED_DELETED_COVERAGE
  );

  private static final String CURRENT_STATUS_MESSAGE = "Current status is {}";
  private static final String FAILED_CREATE_SNAPSHOT_MESSAGE = "Failed to create snapshot";
  private static final String FAILED_DURING_RETRY_MESSAGE = "Failed during retry";
  private static final String FAILED_PROCESS_CHANGES_MESSAGE = "Failed to process changes";
  private static final String FAILED_RETRY_CREATING_SNAPSHOT_MESSAGE = "Failed to retry creating snapshot";
  private static final String FAILED_RETRY_LOADING_HOLDINGS_MESSAGE = "Failed to retry loading holdings";
  private static final String FAILED_SAVE_HOLDINGS_MESSAGE = "Failed to save holdings";
  private static final String FAILED_UPDATE_STATUS_TO_FAILED_MESSAGE = "Failed to update status to failed";
  private static final String LOADING_STATUS_IN_PROGRESS_MESSAGE = "Loading status is already In Progress";
  private static final String SAVING_HOLDINGS_MESSAGE = "Saving holdings to database.";
  private static final String SKIPPING_LOADING_SNAPSHOT_MESSAGE = "Skipping loading snapshot, "
    + "because transaction with id {} is already loaded";

  private final LoadServiceFacade loadServiceFacade;
  private final HoldingsRepository holdingsRepository;
  private final HoldingsStatusRepository holdingsStatusRepository;
  private final RetryStatusRepository retryStatusRepository;
  private final TransactionIdRepository transactionIdRepository;
  private final Vertx vertx;
  private final long snapshotRetryDelay;
  private final int snapshotRetryCount;
  private final long loadHoldingsRetryDelay;
  private final int loadHoldingsRetryCount;
  private final int loadHoldingsTimeout;

  @Autowired
  public HoldingsServiceImpl(Vertx vertx, HoldingsRepository holdingsRepository,
                             @Value("${holdings.snapshot.retry.delay}") long snapshotRetryDelay,
                             @Value("${holdings.snapshot.retry.count}") int snapshotRetryCount,
                             @Value("${holdings.snapshot.retry.delay}") long loadHoldingsRetryDelay,
                             @Value("${holdings.snapshot.retry.count}") int loadHoldingsRetryCount,
                             @Value("${holdings.timeout}") int loadHoldingsTimeout,
                             HoldingsStatusRepository holdingsStatusRepository,
                             RetryStatusRepository retryStatusRepository,
                             TransactionIdRepository transactionIdRepository) {
    this.vertx = vertx;
    this.holdingsRepository = holdingsRepository;
    this.holdingsStatusRepository = holdingsStatusRepository;
    this.retryStatusRepository = retryStatusRepository;
    this.snapshotRetryDelay = snapshotRetryDelay;
    this.snapshotRetryCount = snapshotRetryCount;
    this.loadHoldingsRetryDelay = loadHoldingsRetryDelay;
    this.loadHoldingsRetryCount = loadHoldingsRetryCount;
    this.loadHoldingsTimeout = loadHoldingsTimeout;
    this.loadServiceFacade = LoadServiceFacade.createProxy(vertx, HoldingConstants.LOAD_FACADE_ADDRESS);
    this.transactionIdRepository = transactionIdRepository;
  }

  @Override
  public CompletableFuture<Void> loadHoldings(RMAPITemplateContext context) {
    throw new UnsupportedOperationException();
  }

  @Override
  public CompletableFuture<Void> loadHoldingsById(String credentialsId, RMAPITemplateContext context) {
    final String tenantId = context.getOkapiData().getTenant();
    Future<Void> executeFuture = executeWithLock(START_LOADING_LOCK, () ->
      tryChangingStatusToInProgress(getStatusPopulatingStagingArea(), toUUID(credentialsId), tenantId)
        .thenCompose(o -> resetRetries(toUUID(credentialsId), tenantId, snapshotRetryCount - 1))
        .thenAccept(o -> {
          ConfigurationMessage configuration = new ConfigurationMessage(context.getConfiguration(), credentialsId, tenantId);
          loadServiceFacade.createSnapshot(configuration);
        })
    );
    return mapVertxFuture(executeFuture);
  }

  @Override
  public CompletableFuture<List<DbHoldingInfo>> getHoldingsByIds(List<DbResource> resourcesResult, String credentialsId,
                                                                 String tenantId) {
    return holdingsRepository.findAllById(getTitleIdsAsList(resourcesResult), toUUID(credentialsId), tenantId);
  }

  @Override
  public void saveHolding(HoldingsMessage holdings) {
    final String tenantId = holdings.getTenantId();
    final UUID credentialsId = toUUID(holdings.getCredentialsId());
    saveHoldings(holdings.getHoldingList(), OffsetDateTime.now(), credentialsId, tenantId)
      .thenCompose(o -> holdingsStatusRepository.increaseImportedCount(holdings.getHoldingList().size(),
        1, credentialsId, tenantId)
      )
      .thenCompose(status -> {
          LoadStatusAttributes attributes = status.getData().getAttributes();
          if (hasLoadedLastPage(attributes)) {
            return
              holdingsRepository
                .deleteBeforeTimestamp(getZonedDateTime(attributes.getStarted()).toInstant(), credentialsId, tenantId)
                .thenCompose(o -> holdingsStatusRepository
                  .update(getStatusCompleted(attributes.getTotalCount()), credentialsId, tenantId)
                )
                .thenCompose(o -> transactionIdRepository.save(credentialsId, holdings.getTransactionId(), tenantId));
          }
          return CompletableFuture.completedFuture(null);
        }
      )
      .exceptionally(e -> {
        logger.error(FAILED_SAVE_HOLDINGS_MESSAGE, e);
        return null;
      });
  }

  @Override
  public void processChanges(DeltaReportMessage holdings) {
    final String tenantId = holdings.getTenantId();
    final UUID credentialsId = toUUID(holdings.getCredentialsId());
    processChanges(holdings.getHoldingList(), OffsetDateTime.now(), credentialsId, tenantId)
      .thenCompose(o -> holdingsStatusRepository.increaseImportedCount(holdings.getHoldingList().size(),
        1, credentialsId, tenantId)
      )
      .thenCompose(status -> {
          LoadStatusAttributes attributes = status.getData().getAttributes();
          if (hasLoadedLastPage(attributes)) {
            return
              holdingsStatusRepository.update(getStatusCompleted(attributes.getTotalCount()), credentialsId, tenantId)
                .thenCompose(o -> transactionIdRepository.save(credentialsId, holdings.getTransactionId(), tenantId));
          }
          return CompletableFuture.completedFuture(null);
        }
      )
      .exceptionally(e -> {
        logger.error(FAILED_PROCESS_CHANGES_MESSAGE, e);
        return null;
      });
  }

  @Override
  public void snapshotCreated(SnapshotCreatedMessage message) {
    final String tenantId = message.getTenantId();
    final UUID credentialsId = toUUID(message.getCredentialsId());
    transactionIdRepository.getLastTransactionId(credentialsId, tenantId)
      .thenApply(previousTransactionId -> {
        boolean transactionIsAlreadyLoaded =
          message.getTransactionId() != null && message.getTransactionId().equals(previousTransactionId);
        if (!transactionIsAlreadyLoaded) {
          holdingsStatusRepository.update(getStatusLoadingHoldings(
            message.getTotalCount(), 0, message.getTotalPages(), 0), credentialsId, tenantId)
            .thenCompose(o -> resetRetries(credentialsId, tenantId, loadHoldingsRetryCount - 1))
            .thenAccept(o -> loadServiceFacade.loadHoldings(getLoadHoldingsMessage(message, previousTransactionId)))
            .exceptionally(e -> {
              logger.error(FAILED_CREATE_SNAPSHOT_MESSAGE, e);
              return null;
            });
        } else {
          logger.info(SKIPPING_LOADING_SNAPSHOT_MESSAGE, message.getTransactionId());
          holdingsStatusRepository.update(getStatusCompleted(0), credentialsId, tenantId);
        }
        return null;
      });
  }

  @Override
  public void snapshotFailed(SnapshotFailedMessage message) {
    final String tenantId = message.getTenantId();
    final String credentialsId = message.getCredentialsId();
    setStatusToFailed(toUUID(credentialsId), tenantId, message.getErrorMessage())
      .thenAccept(o ->
        retryAfterDelayIfAttemptsLeft(credentialsId, tenantId, snapshotRetryDelay, o2 ->
          executeWithLock(START_LOADING_LOCK, () ->
            tryChangingStatusToInProgress(getStatusPopulatingStagingArea(), toUUID(credentialsId), tenantId)
              .thenAccept(o3 -> loadServiceFacade
                .createSnapshot(new ConfigurationMessage(message.getConfiguration(), credentialsId, tenantId)))
              .exceptionally(e -> {
                logger.error(FAILED_RETRY_CREATING_SNAPSHOT_MESSAGE, e);
                return null;
              }))));
  }

  @Override
  public void deltaReportCreated(DeltaReportCreatedMessage message, Handler<AsyncResult<Void>> handler) {
    final UUID credentialsId = toUUID(message.getCredentialsId());
    final String tenantId = message.getTenantId();
    holdingsStatusRepository.update(getStatusLoadingHoldings(
      message.getTotalCount(), 0, message.getTotalPages(), 0), credentialsId, tenantId)
      .thenAccept(o -> handler.handle(Future.succeededFuture(null)))
      .exceptionally(e -> {
        logger.error(FAILED_CREATE_SNAPSHOT_MESSAGE, e);
        handler.handle(Future.failedFuture(e));
        return null;
      });
  }

  @Override
  public void loadingFailed(LoadFailedMessage message) {
    final UUID credentialsId = toUUID(message.getCredentialsId());
    final String tenantId = message.getTenantId();
    setStatusToFailed(credentialsId, tenantId, message.getErrorMessage())
      .thenAccept(o ->
        retryAfterDelayIfAttemptsLeft(message.getCredentialsId(), tenantId, loadHoldingsRetryDelay, o2 ->
          executeWithLock(START_LOADING_LOCK, () ->
            {
              final Integer totalCount = message.getTotalCount();
              final Integer totalPages = message.getTotalPages();
              return tryChangingStatusToInProgress(getStatusLoadingHoldings(totalCount, 0, totalPages, 0), credentialsId,
                tenantId)
                .thenCompose(o3 ->
                  transactionIdRepository.getLastTransactionId(credentialsId, tenantId)
                    .thenAccept(previousTransactionId -> loadServiceFacade
                      .loadHoldings(getLoadHoldingsMessage(message, previousTransactionId))))
                .exceptionally(e -> {
                  logger.error(FAILED_RETRY_LOADING_HOLDINGS_MESSAGE, e);
                  return null;
                });
            }
          )));
  }

  public CompletableFuture<Void> processChanges(List<HoldingInReport> holdings, OffsetDateTime updatedAt, UUID credentialsId,
                                                String tenantId) {
    Set<DbHoldingInfo> holdingsToSave = getDbHoldingsByType(holdings, ADDED_OR_UPDATED_CHANGE_TYPES)
      .map(this::mapToHoldingInfoInDb)
      .collect(Collectors.toSet());
    Set<HoldingsId> holdingsToDelete = getDbHoldingsByType(holdings, Collections.singleton(HOLDING_DELETED))
      .map(holding -> new HoldingsId(holding.getTitleId(), holding.getPackageId(), holding.getVendorId()))
      .collect(Collectors.toSet());

    return holdingsRepository.saveAll(holdingsToSave, updatedAt, credentialsId, tenantId)
      .thenCompose(o -> holdingsRepository.deleteAll(holdingsToDelete, credentialsId, tenantId));
  }

  private boolean hasLoadedLastPage(LoadStatusAttributes attributes) {
    final Integer importedPages = attributes.getImportedPages();
    final Integer totalPages = attributes.getTotalPages();
    return attributes.getStatus().getName() == LoadStatusNameEnum.IN_PROGRESS &&
      importedPages.equals(totalPages);
  }

  private CompletableFuture<Void> resetRetries(UUID credentialsId, String tenantId, int retryCount) {
    return retryStatusRepository
      .findByCredentialsId(credentialsId, tenantId)
      .thenCompose(status -> {
        if (status.getTimerId() != null) {
          vertx.cancelTimer(status.getTimerId());
        }
        return retryStatusRepository.update(new RetryStatus(retryCount, null), credentialsId, tenantId);
      });
  }

  private CompletableFuture<Void> tryChangingStatusToInProgress(HoldingsLoadingStatus newStatus, UUID credentialsId,
                                                                String tenantId) {
    return holdingsStatusRepository.findByCredentialsId(credentialsId, tenantId)
      .thenCompose(status -> {
        LoadStatusAttributes attributes = status.getData().getAttributes();
        logger.info(CURRENT_STATUS_MESSAGE, attributes.getStatus().getName());
        if (attributes.getStatus().getName() != LoadStatusNameEnum.IN_PROGRESS || processTimedOut(status)) {
          return holdingsStatusRepository.delete(credentialsId, tenantId)
            .thenCompose(o -> holdingsStatusRepository.save(newStatus, credentialsId, tenantId));
        }
        return failedFuture(new ProcessInProgressException(LOADING_STATUS_IN_PROGRESS_MESSAGE));
      });
  }

  private CompletableFuture<Void> retryAfterDelayIfAttemptsLeft(String credentialsId, String tenantId, long retryDelay,
                                                                Handler<Long> retryHandler) {
    UUID uuid = toUUID(credentialsId);
    return retryStatusRepository.findByCredentialsId(uuid, tenantId)
      .thenAccept(retryStatus -> {
        int retryAttempts = retryStatus.getRetryAttemptsLeft();
        if (retryAttempts >= 1) {
          long timerId = vertx.setTimer(retryDelay, retryHandler);
          retryStatusRepository.update(new RetryStatus(retryAttempts - 1, timerId), uuid, tenantId);
        }
      })
      .exceptionally(e -> {
        logger.error(FAILED_DURING_RETRY_MESSAGE, e);
        return null;
      });
  }

  private CompletableFuture<Void> setStatusToFailed(UUID credentialsId, String tenantId, String message) {
    return holdingsStatusRepository.update(getLoadStatusFailed(createError(message, null).getErrors()),
      credentialsId, tenantId)
      .exceptionally(e -> {
        logger.error(FAILED_UPDATE_STATUS_TO_FAILED_MESSAGE, e);
        return null;
      });
  }

  private Future<Void> executeWithLock(String lockName, Producer<CompletableFuture<Void>> futureProducer) {
    Promise<Lock> lockPromise = Promise.promise();
    Promise<Void> responsePromise = Promise.promise();
    vertx.sharedData().getLock(lockName, lockPromise);
    lockPromise.future().map(lock -> {
      futureProducer.call()
        .whenComplete((o, throwable) -> {
            if (throwable != null) {
              responsePromise.fail(throwable);
            } else {
              responsePromise.complete();
            }
            lock.release();
          }
        );
      return null;
    });
    return responsePromise.future();
  }

  private List<String> getTitleIdsAsList(List<DbResource> resources) {
    return mapItems(resources, dbResource -> IdParser.resourceIdToString(dbResource.getId()));
  }

  private CompletableFuture<Void> saveHoldings(List<Holding> holdings, OffsetDateTime updatedAt, UUID credentialsId,
                                               String tenantId) {
    Set<DbHoldingInfo> dbHoldings = holdings.stream()
      .filter(distinctByKey(this::getHoldingsId))
      .map(holding -> new DbHoldingInfo(
        holding.getTitleId(),
        holding.getPackageId(),
        String.valueOf(holding.getVendorId()),
        holding.getPublicationTitle(),
        holding.getPublisherName(),
        holding.getResourceType()
      ))
      .collect(Collectors.toSet());
    logger.info(SAVING_HOLDINGS_MESSAGE);
    return holdingsRepository.saveAll(dbHoldings, updatedAt, credentialsId, tenantId);
  }

  private Stream<HoldingInReport> getDbHoldingsByType(List<HoldingInReport> holdings,
                                                      Collection<HoldingChangeType> matchingTypes) {
    return holdings.stream()
      .filter(holding -> matchingTypes.contains(holding.getChangeType()));
  }

  private DbHoldingInfo mapToHoldingInfoInDb(HoldingInReport holding) {
    return new DbHoldingInfo(
      holding.getTitleId(),
      holding.getPackageId(),
      String.valueOf(holding.getVendorId()),
      holding.getPublicationTitle(),
      holding.getPublisherName(),
      holding.getResourceType()
    );
  }

  private <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
    Set<Object> seen = ConcurrentHashMap.newKeySet();
    return t -> seen.add(keyExtractor.apply(t));
  }

  private String getHoldingsId(Holding holding) {
    return holding.getVendorId() + "-" + holding.getPackageId() + "-" + holding.getTitleId();
  }

  private boolean processTimedOut(HoldingsLoadingStatus status) {
    String updatedString = status.getData().getAttributes().getUpdated();
    if (StringUtils.isEmpty(updatedString)) {
      return true;
    }
    ZonedDateTime updated = getZonedDateTime(updatedString);
    return ZonedDateTime.now().isAfter(updated.plus(loadHoldingsTimeout, ChronoUnit.MILLIS));
  }

  private ZonedDateTime getZonedDateTime(String stringToParse) {
    return ZonedDateTime.parse(stringToParse, POSTGRES_TIMESTAMP_FORMATTER);
  }
}
