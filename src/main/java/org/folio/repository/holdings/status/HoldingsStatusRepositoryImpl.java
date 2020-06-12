package org.folio.repository.holdings.status;

import static org.folio.common.FunctionUtils.nothing;
import static org.folio.common.ListUtils.createPlaceholders;
import static org.folio.common.LogUtils.logDeleteQuery;
import static org.folio.common.LogUtils.logInsertQuery;
import static org.folio.common.LogUtils.logQuery;
import static org.folio.common.LogUtils.logSelectQuery;
import static org.folio.common.LogUtils.logUpdateQuery;
import static org.folio.db.DbUtils.executeInTransaction;
import static org.folio.db.RowSetUtils.isEmpty;
import static org.folio.db.RowSetUtils.mapFirstItem;
import static org.folio.db.RowSetUtils.mapItems;
import static org.folio.db.RowSetUtils.toJsonObject;
import static org.folio.repository.DbUtil.getHoldingsStatusTableName;
import static org.folio.repository.DbUtil.prepareQuery;
import static org.folio.repository.holdings.status.HoldingsStatusTableConstants.CREDENTIALS_COLUMN;
import static org.folio.repository.holdings.status.HoldingsStatusTableConstants.DELETE_LOADING_STATUS;
import static org.folio.repository.holdings.status.HoldingsStatusTableConstants.GET_HOLDINGS_STATUSES;
import static org.folio.repository.holdings.status.HoldingsStatusTableConstants.GET_HOLDINGS_STATUS_BY_ID;
import static org.folio.repository.holdings.status.HoldingsStatusTableConstants.INSERT_LOADING_STATUS;
import static org.folio.repository.holdings.status.HoldingsStatusTableConstants.JSONB_COLUMN;
import static org.folio.repository.holdings.status.HoldingsStatusTableConstants.UPDATE_IMPORTED_COUNT;
import static org.folio.repository.holdings.status.HoldingsStatusTableConstants.UPDATE_LOADING_STATUS;
import static org.folio.util.FutureUtils.mapResult;
import static org.folio.util.FutureUtils.mapVertxFuture;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import javax.annotation.Nullable;

import io.vertx.core.AsyncResult;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import org.springframework.stereotype.Component;

import org.folio.common.VertxIdProvider;
import org.folio.db.exc.translation.DBExceptionTranslator;
import org.folio.rest.jaxrs.model.HoldingsLoadingStatus;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.persist.SQLConnection;

@Component
public class HoldingsStatusRepositoryImpl implements HoldingsStatusRepository {

