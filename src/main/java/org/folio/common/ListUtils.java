package org.folio.common;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

@UtilityClass
public class ListUtils {

  public static <T, R> List<R> mapItems(Collection<T> source, Function<? super T, ? extends R> mapper) {
    Objects.requireNonNull(source, "Collection is null");
    return source.stream().map(mapper).collect(Collectors.toList());
  }

  public static <T, R> @Nullable List<R> mapItemsNullable(Collection<T> source,
                                                          Function<? super T, ? extends R> mapper) {
    if (source == null) {
      return null;
    }
    return source.stream().map(mapper).filter(Objects::nonNull).collect(Collectors.toList());
  }

  public static String createPlaceholders(int size) {
    return String.join(",", Collections.nCopies(size, "?"));
  }

  public static String createPlaceholders(int placeholderSize, int copiesSize) {
    final String pattern = String.format("(%s)", createPlaceholders(placeholderSize));
    return String.join(",", Collections.nCopies(copiesSize, pattern));
  }

  public static List<String> parseByComma(String string) {
    return StringUtils.isBlank(string)
           ? Collections.emptyList()
           : Arrays.stream(StringUtils.split(string, ',')).map(String::trim).toList();
  }
}
