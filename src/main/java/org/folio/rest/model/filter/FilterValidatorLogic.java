package org.folio.rest.model.filter;

import java.util.function.Function;
import java.util.function.Predicate;
import org.jspecify.annotations.NonNull;

public record FilterValidatorLogic<F extends Filter, P>(
  Function<F, P> extractor,
  Predicate<P> validator,
  String errorMessage
) {

  public static <F extends Filter, P> FilterValidatorLogic<F, P> of(@NonNull Function<F, P> extractor,
                                                                    @NonNull Predicate<P> validator,
                                                                    @NonNull String errorMessage) {
    return new FilterValidatorLogic<>(extractor, validator, errorMessage);
  }
}
