package org.folio.repository.holdings.status;

import static org.folio.common.FutureUtils.mapResult;
import static org.folio.common.FutureUtils.mapVertxFuture;
import static org.folio.common.ListUtils.createPlaceholders;
import static org.folio.repository.DbUtil.getHoldingsStatusTableName;
import static org.folio.repository.DbUtil.mapColumn;
import static org.folio.repository.holdings.status.HoldingsStatusTableConstants.DELETE_LOADING_STATUS;
import static org.folio.repository.holdings.status.HoldingsStatusTableConstants.GET_HOLDINGS_STATUS;
import static org.folio.repository.holdings.status.HoldingsStatusTableConstants.INSERT_LOADING_STATUS;
import static org.folio.repository.holdings.status.HoldingsStatusTableConstants.UPDATE_LOADING_STATUS;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.UpdateResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.folio.rest.jaxrs.model.HoldingsLoadingStatus;
import org.folio.rest.persist.PostgresClient;

@Component
public class HoldingsStatusRepositoryImpl implements HoldingsStatusRepository {
  private static final Logger LOG = LoggerFactory.getLogger(HoldingsStatusRepositoryImpl.class);

  private Vertx vertx;

  @Autowired
  public HoldingsStatusRepositoryImpl(Vertx vertx) {
    this.vertx = vertx;
  }

  @Override
  public CompletableFuture<HoldingsLoadingStatus> get(String tenantId) {

    final String query = String.format(GET_HOLDINGS_STATUS, getHoldingsStatusTableName(tenantId));
    PostgresClient postgresClient = PostgresClient.getInstance(vertx, tenantId);
    LOG.info("Select holdings loading status = " + query);
    Future<ResultSet> future = Future.future();
    postgresClient.select(query, future);
    return mapResult(future, this::mapStatus);
  }

  @Override
  public CompletableFuture<Void> save(HoldingsLoadingStatus status, String tenantId) {

    final String query = String.format(INSERT_LOADING_STATUS, getHoldingsStatusTableName(tenantId), createPlaceholders(2));
    JsonArray parameters = new JsonArray().add(UUID.randomUUID().toString()).add(Json.encode(status));
    LOG.info("Do insert query = " + query);
    Future<UpdateResult> future = Future.future();
    PostgresClient postgresClient = PostgresClient.getInstance(vertx, tenantId);
    postgresClient.execute(query, parameters, future);
    return mapVertxFuture(future).thenApply(result -> null);
  }

  @Override
  public CompletableFuture<Void> update(HoldingsLoadingStatus status, String tenantId) {

    final String query = String.format(UPDATE_LOADING_STATUS, getHoldingsStatusTableName(tenantId), Json.encode(status));
    LOG.info("Do update query = " + query);
    Future<UpdateResult> future = Future.future();
    PostgresClient postgresClient = PostgresClient.getInstance(vertx, tenantId);
    postgresClient.execute(query, future);
    return mapVertxFuture(future).thenApply(result -> null);
  }

  @Override
  public CompletableFuture<Void> delete(String tenantId) {
    final String query = String.format(DELETE_LOADING_STATUS, getHoldingsStatusTableName(tenantId));
    LOG.info("Do delete query = " + query);
    Future<UpdateResult> future = Future.future();
    PostgresClient postgresClient = PostgresClient.getInstance(vertx, tenantId);
    postgresClient.execute(query, future);
    return mapVertxFuture(future).thenApply(result -> null);
  }

  private HoldingsLoadingStatus mapStatus(ResultSet resultSet) {
    return mapColumn(resultSet.getRows().get(0), "jsonb", HoldingsLoadingStatus.class).orElse(null);
  }
}
