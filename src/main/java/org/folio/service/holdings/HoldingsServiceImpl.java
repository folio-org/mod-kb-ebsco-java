package org.folio.service.holdings;

import static java.lang.Integer.parseInt;
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
import static org.folio.repository.holdings.status.HoldingsLoadingStatusFactory.getStatusNotStarted;
import static org.folio.repository.holdings.status.HoldingsLoadingStatusFactory.getStatusPopulatingStagingArea;
import static org.folio.rest.util.DateTimeUtil.getZonedDateTime;
import static org.folio.rest.util.ErrorUtil.createError;
import static org.folio.util.FutureUtils.failedFuture;
import static org.folio.util.FutureUtils.mapVertxFuture;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import java.time.OffsetDateTime;
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
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
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
import org.folio.rest.jaxrs.model.HoldingsLoadingStatus;
import org.folio.rest.jaxrs.model.LoadStatusAttributes;
import org.folio.rest.jaxrs.model.LoadStatusInformation;
import org.folio.rest.jaxrs.model.LoadStatusNameEnum;
import org.folio.rest.util.template.RmApiTemplateContext;
import org.folio.service.holdings.exception.ProcessInProgressException;
import org.folio.service.holdings.message.ConfigurationMessage;
import org.folio.service.holdings.message.DeltaReportCreatedMessage;
import org.folio.service.holdings.message.DeltaReportMessage;
import org.folio.service.holdings.message.HoldingsMessage;
import org.folio.service.holdings.message.LoadFailedMessage;
import org.folio.service.holdings.message.SnapshotCreatedMessage;
import org.folio.service.holdings.message.SnapshotFailedMessage;
import org.glassfish.jersey.internal.util.Producer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Log4j2
@Component
public class HoldingsServiceImpl implements HoldingsService {

  private static final String START_LOADING_LOCK = "getStatus";
  private static final List<HoldingChangeType> ADDED_OR_UPDATED_CHANGE_TYPES = Arrays.asList(
    HOLDING_ADDED,
    HOLDING_UPDATED,
    HOLDING_UPDATED_ADDED_COVERAGE,
    HOLDING_UPDATED_DELETED_COVERAGE
  );

  private static final String CURRENT_STATUS_MESSAGE = "Current status for credentials - {} is {}";
  private static final String FAILED_CREATE_SNAPSHOT_MESSAGE = "Failed to create snapshot";
  private static final String FAILED_DURING_RETRY_MESSAGE = "Failed during retry";
  private static final String FAILED_PROCESS_CHANGES_MESSAGE = "Failed to process changes";
  private static final String FAILED_RETRY_CREATING_SNAPSHOT_MESSAGE = "Failed to retry creating snapshot";
  private static final String FAILED_RETRY_LOADING_HOLDINGS_MESSAGE = "Failed to retry loading holdings";
  private static final String FAILED_SAVE_HOLDINGS_MESSAGE = "Failed to save holdings";
  private static final String FAILED_UPDATE_STATUS_TO_FAILED_MESSAGE = "Failed to update status to failed";
  private static final String FAILED_SAVE_STATUS_MESSAGE = "Failed to save status";
  private static final String LOADING_STATUS_IN_PROGRESS_MESSAGE = "Loading status is already In Progress";
  private static final String SAVING_HOLDINGS_MESSAGE = "Saving holdings to database.";
  private static final String SKIPPING_LOADING_SNAPSHOT_MESSAGE = "Skipping loading snapshot, "
                                                                  + "because transaction with id {} is already loaded";

