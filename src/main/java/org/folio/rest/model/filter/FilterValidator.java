package org.folio.rest.model.filter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.folio.rest.exception.QueryParamsValidationException;
import org.jspecify.annotations.NonNull;

public sealed interface FilterValidator<F extends Filter> permits PackageFilterValidator, TitleFilterValidator,
    ResourceFilterValidator {

  void validate(@NonNull F filter);

  default <P> Optional<String> validate(@NonNull F filter, @NonNull FilterValidatorLogic<F, P> logic) {
    var param = logic.extractor().apply(filter);
    if (logic.validator().test(param)) {
      return Optional.empty();
    } else {
      return Optional.of(logic.errorMessage());
    }
  }

  /**
   * Runs all validators and throws with every failure message. Filters by tags or access types are not validated,
   * they are resolved from the database.
   */
  default void validateAll(@NonNull F filter, @NonNull List<FilterValidatorLogic<F, ?>> validators) {
    if (filter.isTagsFilter() || filter.isAccessTypeFilter()) {
      return;
    }
    var failMessages = validators.stream()
      .map(logic -> validate(filter, logic))
      .flatMap(Optional::stream)
      .collect(Collectors.toCollection(ArrayList::new));
    if (!failMessages.isEmpty()) {
      throw new QueryParamsValidationException(failMessages);
    }
  }
}
