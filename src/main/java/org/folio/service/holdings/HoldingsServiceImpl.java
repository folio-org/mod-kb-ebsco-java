package org.folio.service.holdings;

import static org.folio.common.FutureUtils.failedFuture;
import static org.folio.common.ListUtils.mapItems;
import static org.folio.repository.holdings.status.HoldingsLoadingStatusFactory.getLoadStatusFailed;
import static org.folio.repository.holdings.status.HoldingsLoadingStatusFactory.getStatusCompleted;
import static org.folio.repository.holdings.status.HoldingsLoadingStatusFactory.getStatusLoadingHoldings;
import static org.folio.repository.holdings.status.HoldingsLoadingStatusFactory.getStatusPopulatingStagingArea;
import static org.folio.rest.util.ErrorUtil.createError;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.folio.holdingsiq.model.Holding;
import org.folio.repository.holdings.HoldingInfoInDB;
import org.folio.repository.holdings.HoldingsRepository;
import org.folio.repository.holdings.status.HoldingsStatusRepository;
import org.folio.repository.holdings.status.RetryStatus;
import org.folio.repository.holdings.status.RetryStatusRepository;
import org.folio.repository.resources.ResourceInfoInDB;
import org.folio.rest.jaxrs.model.HoldingsLoadingStatus;
import org.folio.rest.jaxrs.model.LoadStatusAttributes;
import org.folio.rest.jaxrs.model.LoadStatusNameEnum;
import org.folio.rest.util.template.RMAPITemplateContext;
import org.folio.service.holdings.message.LoadFailedMessage;
import org.folio.service.holdings.message.LoadHoldingsMessage;
import org.folio.service.holdings.message.SnapshotCreatedMessage;
import org.folio.service.holdings.message.SnapshotFailedMessage;
import org.glassfish.jersey.internal.util.Producer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.shareddata.Lock;

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
  private Vertx vertx;
  private final LoadServiceFacade loadServiceFacade;
  private long snapshotRetryDelay;
  private int snapshotRetryCount;
  private long loadHoldingsRetryDelay;
  private int loadHoldingsRetryCount;

  @Autowired
  public HoldingsServiceImpl(Vertx vertx, HoldingsRepository holdingsRepository,
                             @Value("${holdings.snapshot.retry.delay}") long snapshotRetryDelay,
                             @Value("${holdings.snapshot.retry.count}") int snapshotRetryCount,
                             @Value("${holdings.snapshot.retry.delay}") long loadHoldingsRetryDelay,
                             @Value("${holdings.snapshot.retry.count}") int loadHoldingsRetryCount,
                             HoldingsStatusRepository holdingsStatusRepository,
                             RetryStatusRepository retryStatusRepository) {
    this.vertx = vertx;
    this.holdingsRepository = holdingsRepository;
    this.holdingsStatusRepository = holdingsStatusRepository;
    this.retryStatusRepository = retryStatusRepository;
    this.snapshotRetryDelay = snapshotRetryDelay;
    this.snapshotRetryCount = snapshotRetryCount;
    this.loadHoldingsRetryDelay = loadHoldingsRetryDelay;
    this.loadHoldingsRetryCount = loadHoldingsRetryCount;
    this.loadServiceFacade = LoadServiceFacade.createProxy(vertx, HoldingConstants.LOAD_FACADE_ADDRESS);
  }

  @Override
  public void loadHoldings(RMAPITemplateContext context) {
    String tenantId = context.getOkapiData().getTenant();
    executeWithLock(START_LOADING_LOCK, () ->
        tryChangingStatusToInProgress(tenantId, getStatusPopulatingStagingArea())
          .thenCompose(o -> resetRetries(tenantId, snapshotRetryCount))
          .thenAccept(o -> {
            logger.error("Starting loading holdings");
            loadServiceFacade.createSnapshot(new ConfigurationMessage(context.getConfiguration(), tenantId));
          })
          .exceptionally(e -> {
            logger.error("Failed to start loading holdings", e);
            return null;
          }));
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
          if (status.getData().getAttributes().getStatus().getName() == LoadStatusNameEnum.IN_PROGRESS &&
            attributes.getImportedPages().equals(attributes.getTotalPages())) {
            return
              holdingsRepository.deleteBeforeTimestamp(ZonedDateTime.parse(status.getData().getAttributes().getStarted(), POSTGRES_TIMESTAMP_FORMATTER).toInstant(), tenantId)
                .thenCompose(o -> holdingsStatusRepository.update(getStatusCompleted(attributes.getTotalCount()), tenantId));
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
  public void snapshotCreated(SnapshotCreatedMessage message) {
    String tenantId = message.getTenantId();
    holdingsStatusRepository.update(getStatusLoadingHoldings(
      message.getTotalCount(), 0, message.getTotalPages(), 0), tenantId)
      .thenCompose(o -> resetRetries(tenantId, loadHoldingsRetryCount))
      .thenAccept(o ->
        loadServiceFacade.loadHoldings(new LoadHoldingsMessage(message.getConfiguration(), tenantId, message.getTotalCount(), message.getTotalPages())))
      .exceptionally(e -> {
        logger.error("Failed to create snapshot", e);
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
  public void loadingFailed(LoadFailedMessage message) {
    String tenantId = message.getTenantId();
    setStatusToFailed(tenantId, message.getErrorMessage())
      .thenAccept(o ->
        retryAfterDelayIfAttemptsLeft(tenantId, loadHoldingsRetryDelay, o2 ->
          executeWithLock(START_LOADING_LOCK, () ->
            tryChangingStatusToInProgress(tenantId, getStatusLoadingHoldings(
              message.getTotalCount(), 0, message.getTotalPages(), 0))
              .thenAccept(o3 -> loadServiceFacade.loadHoldings(new LoadHoldingsMessage(message.getConfiguration(), tenantId,
                message.getTotalCount(), message.getTotalPages())))
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
        if(attributes.getStatus().getName() != LoadStatusNameEnum.IN_PROGRESS){
          return holdingsStatusRepository.update(newStatus, tenantId);
        }
        return failedFuture(new IllegalStateException("Loading status is already In Progress"));
      });
  }

  private CompletableFuture<Void> retryAfterDelayIfAttemptsLeft(String tenantId, long retryDelay, Handler<Long> retryHandler) {
    return retryStatusRepository.get(tenantId)
      .thenAccept(retryStatus -> {
        int retryAttempts = retryStatus.getRetryAttemptsLeft();
        if (retryAttempts > 1) {
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
    Future<Lock> future = Future.future();
    Future<Void> responseFuture = Future.future();
    vertx.sharedData().getLock(lockName, future);
    future.map(lock -> {
      futureProducer.call()
        .whenComplete((o, throwable) -> {
            if (throwable != null) {
              responseFuture.fail(throwable);
            }
            else{
              responseFuture.complete();
            }
            lock.release();
          }
        );
      return null;
    });
    return responseFuture;
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

  private  <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
    Set<Object> seen = ConcurrentHashMap.newKeySet();
    return t -> seen.add(keyExtractor.apply(t));
  }

  private String getHoldingsId(Holding holding) {
    return holding.getVendorId() + "-" + holding.getPackageId() + "-" + holding.getTitleId();
  }
}
