package org.folio.repository.holdings.transaction;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;

import static org.folio.common.ListUtils.createPlaceholders;
import static org.folio.db.DbUtils.createParams;
import static org.folio.repository.DbUtil.getTransactionIdTableName;
import static org.folio.repository.holdings.transaction.TransactionIdTableConstants.GET_LAST_TRANSACTION_ID_BY_CREDENTIALS;
import static org.folio.repository.holdings.transaction.TransactionIdTableConstants.INSERT_TRANSACTION_ID;
import static org.folio.repository.holdings.transaction.TransactionIdTableConstants.TRANSACTION_ID_COLUMN;
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
  private Vertx vertx;

  @Override
  public CompletableFuture<Void> save(String credentialsId, String transactionId, String tenantId) {
    final JsonArray params = createParams(asList(credentialsId, transactionId));
    final String query = String.format(INSERT_TRANSACTION_ID, getTransactionIdTableName(tenantId), createPlaceholders(params.size()));
    LOG.info("Do insert query = " + query);
    Promise<UpdateResult> promise = Promise.promise();
    pgClient(tenantId).execute(query, params, promise);
    return mapVertxFuture(promise.future()).thenApply(result -> null);
  }

  @Override
  public CompletableFuture<String> getLastTransactionId(String credentialsId, String tenantId) {
    final String query = String.format(GET_LAST_TRANSACTION_ID_BY_CREDENTIALS, getTransactionIdTableName(tenantId));
    LOG.info("Select last transaction id = " + query);
    Promise<ResultSet> promise = Promise.promise();
    final JsonArray params = createParams(singleton(credentialsId));
    pgClient(tenantId).select(query, params, promise);
    return mapResult(promise.future(), this::mapId);
  }

  private String mapId(ResultSet resultSet) {
    List<JsonObject> rows = resultSet.getRows();
    if(!rows.isEmpty()){
      return rows.get(0).getString(TRANSACTION_ID_COLUMN);
    }
    return null;
  }

  private PostgresClient pgClient(String tenantId) {
    return PostgresClient.getInstance(vertx, tenantId);
  }
}
