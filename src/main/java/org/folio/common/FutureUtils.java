package org.folio.common;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;
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

  /**
   * Returns a new CompletableFuture that is completed when all of the given futures complete,
   * returned CompletableFuture contains list of values from futures that have succeeded,
   * If future completes exceptionally then the exception is passed to provided exceptionHandler
   */
  public static <T> CompletableFuture<List<T>> allOfSucceeded(Collection<CompletableFuture<T>> futures, Consumer<Throwable> exceptionHandler) {
    return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
      .handle((o, e) ->
        futures.stream()
          .map(future -> future.whenComplete((result, throwable) -> {
            if(throwable != null) {
              exceptionHandler.accept(throwable);
            }
          }))
          .filter(future -> !future.isCompletedExceptionally())
          .map(CompletableFuture::join)
          .collect(Collectors.toList())
      );
  }
}
