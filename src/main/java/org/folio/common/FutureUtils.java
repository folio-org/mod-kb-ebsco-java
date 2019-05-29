package org.folio.common;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import io.vertx.core.Future;

public class FutureUtils {

  private FutureUtils() {
  }

  public static <T> CompletableFuture<T> failedFuture(Throwable ex) {
    CompletableFuture<T> f = new CompletableFuture<>();

    f.completeExceptionally(ex);

    return f;
  }

  public static <T> CompletableFuture<T> mapVertxFuture(Future<T> future) {
    CompletableFuture<T> completableFuture = new CompletableFuture<>();

    future
      .map(completableFuture::complete)
      .otherwise(completableFuture::completeExceptionally);

    return completableFuture;
  }

  public static <T,U> CompletableFuture<T> mapResult(Future<U> future, Function<U, T> mapper) {
    CompletableFuture<T> result = new CompletableFuture<>();

    future.map(mapper)
      .map(result::complete)
      .otherwise(result::completeExceptionally);

    return result;
  }
}