  private static final Logger LOG = LoggerFactory.getLogger(HoldingsStatusRepositoryImpl.class);

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
    final String query = prepareQuery(GET_HOLDINGS_STATUSES, getHoldingsStatusTableName(tenantId));
    logSelectQuery(LOG, query);
    Promise<RowSet<Row>> promise = Promise.promise();
    pgClient(tenantId).select(query, promise);
    return mapResult(promise.future(), this::mapStatusesCollection);
  }

  @Override
  public CompletableFuture<HoldingsLoadingStatus> findByCredentialsId(UUID credentialsId, String tenantId) {
    return get(credentialsId, tenantId, null);
  }

  @Override
  public CompletableFuture<Void> save(HoldingsLoadingStatus status, UUID credentialsId, String tenantId) {
    final Tuple params = Tuple.of(UUID.randomUUID(), credentialsId, toJsonObject(status), vertxIdProvider.getVertxId());
    final String query = prepareQuery(INSERT_LOADING_STATUS,
      getHoldingsStatusTableName(tenantId),
      createPlaceholders(params.size()));
    logInsertQuery(LOG, query, params);
    Promise<RowSet<Row>> promise = Promise.promise();
    pgClient(tenantId).execute(query, params, promise);
    return mapVertxFuture(promise.future().recover(excTranslator.translateOrPassBy())).thenApply(nothing());
  }

  @Override
  public CompletableFuture<Void> update(HoldingsLoadingStatus status, UUID credentialsId, String tenantId) {
    final Tuple params = Tuple.of(toJsonObject(status), vertxIdProvider.getVertxId(), credentialsId);
    final String query = prepareQuery(UPDATE_LOADING_STATUS, getHoldingsStatusTableName(tenantId));
    logUpdateQuery(LOG, query, params);
    Promise<RowSet<Row>> promise = Promise.promise();
    pgClient(tenantId).execute(query, params, promise);
    return mapVertxFuture(promise.future().recover(excTranslator.translateOrPassBy())).thenApply(this::assertUpdated);
  }

  @Override
  public CompletableFuture<Void> delete(UUID credentialsId, String tenantId) {
    final Tuple params = Tuple.of(credentialsId);
    final String query = prepareQuery(DELETE_LOADING_STATUS, getHoldingsStatusTableName(tenantId));
    logDeleteQuery(LOG, query, params);
    Promise<RowSet<Row>> promise = Promise.promise();
    pgClient(tenantId).execute(query, params, promise);
    return mapVertxFuture(promise.future().recover(excTranslator.translateOrPassBy())).thenApply(nothing());
  }

  @Override
  public CompletableFuture<HoldingsLoadingStatus> increaseImportedCount(int holdingsAmount, int pageAmount,
                                                                        UUID credentialsId, String tenantId) {
    return executeInTransaction(tenantId, vertx, (postgresClient, connection) -> {
      final Tuple params = Tuple.of(holdingsAmount, pageAmount, vertxIdProvider.getVertxId(), credentialsId);
      final String query = prepareQuery(UPDATE_IMPORTED_COUNT, getHoldingsStatusTableName(tenantId));
      logQuery(LOG, "Increment imported count query = {} with params = {}", query, params);
      Promise<RowSet<Row>> promise = Promise.promise();
      postgresClient.execute(connection, query, params, promise);
      return mapVertxFuture(promise.future().recover(excTranslator.translateOrPassBy()))
        .thenApply(this::assertUpdated)
        .thenCompose(o -> get(credentialsId, tenantId, connection));
    });
  }

  private List<HoldingsLoadingStatus> mapStatusesCollection(RowSet<Row> resultSet) {
    return mapItems(resultSet, this::mapItem);
  }

  private HoldingsLoadingStatus mapItem(Row row) {
    return Json.decodeValue(row.getValue(JSONB_COLUMN).toString(), HoldingsLoadingStatus.class);
  }

  private CompletableFuture<HoldingsLoadingStatus> get(UUID credentialsId, String tenantId,
                                                       @Nullable AsyncResult<SQLConnection> connection) {
    final Tuple params = Tuple.of(credentialsId);
    final String query = prepareQuery(GET_HOLDINGS_STATUS_BY_ID, getHoldingsStatusTableName(tenantId));
    logSelectQuery(LOG, query, params);
    Promise<RowSet<Row>> promise = Promise.promise();
    if (connection != null) {
      pgClient(tenantId).select(connection, query, params, promise);
    } else {
      pgClient(tenantId).select(query, params, promise);
    }
    return mapResult(promise.future().recover(excTranslator.translateOrPassBy()), this::mapStatus);
  }

  private Void assertUpdated(RowSet<Row> result) {
    if (isEmpty(result)) {
      throw new IllegalArgumentException("Couldn't update holdings status");
    }
    return null;
  }

  private HoldingsLoadingStatus mapStatus(RowSet<Row> resultSet) {
    return mapFirstItem(resultSet, row -> {
      String statusJson = row.getValue(JSONB_COLUMN).toString();
      HoldingsLoadingStatus holdingsLoadingStatus = Json.decodeValue(statusJson, HoldingsLoadingStatus.class);
      holdingsLoadingStatus.getData().getAttributes().setCredentialsId(row.getUUID(CREDENTIALS_COLUMN).toString());
      return holdingsLoadingStatus;
    });
  }

  private PostgresClient pgClient(String tenantId) {
    return PostgresClient.getInstance(vertx, tenantId);
  }
}
