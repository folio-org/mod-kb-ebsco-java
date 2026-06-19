package org.folio.rest.model.filter;

import java.util.function.Function;
import java.util.function.Predicate;
import org.jspecify.annotations.NonNull;

public record FilterValidatorLogic<F extends Filter>(
  Function<F, String> extractor,
  Predicate<String> validator,
  String errorMessage
) {

  public static <F extends Filter> FilterValidatorLogic<F> of(@NonNull Function<F, String> extractor,
                                                              @NonNull Predicate<String> validator,
                                                              @NonNull String errorMessage) {
    return new FilterValidatorLogic<>(extractor, validator, errorMessage);
  }
}
