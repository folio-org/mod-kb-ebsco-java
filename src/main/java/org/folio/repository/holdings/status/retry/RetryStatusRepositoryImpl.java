package org.folio.repository.holdings.status.retry;

import static org.folio.common.FunctionUtils.nothing;
import static org.folio.common.ListUtils.createPlaceholders;
import static org.folio.db.DbUtils.createParams;
import static org.folio.repository.DbUtil.DELETE_LOG_MESSAGE;
import static org.folio.repository.DbUtil.INSERT_LOG_MESSAGE;
import static org.folio.repository.DbUtil.SELECT_LOG_MESSAGE;
import static org.folio.repository.DbUtil.UPDATE_LOG_MESSAGE;
import static org.folio.repository.DbUtil.getRetryStatusTableName;
import static org.folio.repository.DbUtil.prepareQuery;
import static org.folio.repository.holdings.status.retry.RetryStatusTableConstants.DELETE_RETRY_STATUS;
import static org.folio.repository.holdings.status.retry.RetryStatusTableConstants.GET_RETRY_STATUS_BY_CREDENTIALS;
import static org.folio.repository.holdings.status.retry.RetryStatusTableConstants.INSERT_RETRY_STATUS;
import static org.folio.repository.holdings.status.retry.RetryStatusTableConstants.RETRIES_LEFT_COLUMN;
import static org.folio.repository.holdings.status.retry.RetryStatusTableConstants.TIMER_ID_COLUMN;
import static org.folio.repository.holdings.status.retry.RetryStatusTableConstants.UPDATE_RETRY_STATUS;
import static org.folio.util.FutureUtils.mapResult;
import static org.folio.util.FutureUtils.mapVertxFuture;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import org.springframework.stereotype.Component;

import org.folio.db.RowSetUtils;
import org.folio.rest.persist.PostgresClient;

@Component
public class RetryStatusRepositoryImpl implements RetryStatusRepository {

  private static final Logger LOG = LoggerFactory.getLogger(RetryStatusRepositoryImpl.class);

  private final Vertx vertx;

  public RetryStatusRepositoryImpl(Vertx vertx) {
    this.vertx = vertx;
  }

  @Override
  public CompletableFuture<RetryStatus> findByCredentialsId(String credentialsId, String tenantId) {
    final String query = prepareQuery(GET_RETRY_STATUS_BY_CREDENTIALS, getRetryStatusTableName(tenantId));
    final Tuple parameters = Tuple.of(credentialsId);
    LOG.info(SELECT_LOG_MESSAGE, query);
    Promise<RowSet<Row>> promise = Promise.promise();
    pgClient(tenantId).select(query, parameters, promise);
    return mapResult(promise.future(), this::mapStatus);
  }

  @Override
  public CompletableFuture<Void> save(RetryStatus status, String credentialsId, String tenantId) {
    final String query = prepareQuery(INSERT_RETRY_STATUS, getRetryStatusTableName(tenantId), createPlaceholders(4));
    final Tuple parameters = createInsertParameters(credentialsId, status);
    LOG.info(INSERT_LOG_MESSAGE, query);
    Promise<RowSet<Row>> promise = Promise.promise();
    pgClient(tenantId).execute(query, parameters, promise);
    return mapVertxFuture(promise.future()).thenApply(nothing());
  }

  @Override
  public CompletableFuture<Void> update(RetryStatus retryStatus, String credentialsId, String tenantId) {
    final String query = prepareQuery(UPDATE_RETRY_STATUS, getRetryStatusTableName(tenantId));
    final Tuple parameters = createUpdateParameters(credentialsId, retryStatus);
    LOG.info(UPDATE_LOG_MESSAGE, query);
    Promise<RowSet<Row>> promise = Promise.promise();
    pgClient(tenantId).execute(query, parameters, promise);
    return mapVertxFuture(promise.future()).thenApply(nothing());
  }

  @Override
  public CompletableFuture<Void> delete(String credentialsId, String tenantId) {
    final String query = prepareQuery(DELETE_RETRY_STATUS, getRetryStatusTableName(tenantId));
    final Tuple parameters = Tuple.of(credentialsId);
    LOG.info(DELETE_LOG_MESSAGE, query);
    Promise<RowSet<Row>> promise = Promise.promise();
    pgClient(tenantId).execute(query, parameters, promise);
    return mapVertxFuture(promise.future()).thenApply(nothing());
  }

  private Tuple createInsertParameters(String credentialsId, RetryStatus retryStatus) {
    Tuple parameters = createParams(Arrays.asList(UUID.randomUUID(), credentialsId, retryStatus.getRetryAttemptsLeft()));
    if (retryStatus.getTimerId() != null) {
      parameters.addLong(retryStatus.getTimerId());
    } else {
      parameters.addValue(null);
    }
    return parameters;
  }

  private Tuple createUpdateParameters(String credentialsId, RetryStatus retryStatus) {
    Tuple parameters = createParams(Arrays.asList(credentialsId, retryStatus.getRetryAttemptsLeft()));
    if (retryStatus.getTimerId() != null) {
      parameters.addLong(retryStatus.getTimerId());
    } else {
      parameters.addValue(null);
    }
    return parameters;
  }

  private RetryStatus mapStatus(RowSet<Row> resultSet) {
    return RowSetUtils.mapFirstItem(resultSet, row -> new RetryStatus(
      row.getInteger(RETRIES_LEFT_COLUMN),
      row.getLong(TIMER_ID_COLUMN))
    );
  }

  private PostgresClient pgClient(String tenantId) {
    return PostgresClient.getInstance(vertx, tenantId);
  }
}
