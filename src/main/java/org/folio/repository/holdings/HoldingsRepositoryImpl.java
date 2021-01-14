package org.folio.repository.holdings;

import static org.folio.common.FunctionUtils.nothing;
import static org.folio.common.ListUtils.createInsertPlaceholders;
import static org.folio.common.ListUtils.mapItems;
import static org.folio.common.LogUtils.logDeleteQueryDebugLevel;
import static org.folio.common.LogUtils.logDeleteQueryInfoLevel;
import static org.folio.common.LogUtils.logInsertQueryDebugLevel;
import static org.folio.common.LogUtils.logSelectQueryInfoLevel;
import static org.folio.db.DbUtils.executeInTransaction;
import static org.folio.repository.DbUtil.getHoldingsTableName;
import static org.folio.repository.DbUtil.prepareQuery;
import static org.folio.repository.holdings.HoldingsTableConstants.DELETE_BY_PK_HOLDINGS;
import static org.folio.repository.holdings.HoldingsTableConstants.DELETE_OLD_RECORDS_BY_CREDENTIALS_ID;
import static org.folio.repository.holdings.HoldingsTableConstants.GET_BY_PK_HOLDINGS;
import static org.folio.repository.holdings.HoldingsTableConstants.INSERT_OR_UPDATE_HOLDINGS;
import static org.folio.repository.holdings.HoldingsTableConstants.PACKAGE_ID_COLUMN;
import static org.folio.repository.holdings.HoldingsTableConstants.PUBLICATION_TITLE_COLUMN;
import static org.folio.repository.holdings.HoldingsTableConstants.PUBLISHER_NAME_COLUMN;
import static org.folio.repository.holdings.HoldingsTableConstants.RESOURCE_TYPE_COLUMN;
import static org.folio.repository.holdings.HoldingsTableConstants.TITLE_ID_COLUMN;
import static org.folio.repository.holdings.HoldingsTableConstants.VENDOR_ID_COLUMN;
import static org.folio.util.FutureUtils.mapResult;
import static org.folio.util.FutureUtils.mapVertxFuture;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import io.vertx.core.AsyncResult;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import org.springframework.stereotype.Component;

import org.folio.db.RowSetUtils;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.persist.SQLConnection;
import org.folio.rest.util.IdParser;

@Component
public class HoldingsRepositoryImpl implements HoldingsRepository {

  private static final Logger LOG = LoggerFactory.getLogger(HoldingsRepositoryImpl.class);

  private static final int MAX_BATCH_SIZE = 200;

  private final Vertx vertx;

  public HoldingsRepositoryImpl(Vertx vertx) {
    this.vertx = vertx;
  }

  @Override
  public CompletableFuture<Void> saveAll(Set<DbHoldingInfo> holdings, OffsetDateTime updatedAt, UUID credentialsId,
                                         String tenantId) {
    return executeInTransaction(tenantId, vertx, (postgresClient, connection) ->
      executeInBatches(holdings,
        batch -> saveHoldings(batch, updatedAt, credentialsId, tenantId, connection, postgresClient))
    );
  }

  @Override
  public CompletableFuture<Void> deleteBeforeTimestamp(OffsetDateTime timestamp, UUID credentialsId, String tenantId) {
    final String query = prepareQuery(DELETE_OLD_RECORDS_BY_CREDENTIALS_ID, getHoldingsTableName(tenantId));
    final Tuple params = Tuple.of(credentialsId, timestamp);
    logDeleteQueryInfoLevel(LOG, query, params);
    Promise<RowSet<Row>> promise = Promise.promise();
    pgClient(tenantId).execute(query, params, promise);
    return mapVertxFuture(promise.future()).thenApply(nothing());
  }

  @Override
  public CompletableFuture<List<DbHoldingInfo>> findAllById(List<String> resourceIds, UUID credentialsId, String tenantId) {
    if (resourceIds.isEmpty()) {
      return CompletableFuture.completedFuture(new ArrayList<>());
    }
    final String resourceIdString = getHoldingsPkKeys(credentialsId, resourceIds);
    final String query = prepareQuery(GET_BY_PK_HOLDINGS, getHoldingsTableName(tenantId), resourceIdString);
    logSelectQueryInfoLevel(LOG, query);
    Promise<RowSet<Row>> promise = Promise.promise();
    pgClient(tenantId).select(query, promise);
    return mapResult(promise.future(), this::mapHoldings);
  }

