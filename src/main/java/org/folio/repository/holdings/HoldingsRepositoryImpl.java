package org.folio.repository.holdings;

import static org.folio.common.FutureUtils.mapVertxFuture;
import static org.folio.repository.DbUtil.executeInTransaction;
import static org.folio.repository.DbUtil.getTableName;
import static org.folio.tag.repository.resources.HoldingsTableConstants.HOLDINGS_TABLE;
import static org.folio.tag.repository.resources.HoldingsTableConstants.INSERT_OR_UPDATE_HOLDINGS_STATEMENT;
import static org.folio.tag.repository.resources.HoldingsTableConstants.REMOVE_FROM_HOLDINGS;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.google.common.collect.Lists;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
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
  public CompletableFuture<Void> saveHoldings(List<DbHolding> holdings, String tenantId) {
    return executeInTransaction(tenantId, vertx, (postgresClient, connection) -> {
      List<List<DbHolding>> batches = Lists.partition(holdings, MAX_BATCH_SIZE);
      CompletableFuture<Void> future = CompletableFuture.completedFuture(null);
      for (List<DbHolding> batch : batches) {
        future = future.thenCompose(o ->
          saveHoldings(batch, tenantId, connection, postgresClient));
      }
      return future;
    });
  }

  private CompletableFuture<Void> saveHoldings(List<DbHolding> holdings, String tenantId,
                                               AsyncResult<SQLConnection> connection, PostgresClient postgresClient) {
    String placeholders = createInsertPlaceholders(holdings);
    JsonArray parameters = createParameters(holdings);
    final String query = String.format(INSERT_OR_UPDATE_HOLDINGS_STATEMENT, getTableName(tenantId, HOLDINGS_TABLE), placeholders);
    Future<UpdateResult> future = Future.future();
    postgresClient.execute(connection, query, parameters, future.completer());
    return mapVertxFuture(future).thenApply(result -> null);
  }

  @Override
  public CompletableFuture<Void> removeHoldings(String tenantId){
    final String query = String.format(REMOVE_FROM_HOLDINGS, getTableName(tenantId, HOLDINGS_TABLE));
    LOG.info("Do delete query = " + query);
    PostgresClient postgresClient = PostgresClient.getInstance(vertx, tenantId);
    Future<UpdateResult> future = Future.future();
    postgresClient.execute(query, future.completer());
    return mapVertxFuture(future).thenApply(result -> null);
  }

  private String getHoldingsId(DbHolding holding) {
    return holding.getVendorId() + "-" + holding.getPackageId() + "-" + holding.getTitleId();
  }

  private String createInsertPlaceholders(List<DbHolding> holdings) {
    return String.join(",", Collections.nCopies(holdings.size(),"(?,?)"));
  }

  private JsonArray createParameters(List<DbHolding> holdings) {
    JsonArray params = new JsonArray();
    holdings.forEach(holding -> {
      params.add(getHoldingsId(holding));
      params.add(Json.encode(holding));
    });
    return params;
  }
}