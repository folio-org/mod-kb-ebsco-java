package org.folio.repository.holdings;

import static java.util.Collections.singleton;

import static org.folio.common.FunctionUtils.nothing;
import static org.folio.common.ListUtils.createInsertPlaceholders;
import static org.folio.common.ListUtils.mapItems;
import static org.folio.db.DbUtils.createParams;
import static org.folio.db.DbUtils.executeInTransaction;
import static org.folio.repository.DbUtil.getHoldingsTableName;
import static org.folio.repository.DbUtil.mapRow;
import static org.folio.repository.holdings.HoldingsTableConstants.DELETE_BY_PK_HOLDINGS;
import static org.folio.repository.holdings.HoldingsTableConstants.DELETE_OLD_RECORDS_BY_CREDENTIALS_ID;
import static org.folio.repository.holdings.HoldingsTableConstants.GET_BY_PK_HOLDINGS;
import static org.folio.repository.holdings.HoldingsTableConstants.INSERT_OR_UPDATE_HOLDINGS;
import static org.folio.util.FutureUtils.mapResult;
import static org.folio.util.FutureUtils.mapVertxFuture;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import io.vertx.core.AsyncResult;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.sql.UpdateResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.folio.rest.persist.PostgresClient;
import org.folio.rest.util.IdParser;

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
  public CompletableFuture<Void> saveAll(Set<HoldingInfoInDB> holdings, Instant updatedAt, String credentialsId, String tenantId) {
    return executeInTransaction(tenantId, vertx, (postgresClient, connection) ->
      executeInBatches(holdings, batch -> saveHoldings(batch, updatedAt, credentialsId, tenantId, connection, postgresClient)));
  }

  private CompletableFuture<Void> saveHoldings(List<HoldingInfoInDB> holdings, Instant updatedAt, String credentialsId, String tenantId,
                                               AsyncResult<SQLConnection> connection, PostgresClient postgresClient) {
    final JsonArray parameters = createParameters(credentialsId, holdings, updatedAt);
    final String query = String.format(INSERT_OR_UPDATE_HOLDINGS, getHoldingsTableName(tenantId),
      createInsertPlaceholders(9, holdings.size()));
    LOG.info("Do insert query = " + query);
    Promise<UpdateResult> promise = Promise.promise();
    postgresClient.execute(connection, query, parameters, promise);
    return mapVertxFuture(promise.future()).thenApply(nothing());
  }

  @Override
  public CompletableFuture<Void> deleteBeforeTimestamp(Instant timestamp, String credentialsId, String tenantId){
    final String query = String.format(DELETE_OLD_RECORDS_BY_CREDENTIALS_ID, getHoldingsTableName(tenantId), timestamp.toString());
    LOG.info("Do delete query = " + query);
    Promise<UpdateResult> promise = Promise.promise();
    final JsonArray params = createParams(singleton(credentialsId));
    pgClient(tenantId).execute(query, params, promise);
    return mapVertxFuture(promise.future()).thenApply(nothing());
  }

  @Override
  public CompletableFuture<List<HoldingInfoInDB>> findAllById(List<String> resourceIds, String credentialsId, String tenantId) {
    if(resourceIds.isEmpty()){
      return CompletableFuture.completedFuture(new ArrayList<>());
    }
    final String resourceIdString = getHoldingsPkKeys(credentialsId, resourceIds);
    final String query = String.format(GET_BY_PK_HOLDINGS, getHoldingsTableName(tenantId), resourceIdString);
    LOG.info("Do select query = " + query);
    Promise<ResultSet> promise = Promise.promise();
    pgClient(tenantId).select(query, promise);
    return mapResult(promise.future(), this::mapHoldings);
  }

  @Override
  public CompletableFuture<Void> deleteAll(Set<HoldingsId> holdings, String credentialsId, String tenantId) {
    return executeInTransaction(tenantId, vertx, (postgresClient, connection) ->
      executeInBatches(holdings, batch -> deleteHoldings(batch, credentialsId,tenantId, connection, postgresClient)));
  }

  private CompletableFuture<Void> deleteHoldings(List<HoldingsId> holdings, String credentialsId, String tenantId,
                                               AsyncResult<SQLConnection> connection, PostgresClient postgresClient) {
    final String parameters = getHoldingsPkKeys(credentialsId, mapItems(holdings, IdParser::getResourceId));
    final String query = String.format(DELETE_BY_PK_HOLDINGS, getHoldingsTableName(tenantId), parameters);
    LOG.info("Do delete query = " + query);
    Promise<UpdateResult> promise = Promise.promise();
    postgresClient.execute(connection, query, promise);
    return mapVertxFuture(promise.future()).thenApply(nothing());
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
    return mapItems(resultSet.getRows(), row -> mapRow(row, HoldingInfoInDB.class).orElse(null));
  }

  private String getHoldingsId(HoldingInfoInDB holding) {
    return holding.getVendorId() + "-" + holding.getPackageId() + "-" + holding.getTitleId();
  }

  private JsonArray createParameters(String credentialsId, List<HoldingInfoInDB> holdings, Instant updatedAt) {
    JsonArray params = new JsonArray();
    holdings.forEach(holding -> {
      params.add(getHoldingsId(holding));
      params.add(credentialsId);
      params.add(holding.getVendorId());
      params.add(holding.getPackageId());
      params.add(holding.getTitleId());
      params.add(holding.getResourceType());
      params.add(holding.getPublisherName());
      params.add(holding.getPublicationTitle());
      params.add(updatedAt);
    });
    return params;
  }

  private String getHoldingsPkKeys(String credentialsId, List<String> resourceIds) {
    return resourceIds.stream()
      .map(id -> "('" + credentialsId + "', '"+ id.concat("')"))
      .collect(Collectors.joining(","));
  }

  private PostgresClient pgClient(String tenantId) {
    return PostgresClient.getInstance(vertx, tenantId);
  }
}
