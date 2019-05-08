package org.folio.tag.repository;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.sql.ResultSet;

import org.folio.rest.persist.PostgresClient;

public class DbUtil {
  private DbUtil() {}

  public static <T> CompletableFuture<T> mapVertxFuture(Future<T> future) {
    CompletableFuture<T> completableFuture = new CompletableFuture<>();

    future
      .map(completableFuture::complete)
      .otherwise(completableFuture::completeExceptionally);

    return completableFuture;
  }

  public static <T> CompletableFuture<T> mapResultSet(Future<ResultSet> future, Function<ResultSet, T> mapper) {
    CompletableFuture<T> result = new CompletableFuture<>();

    future.map(mapper)
      .map(result::complete)
      .otherwise(result::completeExceptionally);

    return result;
  }

  public static JsonArray createInsertOrUpdateParameters(String id, String name) {
    return new JsonArray()
      .add(id)
      .add(name)
      .add(name);
  }

  public static String getTableName(String tenantId, String tableName) {
    return PostgresClient.convertToPsqlStandard(tenantId) + "." + tableName;
  }
}