  private LoadServiceFacade loadServiceFacade;
  private HoldingsRepository holdingsRepository;
  private HoldingsStatusRepository holdingsStatusRepository;
  private RetryStatusRepository retryStatusRepository;
  private TransactionIdRepository transactionIdRepository;
  private Vertx vertx;
  private long snapshotRetryDelay;
  private int snapshotRetryCount;
  private long loadHoldingsRetryDelay;
  private int loadHoldingsRetryCount;
  private int loadHoldingsTimeout;

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
  public CompletableFuture<Void> loadSingleHoldings(RmApiTemplateContext context) {
    final String tenantId = context.getOkapiData().getTenant();
    final String credentialsId = context.getCredentialsId();
    log.debug("loadSingleHoldings:: by [tenant: {}]", tenantId);

    Future<Void> executeFuture = executeWithLock(START_LOADING_LOCK, () ->
      tryChangingStatusToInProgress(getStatusPopulatingStagingArea(), toUUID(credentialsId), tenantId)
        .thenCompose(o -> resetRetries(snapshotRetryCount - 1, toUUID(credentialsId), tenantId))
        .thenAccept(o -> {
          ConfigurationMessage configuration =
            new ConfigurationMessage(context.getConfiguration(), credentialsId, tenantId);
          loadServiceFacade.createSnapshot(configuration);
        })
    );
    return mapVertxFuture(executeFuture);
  }

  @Override
  public CompletableFuture<List<DbHoldingInfo>> getHoldingsByIds(List<String> ids, String credentialsId,
                                                                 String tenantId) {
    return holdingsRepository.findAllById(ids, toUUID(credentialsId), tenantId);
  }

  @Override
  public CompletableFuture<Boolean> canStartLoading(String tenant) {
    return holdingsStatusRepository.findAll(tenant)
      .thenCompose(this::canStart);
  }

  @Override
  public CompletableFuture<Boolean> canStartLoading(String credentialsId, String tenant) {
    return holdingsStatusRepository.findByCredentialsId(toUUID(credentialsId), tenant)
      .thenApply(this::canChangeStatus);
  }

  @Override
  public CompletableFuture<Void> setUpCredentials(String credentialsId, String tenant) {
    UUID credentialsUuid = toUUID(credentialsId);
    return setStatusToNotStarted(credentialsUuid, tenant)
      .thenCompose(v -> resetRetries(0, credentialsUuid, tenant));
  }

  @Override
  public CompletableFuture<List<DbHoldingInfo>> getHoldingsByPackageId(String packageId, String credentialsId,
                                                                       String tenantId) {
    return holdingsRepository.findAllByPackageId(Integer.parseInt(packageId), toUUID(credentialsId), tenantId);
  }

  @Override
  public void saveHolding(HoldingsMessage holdings) {
    final String tenantId = holdings.getTenantId();
    final UUID credentialsId = toUUID(holdings.getCredentialsId());
    log.debug("saveHolding:: by [tenant: {}]", tenantId);

    saveHoldings(holdings.getHoldingList(), OffsetDateTime.now(), credentialsId, tenantId)
      .thenCompose(o -> holdingsStatusRepository.increaseImportedCount(holdings.getHoldingList().size(),
        1, credentialsId, tenantId)
      )
      .thenCompose(status -> {
        LoadStatusAttributes attributes = status.getData().getAttributes();

        if (hasLoadedLastPage(status)) {
          log.debug("saveHolding:: Attempts to delete holdings by timestamp & update holding status");
          return holdingsRepository
            .deleteBeforeTimestamp(getZonedDateTime(attributes.getStarted()), credentialsId, tenantId)
            .thenCompose(o -> holdingsStatusRepository
              .update(getStatusCompleted(attributes.getTotalCount()), credentialsId, tenantId)
            )
            .thenCompose(o -> transactionIdRepository.save(credentialsId, holdings.getTransactionId(), tenantId));
        }
        return CompletableFuture.completedFuture(null);
      })
      .exceptionally(e -> {
        log.warn(FAILED_SAVE_HOLDINGS_MESSAGE, e);
        return null;
      });
  }

