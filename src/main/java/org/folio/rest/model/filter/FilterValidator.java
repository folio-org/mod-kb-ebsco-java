package org.folio.rest.model.filter;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import org.jspecify.annotations.NonNull;

public sealed interface FilterValidator<F extends Filter> permits PackageFilterValidator {

  void validate(@NonNull F filter);

  default Optional<String> validate(@NonNull F filter,
                                    @NonNull Function<F, String> paramExtractor,
                                    @NonNull Predicate<String> validator,
                                    @NonNull String errorMessage) {
    var param = paramExtractor.apply(filter);
    if (validator.test(param)) {
      return Optional.empty();
    } else {
      return Optional.of(errorMessage);
    }
  }

  default Optional<String> validate(@NonNull F filter, @NonNull FilterValidatorLogic<F> logic) {
    var param = logic.extractor().apply(filter);
    if (logic.validator().test(param)) {
      return Optional.empty();
    } else {
      return Optional.of(logic.errorMessage());
    }
  }
}
