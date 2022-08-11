package org.folio.common;

import java.util.function.Function;
import lombok.experimental.UtilityClass;

@UtilityClass
public class FunctionUtils {

  public static <T> Function<T, Void> nothing() {
    return result -> null;
  }
}
