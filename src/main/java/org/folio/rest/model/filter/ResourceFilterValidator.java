package org.folio.rest.model.filter;

import static org.folio.rest.model.filter.FilterRules.optional;
import static org.folio.rest.model.filter.FilterValidatorLogic.of;

import java.util.List;
import org.folio.holdingsiq.model.TitleFilterType;
import org.folio.rest.util.IdParser;
import org.jspecify.annotations.NonNull;

public final class ResourceFilterValidator implements FilterValidator<ResourceFilter> {

  static final String INVALID_QUERY_PARAMETER_MESSAGE = "Invalid Query Parameter for q: cannot be empty";
  static final String INVALID_SORT_PARAMETER_MESSAGE = "Invalid Query Parameter for sort";
  static final String INVALID_QUERY_FIELD_PARAMETER_MESSAGE = "Invalid Query Parameter for q[field]";
  static final String INVALID_FILTER_TYPE_PARAMETER_MESSAGE = "Invalid Query Parameter for filter[type]";
  static final String INVALID_FILTER_SELECTED_PARAMETER_MESSAGE =
    "Invalid Query Parameter for filter[selected]";
  static final String INVALID_FILTER_ACCESS_PARAMETER_MESSAGE = "Invalid Query Parameter for filter[access]";
  static final String INVALID_PACKAGE_ID_PARAMETER_MESSAGE = "Invalid package id";

  private static final List<FilterValidatorLogic<ResourceFilter, ?>> VALIDATORS = List.of(
    of(ResourceFilter::getSort, FilterRules::isSortValid, INVALID_SORT_PARAMETER_MESSAGE),
    of(ResourceFilter::getQueryField, optional(TitleSearch.FIELDS::contains),
      INVALID_QUERY_FIELD_PARAMETER_MESSAGE),
    of(ResourceFilter::getQuery, FilterRules::isQueryValid, INVALID_QUERY_PARAMETER_MESSAGE),
    of(ResourceFilter::getFilterType, optional(TitleFilterType::contains),
      INVALID_FILTER_TYPE_PARAMETER_MESSAGE),
    of(ResourceFilter::getFilterSelected, FilterRules::isFilterSelectedValid,
      INVALID_FILTER_SELECTED_PARAMETER_MESSAGE),
    of(ResourceFilter::getFilterAccess, FilterRules::isFilterAccessValid,
      INVALID_FILTER_ACCESS_PARAMETER_MESSAGE),
    of(ResourceFilter::getPackageId, ResourceFilterValidator::isPackageIdValid,
      INVALID_PACKAGE_ID_PARAMETER_MESSAGE)
  );

  @Override
  public void validate(@NonNull ResourceFilter filter) {
    validateAll(filter, VALIDATORS);
  }

  private static boolean isPackageIdValid(String packageId) {
    if (packageId == null) {
      return true;
    }
    try {
      IdParser.parsePackageId(packageId);
      return true;
    } catch (RuntimeException e) {
      return false;
    }
  }
}
