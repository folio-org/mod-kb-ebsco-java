package org.folio.common;

import java.util.concurrent.CompletableFuture;

public class FutureUtils {

  private FutureUtils() {
  }

  public static <T> CompletableFuture<T> failedFuture(Throwable ex) {
    CompletableFuture<T> f = new CompletableFuture<>();

    f.completeExceptionally(ex);

    return f;
  }
}
