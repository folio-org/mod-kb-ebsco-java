package org.folio.repository.holdings.status;

import static org.folio.common.FunctionUtils.nothing;
import static org.folio.common.LogUtils.logDebugLevel;
import static org.folio.common.LogUtils.logDeleteQueryDebugLevel;
import static org.folio.common.LogUtils.logInsertQueryDebugLevel;
import static org.folio.common.LogUtils.logSelectQueryDebugLevel;
import static org.folio.common.LogUtils.logUpdateQueryDebugLevel;
import static org.folio.db.RowSetUtils.isEmpty;
import static org.folio.db.RowSetUtils.mapFirstItem;
import static org.folio.db.RowSetUtils.mapItems;
import static org.folio.db.RowSetUtils.toJsonObject;
import static org.folio.repository.holdings.status.HoldingsStatusTableConstants.CREDENTIALS_COLUMN;
import static org.folio.repository.holdings.status.HoldingsStatusTableConstants.JSONB_COLUMN;
import static org.folio.repository.holdings.status.HoldingsStatusTableConstants.deleteLoadingStatus;
import static org.folio.repository.holdings.status.HoldingsStatusTableConstants.getHoldingsStatusById;
import static org.folio.repository.holdings.status.HoldingsStatusTableConstants.getHoldingsStatuses;
import static org.folio.repository.holdings.status.HoldingsStatusTableConstants.insertLoadingStatus;
import static org.folio.repository.holdings.status.HoldingsStatusTableConstants.updateImportedCount;
import static org.folio.repository.holdings.status.HoldingsStatusTableConstants.updateLoadingStatus;
import static org.folio.util.FutureUtils.mapResult;
import static org.folio.util.FutureUtils.mapVertxFuture;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.pgclient.PgConnection;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import lombok.extern.log4j.Log4j2;
import org.folio.common.VertxIdProvider;
import org.folio.db.exc.translation.DBExceptionTranslator;
import org.folio.rest.jaxrs.model.HoldingsLoadingStatus;
import org.folio.rest.persist.PostgresClient;
import org.springframework.stereotype.Component;

@Log4j2
@Component
public class HoldingsStatusRepositoryImpl implements HoldingsStatusRepository {
  private final Vertx vertx;
  private final VertxIdProvider vertxIdProvider;
  private final DBExceptionTranslator excTranslator;

  public HoldingsStatusRepositoryImpl(Vertx vertx, VertxIdProvider vertxIdProvider,
                                      DBExceptionTranslator excTranslator) {
    this.vertx = vertx;
    this.vertxIdProvider = vertxIdProvider;
    this.excTranslator = excTranslator;
  }

  @Override
  public CompletableFuture<List<HoldingsLoadingStatus>> findAll(String tenantId) {
    final String query = getHoldingsStatuses(tenantId);
    logSelectQueryDebugLevel(log, query);
    Promise<RowSet<Row>> promise = Promise.promise();
    pgClient(tenantId).select(query, promise);
    return mapResult(promise.future(), this::mapStatusesCollection);
  }

  @Override
  public CompletableFuture<HoldingsLoadingStatus> findByCredentialsId(UUID credentialsId, String tenantId) {
    return get(credentialsId, tenantId);
  }

  @Override
  public CompletableFuture<Void> save(HoldingsLoadingStatus status, UUID credentialsId, String tenantId) {
    final Tuple params = Tuple.of(UUID.randomUUID(), credentialsId, toJsonObject(status), vertxIdProvider.getVertxId());
    final String query = insertLoadingStatus(tenantId, params);
    logInsertQueryDebugLevel(log, query, params);
    Promise<RowSet<Row>> promise = Promise.promise();
    pgClient(tenantId).execute(query, params, promise);
    return mapVertxFuture(promise.future().recover(excTranslator.translateOrPassBy())).thenApply(nothing());
  }

