package org.folio.tag.repository;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import io.vertx.core.Future;
import io.vertx.ext.sql.ResultSet;

public class DbUtil {
  private DbUtil() {}

  public static <T> CompletableFuture<T> mapVertxFuture(Future<T> future){
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
}