  @Override
  public CompletableFuture<Void> deleteAll(Set<HoldingsId> holdings, UUID credentialsId, String tenantId) {
    return executeInTransaction(tenantId, vertx, (postgresClient, connection) ->
      executeInBatches(holdings, batch -> deleteHoldings(batch, credentialsId, tenantId, connection, postgresClient))
    );
  }

  private CompletableFuture<Void> saveHoldings(List<DbHoldingInfo> holdings, OffsetDateTime updatedAt, UUID credentialsId,
                                               String tenantId, AsyncResult<SQLConnection> connection,
                                               PostgresClient postgresClient) {
    final Tuple parameters = createParameters(credentialsId, holdings, updatedAt);
    final String query = prepareQuery(INSERT_OR_UPDATE_HOLDINGS, getHoldingsTableName(tenantId),
      createInsertPlaceholders(9, holdings.size()));
    logInsertQueryDebugLevel(LOG, query, parameters);
    Promise<RowSet<Row>> promise = Promise.promise();
    postgresClient.execute(connection, query, parameters, promise);
    return mapVertxFuture(promise.future()).thenApply(nothing());
  }

  private CompletableFuture<Void> deleteHoldings(List<HoldingsId> holdings, UUID credentialsId, String tenantId,
                                                 AsyncResult<SQLConnection> connection, PostgresClient postgresClient) {
    final String parameters = getHoldingsPkKeys(credentialsId, mapItems(holdings, IdParser::getResourceId));
    final String query = prepareQuery(DELETE_BY_PK_HOLDINGS, getHoldingsTableName(tenantId), parameters);
    logDeleteQueryDebugLevel(LOG, query);
    Promise<RowSet<Row>> promise = Promise.promise();
    postgresClient.execute(connection, query, promise);
    return mapVertxFuture(promise.future()).thenApply(nothing());
  }

  /**
   * Splits items into batches and sequentially executes batchOperation on each batch
   *
   * @param <T>            Type of process items
   * @param items          items to process in batches
   * @param batchOperation operation to execute on each batch
   * @return future that will be completed when all batches are successfully processed
   */
  private <T> CompletableFuture<Void> executeInBatches(Set<T> items,
                                                       Function<List<T>, CompletableFuture<Void>> batchOperation) {
    List<List<T>> batches = Lists.partition(Lists.newArrayList(items), HoldingsRepositoryImpl.MAX_BATCH_SIZE);
    CompletableFuture<Void> future = CompletableFuture.completedFuture(null);
    for (List<T> batch : batches) {
      future = future.thenCompose(o -> batchOperation.apply(batch));
    }
    return future;
  }

  private List<DbHoldingInfo> mapHoldings(RowSet<Row> resultSet) {
    return RowSetUtils.mapItems(resultSet, row -> DbHoldingInfo.builder()
      .titleId(row.getString(TITLE_ID_COLUMN))
      .packageId(row.getString(PACKAGE_ID_COLUMN))
      .vendorId(row.getString(VENDOR_ID_COLUMN))
      .publicationTitle(row.getString(PUBLICATION_TITLE_COLUMN))
      .publisherName(row.getString(PUBLISHER_NAME_COLUMN))
      .resourceType(row.getString(RESOURCE_TYPE_COLUMN))
      .build()
    );
  }

  private String getHoldingsId(DbHoldingInfo holding) {
    return holding.getVendorId() + "-" + holding.getPackageId() + "-" + holding.getTitleId();
  }

  private Tuple createParameters(UUID credentialsId, List<DbHoldingInfo> holdings, OffsetDateTime updatedAt) {
    Tuple params = Tuple.tuple();
    holdings.forEach(holding -> {
      params.addValue(credentialsId);
      params.addValue(getHoldingsId(holding));
      params.addValue(holding.getVendorId());
      params.addValue(holding.getPackageId());
      params.addValue(holding.getTitleId());
      params.addValue(holding.getResourceType());
      params.addValue(holding.getPublisherName());
      params.addValue(holding.getPublicationTitle());
      params.addValue(updatedAt);
    });
    return params;
  }

  private String getHoldingsPkKeys(UUID credentialsId, List<String> resourceIds) {
    return resourceIds.stream()
      .collect(Collectors.joining(",", "('" + credentialsId + "', '", "')"));
  }

  private PostgresClient pgClient(String tenantId) {
    return PostgresClient.getInstance(vertx, tenantId);
  }
}
