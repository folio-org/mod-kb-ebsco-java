package org.folio.repository.holdings;

import static org.folio.common.FutureUtils.mapResult;
import static org.folio.common.FutureUtils.mapVertxFuture;
import static org.folio.common.ListUtils.createInsertPlaceholders;
import static org.folio.common.ListUtils.createPlaceholders;
import static org.folio.common.ListUtils.mapItems;
import static org.folio.db.DbUtils.executeInTransaction;
import static org.folio.repository.DbUtil.getHoldingsTableName;
import static org.folio.repository.DbUtil.mapColumn;
import static org.folio.repository.holdings.HoldingsTableConstants.DELETE_HOLDINGS_BY_ID_LIST;
import static org.folio.repository.holdings.HoldingsTableConstants.GET_HOLDINGS_BY_IDS;
import static org.folio.repository.holdings.HoldingsTableConstants.INSERT_OR_UPDATE_HOLDINGS_STATEMENT;
import static org.folio.repository.holdings.HoldingsTableConstants.REMOVE_FROM_HOLDINGS;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;

import io.vertx.core.AsyncResult;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.sql.UpdateResult;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.folio.rest.persist.PostgresClient;

@Component
public class HoldingsRepositoryImpl implements HoldingsRepository {
  private static final Logger LOG = LoggerFactory.getLogger(HoldingsRepositoryImpl.class);

  private static final int MAX_BATCH_SIZE = 200;
  private Vertx vertx;

  @Autowired
  public HoldingsRepositoryImpl(Vertx vertx) {
    this.vertx = vertx;
  }

  @Override
  public CompletableFuture<Void> saveAll(Set<HoldingInfoInDB> holdings, Instant updatedAt, String tenantId) {
    return executeInTransaction(tenantId, vertx, (postgresClient, connection) ->
      executeInBatches(holdings, batch -> saveHoldings(batch, updatedAt, tenantId, connection, postgresClient)));
  }

  private CompletableFuture<Void> saveHoldings(List<HoldingInfoInDB> holdings, Instant updatedAt, String tenantId,
                                               AsyncResult<SQLConnection> connection, PostgresClient postgresClient) {
    JsonArray parameters = createParameters(holdings, updatedAt);
    final String query = String.format(INSERT_OR_UPDATE_HOLDINGS_STATEMENT, getHoldingsTableName(tenantId),
      createInsertPlaceholders(3, holdings.size()));
    LOG.info("Do insert query = " + query);
    Promise<UpdateResult> promise = Promise.promise();
    postgresClient.execute(connection, query, parameters, promise);
    return mapVertxFuture(promise.future()).thenApply(result -> null);
  }

  @Override
  public CompletableFuture<Void> deleteBeforeTimestamp(Instant timestamp, String tenantId){
    final String query = String.format(REMOVE_FROM_HOLDINGS, getHoldingsTableName(tenantId), timestamp.toString());
    LOG.info("Do delete query = " + query);
    PostgresClient postgresClient = PostgresClient.getInstance(vertx, tenantId);
    Promise<UpdateResult> promise = Promise.promise();
    postgresClient.execute(query, promise);
    return mapVertxFuture(promise.future()).thenApply(result -> null);
  }

  @Override
  public CompletableFuture<List<HoldingInfoInDB>> findAllById(List<String> resourceIds, String tenantId) {
    final String resourceIdString = resourceIds.isEmpty() ? "''" :
      resourceIds.stream().map(id -> "'" + id.concat("'")).collect(Collectors.joining(","));
    final String query = String.format(GET_HOLDINGS_BY_IDS, getHoldingsTableName(tenantId), resourceIdString);
    LOG.info("Do select query = " + query);
    PostgresClient postgresClient = PostgresClient.getInstance(vertx, tenantId);
    Promise<ResultSet> promise = Promise.promise();
    postgresClient.select(query, promise);
    return mapResult(promise.future(), this::mapHoldings);
  }

  @Override
  public CompletableFuture<Void> deleteAll(Set<HoldingsId> holdings, String tenantId) {
    return executeInTransaction(tenantId, vertx, (postgresClient, connection) ->
      executeInBatches(holdings, batch -> deleteHoldings(batch, tenantId, connection, postgresClient)));
  }

  private CompletableFuture<Void> deleteHoldings(List<HoldingsId> holdings, String tenantId,
                                               AsyncResult<SQLConnection> connection, PostgresClient postgresClient) {
    JsonArray parameters = createHoldingsIdParameters(holdings);
    final String query = String.format(DELETE_HOLDINGS_BY_ID_LIST, getHoldingsTableName(tenantId),
      createPlaceholders(holdings.size()));
    LOG.info("Do delete query = " + query);
    Promise<UpdateResult> promise = Promise.promise();
    postgresClient.execute(connection, query, parameters, promise);
    return mapVertxFuture(promise.future()).thenApply(result -> null);
  }

  /**
   * Splits items into batches and sequentially executes batchOperation on each batch
   * @param <T> Type of process items
   * @param items items to process in batches
   * @param batchOperation operation to execute on each batch
   * @return future that will be completed when all batches are successfully processed
   */
  private <T> CompletableFuture<Void> executeInBatches(Set<T> items, Function<List<T>, CompletableFuture<Void>> batchOperation) {
    List<List<T>> batches = Lists.partition(Lists.newArrayList(items), HoldingsRepositoryImpl.MAX_BATCH_SIZE);
    CompletableFuture<Void> future = CompletableFuture.completedFuture(null);
    for (List<T> batch : batches) {
      future = future.thenCompose(o -> batchOperation.apply(batch));
    }
    return future;
  }

  private List<HoldingInfoInDB> mapHoldings(ResultSet resultSet) {
    return mapItems(resultSet.getRows(), row -> mapColumn(row, "jsonb", HoldingInfoInDB.class).orElse(null));
  }

  private String getHoldingsId(HoldingInfoInDB holding) {
    return holding.getVendorId() + "-" + holding.getPackageId() + "-" + holding.getTitleId();
  }

  private JsonArray createParameters(List<HoldingInfoInDB> holdings, Instant updatedAt) {
    JsonArray params = new JsonArray();
    holdings.forEach(holding -> {
      params.add(getHoldingsId(holding));
      params.add(Json.encode(holding));
      params.add(updatedAt);
    });
    return params;
  }
  private JsonArray createHoldingsIdParameters(List<HoldingsId> ids) {
    return new JsonArray(
      ids.stream()
      .map(id -> id.getVendorId() + "-" + id.getPackageId() + "-" + id.getTitleId())
      .collect(Collectors.toList())
    );
  }

}
