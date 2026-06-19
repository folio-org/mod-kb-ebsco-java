package org.folio.rest.model.filter;

import static java.util.Arrays.asList;
import static org.apache.commons.collections4.IterableUtils.countMatches;
import static org.apache.commons.collections4.IterableUtils.matchesAny;
import static org.apache.commons.lang3.StringUtils.isNumeric;
import static org.folio.rest.util.RestConstants.FILTER_SELECTED_MAPPING;
import static org.folio.rest.util.RestConstants.SUPPORTED_PACKAGE_FILTER_TYPE_VALUES;
import static org.folio.rest.util.RestConstants.SUPPORTED_TITLE_FILTER_TYPE_VALUES;

import jakarta.validation.ValidationException;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.folio.holdingsiq.model.Sort;
import org.folio.rest.util.IdParser;

public final class FilterValidators {

  static final String INVALID_QUERY_PARAMETER_MESSAGE = "Search parameter cannot be empty";
  static final String INVALID_SORT_PARAMETER_MESSAGE = "Invalid Query Parameter for sort";
  static final String INVALID_FILTER_CUSTOM_PARAMETER_MESSAGE =
    "Invalid Query Parameter for filter[custom]: only 'true' is supported";
  static final String INVALID_FILTER_TYPE_PARAMETER_MESSAGE = "Invalid Query Parameter for filter[type]";
  static final String INVALID_FILTER_SELECTED_PARAMETER_MESSAGE =
    "Invalid Query Parameter for filter[selected]";
  static final String INVALID_FILTER_PACKAGE_IDS_PARAMETER_MESSAGE =
    "Invalid Query Parameter for filter[packageIds]";
  static final String CONFLICTING_KEYWORD_SEARCH_PARAMETERS_MESSAGE = "Conflicting filter parameters";
  static final String MISSING_KEYWORD_SEARCH_PARAMETERS_MESSAGE =
    "All of filter[name], filter[isxn], filter[subject] and filter[publisher] cannot be missing.";
  static final String INVALID_KEYWORD_SEARCH_PARAMETERS_MESSAGE =
    "Value of required parameter filter[name], filter[isxn], filter[subject] or filter[publisher] is missing.";

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

  public static void validatePackage(PackageRecordFilter filter) {
    if (CollectionUtils.isNotEmpty(filter.getFilterTags())
        || CollectionUtils.isNotEmpty(filter.getFilterAccessType())) {
      return;
    }
    validateSort(filter.getSort());
    validateQuery(filter.getQuery());
    validateFilterType(filter.getFilterType(), SUPPORTED_PACKAGE_FILTER_TYPE_VALUES);
    validateFilterCustom(filter.getFilterCustom());
    validateFilterSelected(filter.getFilterSelected());
    validateProviderId(filter.getProviderId());
  }

  public static void validateResource(ResourceFilter filter) {
    if (CollectionUtils.isNotEmpty(filter.getFilterTags())
        || CollectionUtils.isNotEmpty(filter.getFilterAccessType())) {
      return;
    }
    validateSort(filter.getSort());
    validatePackageId(filter.getPackageId());
    validateFilterType(filter.getFilterType(), SUPPORTED_TITLE_FILTER_TYPE_VALUES);
    validateFilterSelected(filter.getFilterSelected());
    validateKeywordSearch(filter.getFilterName(), filter.getFilterIsxn(),
        filter.getFilterSubject(), filter.getFilterPublisher(), true);
  }

  public static void validateTitle(TitleFilter filter) {
    if (CollectionUtils.isNotEmpty(filter.getFilterTags())
        || CollectionUtils.isNotEmpty(filter.getFilterAccessType())) {
      return;
    }
    validateSort(filter.getSort());
    validateFilterType(filter.getFilterType(), SUPPORTED_TITLE_FILTER_TYPE_VALUES);
    validateFilterSelected(filter.getFilterSelected());
    validateFilterPackageIds(filter.getFilterPackageIds());
    validateKeywordSearch(filter.getFilterName(), filter.getFilterIsxn(),
        filter.getFilterSubject(), filter.getFilterPublisher(), false);
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

  private static void validateFilterType(String filterType, Collection<String> supportedValues) {
    if (filterType != null && !supportedValues.contains(filterType)) {
      throw new ValidationException(INVALID_FILTER_TYPE_PARAMETER_MESSAGE);
    }
  }

  private static void validateFilterCustom(String filterCustom) {
    if (filterCustom != null && !Boolean.parseBoolean(filterCustom)) {
      throw new ValidationException(INVALID_FILTER_CUSTOM_PARAMETER_MESSAGE);
    }
  }

  private static void validateFilterSelected(String filterSelected) {
    if (filterSelected != null && !FILTER_SELECTED_MAPPING.containsKey(filterSelected)) {
      throw new ValidationException(INVALID_FILTER_SELECTED_PARAMETER_MESSAGE);
    }
  }

  private static void validateFilterPackageIds(List<String> packageIds) {
    if (packageIds != null && !packageIds.isEmpty()) {
      packageIds.forEach(id -> {
        if (!isNumeric(id)) {
          throw new ValidationException(INVALID_FILTER_PACKAGE_IDS_PARAMETER_MESSAGE);
        }
      });
    }
  }

  private static void validateKeywordSearch(String filterName, String filterIsxn,
                                           String filterSubject, String filterPublisher,
                                           boolean allowNullFilters) {
    List<String> searchParameters = asList(filterName, filterIsxn, filterSubject, filterPublisher);

    long nonNullFilters = countMatches(searchParameters, Objects::nonNull);

    if (nonNullFilters > 1) {
      throw new ValidationException(CONFLICTING_KEYWORD_SEARCH_PARAMETERS_MESSAGE);
    }
    if (nonNullFilters < 1 && !allowNullFilters) {
      throw new ValidationException(MISSING_KEYWORD_SEARCH_PARAMETERS_MESSAGE);
    }
    if (matchesAny(searchParameters, StringUtils.EMPTY::equals)) {
      throw new ValidationException(INVALID_KEYWORD_SEARCH_PARAMETERS_MESSAGE);
    }
  }

  private static void validateProviderId(String providerId) {
    if (providerId != null) {
      IdParser.parseProviderId(providerId);
    }
  }

  private static void validatePackageId(String packageId) {
    if (packageId != null) {
      IdParser.parsePackageId(packageId);
    }
  }
}
