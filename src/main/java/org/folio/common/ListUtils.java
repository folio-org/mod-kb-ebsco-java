package org.folio.common;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class ListUtils {

  private ListUtils() {
  }

  public static <T, R> List<R> mapItems(Collection<T> source, Function<? super T, ? extends R> mapper) {
    Objects.requireNonNull(source, "Collection is null");
    return source.stream().map(mapper).collect(Collectors.toList());
  }

  public static String createPlaceholders(int size) {
    return String.join(",", Collections.nCopies(size, "?"));
  }

  public static  String createInsertPlaceholders(int placeholderSize, int copiesSize) {
    final String pattern = String.format("(%s)", createPlaceholders(placeholderSize));
    return String.join(",", Collections.nCopies(copiesSize, pattern));
  }
}
