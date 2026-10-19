package org.folio.rest.model.filter;

import static org.folio.rest.util.RestConstants.FILTER_FREE_ACCESS_MAPPING;
import static org.folio.rest.util.RestConstants.FILTER_SELECTED_MAPPING;

import java.util.function.Predicate;
import org.apache.commons.lang3.StringUtils;
import org.folio.holdingsiq.model.SearchType;
import org.folio.holdingsiq.model.Sort;

/**
 * Validation rules shared by the package, title and resource filter validators. A missing optional parameter is
 * always valid.
 */
final class FilterRules {

  private FilterRules() {
  }

  static boolean isSortValid(String sort) {
    return Sort.contains(sort.toUpperCase());
  }

  static boolean isQueryValid(String query) {
    return query == null || !StringUtils.isBlank(query);
  }

  static boolean isQueryTypeValid(String queryType) {
    return optional(SearchType::contains).test(queryType);
  }

  static boolean isFilterSelectedValid(String filterSelected) {
    return optional(FILTER_SELECTED_MAPPING::containsKey).test(filterSelected);
  }

  static boolean isFilterAccessValid(String filterAccess) {
    return optional(FILTER_FREE_ACCESS_MAPPING::containsKey).test(filterAccess);
  }

  static Predicate<String> optional(Predicate<String> rule) {
    return value -> value == null || rule.test(value);
  }
}
