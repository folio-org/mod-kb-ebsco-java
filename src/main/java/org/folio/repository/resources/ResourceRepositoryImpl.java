package org.folio.repository.resources;

import static org.folio.common.FutureUtils.mapVertxFuture;
import static org.folio.repository.DbUtil.createInsertOrUpdateParameters;
import static org.folio.repository.DbUtil.getTableName;
import static org.folio.repository.resources.ResourceTableConstants.DELETE_RESOURCE_STATEMENT;
import static org.folio.repository.resources.ResourceTableConstants.INSERT_OR_UPDATE_RESOURCE_STATEMENT;
import static org.folio.repository.resources.ResourceTableConstants.RESOURCES_TABLE_NAME;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.sql.UpdateResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.folio.holdingsiq.model.Title;
import org.folio.rest.persist.PostgresClient;

@Component
public class ResourceRepositoryImpl implements ResourceRepository {

  private static final Logger LOG = LoggerFactory.getLogger(ResourceRepositoryImpl.class);

  private Vertx vertx;

  @Autowired
  public ResourceRepositoryImpl(Vertx vertx) {
    this.vertx = vertx;
  }

  @Override
  public CompletableFuture<Void> saveResource(String resourceId, Title title, String tenantId) {

    JsonArray parameters = createInsertOrUpdateParameters(resourceId, title.getTitleName());

    final String query = String.format(INSERT_OR_UPDATE_RESOURCE_STATEMENT,
      getTableName(tenantId, RESOURCES_TABLE_NAME));

    PostgresClient postgresClient = PostgresClient.getInstance(vertx, tenantId);

    LOG.info("Do insert query = " + query);
    Future<UpdateResult> future = Future.future();
    postgresClient.execute(query, parameters, future.completer());
    return mapVertxFuture(future).thenApply(result -> null);
  }

  @Override
  public CompletableFuture<Void> deleteResource(String resourceId, String tenantId) {
    JsonArray parameter = new JsonArray(Collections.singletonList(resourceId));

    final String query = String.format(DELETE_RESOURCE_STATEMENT, getTableName(tenantId, RESOURCES_TABLE_NAME));

    PostgresClient postgresClient = PostgresClient.getInstance(vertx, tenantId);

    LOG.info("Do delete query = " + query);
    Future<UpdateResult> future = Future.future();
    postgresClient.execute(query, parameter, future.completer());
    return mapVertxFuture(future).thenApply(result -> null);
  }
}
