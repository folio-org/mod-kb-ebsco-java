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
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.folio.holdingsiq.model.Sort;
import org.folio.repository.RecordType;
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

  private static final Map<RecordType, FilterValidator> VALIDATORS = new EnumMap<>(RecordType.class);

  static {
    VALIDATORS.put(RecordType.PROVIDER, FilterValidators::validateProvider);
    VALIDATORS.put(RecordType.PACKAGE, FilterValidators::validatePackage);
    VALIDATORS.put(RecordType.RESOURCE, FilterValidators::validateResource);
    VALIDATORS.put(RecordType.TITLE, FilterValidators::validateTitle);
  }

  private FilterValidators() {
  }

  public static void validate(Filter filter) {
    if (CollectionUtils.isNotEmpty(filter.getFilterTags())
        || CollectionUtils.isNotEmpty(filter.getFilterAccessType())) {
      return;
    }

    validateSort(filter);

    FilterValidator validator = VALIDATORS.get(filter.getRecordType());
    if (validator != null) {
      validator.validate(filter);
    }
  }

  private static void validateProvider(Filter filter) {
    validateQuery(filter);
  }

  private static void validatePackage(Filter filter) {
    validateQuery(filter);
    validateFilterType(filter, SUPPORTED_PACKAGE_FILTER_TYPE_VALUES);
    validateFilterCustom(filter);
    validateFilterSelected(filter);
    validateProviderId(filter);
  }

  private static void validateResource(Filter filter) {
    validatePackageId(filter);
    validateFilterType(filter, SUPPORTED_TITLE_FILTER_TYPE_VALUES);
    validateFilterSelected(filter);
    validateKeywordSearch(filter, true);
  }

  private static void validateTitle(Filter filter) {
    validateFilterType(filter, SUPPORTED_TITLE_FILTER_TYPE_VALUES);
    validateFilterSelected(filter);
    validateFilterPackageIds(filter);
    validateKeywordSearch(filter, false);
  }

  private static void validateSort(Filter filter) {
    if (!Sort.contains(filter.getSort().toUpperCase())) {
      throw new ValidationException(INVALID_SORT_PARAMETER_MESSAGE);
    }
  }

  private static void validateQuery(Filter filter) {
    if ("".equals(filter.getQuery())) {
      throw new ValidationException(INVALID_QUERY_PARAMETER_MESSAGE);
    }
  }

  private static void validateFilterType(Filter filter, Collection<String> supportedValues) {
    if (filter.getFilterType() != null && !supportedValues.contains(filter.getFilterType())) {
      throw new ValidationException(INVALID_FILTER_TYPE_PARAMETER_MESSAGE);
    }
  }

  private static void validateFilterCustom(Filter filter) {
    if (filter.getFilterCustom() != null && !Boolean.parseBoolean(filter.getFilterCustom())) {
      throw new ValidationException(INVALID_FILTER_CUSTOM_PARAMETER_MESSAGE);
    }
  }

  private static void validateFilterSelected(Filter filter) {
    if (filter.getFilterSelected() != null
        && !FILTER_SELECTED_MAPPING.containsKey(filter.getFilterSelected())) {
      throw new ValidationException(INVALID_FILTER_SELECTED_PARAMETER_MESSAGE);
    }
  }

  private static void validateFilterPackageIds(Filter filter) {
    List<String> packageIds = filter.getFilterPackageIds();
    if (packageIds != null && !packageIds.isEmpty()) {
      packageIds.forEach(id -> {
        if (!isNumeric(id)) {
          throw new ValidationException(INVALID_FILTER_PACKAGE_IDS_PARAMETER_MESSAGE);
        }
      });
    }
  }

  private static void validateKeywordSearch(Filter filter, boolean allowNullFilters) {
    List<String> searchParameters =
      asList(filter.getFilterName(), filter.getFilterIsxn(),
        filter.getFilterSubject(), filter.getFilterPublisher());

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

  private static void validateProviderId(Filter filter) {
    if (filter.getProviderId() != null) {
      IdParser.parseProviderId(filter.getProviderId());
    }
  }

  private static void validatePackageId(Filter filter) {
    if (filter.getPackageId() != null) {
      IdParser.parsePackageId(filter.getPackageId());
    }
  }
}
