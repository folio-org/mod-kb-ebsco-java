package org.folio.repository.holdings.status;

import static org.folio.common.FutureUtils.mapResult;
import static org.folio.common.FutureUtils.mapVertxFuture;
import static org.folio.common.ListUtils.createPlaceholders;
import static org.folio.repository.DbUtil.getRetryStatusTableName;
import static org.folio.repository.holdings.status.RetryStatusTableConstants.DELETE_RETRY_STATUS;
import static org.folio.repository.holdings.status.RetryStatusTableConstants.GET_RETRY_STATUS;
import static org.folio.repository.holdings.status.RetryStatusTableConstants.INSERT_RETRY_STATUS;
import static org.folio.repository.holdings.status.RetryStatusTableConstants.RETRIES_LEFT_COLUMN;
import static org.folio.repository.holdings.status.RetryStatusTableConstants.TIMER_ID_COLUMN;
import static org.folio.repository.holdings.status.RetryStatusTableConstants.UPDATE_RETRY_STATUS;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.folio.rest.persist.PostgresClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.UpdateResult;

@Component
public class RetryStatusRepositoryImpl implements RetryStatusRepository {
  private static final Logger LOG = LoggerFactory.getLogger(RetryStatusRepositoryImpl.class);
  private Vertx vertx;

  @Autowired
  public RetryStatusRepositoryImpl(Vertx vertx) {
    this.vertx = vertx;
  }

  @Override
  public CompletableFuture<RetryStatus> get(String tenantId) {
    final String query = String.format(GET_RETRY_STATUS, getRetryStatusTableName(tenantId));
    PostgresClient postgresClient = PostgresClient.getInstance(vertx, tenantId);
    LOG.info("Select retry status = " + query);
    Future<ResultSet> future = Future.future();
      postgresClient.select(query, future);
    return mapResult(future, this::mapStatus);
  }

  @Override
  public CompletableFuture<Void> save(RetryStatus status, String tenantId) {
    final String query = String.format(INSERT_RETRY_STATUS, getRetryStatusTableName(tenantId), createPlaceholders(3));
    LOG.info("Do insert query = " + query);
    Future<UpdateResult> future = Future.future();
    PostgresClient postgresClient = PostgresClient.getInstance(vertx, tenantId);
    postgresClient.execute(query, createInsertParameters(status), future);
    return mapVertxFuture(future).thenApply(result -> null);
  }

  @Override
  public CompletableFuture<Void> update(RetryStatus retryStatus, String tenantId) {
    final String query = String.format(UPDATE_RETRY_STATUS, getRetryStatusTableName(tenantId));
    JsonArray parameters = createUpdateParameters(retryStatus);
    LOG.info("Do update query = " + query);
    Future<UpdateResult> future = Future.future();
    PostgresClient postgresClient = PostgresClient.getInstance(vertx, tenantId);
    postgresClient.execute(query, parameters, future);
    return mapVertxFuture(future)
      .thenApply(result -> null);
  }

  private JsonArray createInsertParameters(RetryStatus retryStatus) {
    JsonArray parameters = new JsonArray()
      .add(UUID.randomUUID().toString())
      .add(retryStatus.getRetryAttemptsLeft());
    if(retryStatus.getTimerId() != null){
      parameters.add(retryStatus.getTimerId());
    }else{
      parameters.addNull();
    }
    return parameters;
  }

  private JsonArray createUpdateParameters(RetryStatus retryStatus) {
    JsonArray parameters = new JsonArray()
      .add(retryStatus.getRetryAttemptsLeft());
    if(retryStatus.getTimerId() != null){
      parameters.add(retryStatus.getTimerId());
    }else{
      parameters.addNull();
    }
    return parameters;
  }

  @Override
  public CompletableFuture<Void> delete(String tenantId) {
    final String query = String.format(DELETE_RETRY_STATUS, getRetryStatusTableName(tenantId));
    LOG.info("Do delete query = " + query);
    Future<UpdateResult> future = Future.future();
    PostgresClient postgresClient = PostgresClient.getInstance(vertx, tenantId);
    postgresClient.execute(query, future);
    return mapVertxFuture(future).thenApply(result -> null);
  }

  private RetryStatus mapStatus(ResultSet resultSet) {
    JsonObject row = resultSet.getRows().get(0);
    return new RetryStatus(row.getInteger(RETRIES_LEFT_COLUMN),
      row.getLong(TIMER_ID_COLUMN));
  }
}
