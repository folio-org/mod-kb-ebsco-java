package org.folio.repository.holdings.status;

import static org.folio.common.FunctionUtils.nothing;
import static org.folio.common.ListUtils.createPlaceholders;
import static org.folio.repository.DbUtil.getTransactionIdTableName;
import static org.folio.repository.holdings.status.TransactionIdTableConstants.GET_LAST_TRANSACTION_ID;
import static org.folio.repository.holdings.status.TransactionIdTableConstants.INSERT_TRANSACTION_ID;
import static org.folio.repository.holdings.status.TransactionIdTableConstants.TRANSACTION_ID_COLUMN;
import static org.folio.util.FutureUtils.mapResult;
import static org.folio.util.FutureUtils.mapVertxFuture;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.UpdateResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.folio.rest.persist.PostgresClient;

@Component
public class TransactionIdRepositoryImpl implements TransactionIdRepository {
  private static final Logger LOG = LoggerFactory.getLogger(TransactionIdRepositoryImpl.class);

  @Autowired
  Vertx vertx;

  @Override
  public CompletableFuture<Void> save(String transactionId, String tenantId) {
    final String query = String.format(INSERT_TRANSACTION_ID, getTransactionIdTableName(tenantId), createPlaceholders(1));
    LOG.info("Do insert query = " + query);
    Promise<UpdateResult> promise = Promise.promise();
    PostgresClient postgresClient = PostgresClient.getInstance(vertx, tenantId);
    postgresClient.execute(query, new JsonArray().add(transactionId), promise);
    return mapVertxFuture(promise.future()).thenApply(nothing());
  }

  @Override
  public CompletableFuture<String> getLastTransactionId(String tenantId) {
    final String query = String.format(GET_LAST_TRANSACTION_ID, getTransactionIdTableName(tenantId));
    PostgresClient postgresClient = PostgresClient.getInstance(vertx, tenantId);
    LOG.info("Select last transaction id = " + query);
    Promise<ResultSet> promise = Promise.promise();
    postgresClient.select(query, promise);
    return mapResult(promise.future(), this::mapId);
  }

  private String mapId(ResultSet resultSet) {
    List<JsonObject> rows = resultSet.getRows();
    if(!rows.isEmpty()){
      return rows.get(0).getString(TRANSACTION_ID_COLUMN);
    }
    return null;
  }
}
