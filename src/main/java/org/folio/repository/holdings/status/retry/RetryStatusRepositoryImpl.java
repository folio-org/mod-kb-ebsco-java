package org.folio.repository.holdings.status.retry;

import static org.folio.common.ListUtils.createPlaceholders;
import static org.folio.repository.DbUtil.getRetryStatusTableName;
import static org.folio.repository.holdings.status.retry.RetryStatusTableConstants.DELETE_RETRY_STATUS;
import static org.folio.repository.holdings.status.retry.RetryStatusTableConstants.GET_RETRY_STATUS_BY_CREDENTIALS;
import static org.folio.repository.holdings.status.retry.RetryStatusTableConstants.INSERT_RETRY_STATUS;
import static org.folio.repository.holdings.status.retry.RetryStatusTableConstants.RETRIES_LEFT_COLUMN;
import static org.folio.repository.holdings.status.retry.RetryStatusTableConstants.TIMER_ID_COLUMN;
import static org.folio.repository.holdings.status.retry.RetryStatusTableConstants.UPDATE_RETRY_STATUS;
import static org.folio.util.FutureUtils.mapResult;
import static org.folio.util.FutureUtils.mapVertxFuture;

import java.util.UUID;
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
public class RetryStatusRepositoryImpl implements RetryStatusRepository {
  private static final Logger LOG = LoggerFactory.getLogger(RetryStatusRepositoryImpl.class);
  private Vertx vertx;

  @Autowired
  public RetryStatusRepositoryImpl(Vertx vertx) {
    this.vertx = vertx;
  }

  @Override
  public CompletableFuture<RetryStatus> findByCredentialsId(String credentialsId, String tenantId) {
    final String query = String.format(GET_RETRY_STATUS_BY_CREDENTIALS, getRetryStatusTableName(tenantId));
    LOG.info("Select retry status = " + query);
    Promise<ResultSet> promise = Promise.promise();
    pgClient(tenantId).select(query, new JsonArray().add(credentialsId), promise);

    return mapResult(promise.future(), this::mapStatus);
  }

  @Override
  public CompletableFuture<Void> save(RetryStatus status, String credentialsId, String tenantId) {
    final String query = String.format(INSERT_RETRY_STATUS, getRetryStatusTableName(tenantId), createPlaceholders(4));
    LOG.info("Do insert query = " + query);
    Promise<UpdateResult> promise = Promise.promise();
    pgClient(tenantId).execute(query, createInsertParameters(credentialsId, status), promise);
    return mapVertxFuture(promise.future()).thenApply(result -> null);
  }

  @Override
  public CompletableFuture<Void> update(RetryStatus retryStatus, String credentialsId, String tenantId) {
    final String query = String.format(UPDATE_RETRY_STATUS, getRetryStatusTableName(tenantId));
    final JsonArray parameters = createUpdateParameters(credentialsId, retryStatus);
    LOG.info("Do update query = " + query);
    Promise<UpdateResult> promise = Promise.promise();
    pgClient(tenantId).execute(query, parameters, promise);
    return mapVertxFuture(promise.future()).thenApply(result -> null);
  }

  @Override
  public CompletableFuture<Void> delete(String credentialsId, String tenantId) {
    final String query = String.format(DELETE_RETRY_STATUS, getRetryStatusTableName(tenantId));
    LOG.info("Do delete query = " + query);
    Promise<UpdateResult> promise = Promise.promise();
    pgClient(tenantId).execute(query, new JsonArray().add(credentialsId), promise);
    return mapVertxFuture(promise.future()).thenApply(result -> null);
  }

  private JsonArray createInsertParameters(String credentialsId, RetryStatus retryStatus) {
    JsonArray parameters = new JsonArray()
      .add(UUID.randomUUID().toString())
      .add(credentialsId)
      .add(retryStatus.getRetryAttemptsLeft());
    if(retryStatus.getTimerId() != null){
      parameters.add(retryStatus.getTimerId());
    } else {
      parameters.addNull();
    }
    return parameters;
  }

  private JsonArray createUpdateParameters(String credentialsId, RetryStatus retryStatus) {
    JsonArray parameters = new JsonArray()
      .add(credentialsId)
      .add(retryStatus.getRetryAttemptsLeft());
    if(retryStatus.getTimerId() != null){
      parameters.add(retryStatus.getTimerId());
    } else {
      parameters.addNull();
    }
    return parameters;
  }

  private RetryStatus mapStatus(ResultSet resultSet) {
    JsonObject row = resultSet.getRows().get(0);
    return new RetryStatus(
      row.getInteger(RETRIES_LEFT_COLUMN),
      row.getLong(TIMER_ID_COLUMN));
  }

  private PostgresClient pgClient(String tenantId) {
    return PostgresClient.getInstance(vertx, tenantId);
  }
}
