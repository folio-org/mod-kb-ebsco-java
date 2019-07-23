package org.folio.service.holdings;

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
import org.folio.rest.jaxrs.model.LoadStatusAttributes;
import org.folio.rest.util.template.RMAPITemplateContext;
import org.folio.service.holdings.message.LoadFailedMessage;
import org.folio.service.holdings.message.SnapshotCreatedMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

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
    holdingsStatusRepository.update(getStatusPopulatingStagingArea(), tenantId)
      .thenCompose(o -> retryStatusRepository.update(new RetryStatus(snapshotRetryCount, null), tenantId))
      .thenAccept(o -> loadServiceFacade.createSnapshot(new ConfigurationMessage(context.getConfiguration(), tenantId)));
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
          if (attributes.getImportedPages().equals(attributes.getTotalPages())) {
            return
              holdingsRepository.deleteBeforeTimestamp(ZonedDateTime.parse(status.getData().getAttributes().getStarted(), POSTGRES_TIMESTAMP_FORMATTER).toInstant(), tenantId)
                .thenCompose(o -> holdingsStatusRepository.update(getStatusCompleted(attributes.getTotalCount()), tenantId));
          }
          return CompletableFuture.completedFuture(null);
        }
      )
      .exceptionally(e -> {
        logger.error("Failed to save holdings", e);
        setStatusToFailed(tenantId, e.getMessage());
        return null;
      });
  }

  @Override
  public void snapshotCreated(SnapshotCreatedMessage message) {
    String tenantId = message.getTenantId();
    holdingsStatusRepository.update(getStatusLoadingHoldings(
      message.getTotalCount(), 0, message.getTotalPages(), 0), tenantId)
      .thenCompose(o -> retryStatusRepository.update(new RetryStatus(loadHoldingsRetryCount, null), tenantId))
      .thenAccept(o ->
        loadServiceFacade.loadHoldings(new ConfigurationMessage(message.getConfiguration(), tenantId)))
      .exceptionally(e -> {
        logger.error("Failed to create snapshot", e);
        setStatusToFailed(tenantId, e.getMessage());
        return null;
      });
  }

  @Override
  public void snapshotFailed(LoadFailedMessage message) {
    setStatusToFailed(message.getTenantId(), message.getErrorMessage());
    retryIfAttemptsLeft(message.getTenantId(), snapshotRetryDelay, o -> loadServiceFacade.createSnapshot(new ConfigurationMessage(message.getConfiguration(), message.getTenantId())));
  }

  @Override
  public void loadingFailed(LoadFailedMessage message) {
    setStatusToFailed(message.getTenantId(), message.getErrorMessage());
    retryIfAttemptsLeft(message.getTenantId(), loadHoldingsRetryDelay, o -> loadServiceFacade.loadHoldings(new ConfigurationMessage(message.getConfiguration(), message.getTenantId())));
  }

  private void retryIfAttemptsLeft(String tenantId, long retryDelay, Handler<Long> retryHandler) {
    retryStatusRepository.get(tenantId)
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

  private void setStatusToFailed(String tenantId, String message) {
    holdingsStatusRepository.update(getLoadStatusFailed(createError(message, null).getErrors()),
      tenantId)
      .exceptionally(e -> {
        logger.error("Failed to update status to failed", e);
        return null;
      });
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
