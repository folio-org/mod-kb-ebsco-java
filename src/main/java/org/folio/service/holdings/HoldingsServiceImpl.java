package org.folio.service.holdings;

import static org.folio.common.FutureUtils.failedFuture;
import static org.folio.common.FutureUtils.mapVertxFuture;
import static org.folio.common.ListUtils.mapItems;
import static org.folio.holdingsiq.model.HoldingChangeType.HOLDING_ADDED;
import static org.folio.holdingsiq.model.HoldingChangeType.HOLDING_DELETED;
import static org.folio.holdingsiq.model.HoldingChangeType.HOLDING_UPDATED;
import static org.folio.holdingsiq.model.HoldingChangeType.HOLDING_UPDATED_ADDED_COVERAGE;
import static org.folio.holdingsiq.model.HoldingChangeType.HOLDING_UPDATED_DELETED_COVERAGE;
import static org.folio.repository.holdings.status.HoldingsLoadingStatusFactory.getLoadStatusFailed;
import static org.folio.repository.holdings.status.HoldingsLoadingStatusFactory.getStatusCompleted;
import static org.folio.repository.holdings.status.HoldingsLoadingStatusFactory.getStatusLoadingHoldings;
import static org.folio.repository.holdings.status.HoldingsLoadingStatusFactory.getStatusPopulatingStagingArea;
import static org.folio.rest.util.ErrorUtil.createError;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
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
import org.folio.repository.holdings.HoldingInfoInDB;
import org.folio.repository.holdings.HoldingsId;
import org.folio.repository.holdings.HoldingsRepository;
import org.folio.repository.holdings.status.HoldingsStatusRepository;
import org.folio.repository.holdings.status.RetryStatus;
import org.folio.repository.holdings.status.RetryStatusRepository;
import org.folio.repository.holdings.status.TransactionIdRepository;
import org.folio.repository.resources.ResourceInfoInDB;
import org.folio.rest.jaxrs.model.HoldingsLoadingStatus;
import org.folio.rest.jaxrs.model.LoadStatusAttributes;
import org.folio.rest.jaxrs.model.LoadStatusNameEnum;
import org.folio.rest.util.template.RMAPITemplateContext;
import org.folio.service.holdings.exception.ProcessInProgressException;
import org.folio.service.holdings.message.DeltaReportCreatedMessage;
import org.folio.service.holdings.message.LoadFailedMessage;
import org.folio.service.holdings.message.LoadHoldingsMessage;
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
  private HoldingsRepository holdingsRepository;
  private HoldingsStatusRepository holdingsStatusRepository;
  private RetryStatusRepository retryStatusRepository;
  private TransactionIdRepository transactionIdRepository;
  private Vertx vertx;
  private final LoadServiceFacade loadServiceFacade;
  private long snapshotRetryDelay;
  private int snapshotRetryCount;
  private long loadHoldingsRetryDelay;
  private int loadHoldingsRetryCount;
  private int loadHoldingsTimeout;
  private static final List<HoldingChangeType> ADDED_OR_UPDATED_CHANGE_TYPES = Arrays.asList(
    HOLDING_ADDED,
    HOLDING_UPDATED,
    HOLDING_UPDATED_ADDED_COVERAGE,
    HOLDING_UPDATED_DELETED_COVERAGE);

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
    String tenantId = context.getOkapiData().getTenant();
    Future<Void> executeFuture = executeWithLock(START_LOADING_LOCK, () ->
      tryChangingStatusToInProgress(tenantId, getStatusPopulatingStagingArea())
        .thenCompose(o -> resetRetries(tenantId, snapshotRetryCount - 1))
        .thenAccept(o -> loadServiceFacade.createSnapshot(new ConfigurationMessage(context.getConfiguration(), tenantId)))
    );
    return mapVertxFuture(executeFuture);
  }

  @Override
  public CompletableFuture<List<HoldingInfoInDB>> getHoldingsByIds(List<ResourceInfoInDB> resourcesResult, String tenantId) {
    return holdingsRepository.findAllById(getTitleIdsAsList(resourcesResult), tenantId);
  }

  @Override
  public void saveHolding(HoldingsMessage holdings) {
    String tenantId = holdings.getTenantId();
    saveHoldings(holdings.getHoldingList(), Instant.now(), tenantId)
      .thenCompose(o -> holdingsStatusRepository.increaseImportedCount(holdings.getHoldingList().size(), 1, tenantId))
      .thenCompose(status -> {
          LoadStatusAttributes attributes = status.getData().getAttributes();
          if (hasLoadedLastPage(attributes, attributes.getImportedPages(), attributes.getTotalPages())) {
            return
              holdingsRepository.deleteBeforeTimestamp(ZonedDateTime.parse(status.getData().getAttributes().getStarted(), POSTGRES_TIMESTAMP_FORMATTER).toInstant(), tenantId)
                .thenCompose(o -> holdingsStatusRepository.update(getStatusCompleted(attributes.getTotalCount()), tenantId))
                .thenCompose(o -> transactionIdRepository.save(holdings.getTransactionId(), tenantId));
          }
          return CompletableFuture.completedFuture(null);
        }
      )
      .exceptionally(e -> {
        logger.error("Failed to save holdings", e);
        return null;
      });
  }

  @Override
  public void processChanges(DeltaReportMessage holdings) {
    String tenantId = holdings.getTenantId();
    processChanges(holdings.getHoldingList(), Instant.now(), tenantId)
      .thenCompose(o -> holdingsStatusRepository.increaseImportedCount(holdings.getHoldingList().size(), 1, tenantId))
      .thenCompose(status -> {
          LoadStatusAttributes attributes = status.getData().getAttributes();
          if (hasLoadedLastPage(attributes, attributes.getImportedPages(), attributes.getTotalPages())) {
            return
              holdingsStatusRepository.update(getStatusCompleted(attributes.getTotalCount()), tenantId)
                .thenCompose(o -> transactionIdRepository.save(holdings.getTransactionId(), tenantId));
          }
          return CompletableFuture.completedFuture(null);
        }
      )
      .exceptionally(e -> {
        logger.error("Failed to process changes", e);
        return null;
      });
  }

  private boolean hasLoadedLastPage(LoadStatusAttributes attributes, Integer importedPages, Integer totalPages) {
    return attributes.getStatus().getName() == LoadStatusNameEnum.IN_PROGRESS &&
      importedPages.equals(totalPages);
  }

  @Override
  public void snapshotCreated(SnapshotCreatedMessage message) {
    String tenantId = message.getTenantId();
    transactionIdRepository.getLastTransactionId(tenantId)
      .thenApply(previousTransactionId -> {
        boolean transactionIsAlreadyLoaded =
          message.getTransactionId()!= null && message.getTransactionId().equals(previousTransactionId);
        if(!transactionIsAlreadyLoaded){
          holdingsStatusRepository.update(getStatusLoadingHoldings(
            message.getTotalCount(), 0, message.getTotalPages(), 0), tenantId)
            .thenCompose(o -> resetRetries(tenantId, loadHoldingsRetryCount - 1))
            .thenAccept(o -> loadServiceFacade.loadHoldings(new LoadHoldingsMessage(message.getConfiguration(), tenantId,
              message.getTotalCount(), message.getTotalPages(), message.getTransactionId(), previousTransactionId)))
            .exceptionally(e -> {
              logger.error("Failed to create snapshot", e);
              return null;
            });
        }else{
          logger.info("Skipping loading snapshot, because transaction with id {} is already loaded", message.getTransactionId());
          holdingsStatusRepository.update(getStatusCompleted(0), tenantId);
        }
        return null;
      });
  }

  @Override
  public void snapshotFailed(SnapshotFailedMessage message) {
    String tenantId = message.getTenantId();
    setStatusToFailed(tenantId, message.getErrorMessage())
      .thenAccept(o ->
        retryAfterDelayIfAttemptsLeft(tenantId, snapshotRetryDelay, o2 ->
          executeWithLock(START_LOADING_LOCK, () ->
            tryChangingStatusToInProgress(tenantId, getStatusPopulatingStagingArea())
              .thenAccept(o3 -> loadServiceFacade.createSnapshot(new ConfigurationMessage(message.getConfiguration(), tenantId)))
              .exceptionally(e -> {
                logger.error("Failed to retry creating snapshot", e);
                return null;
              }))));
  }

  @Override
  public void deltaReportCreated(DeltaReportCreatedMessage message, Handler<AsyncResult<Void>> handler) {
    holdingsStatusRepository.update(getStatusLoadingHoldings(
      message.getTotalCount(), 0, message.getTotalPages(), 0), message.getTenantId())
      .thenAccept(o -> handler.handle(Future.succeededFuture(null)))
      .exceptionally(e -> {
        logger.error("Failed to create snapshot", e);
        handler.handle(Future.failedFuture(e));
        return null;
      });
  }

  @Override
  public void loadingFailed(LoadFailedMessage message) {
    String tenantId = message.getTenantId();
    setStatusToFailed(tenantId, message.getErrorMessage())
      .thenAccept(o ->
        retryAfterDelayIfAttemptsLeft(tenantId, loadHoldingsRetryDelay, o2 ->
          executeWithLock(START_LOADING_LOCK, () ->
            tryChangingStatusToInProgress(tenantId, getStatusLoadingHoldings(
              message.getTotalCount(), 0, message.getTotalPages(), 0))
              .thenCompose(o3 ->
                  transactionIdRepository.getLastTransactionId(tenantId)
                    .thenAccept(previousTransactionId -> loadServiceFacade.loadHoldings(new LoadHoldingsMessage(message.getConfiguration(), tenantId,
                      message.getTotalCount(), message.getTotalPages(), message.getTransactionId(), previousTransactionId))))
            .exceptionally(e -> {
              logger.error("Failed to retry loading holdings", e);
              return null;
            })
          )));
  }

  private CompletableFuture<Void> resetRetries(String tenantId, int retryCount) {
    return retryStatusRepository
      .get(tenantId)
      .thenCompose(status -> {
        if(status.getTimerId() != null) {
          vertx.cancelTimer(status.getTimerId());
        }
        return retryStatusRepository.update(new RetryStatus(retryCount, null), tenantId);
      });
  }
  private CompletableFuture<Void> tryChangingStatusToInProgress(String tenantId, HoldingsLoadingStatus newStatus){
    return holdingsStatusRepository.get(tenantId)
      .thenCompose(status -> {
        LoadStatusAttributes attributes = status.getData().getAttributes();
        logger.info("Current status is {}", attributes.getStatus().getName());
        if(attributes.getStatus().getName() != LoadStatusNameEnum.IN_PROGRESS || processTimedOut(status)){
          return holdingsStatusRepository.delete(tenantId)
            .thenCompose(o -> holdingsStatusRepository.save(newStatus, tenantId));
        }
        return failedFuture(new ProcessInProgressException("Loading status is already In Progress"));
      });
  }

  private CompletableFuture<Void> retryAfterDelayIfAttemptsLeft(String tenantId, long retryDelay, Handler<Long> retryHandler) {
    return retryStatusRepository.get(tenantId)
      .thenAccept(retryStatus -> {
        int retryAttempts = retryStatus.getRetryAttemptsLeft();
        if (retryAttempts >= 1) {
          long timerId = vertx.setTimer(retryDelay,
            retryHandler);
          retryStatusRepository.update(new RetryStatus(retryAttempts - 1 , timerId), tenantId);
        }
      })
    .exceptionally(e -> {
      logger.error("Failed during retry", e);
      return null;
    });
  }

  private CompletableFuture<Void> setStatusToFailed(String tenantId, String message) {
    return holdingsStatusRepository.update(getLoadStatusFailed(createError(message, null).getErrors()),
      tenantId)
      .exceptionally(e -> {
        logger.error("Failed to update status to failed", e);
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
            }
            else{
              responsePromise.complete();
            }
            lock.release();
          }
        );
      return null;
    });
    return responsePromise.future();
  }

  private List<String> getTitleIdsAsList(List<ResourceInfoInDB> resources){
    return mapItems(resources, dbResource -> dbResource.getId().getProviderIdPart() + "-"
      + dbResource.getId().getPackageIdPart() + "-" + dbResource.getId().getTitleIdPart());
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

  public CompletableFuture<Void> processChanges(List<HoldingInReport> holdings, Instant updatedAt, String tenantId) {
    Set<HoldingInfoInDB> holdingsToSave = getDbHoldingsByType(holdings, ADDED_OR_UPDATED_CHANGE_TYPES)
      .map(this::mapToHoldingInfoInDb)
      .collect(Collectors.toSet());
    Set<HoldingsId> holdingsToDelete = getDbHoldingsByType(holdings, Collections.singleton(HOLDING_DELETED))
      .map(holding -> new HoldingsId(holding.getTitleId(), holding.getPackageId(), holding.getVendorId()))
      .collect(Collectors.toSet());

    return holdingsRepository.saveAll(holdingsToSave, updatedAt, tenantId)
      .thenCompose(o -> holdingsRepository.deleteAll(holdingsToDelete, tenantId));
  }

  private Stream<HoldingInReport> getDbHoldingsByType(List<HoldingInReport> holdings, Collection<HoldingChangeType> matchingTypes) {
    return holdings.stream()
      .filter(holding -> matchingTypes.contains(holding.getChangeType()));
  }

  private HoldingInfoInDB mapToHoldingInfoInDb(HoldingInReport holding){
    return new HoldingInfoInDB(
      holding.getTitleId(),
      holding.getPackageId(),
      holding.getVendorId(),
      holding.getPublicationTitle(),
      holding.getPublisherName(),
      holding.getResourceType()
    );
  }

  private  <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
    Set<Object> seen = ConcurrentHashMap.newKeySet();
    return t -> seen.add(keyExtractor.apply(t));
  }

  private String getHoldingsId(Holding holding) {
    return holding.getVendorId() + "-" + holding.getPackageId() + "-" + holding.getTitleId();
  }

  private boolean processTimedOut(HoldingsLoadingStatus status) {
    String updatedString = status.getData().getAttributes().getUpdated();
    if(StringUtils.isEmpty(updatedString)){
      return true;
    }
    ZonedDateTime updated = ZonedDateTime.parse(updatedString, POSTGRES_TIMESTAMP_FORMATTER);
    return ZonedDateTime.now().isAfter(updated.plus(loadHoldingsTimeout, ChronoUnit.MILLIS));

  }
}
