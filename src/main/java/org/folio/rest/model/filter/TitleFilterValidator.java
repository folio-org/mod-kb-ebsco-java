package org.folio.rest.model.filter;

import static org.folio.rest.model.filter.FilterRules.optional;
import static org.folio.rest.model.filter.FilterValidatorLogic.of;

import java.util.List;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.folio.holdingsiq.model.TitleFilterType;
import org.jspecify.annotations.NonNull;

public final class TitleFilterValidator implements FilterValidator<TitleFilter> {

  static final String INVALID_QUERY_PARAMETER_MESSAGE = "Invalid Query Parameter for q: cannot be empty";
  static final String MISSING_QUERY_PARAMETER_MESSAGE = "Invalid Query Parameter for q: is required";
  static final String INVALID_SORT_PARAMETER_MESSAGE = "Invalid Query Parameter for sort";
  static final String INVALID_QUERY_TYPE_PARAMETER_MESSAGE = "Invalid Query Parameter for q[type]";
  static final String INVALID_QUERY_FIELD_PARAMETER_MESSAGE = "Invalid Query Parameter for q[field]";
  static final String INVALID_FILTER_TYPE_PARAMETER_MESSAGE = "Invalid Query Parameter for filter[type]";
  static final String INVALID_FILTER_SELECTED_PARAMETER_MESSAGE =
    "Invalid Query Parameter for filter[selected]";
  static final String INVALID_FILTER_ACCESS_PARAMETER_MESSAGE = "Invalid Query Parameter for filter[access]";
  static final String INVALID_FILTER_PACKAGE_IDS_PARAMETER_MESSAGE =
    "Invalid Query Parameter for filter[packageIds]";

  private static final List<FilterValidatorLogic<TitleFilter, ?>> VALIDATORS = List.of(
    of(TitleFilter::getSort, FilterRules::isSortValid, INVALID_SORT_PARAMETER_MESSAGE),
    of(TitleFilter::getQueryType, FilterRules::isQueryTypeValid,
      INVALID_QUERY_TYPE_PARAMETER_MESSAGE),
    of(TitleFilter::getQueryField, optional(TitleSearch.FIELDS::contains),
      INVALID_QUERY_FIELD_PARAMETER_MESSAGE),
    // HoldingsIQ rejects a titles search without a search term
    of(TitleFilter::getQuery, Objects::nonNull, MISSING_QUERY_PARAMETER_MESSAGE),
    of(TitleFilter::getQuery, FilterRules::isQueryValid, INVALID_QUERY_PARAMETER_MESSAGE),
    of(TitleFilter::getFilterType, optional(TitleFilterType::contains),
      INVALID_FILTER_TYPE_PARAMETER_MESSAGE),
    of(TitleFilter::getFilterSelected, FilterRules::isFilterSelectedValid,
      INVALID_FILTER_SELECTED_PARAMETER_MESSAGE),
    of(TitleFilter::getFilterAccess, FilterRules::isFilterAccessValid,
      INVALID_FILTER_ACCESS_PARAMETER_MESSAGE),
    of(TitleFilter::getFilterPackageIds, TitleFilterValidator::isFilterPackageIdsValid,
      INVALID_FILTER_PACKAGE_IDS_PARAMETER_MESSAGE)
  );

  @Override
  public void validate(@NonNull TitleFilter filter) {
    validateAll(filter, VALIDATORS);
  }

  private static boolean isFilterPackageIdsValid(List<String> packageIds) {
    return packageIds == null || packageIds.stream().allMatch(TitleFilterValidator::isIntegerValue);
  }

  private static boolean isIntegerValue(String value) {
    return StringUtils.isNumeric(value) && NumberUtils.toInt(value, -1) >= 0;
  }
}
