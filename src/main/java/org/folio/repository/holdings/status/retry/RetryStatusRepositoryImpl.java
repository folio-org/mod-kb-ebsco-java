package org.folio.repository.holdings.status.retry;

import static org.folio.common.FunctionUtils.nothing;
import static org.folio.common.LogUtils.logDeleteQueryInfoLevel;
import static org.folio.common.LogUtils.logInsertQueryDebugLevel;
import static org.folio.common.LogUtils.logSelectQueryDebugLevel;
import static org.folio.common.LogUtils.logUpdateQueryDebugLevel;
import static org.folio.db.DbUtils.createParams;
import static org.folio.repository.holdings.status.retry.RetryStatusTableConstants.ATTEMPTS_LEFT_COLUMN;
import static org.folio.repository.holdings.status.retry.RetryStatusTableConstants.TIMER_ID_COLUMN;
import static org.folio.repository.holdings.status.retry.RetryStatusTableConstants.deleteRetryStatus;
import static org.folio.repository.holdings.status.retry.RetryStatusTableConstants.getRetryStatusByCredentials;
import static org.folio.repository.holdings.status.retry.RetryStatusTableConstants.insertRetryStatus;
import static org.folio.repository.holdings.status.retry.RetryStatusTableConstants.updateRetryStatus;
import static org.folio.util.FutureUtils.mapResult;

import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import lombok.extern.log4j.Log4j2;
import org.folio.db.RowSetUtils;
import org.folio.db.exc.translation.DBExceptionTranslator;
import org.folio.rest.persist.PostgresClient;
import org.springframework.stereotype.Component;

@Log4j2
@Component
public class RetryStatusRepositoryImpl implements RetryStatusRepository {
  private final Vertx vertx;
  private final DBExceptionTranslator excTranslator;

  public RetryStatusRepositoryImpl(Vertx vertx, DBExceptionTranslator excTranslator) {
    this.vertx = vertx;
    this.excTranslator = excTranslator;
  }

  @Override
  public CompletableFuture<RetryStatus> findByCredentialsId(UUID credentialsId, String tenantId) {
    final String query = getRetryStatusByCredentials(tenantId);
    final Tuple parameters = Tuple.of(credentialsId);
    logSelectQueryDebugLevel(log, query, parameters);
    Promise<RowSet<Row>> promise = Promise.promise();
    pgClient(tenantId).select(query, parameters, promise);
    return mapResult(promise.future().recover(excTranslator.translateOrPassBy()), this::mapStatus);
  }

  @Override
  public CompletableFuture<Void> save(RetryStatus status, UUID credentialsId, String tenantId) {
    final String query = insertRetryStatus(tenantId);
    final Tuple parameters = createInsertParameters(credentialsId, status);
    logInsertQueryDebugLevel(log, query, parameters);
    Promise<RowSet<Row>> promise = Promise.promise();
    pgClient(tenantId).execute(query, parameters, promise);
    return mapResult(promise.future().recover(excTranslator.translateOrPassBy()), nothing());
  }

  @Override
  public CompletableFuture<Void> update(RetryStatus retryStatus, UUID credentialsId, String tenantId) {
    final String query = updateRetryStatus(tenantId);
    final Tuple parameters = createUpdateParameters(credentialsId, retryStatus);
    logUpdateQueryDebugLevel(log, query, parameters);
    Promise<RowSet<Row>> promise = Promise.promise();
    pgClient(tenantId).execute(query, parameters, promise);
    return mapResult(promise.future().recover(excTranslator.translateOrPassBy()), nothing());
  }

  @Override
  public CompletableFuture<Void> delete(UUID credentialsId, String tenantId) {
    final String query = deleteRetryStatus(tenantId);
    final Tuple parameters = Tuple.of(credentialsId);
    logDeleteQueryInfoLevel(log, query, parameters);
    Promise<RowSet<Row>> promise = Promise.promise();
    pgClient(tenantId).execute(query, parameters, promise);
    return mapResult(promise.future().recover(excTranslator.translateOrPassBy()), nothing());
  }

  private Tuple createInsertParameters(UUID credentialsId, RetryStatus retryStatus) {
    return createParams(
      UUID.randomUUID(),
      credentialsId,
      retryStatus.getRetryAttemptsLeft(),
      retryStatus.getTimerId()
    );
  }

  private Tuple createUpdateParameters(UUID credentialsId, RetryStatus retryStatus) {
    return createParams(
      retryStatus.getRetryAttemptsLeft(),
      retryStatus.getTimerId(),
      credentialsId
    );
  }

  private RetryStatus mapStatus(RowSet<Row> resultSet) {
    return RowSetUtils.mapFirstItem(resultSet, row -> new RetryStatus(
      row.getInteger(ATTEMPTS_LEFT_COLUMN),
      row.getLong(TIMER_ID_COLUMN))
    );
  }

  private PostgresClient pgClient(String tenantId) {
    return PostgresClient.getInstance(vertx, tenantId);
  }
}
