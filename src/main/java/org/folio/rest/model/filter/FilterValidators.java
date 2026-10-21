package org.folio.rest.model.filter;

import jakarta.validation.ValidationException;
import org.apache.commons.collections4.CollectionUtils;
import org.folio.holdingsiq.model.Sort;

public final class FilterValidators {

  static final String INVALID_QUERY_PARAMETER_MESSAGE = "Search parameter cannot be empty";
  static final String INVALID_SORT_PARAMETER_MESSAGE = "Invalid Query Parameter for sort";

  private FilterValidators() {
  }

  public static void validateProvider(ProviderFilter filter) {
    if (CollectionUtils.isNotEmpty(filter.getFilterTags())
        || CollectionUtils.isNotEmpty(filter.getFilterAccessType())) {
      return;
    }
    validateSort(filter.getSort());
    validateQuery(filter.getQuery());
  }

  private static void validateSort(String sort) {
    if (!Sort.contains(sort.toUpperCase())) {
      throw new ValidationException(INVALID_SORT_PARAMETER_MESSAGE);
    }
  }

  private static void validateQuery(String query) {
    if ("".equals(query)) {
      throw new ValidationException(INVALID_QUERY_PARAMETER_MESSAGE);
    }
  }
}
