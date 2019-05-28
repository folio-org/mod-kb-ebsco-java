package org.folio.common;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class FutureUtils {

  private FutureUtils() {
  }

  public static <T> CompletableFuture<T> failedFuture(Throwable ex) {
    CompletableFuture<T> f = new CompletableFuture<>();

    f.completeExceptionally(ex);

    return f;
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