  @Override
  public void processChanges(DeltaReportMessage holdings) {
    final String tenantId = holdings.getTenantId();
    final UUID credentialsId = toUUID(holdings.getCredentialsId());
    log.debug("processChanges:: by [tenant: {}]", tenantId);

    processHoldingsChanges(holdings.getHoldingList(), OffsetDateTime.now(), credentialsId, tenantId)
      .thenCompose(o -> holdingsStatusRepository.increaseImportedCount(holdings.getHoldingList().size(),
        1, credentialsId, tenantId)
      )
      .thenCompose(status -> {
        LoadStatusAttributes attributes = status.getData().getAttributes();

        if (hasLoadedLastPage(status)) {
          log.debug("processChanges:: Attempts to update holding status & save transactionId");
          return holdingsStatusRepository
            .update(getStatusCompleted(attributes.getTotalCount()), credentialsId, tenantId)
            .thenCompose(o -> transactionIdRepository.save(credentialsId, holdings.getTransactionId(), tenantId));
        }
        return CompletableFuture.completedFuture(null);
      })
      .exceptionally(e -> {
        log.warn(FAILED_PROCESS_CHANGES_MESSAGE, e);
        return null;
      });
  }

  @Override
  public void snapshotCreated(SnapshotCreatedMessage message) {
    final String tenantId = message.getTenantId();
    final UUID credentialsId = toUUID(message.getCredentialsId());
    log.debug("snapshotCreated:: by [tenant: {}]", message.getTenantId());

    transactionIdRepository.getLastTransactionId(credentialsId, tenantId)
      .thenApply(previousTransactionId -> {

        if (!isTransactionIsAlreadyLoaded(message, previousTransactionId)) {
          log.info("snapshotCreated:: Transaction is not loaded. Attempts to update holding status");

          holdingsStatusRepository.update(getStatusLoadingHoldings(
              message.getTotalCount(), 0, message.getTotalPages(), 0), credentialsId, tenantId)
            .thenCompose(o -> resetRetries(loadHoldingsRetryCount - 1, credentialsId, tenantId))
            .thenAccept(o -> loadServiceFacade.loadHoldings(getLoadHoldingsMessage(message, previousTransactionId)))
            .exceptionally(e -> {
              log.warn(FAILED_CREATE_SNAPSHOT_MESSAGE, e);
              return null;
            });
        } else {
          log.info(SKIPPING_LOADING_SNAPSHOT_MESSAGE, message.getTransactionId());
          holdingsStatusRepository.update(getStatusCompleted(0), credentialsId, tenantId);
        }
        return null;
      });
  }

  @Override
  public void snapshotFailed(SnapshotFailedMessage message) {
    final String tenantId = message.getTenantId();
    final String credentialsId = message.getCredentialsId();
    log.debug("snapshotFailed:: by [tenant: {}]", tenantId);

    setStatusToFailed(toUUID(credentialsId), tenantId, message.getErrorMessage())
      .thenAccept(o ->
        retryAfterDelay(credentialsId, tenantId, snapshotRetryDelay, o2 ->
          executeWithLock(START_LOADING_LOCK, () ->
            tryChangingStatusToInProgress(getStatusPopulatingStagingArea(), toUUID(credentialsId), tenantId)
              .thenAccept(o3 -> loadServiceFacade
                .createSnapshot(new ConfigurationMessage(message.getConfiguration(), credentialsId, tenantId)))
              .exceptionally(e -> {
                log.warn(FAILED_RETRY_CREATING_SNAPSHOT_MESSAGE, e);
                return null;
              }))));
  }

  @Override
  public Future<Void> deltaReportCreated(DeltaReportCreatedMessage message) {
    final UUID credentialsId = toUUID(message.getCredentialsId());
    final String tenantId = message.getTenantId();
    log.debug("deltaReportCreated:: by [tenant: {}]", tenantId);
    Promise<Void> promise = Promise.promise();
    holdingsStatusRepository.update(getStatusLoadingHoldings(
        message.getTotalCount(), 0, message.getTotalPages(), 0), credentialsId, tenantId)
      .thenAccept(o -> promise.handle(Future.succeededFuture(null)))
      .exceptionally(e -> {
        log.warn(FAILED_CREATE_SNAPSHOT_MESSAGE, e);
        promise.handle(Future.failedFuture(e));
        return null;
      });
    return promise.future();
  }

