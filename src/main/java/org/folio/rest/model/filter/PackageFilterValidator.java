package org.folio.rest.model.filter;

import static org.folio.rest.model.filter.FilterRules.optional;
import static org.folio.rest.model.filter.FilterValidatorLogic.of;
import static org.folio.rest.util.RestConstants.FILTER_VISIBILITY_MAPPING;

import java.util.List;
import java.util.regex.Pattern;
import org.folio.holdingsiq.model.PackageFilterType;
import org.folio.holdingsiq.model.PackageSearchField;
import org.jspecify.annotations.NonNull;

public final class PackageFilterValidator implements FilterValidator<PackageRecordFilter> {

  static final String INVALID_QUERY_PARAMETER_MESSAGE = "Invalid Query Parameter for q: cannot be empty";
  static final String INVALID_SORT_PARAMETER_MESSAGE = "Invalid Query Parameter for sort";
  static final String INVALID_QUERY_TYPE_PARAMETER_MESSAGE = "Invalid Query Parameter for q[type]";
  static final String INVALID_QUERY_FIELD_PARAMETER_MESSAGE = "Invalid Query Parameter for q[field]";
  static final String INVALID_FILTER_CUSTOM_PARAMETER_MESSAGE =
    "Invalid Query Parameter for filter[custom]: only 'true' is supported";
  static final String INVALID_FILTER_TYPE_PARAMETER_MESSAGE = "Invalid Query Parameter for filter[type]";
  static final String INVALID_FILTER_FREE_ACCESS_PARAMETER_MESSAGE = "Invalid Query Parameter for filter[free-access]";
  static final String INVALID_FILTER_VISIBILITY_PARAMETER_MESSAGE = "Invalid Query Parameter for filter[visibility]";
  static final String INVALID_FILTER_SELECTED_PARAMETER_MESSAGE =
    "Invalid Query Parameter for filter[selected]";
  static final String INVALID_FILTER_SUBJECT_PARAMETER_MESSAGE =
    "Invalid Query Parameter for filter[subject]: only numeric subject ids are supported";

  private static final Pattern SUBJECT_ID_PATTERN = Pattern.compile("\\d{1,9}");

  private static final List<FilterValidatorLogic<PackageRecordFilter, ?>> VALIDATORS = List.of(
    of(PackageRecordFilter::getSort, FilterRules::isSortValid, INVALID_SORT_PARAMETER_MESSAGE),
    of(PackageRecordFilter::getQueryType, FilterRules::isQueryTypeValid,
      INVALID_QUERY_TYPE_PARAMETER_MESSAGE),
    of(PackageRecordFilter::getQueryField, optional(PackageSearchField::contains),
      INVALID_QUERY_FIELD_PARAMETER_MESSAGE),
    of(PackageRecordFilter::getQuery, FilterRules::isQueryValid, INVALID_QUERY_PARAMETER_MESSAGE),
    of(PackageRecordFilter::getFilterType, optional(PackageFilterType::contains),
      INVALID_FILTER_TYPE_PARAMETER_MESSAGE),
    of(PackageRecordFilter::getFilterCustom, optional(Boolean::parseBoolean),
      INVALID_FILTER_CUSTOM_PARAMETER_MESSAGE),
    of(PackageRecordFilter::getFilterSelected, FilterRules::isFilterSelectedValid,
      INVALID_FILTER_SELECTED_PARAMETER_MESSAGE),
    of(PackageRecordFilter::getFilterVisibility, optional(FILTER_VISIBILITY_MAPPING::containsKey),
      INVALID_FILTER_VISIBILITY_PARAMETER_MESSAGE),
    of(PackageRecordFilter::getFilterFreeAccess, FilterRules::isFilterAccessValid,
      INVALID_FILTER_FREE_ACCESS_PARAMETER_MESSAGE),
    of(PackageRecordFilter::getFilterSubject, PackageFilterValidator::isFilterSubjectValid,
      INVALID_FILTER_SUBJECT_PARAMETER_MESSAGE)
  );

  @Override
  public void validate(@NonNull PackageRecordFilter filter) {
    validateAll(filter, VALIDATORS);
  }

  private static boolean isFilterSubjectValid(List<String> filterSubject) {
    if (filterSubject == null) {
      return true;
    }
    return filterSubject.stream().allMatch(id -> id != null && SUBJECT_ID_PATTERN.matcher(id).matches());
  }
}
