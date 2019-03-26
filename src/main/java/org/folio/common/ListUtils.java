package org.folio.common;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ListUtils {

  private ListUtils() {
  }

  public static <T, R> List<R> mapItems(List<T> source, Function<? super T, ? extends R> mapper) {
    return source.stream().map(mapper).collect(Collectors.toList());
  }
}