  @Override
  public CompletableFuture<Void> update(HoldingsLoadingStatus status, UUID credentialsId, String tenantId) {
    final Tuple params = Tuple.of(toJsonObject(status), vertxIdProvider.getVertxId(), credentialsId);
    final String query = updateLoadingStatus(tenantId);
    logUpdateQueryDebugLevel(log, query, params);
    Promise<RowSet<Row>> promise = Promise.promise();
    pgClient(tenantId).execute(query, params, promise);
    return mapVertxFuture(promise.future().recover(excTranslator.translateOrPassBy())).thenApply(this::assertUpdated);
  }

  @Override
  public CompletableFuture<Void> delete(UUID credentialsId, String tenantId) {
    final Tuple params = Tuple.of(credentialsId);
    final String query = deleteLoadingStatus(tenantId);
    logDeleteQueryDebugLevel(log, query, params);
    Promise<RowSet<Row>> promise = Promise.promise();
    pgClient(tenantId).execute(query, params, promise);
    return mapVertxFuture(promise.future().recover(excTranslator.translateOrPassBy())).thenApply(nothing());
  }

  @Override
  public CompletableFuture<HoldingsLoadingStatus> increaseImportedCount(int holdingsAmount, int pageAmount,
                                                                        UUID credentialsId, String tenantId) {
    PostgresClient postgresClient = PostgresClient.getInstance(vertx, tenantId);

    Future<HoldingsLoadingStatus> future = postgresClient.withTransaction(connection -> {
      final Tuple params = Tuple.of(holdingsAmount, pageAmount, vertxIdProvider.getVertxId(), credentialsId);
      final String query = updateImportedCount(tenantId);
      logDebugLevel(log, "Increment imported count query = {} with params = {}", query, params);

      Promise<RowSet<Row>> promise = Promise.promise();
      connection
        .preparedQuery(query)
        .execute(params)
        .onComplete(promise);

      return promise.future().recover(excTranslator.translateOrPassBy())
        .map(this::assertUpdated)
        .compose(o -> get(credentialsId, tenantId, connection));
    });

    return mapVertxFuture(future);
  }

  private List<HoldingsLoadingStatus> mapStatusesCollection(RowSet<Row> resultSet) {
    return mapItems(resultSet, this::mapItem);
  }

  private HoldingsLoadingStatus mapItem(Row row) {
    String statusJson = row.getValue(JSONB_COLUMN).toString();
    HoldingsLoadingStatus holdingsLoadingStatus = Json.decodeValue(statusJson, HoldingsLoadingStatus.class);
    holdingsLoadingStatus.getData().getAttributes().setCredentialsId(row.getUUID(CREDENTIALS_COLUMN).toString());
    return holdingsLoadingStatus;
  }

  private Future<HoldingsLoadingStatus> get(UUID credentialsId, String tenantId, PgConnection connection) {
    final Tuple params = Tuple.of(credentialsId);
    final String query = getHoldingsStatusById(tenantId);
    logSelectQueryDebugLevel(log, query, params);

    Promise<RowSet<Row>> promise = Promise.promise();
    connection
      .preparedQuery(query)
      .execute(params)
      .onComplete(promise);

    return promise.future().recover(excTranslator.translateOrPassBy()).map(this::mapStatus);
  }

  private CompletableFuture<HoldingsLoadingStatus> get(UUID credentialsId, String tenantId) {
    PostgresClient client = PostgresClient.getInstance(vertx, tenantId);
    Future<HoldingsLoadingStatus> future = client
      .withConnection(conn -> get(credentialsId, tenantId, conn));
    return mapVertxFuture(future);
  }

  private Void assertUpdated(RowSet<Row> result) {
    if (isEmpty(result)) {
      throw new IllegalArgumentException("Couldn't update holdings status");
    }
    return null;
  }

  private HoldingsLoadingStatus mapStatus(RowSet<Row> resultSet) {
    return mapFirstItem(resultSet, this::mapItem);
  }

  private PostgresClient pgClient(String tenantId) {
    return PostgresClient.getInstance(vertx, tenantId);
  }
}
