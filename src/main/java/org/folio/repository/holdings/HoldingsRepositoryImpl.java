package org.folio.repository.holdings;

import static org.folio.common.FutureUtils.mapResult;
import static org.folio.common.FutureUtils.mapVertxFuture;
import static org.folio.common.ListUtils.createInsertPlaceholders;
import static org.folio.common.ListUtils.mapItems;
import static org.folio.repository.DbUtil.executeInTransaction;
import static org.folio.repository.DbUtil.getHoldingsTableName;
import static org.folio.repository.DbUtil.mapColumn;
import static org.folio.repository.holdings.HoldingsTableConstants.GET_HOLDINGS_BY_IDS;
import static org.folio.repository.holdings.HoldingsTableConstants.INSERT_OR_UPDATE_HOLDINGS_STATEMENT;
import static org.folio.repository.holdings.HoldingsTableConstants.REMOVE_FROM_HOLDINGS;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.folio.rest.persist.PostgresClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.sql.UpdateResult;

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
    return executeInTransaction(tenantId, vertx, (postgresClient, connection) -> {
      List<List<HoldingInfoInDB>> batches = Lists.partition(Lists.newArrayList(holdings), MAX_BATCH_SIZE);
      CompletableFuture<Void> future = CompletableFuture.completedFuture(null);
      for (List<HoldingInfoInDB> batch : batches) {
        future = future.thenCompose(o ->
          saveHoldings(batch, updatedAt, tenantId, connection, postgresClient));
      }
      return future;
    });
  }

  private CompletableFuture<Void> saveHoldings(List<HoldingInfoInDB> holdings, Instant updatedAt, String tenantId,
                                               AsyncResult<SQLConnection> connection, PostgresClient postgresClient) {
    JsonArray parameters = createParameters(holdings, updatedAt);
    final String query = String.format(INSERT_OR_UPDATE_HOLDINGS_STATEMENT, getHoldingsTableName(tenantId),
      createInsertPlaceholders(3, holdings.size()));
    Future<UpdateResult> future = Future.future();
    postgresClient.execute(connection, query, parameters, future);
    return mapVertxFuture(future).thenApply(result -> null);
  }

  @Override
  public CompletableFuture<Void> deleteBeforeTimestamp(Instant timestamp, String tenantId){
    final String query = String.format(REMOVE_FROM_HOLDINGS, getHoldingsTableName(tenantId), timestamp.toString());
    LOG.info("Do delete query = " + query);
    PostgresClient postgresClient = PostgresClient.getInstance(vertx, tenantId);
    Future<UpdateResult> future = Future.future();
    postgresClient.execute(query, future);
    return mapVertxFuture(future).thenApply(result -> null);
  }

  @Override
  public CompletableFuture<List<HoldingInfoInDB>> findAllById(List<String> resourceIds, String tenantId) {
    final String resourceIdString = resourceIds.isEmpty() ? "''" :
      resourceIds.stream().map(id -> "'" + id.concat("'")).collect(Collectors.joining(","));
    final String query = String.format(GET_HOLDINGS_BY_IDS, getHoldingsTableName(tenantId), resourceIdString);
    LOG.info("Do select query = " + query);
    PostgresClient postgresClient = PostgresClient.getInstance(vertx, tenantId);
    Future<ResultSet> future = Future.future();
    postgresClient.select(query, future);
    return mapResult(future, this::mapHoldings);
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
}
