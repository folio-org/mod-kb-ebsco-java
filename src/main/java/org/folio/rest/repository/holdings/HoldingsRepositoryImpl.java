package org.folio.rest.repository.holdings;

import static org.folio.tag.repository.DbUtil.getTableName;
import static org.folio.tag.repository.DbUtil.mapVertxFuture;

import java.util.concurrent.CompletableFuture;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.sql.UpdateResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.folio.holdingsiq.model.Holding;
import org.folio.rest.persist.PostgresClient;
import org.folio.tag.repository.DbUtil;

@Component
public class HoldingsRepositoryImpl implements HoldingsRepository {

  public static final String HOLDINGS_TABLE = "holdings";
  public static final String ID_COLUMN = "id";
  public static final String JSONB_COLUMN = "jsonb";
  public static final String HOLDINGS_FIELD_LIST = String.format("%s, %s", ID_COLUMN, JSONB_COLUMN);
  public static final String INSERT_OR_UPDATE_HOLDINGS_STATEMENT = "INSERT INTO %s(" + HOLDINGS_FIELD_LIST
      + ") VALUES (?, ?) " + "ON CONFLICT (" + ID_COLUMN + ") DO UPDATE " + "SET " + JSONB_COLUMN + " = ?;";
  private Vertx vertx;

  @Autowired
  public HoldingsRepositoryImpl(Vertx vertx) {
    this.vertx = vertx;
  }

  @Override
  public CompletableFuture<Void> saveHolding(Holding holding, String tenantId) {
    final String query = String.format(INSERT_OR_UPDATE_HOLDINGS_STATEMENT, getTableName(tenantId, HOLDINGS_TABLE));
    PostgresClient postgresClient = PostgresClient.getInstance(vertx, tenantId);

    Future<UpdateResult> future = Future.future();
    final String encodedHolding = Json.encode(holding);
    final String holdingResourceId = holding.getVendorId() + "-" + holding.getPackageId() + "-" + holding.getTitleId();
    final JsonArray parameters = DbUtil.createInsertOrUpdateParameters(holdingResourceId, encodedHolding);
    postgresClient.execute(query, parameters, future.completer());
    return mapVertxFuture(future).thenApply(result -> null);
  }
}
