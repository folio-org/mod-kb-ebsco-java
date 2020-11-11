package org.folio.common;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;

@UtilityClass
public class ListUtils {

  private static final Pattern SPLIT_BY_COMMA_PATTERN = Pattern.compile("\\s*,\\s*");


  public static <T, R> List<R> mapItems(Collection<T> source, Function<? super T, ? extends R> mapper) {
    Objects.requireNonNull(source, "Collection is null");
    return source.stream().map(mapper).collect(Collectors.toList());
  }

  public static String createPlaceholders(int size) {
    return String.join(",", Collections.nCopies(size, "?"));
  }

  public static String createInsertPlaceholders(int placeholderSize, int copiesSize) {
    final String pattern = String.format("(%s)", createPlaceholders(placeholderSize));
    return String.join(",", Collections.nCopies(copiesSize, pattern));
  }

  public static List<String> parseByComma(String string) {
    return StringUtils.isBlank(string)
      ? Collections.emptyList()
      : Arrays.asList(SPLIT_BY_COMMA_PATTERN.split(string));
  }
}
