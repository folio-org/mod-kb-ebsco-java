package org.folio.rest.model.filter;

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.folio.rest.model.filter.FilterValidatorLogic.of;
import static org.folio.rest.util.RestConstants.FILTER_FREE_ACCESS_MAPPING;
import static org.folio.rest.util.RestConstants.FILTER_SELECTED_MAPPING;
import static org.folio.rest.util.RestConstants.FILTER_VISIBILITY_MAPPING;
import static org.folio.rest.util.RestConstants.SUPPORTED_PACKAGE_FILTER_TYPE_VALUES;

import java.util.List;
import java.util.Optional;
import org.folio.holdingsiq.model.PackageSearchField;
import org.folio.holdingsiq.model.SearchType;
import org.folio.holdingsiq.model.Sort;
import org.folio.rest.exception.QueryParamsValidationException;
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
  public void validate(@NonNull PackageRecordFilter filter) {
    if (isNotEmpty(filter.getFilterTags())
        || isNotEmpty(filter.getFilterAccessType())) {
      return;
    }
    var failMessages = VALIDATORS.stream()
      .map(logic -> validate(filter, logic))
      .filter(Optional::isPresent)
      .map(Optional::get)
      .toList();
    if (!failMessages.isEmpty()) {
      throw new QueryParamsValidationException(failMessages);
    }
  }

  private static boolean isFilterFreeAccessValid(String filterFreeAccess) {
    if (filterFreeAccess == null) {
      return true;
    }
    return FILTER_FREE_ACCESS_MAPPING.containsKey(filterFreeAccess);
  }

  private static boolean isFilterVisibilityValid(String filterVisibility) {
    if (filterVisibility == null) {
      return true;
    }
    return FILTER_VISIBILITY_MAPPING.containsKey(filterVisibility);
  }

  private static boolean isFilterSelectedValid(String filterSelected) {
    if (filterSelected == null) {
      return true;
    }
    return FILTER_SELECTED_MAPPING.containsKey(filterSelected);
  }

  private static boolean isFilterCustomValid(String filterCustom) {
    if (filterCustom == null) {
      return true;
    }
    return Boolean.parseBoolean(filterCustom);
  }

  private static boolean isFilterTypeValid(String filterType) {
    if (filterType == null) {
      return true;
    }
    return SUPPORTED_PACKAGE_FILTER_TYPE_VALUES.contains(filterType);
  }

  private static boolean isQueryValid(String anObject) {
    return !"".equals(anObject);
  }

  private static boolean isQueryFieldValid(String queryField) {
    if (queryField == null) {
      return true;
    }
    return PackageSearchField.contains(queryField);
  }

  private static boolean isQueryTypeValid(String queryType) {
    if (queryType == null) {
      return true;
    }
    return SearchType.contains(queryType);
  }

  private static boolean isSortValid(String sort) {
    return Sort.contains(sort.toUpperCase());
  }
}
