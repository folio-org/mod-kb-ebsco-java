package org.folio.common;

import java.util.function.Function;

public final class FunctionUtils {

  private FunctionUtils() {
  }

  public static  <T> Function<T, Void> nothing() {
    return result -> null;
  }
}
