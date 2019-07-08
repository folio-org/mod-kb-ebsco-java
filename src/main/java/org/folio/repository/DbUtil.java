package org.folio.repository;

import static org.folio.repository.holdings.HoldingsTableConstants.HOLDINGS_TABLE;
import static org.folio.repository.holdings.status.HoldingsStatusTableConstants.HOLDINGS_STATUS_TABLE;
import static org.folio.repository.packages.PackageTableConstants.PACKAGES_TABLE_NAME;
import static org.folio.repository.providers.ProviderTableConstants.PROVIDERS_TABLE_NAME;
import static org.folio.repository.resources.ResourceTableConstants.RESOURCES_TABLE_NAME;
import static org.folio.repository.tag.TagTableConstants.TAGS_TABLE_NAME;
import static org.folio.repository.titles.TitlesTableConstants.TITLES_TABLE_NAME;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.sql.SQLConnection;
import org.apache.commons.lang3.mutable.MutableObject;

import org.folio.common.FutureUtils;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.tools.utils.ObjectMapperTool;

public class DbUtil {
  private DbUtil() {}

  private static final Logger LOG = LoggerFactory.getLogger(DbUtil.class);

  public static JsonArray createInsertOrUpdateParameters(String id, String name) {
    return new JsonArray()
      .add(id)
      .add(name)
      .add(name);
  }

  public static CompletableFuture<Void> executeInTransaction(String tenantId, Vertx vertx,
                                                       BiFunction<PostgresClient, AsyncResult<SQLConnection>, CompletableFuture<Void>> action) {
    PostgresClient postgresClient = PostgresClient.getInstance(vertx, tenantId);
    MutableObject<AsyncResult<SQLConnection>> mutableConnection = new MutableObject<>();
    CompletableFuture<Boolean> rollbackFuture = new CompletableFuture<>();

    return CompletableFuture.completedFuture(null)
      .thenCompose(o -> {
        CompletableFuture<Void> startTxFuture = new CompletableFuture<>();
        postgresClient.startTx(connection -> {
          mutableConnection.setValue(connection);
          startTxFuture.complete(null);
        });
        return startTxFuture;
      })
      .thenCompose(o -> action.apply(postgresClient, mutableConnection.getValue()))
      .thenCompose(o -> endTransaction(postgresClient, mutableConnection))
      .whenComplete((result, ex) -> {
        if (ex != null) {
          LOG.info("Transaction was not successful. Roll back changes.");
          postgresClient.rollbackTx(mutableConnection.getValue(), rollback -> rollbackFuture.completeExceptionally(ex));
        } else {
          rollbackFuture.complete(null);
        }
      })
      .thenCombine(rollbackFuture, (o, aBoolean) -> null);
  }

  private static CompletionStage<Void> endTransaction(PostgresClient postgresClient, MutableObject<AsyncResult<SQLConnection>> mutableConnection) {
    Future<Void> vertxFuture = Future.future();
    postgresClient.endTx(mutableConnection.getValue(), vertxFuture.completer());
    return FutureUtils.mapVertxFuture(vertxFuture);
  }

  private static String getTableName(String tenantId, String tableName) {
    return PostgresClient.convertToPsqlStandard(tenantId) + "." + tableName;
  }

  public static String getTitlesTableName(String tenantId) {
    return getTableName(tenantId, TITLES_TABLE_NAME);
  }

  public static String getResourcesTableName(String tenantId) {
    return getTableName(tenantId, RESOURCES_TABLE_NAME);
  }

  public static String getProviderTableName(String tenantId) {
    return getTableName(tenantId, PROVIDERS_TABLE_NAME);
  }

  public static String getPackagesTableName(String tenantId) {
    return getTableName(tenantId, PACKAGES_TABLE_NAME);
  }

  public static String getTagsTableName(String tenantId) {
    return getTableName(tenantId, TAGS_TABLE_NAME);
  }

  public static String getHoldingsTableName(String tenantId) {
    return getTableName(tenantId, HOLDINGS_TABLE);
  }

  public static String getHoldingsStatusTableName(String tenantId) {
    return getTableName(tenantId, HOLDINGS_STATUS_TABLE);
  }

  public static <T> Optional<T> mapColumn(JsonObject row, String columnName, Class<T> tClass){
    try {
      return Optional.of(ObjectMapperTool.getMapper().readValue(row.getString(columnName), tClass));
    } catch (IOException e) {
      return Optional.empty();
    }
  }
}