  @Override
  public void loadingFailed(LoadFailedMessage message) {
    final UUID credentialsId = toUUID(message.getCredentialsId());
    final String tenantId = message.getTenantId();
    log.debug("loadingFailed:: by [tenant: {}]", tenantId);

    setStatusToFailed(credentialsId, tenantId, message.getErrorMessage())
      .thenAccept(o ->
        retryAfterDelay(message.getCredentialsId(), tenantId, loadHoldingsRetryDelay, o2 ->
          executeWithLock(START_LOADING_LOCK, () -> {
            final Integer totalCount = message.getTotalCount();
            final Integer totalPages = message.getTotalPages();
            return tryChangingStatusToInProgress(getStatusLoadingHoldings(totalCount, 0, totalPages, 0),
              credentialsId, tenantId)
              .thenCompose(o3 ->
                transactionIdRepository.getLastTransactionId(credentialsId, tenantId)
                  .thenAccept(previousTransactionId -> loadServiceFacade
                    .loadHoldings(getLoadHoldingsMessage(message, previousTransactionId))))
              .exceptionally(e -> {
                log.warn(FAILED_RETRY_LOADING_HOLDINGS_MESSAGE, e);
                return null;
              });
          })));
  }

  public CompletableFuture<Void> processHoldingsChanges(List<HoldingInReport> holdings, OffsetDateTime updatedAt,
                                                        UUID credentialsId,
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

  private CompletableFuture<Boolean> canStart(List<HoldingsLoadingStatus> statuses) {
    return CompletableFuture.completedFuture(statuses.stream().allMatch(this::canChangeStatus));
  }

  private boolean canChangeStatus(HoldingsLoadingStatus status) {
    return !isInProgress(status) || isHangedLoading(status);
  }

  private boolean isHangedLoading(HoldingsLoadingStatus status) {
    String started = status.getData().getAttributes().getStarted();
    if (StringUtils.isEmpty(started)) {
      return true;
    }
    OffsetDateTime startDate = getZonedDateTime(started);
    return isInProgress(status)
           && OffsetDateTime.now().minusDays(5).isAfter(startDate);
  }

  private boolean isInProgress(HoldingsLoadingStatus status) {
    LoadStatusInformation statusInfo = status.getData().getAttributes().getStatus();
    return statusInfo.getName() == LoadStatusNameEnum.IN_PROGRESS;
  }

  private boolean isTransactionIsAlreadyLoaded(SnapshotCreatedMessage message, String previousTransactionId) {
    final String transactionId = message.getTransactionId();
    return transactionId != null && transactionId.equals(previousTransactionId);
  }

  private boolean hasLoadedLastPage(HoldingsLoadingStatus status) {
    LoadStatusAttributes attributes = status.getData().getAttributes();
    final Integer importedPages = attributes.getImportedPages();
    final Integer totalPages = attributes.getTotalPages();
    return isInProgress(status) && importedPages.equals(totalPages);
  }

  private CompletableFuture<Void> resetRetries(int retryCount, UUID credentialsId, String tenantId) {
    return retryStatusRepository
      .findByCredentialsId(credentialsId, tenantId)
      .thenCompose(status -> {
        if (status == null) {
          return retryStatusRepository.save(new RetryStatus(retryCount, null), credentialsId, tenantId);
        } else {
          if (status.getTimerId() != null) {
            vertx.cancelTimer(status.getTimerId());
          }
          return retryStatusRepository.update(new RetryStatus(retryCount, null), credentialsId, tenantId);
        }
      });
  }

  private CompletableFuture<Void> tryChangingStatusToInProgress(HoldingsLoadingStatus newStatus, UUID credentialsId,
                                                                String tenantId) {
    return holdingsStatusRepository.findByCredentialsId(credentialsId, tenantId)
      .thenCompose(status -> {
        LoadStatusAttributes attributes = status.getData().getAttributes();
        log.info(CURRENT_STATUS_MESSAGE, credentialsId, attributes.getStatus().getName());
        if (!isInProgress(status) || processTimedOut(status)) {
          return holdingsStatusRepository.delete(credentialsId, tenantId)
            .thenCompose(o -> holdingsStatusRepository.save(newStatus, credentialsId, tenantId));
        }
        return failedFuture(new ProcessInProgressException(LOADING_STATUS_IN_PROGRESS_MESSAGE));
      });
  }

  private CompletableFuture<Void> retryAfterDelay(String credentialsId, String tenantId, long retryDelay,
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
        log.warn(FAILED_DURING_RETRY_MESSAGE, e);
        return null;
      });
  }

  private CompletableFuture<Void> setStatusToFailed(UUID credentialsId, String tenantId, String message) {
    return holdingsStatusRepository.update(getLoadStatusFailed(createError(message).getErrors()),
        credentialsId, tenantId)
      .exceptionally(e -> {
        log.warn(FAILED_UPDATE_STATUS_TO_FAILED_MESSAGE, e);
        return null;
      });
  }

  private CompletableFuture<Void> setStatusToNotStarted(UUID credentialsId, String tenantId) {
    return holdingsStatusRepository.save(getStatusNotStarted(), credentialsId, tenantId)
      .exceptionally(e -> {
        log.warn(FAILED_SAVE_STATUS_MESSAGE, e);
        return null;
      });
  }

  private Future<Void> executeWithLock(String lockName, Producer<CompletableFuture<Void>> futureProducer) {
    Promise<Void> responsePromise = Promise.promise();
    vertx.sharedData().getLock(lockName)
      .map(lock -> {
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

  private CompletableFuture<Void> saveHoldings(List<Holding> holdings, OffsetDateTime updatedAt, UUID credentialsId,
                                               String tenantId) {
    Set<DbHoldingInfo> dbHoldings = holdings.stream()
      .filter(distinctByKey(this::getHoldingsId))
      .map(holding -> DbHoldingInfo.builder()
        .titleId(parseInt(holding.getTitleId()))
        .packageId(parseInt(holding.getPackageId()))
        .vendorId(holding.getVendorId())
        .publicationTitle(holding.getPublicationTitle())
        .publisherName(holding.getPublisherName())
        .resourceType(holding.getResourceType())
        .build())
      .collect(Collectors.toSet());
    log.info(SAVING_HOLDINGS_MESSAGE);
    return holdingsRepository.saveAll(dbHoldings, updatedAt, credentialsId, tenantId);
  }

  private Stream<HoldingInReport> getDbHoldingsByType(List<HoldingInReport> holdings,
                                                      Collection<HoldingChangeType> matchingTypes) {
    return holdings.stream()
      .filter(holding -> matchingTypes.contains(holding.getChangeType()));
  }

  private DbHoldingInfo mapToHoldingInfoInDb(HoldingInReport holding) {
    return DbHoldingInfo.builder()
      .titleId(parseInt(holding.getTitleId()))
      .packageId(parseInt(holding.getPackageId()))
      .vendorId(holding.getVendorId())
      .publicationTitle(holding.getPublicationTitle())
      .publisherName(holding.getPublisherName())
      .resourceType(holding.getResourceType())
      .build();
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
    OffsetDateTime updated = getZonedDateTime(updatedString);
    return isInProgress(status)
           && OffsetDateTime.now().isAfter(updated.plus(loadHoldingsTimeout, ChronoUnit.MILLIS));
  }
}
