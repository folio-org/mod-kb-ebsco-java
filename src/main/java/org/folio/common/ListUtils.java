package org.folio.common;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ListUtils {

  private ListUtils() {
  }

  public static <T, R> List<R> mapItems(Collection<T> source, Function<? super T, ? extends R> mapper) {
    return source.stream().map(mapper).collect(Collectors.toList());
  }

  public static String createPlaceholders(int size) {
    return String.join(",", Collections.nCopies(size, "?"));
  }

  public static  String createInsertPlaceholders(int size) {
    return String.join(",", Collections.nCopies(size,"(?,?)"));
  }
}
