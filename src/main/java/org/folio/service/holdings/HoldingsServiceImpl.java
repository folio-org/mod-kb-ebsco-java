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
import org.folio.repository.resources.ResourceInfoInDB;
import org.folio.rest.jaxrs.model.LoadStatusAttributes;
import org.folio.rest.util.template.RMAPITemplateContext;
import org.folio.service.holdings.message.LoadFailedMessage;
import org.folio.service.holdings.message.SnapshotCreatedMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
  private final LoadServiceFacade loadServiceFacade;

  @Autowired
  public HoldingsServiceImpl(Vertx vertx, HoldingsRepository holdingsRepository,
                             HoldingsStatusRepository holdingsStatusRepository) {
    this.holdingsRepository = holdingsRepository;
    this.holdingsStatusRepository = holdingsStatusRepository;
    this.loadServiceFacade = LoadServiceFacade.createProxy(vertx, HoldingConstants.LOAD_FACADE_ADDRESS);
  }

  @Override
  public void loadHoldings(RMAPITemplateContext context) {
    String tenant = context.getOkapiData().getTenant();
    holdingsStatusRepository.update(getStatusPopulatingStagingArea(), tenant)
      .thenAccept(o -> loadServiceFacade.createSnapshot(new ConfigurationMessage(context.getConfiguration(), tenant)));
  }

  @Override
  public CompletableFuture<List<HoldingInfoInDB>> getHoldingsByIds(List<ResourceInfoInDB> resourcesResult, String tenant) {
    return holdingsRepository.findAllById(getTitleIdsAsList(resourcesResult), tenant);
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
        logger.error(e.getMessage(), e);
        setStatusToFailed(tenantId, e.getMessage());
        return null;
      });
  }

  @Override
  public void snapshotCreated(SnapshotCreatedMessage message) {
    String tenantId = message.getTenantId();
    holdingsStatusRepository.update(getStatusLoadingHoldings(
      message.getTotalCount(), 0, message.getTotalPages(), 0), tenantId)
      .thenAccept(o ->
        loadServiceFacade.loadHoldings(new ConfigurationMessage(message.getConfiguration(), tenantId)))
      .exceptionally(e -> {
        logger.error(e.getMessage(), e);
        setStatusToFailed(tenantId, e.getMessage());
        return null;
      });
  }

  @Override
  public void snapshotFailed(LoadFailedMessage message) {
    setStatusToFailed(message.getTenantId(), message.getErrorMessage());
  }

  @Override
  public void loadingFailed(LoadFailedMessage message) {
    setStatusToFailed(message.getTenantId(), message.getErrorMessage());
  }

  private void setStatusToFailed(String tenantId, String message) {
    holdingsStatusRepository.update(getLoadStatusFailed(createError(message, null).getErrors()),
      tenantId)
      .exceptionally(e -> {
        logger.error(e.getMessage(), e);
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
