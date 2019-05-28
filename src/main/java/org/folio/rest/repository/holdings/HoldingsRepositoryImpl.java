package org.folio.rest.repository.holdings;

import static org.folio.tag.repository.DbUtil.getTableName;
import static org.folio.tag.repository.DbUtil.mapVertxFuture;

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

import org.folio.holdingsiq.model.Holding;
import org.folio.rest.persist.PostgresClient;
import org.folio.tag.repository.DbUtil;

@Component
public class HoldingsRepositoryImpl implements HoldingsRepository {
  private static final Logger LOG = LoggerFactory.getLogger(HoldingsRepositoryImpl.class);

  public static final String HOLDINGS_TABLE = "holdings";
  private static final String ID_COLUMN = "id";
  private static final String JSONB_COLUMN = "jsonb";
  private static final String HOLDINGS_FIELD_LIST = String.format("%s, %s", ID_COLUMN, JSONB_COLUMN);
  private static final String INSERT_OR_UPDATE_HOLDINGS_STATEMENT =
    "INSERT INTO %s(" + HOLDINGS_FIELD_LIST + ") VALUES %s" +
      "ON CONFLICT (" + ID_COLUMN + ") DO NOTHING";
  private static final String REMOVE_FROM_HOLDINGS = "DELETE FROM %s;";
  private static final int MAX_BATCH_SIZE = 200;
  private Vertx vertx;

  @Autowired
  public HoldingsRepositoryImpl(Vertx vertx) {
    this.vertx = vertx;
  }

  @Override
  public CompletableFuture<Void> saveHolding(List<Holding> holdings, String tenantId) {
    return DbUtil.executeInTransaction(tenantId, vertx, (postgresClient, connection) -> {
      List<List<Holding>> batches = Lists.partition(holdings, MAX_BATCH_SIZE);
      CompletableFuture<Void> future = CompletableFuture.completedFuture(null);
      for (List<Holding> batch : batches) {
        future = future.thenCompose(o ->
          saveHoldings(batch, tenantId, connection, postgresClient));
      }
      return future;
    });
  }

  private CompletableFuture<Void> saveHoldings(List<Holding> holdings, String tenantId,
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

  private String getHoldingsId(Holding holding) {
    return holding.getVendorId() + "-" + holding.getPackageId() + "-" + holding.getTitleId();
  }

  private String createInsertPlaceholders(List<Holding> holdings) {
    return String.join(",", Collections.nCopies(holdings.size(),"(?,?)"));
  }

  private JsonArray createParameters(List<Holding> holdings) {
    JsonArray params = new JsonArray();
    holdings.forEach(holding -> {
      params.add(getHoldingsId(holding));
      params.add(Json.encode(holding));
    });
    return params;
  }
}
