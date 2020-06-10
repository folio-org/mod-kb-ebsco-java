package org.folio.repository.holdings.status;

import static java.util.Arrays.asList;

import static org.folio.common.FunctionUtils.nothing;
import static org.folio.common.ListUtils.createPlaceholders;
import static org.folio.db.DbUtils.createParams;
import static org.folio.db.DbUtils.executeInTransaction;
import static org.folio.repository.DbUtil.getHoldingsStatusTableName;
import static org.folio.repository.DbUtil.mapColumn;
import static org.folio.repository.holdings.status.HoldingsStatusTableConstants.DELETE_LOADING_STATUS;
import static org.folio.repository.holdings.status.HoldingsStatusTableConstants.GET_HOLDINGS_STATUS_BY_ID;
import static org.folio.repository.holdings.status.HoldingsStatusTableConstants.INSERT_LOADING_STATUS;
import static org.folio.repository.holdings.status.HoldingsStatusTableConstants.UPDATE_IMPORTED_COUNT;
import static org.folio.repository.holdings.status.HoldingsStatusTableConstants.UPDATE_LOADING_STATUS;
import static org.folio.util.FutureUtils.mapResult;
import static org.folio.util.FutureUtils.mapVertxFuture;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import javax.annotation.Nullable;

import io.vertx.core.AsyncResult;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.sql.UpdateResult;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.folio.common.VertxIdProvider;
import org.folio.rest.jaxrs.model.HoldingsLoadingStatus;
import org.folio.rest.persist.PostgresClient;

@Component
public class HoldingsStatusRepositoryImpl implements HoldingsStatusRepository {
  private static final Logger LOG = LoggerFactory.getLogger(HoldingsStatusRepositoryImpl.class);

  private Vertx vertx;
  private VertxIdProvider vertxIdProvider;

  @Autowired
  public HoldingsStatusRepositoryImpl(Vertx vertx, VertxIdProvider vertxIdProvider) {
    this.vertx = vertx;
    this.vertxIdProvider = vertxIdProvider;
  }

  @Override
  public CompletableFuture<HoldingsLoadingStatus> findByCredentialsId(String credentialsId, String tenantId) {
    return get(credentialsId, tenantId,null).thenApply(status -> setCredentialsId(status, credentialsId));
  }

  @NotNull
  private HoldingsLoadingStatus setCredentialsId(HoldingsLoadingStatus status, String credentialsId) {
    status.getData().getAttributes().setCredentialsId(credentialsId);
    return status;
  }

  @Override
  public CompletableFuture<Void> save(HoldingsLoadingStatus status, String credentialsId, String tenantId) {
    final JsonArray parameters = createParams(asList(UUID.randomUUID().toString(), credentialsId, Json.encode(status), vertxIdProvider.getVertxId()));
    final String query = String.format(INSERT_LOADING_STATUS, getHoldingsStatusTableName(tenantId), createPlaceholders(parameters.size()));
    LOG.info("Do insert query = " + query);
    Promise<UpdateResult> promise = Promise.promise();
    pgClient(tenantId).execute(query, parameters, promise);
    return mapVertxFuture(promise.future()).thenApply(nothing());
  }

  @Override
  public CompletableFuture<Void> update(HoldingsLoadingStatus status, String credentialsId, String tenantId) {
    final String query = String.format(UPDATE_LOADING_STATUS, getHoldingsStatusTableName(tenantId));
    final String vertxId = vertxIdProvider.getVertxId();
    final JsonArray parameters = createParams(asList(Json.encode(status), vertxId, credentialsId));
    LOG.info("Do update query = " + query);
    Promise<UpdateResult> promise = Promise.promise();
    pgClient(tenantId).execute(query, parameters, promise);
    return mapVertxFuture(promise.future()).thenApply(this::assertUpdated);
  }

  @Override
  public CompletableFuture<Void> delete(String credentialsId, String tenantId) {
    final String query = String.format(DELETE_LOADING_STATUS, getHoldingsStatusTableName(tenantId));
    LOG.info("Do delete query = " + query);
    Promise<UpdateResult> promise = Promise.promise();
    final JsonArray params = createParams(Collections.singleton(credentialsId));
    pgClient(tenantId).execute(query, params, promise);
    return mapVertxFuture(promise.future()).thenApply(nothing());
  }

  @Override
  public CompletableFuture<HoldingsLoadingStatus> increaseImportedCount(int holdingsAmount, int pageAmount,
                                                                        String credentialsId, String tenantId) {
    return executeInTransaction(tenantId, vertx, (postgresClient, connection) -> {
      final String query = String.format(UPDATE_IMPORTED_COUNT, getHoldingsStatusTableName(tenantId), holdingsAmount, pageAmount);
      final String vertxId = vertxIdProvider.getVertxId();
      final JsonArray parameters = new JsonArray().add(vertxId).add(credentialsId);
      LOG.info("Increment imported count query = " + query);
      Promise<UpdateResult> promise = Promise.promise();
      postgresClient.execute(connection, query, parameters, promise);
      return mapVertxFuture(promise.future())
        .thenApply(this::assertUpdated)
        .thenCompose(o -> get(credentialsId, tenantId, connection));
    });
  }

  private CompletableFuture<HoldingsLoadingStatus> get(String credentialsId, String tenantId, @Nullable  AsyncResult<SQLConnection> connection) {
    final String query = String.format(GET_HOLDINGS_STATUS_BY_ID, getHoldingsStatusTableName(tenantId));
    LOG.info("Select holdings loading status = " + query);
    final JsonArray params = createParams(Collections.singleton(credentialsId));
    Promise<ResultSet> promise = Promise.promise();
    if(connection != null) {
      pgClient(tenantId).select(connection, query, params, promise);
    } else{
      pgClient(tenantId).select(query, params, promise);
    }
    return mapResult(promise.future(), this::mapStatus);
  }

  private Void assertUpdated(UpdateResult result) {
    if(result.getUpdated() == 0){
      throw new IllegalArgumentException("Couldn't update holdings status");
    }
    return null;
  }

  private HoldingsLoadingStatus mapStatus(ResultSet resultSet) {
    return mapColumn(resultSet.getRows().get(0), "jsonb", HoldingsLoadingStatus.class).orElse(null);
  }

  private PostgresClient pgClient(String tenantId) {
    return PostgresClient.getInstance(vertx, tenantId);
  }
}
