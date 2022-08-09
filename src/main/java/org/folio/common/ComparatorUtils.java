package org.folio.common;

import static java.util.Comparator.comparing;
import static java.util.Comparator.nullsFirst;

import java.util.Comparator;
import java.util.function.Function;
import lombok.experimental.UtilityClass;

@UtilityClass
public final class ComparatorUtils {

  public static Comparator<Double> nullsFirstDouble() {
    return nullsFirst(Double::compare);
  }

  public static Comparator<Integer> nullsFirstInteger() {
    return nullsFirst(Integer::compare);
  }

  public static <T> Comparator<T> nullFirstDouble(Function<T, Double> extractor) {
    return comparing(extractor, nullsFirstDouble());
  }

  public static <T> Comparator<T> nullFirstInteger(Function<T, Integer> extractor) {
    return comparing(extractor, nullsFirstInteger());
  }
}
