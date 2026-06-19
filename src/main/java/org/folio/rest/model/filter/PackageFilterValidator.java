package org.folio.rest.model.filter;

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.folio.rest.model.filter.FilterValidatorLogic.of;
import static org.folio.rest.util.RestConstants.FILTER_FREE_ACCESS_MAPPING;
import static org.folio.rest.util.RestConstants.FILTER_SELECTED_MAPPING;
import static org.folio.rest.util.RestConstants.FILTER_VISIBILITY_MAPPING;
import static org.folio.rest.util.RestConstants.SUPPORTED_PACKAGE_FILTER_TYPE_VALUES;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.folio.holdingsiq.model.PackageSearchField;
import org.folio.holdingsiq.model.SearchType;
import org.folio.holdingsiq.model.Sort;
import org.jspecify.annotations.NonNull;

public final class PackageFilterValidator implements FilterValidator<PackageRecordFilter> {

  static final String INVALID_QUERY_PARAMETER_MESSAGE = "Invalid Query Parameter for q: cannot be empty";
  static final String INVALID_SORT_PARAMETER_MESSAGE = "Invalid Query Parameter for sort";
  static final String INVALID_QUERY_TYPE_PARAMETER_MESSAGE = "Invalid Query Parameter for qType";
  static final String INVALID_QUERY_FIELD_PARAMETER_MESSAGE = "Invalid Query Parameter for qField";
  static final String INVALID_FILTER_CUSTOM_PARAMETER_MESSAGE =
    "Invalid Query Parameter for filter[custom]: only 'true' is supported";
  static final String INVALID_FILTER_TYPE_PARAMETER_MESSAGE = "Invalid Query Parameter for filter[type]";
  static final String INVALID_FILTER_FREE_ACCESS_PARAMETER_MESSAGE = "Invalid Query Parameter for filter[free-access]";
  static final String INVALID_FILTER_VISIBILITY_PARAMETER_MESSAGE = "Invalid Query Parameter for filter[visibility]";
  static final String INVALID_FILTER_SELECTED_PARAMETER_MESSAGE =
    "Invalid Query Parameter for filter[selected]";

  private static final List<FilterValidatorLogic<PackageRecordFilter>> VALIDATORS = List.of(
    of(PackageRecordFilter::getSort, PackageFilterValidator::isSortValid, INVALID_SORT_PARAMETER_MESSAGE),
    of(PackageRecordFilter::getQueryType, PackageFilterValidator::isQueryTypeValid,
      INVALID_QUERY_TYPE_PARAMETER_MESSAGE),
    of(PackageRecordFilter::getQueryField, PackageFilterValidator::isQueryFieldValid,
      INVALID_QUERY_FIELD_PARAMETER_MESSAGE),
    of(PackageRecordFilter::getQuery, PackageFilterValidator::isQueryValid, INVALID_QUERY_PARAMETER_MESSAGE),
    of(PackageRecordFilter::getFilterType, PackageFilterValidator::isFilterTypeValid,
      INVALID_FILTER_TYPE_PARAMETER_MESSAGE),
    of(PackageRecordFilter::getFilterCustom, PackageFilterValidator::isFilterCustomValid,
      INVALID_FILTER_CUSTOM_PARAMETER_MESSAGE),
    of(PackageRecordFilter::getFilterSelected, PackageFilterValidator::isFilterSelectedValid,
      INVALID_FILTER_SELECTED_PARAMETER_MESSAGE),
    of(PackageRecordFilter::getFilterVisibility, PackageFilterValidator::isFilterVisibilityValid,
      INVALID_FILTER_VISIBILITY_PARAMETER_MESSAGE),
    of(PackageRecordFilter::getFilterFreeAccess, PackageFilterValidator::isFilterFreeAccessValid,
      INVALID_FILTER_FREE_ACCESS_PARAMETER_MESSAGE)
  );

  @Override
  public List<String> validate(@NonNull PackageRecordFilter filter) {
    if (isNotEmpty(filter.getFilterTags())
        || isNotEmpty(filter.getFilterAccessType())) {
      return Collections.emptyList();
    }
    return VALIDATORS.stream()
      .map(logic -> validate(filter, logic))
      .filter(Optional::isPresent)
      .map(Optional::get)
      .toList();
  }

  private static boolean isFilterFreeAccessValid(String filterFreeAccess) {
    return filterFreeAccess != null && !FILTER_FREE_ACCESS_MAPPING.containsKey(filterFreeAccess);
  }

  private static boolean isFilterVisibilityValid(String filterVisibility) {
    return filterVisibility != null && !FILTER_VISIBILITY_MAPPING.containsKey(filterVisibility);
  }

  private static boolean isFilterSelectedValid(String filterSelected) {
    return filterSelected != null && !FILTER_SELECTED_MAPPING.containsKey(filterSelected);
  }

  private static boolean isFilterCustomValid(String filterCustom) {
    return filterCustom != null && !Boolean.parseBoolean(filterCustom);
  }

  private static boolean isFilterTypeValid(String filterType) {
    return filterType != null && !SUPPORTED_PACKAGE_FILTER_TYPE_VALUES.contains(filterType);
  }

  private static boolean isQueryValid(String anObject) {
    return "".equals(anObject);
  }

  private static boolean isQueryFieldValid(String queryField) {
    return !PackageSearchField.contains(queryField.toUpperCase());
  }

  private static boolean isQueryTypeValid(String queryType) {
    return !SearchType.contains(queryType.toUpperCase());
  }

  private static boolean isSortValid(String sort) {
    return !Sort.contains(sort.toUpperCase());
  }
}
