package org.folio.rest.model.filter;

import static java.util.Arrays.asList;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.collections4.IterableUtils.countMatches;
import static org.apache.commons.collections4.IterableUtils.matchesAll;
import static org.apache.commons.collections4.IterableUtils.matchesAny;
import static org.apache.commons.lang3.StringUtils.appendIfMissing;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.folio.rest.util.RestConstants.*;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

import javax.validation.ValidationException;

import lombok.Builder;
import lombok.Value;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import org.folio.holdingsiq.model.FilterQuery;
import org.folio.holdingsiq.model.PackageId;
import org.folio.holdingsiq.model.Sort;
import org.folio.repository.RecordType;
import org.folio.rest.util.IdParser;

@Value
@Builder
public class Filter {

  String query;
  String filterIsxn;
  String filterName;
  String filterPublisher;
  String filterSubject;
  String filterType;
  String filterCustom;
  String filterSelected;

  String packageId;
  String providerId;

  List<String> filterTags;
  List<String> filterAccessType;

  String sort;
  int page;
  int count;

  RecordType recordType;

  public static FilterBuilder builder() {
    return new FilterBuilder() {

      @Override
      public Filter build() {
        validate();
        return super.build();
      }
    };
  }

  public boolean isTagsFilter() {
    return isCheckedFilter(filterTags, filterAccessType);
  }

  public boolean isAccessTypeFilter() {
    return isCheckedFilter(filterAccessType, filterTags);
  }

  public AccessTypeFilter createAccessTypeFilter() {
    AccessTypeFilter accessTypeFilter = new AccessTypeFilter();
    accessTypeFilter.setAccessTypeNames(filterAccessType);
    accessTypeFilter.setCount(count);
    accessTypeFilter.setPage(page);

    if (recordType == RecordType.PACKAGE) {
      accessTypeFilter.setRecordIdPrefix(providerId);
      accessTypeFilter.setRecordType(RecordType.PACKAGE);
    } else if (recordType == RecordType.RESOURCE) {
      accessTypeFilter.setRecordIdPrefix(packageId);
      accessTypeFilter.setRecordType(RecordType.RESOURCE);
    } else if (recordType == RecordType.TITLE) {
      accessTypeFilter.setRecordType(RecordType.RESOURCE);
    }
    return accessTypeFilter;
  }

  public TagFilter createTagFilter() {
    TagFilter.TagFilterBuilder builder = TagFilter.builder()
      .tags(filterTags)
      .recordType(recordType)
      .offset((page - 1) * count)
      .count(count);

    if (recordType == RecordType.PACKAGE) {
      builder.recordIdPrefix(createRecordIdPrefix(providerId));
    } else if (recordType == RecordType.RESOURCE) {
      builder.recordIdPrefix(createRecordIdPrefix(packageId));
    } else if (recordType == RecordType.TITLE) {
      builder.recordType(RecordType.RESOURCE);
      builder.recordIdPrefix("");
    } else {
      builder.recordIdPrefix("");
    }

    return builder.build();
  }

  public FilterQuery createFilterQuery() {
    return FilterQuery.builder()
      .type(filterType)
      .name(filterName)
      .isxn(filterIsxn)
      .subject(filterSubject)
      .publisher(filterPublisher)
      .selected(getFilterSelected())
      .build();
  }

  public Long getProviderId() {
    return IdParser.parseProviderId(providerId);
  }

  public PackageId getPackageId() {
    return IdParser.parsePackageId(packageId);
  }

  public String getFilterSelected() {
    return filterSelected == null ? null : FILTER_SELECTED_MAPPING.get(filterSelected);
  }

  public Boolean getFilterCustom() {
    return filterCustom == null ? null : Boolean.parseBoolean(filterCustom);
  }

  public Sort getSort() {
    return Sort.valueOf(sort.toUpperCase());
  }

  private String createRecordIdPrefix(String providerId) {
    return isBlank(providerId) ? "" : appendIfMissing(providerId, "-");
  }

  private boolean isCheckedFilter(List<String> checkedFilter, List<String> otherFilter) {
    return isNotEmpty(checkedFilter)
      && matchesAll(checkedFilter, StringUtils::isNotBlank)
      && CollectionUtils.isEmpty(otherFilter)
      && matchesAll(
      asList(query, filterIsxn, filterName, filterPublisher, filterSubject, filterCustom, filterSelected),
      StringUtils::isBlank);
  }

  public static class FilterBuilder {

    private static final String INVALID_QUERY_PARAMETER_MESSAGE = "Search parameter cannot be empty";
    private static final String INVALID_SORT_PARAMETER_MESSAGE = "Invalid Query Parameter for sort";
    private static final String INVALID_FILTER_CUSTOM_PARAMETER_MESSAGE = "Invalid Query Parameter for filter[custom]";
    private static final String INVALID_FILTER_CUSTOM_WITH_FALSE_PARAMETER_MESSAGE = "Query Parameter false is not supported for filter[custom]";
    private static final String INVALID_FILTER_TYPE_PARAMETER_MESSAGE = "Invalid Query Parameter for filter[type]";
    private static final String INVALID_FILTER_SELECTED_PARAMETER_MESSAGE = "Invalid Query Parameter for filter[selected]";
    private static final String CONFLICTING_KEYWORD_SEARCH_PARAMETERS_MESSAGE = "Conflicting filter parameters";
    private static final String MISSING_KEYWORD_SEARCH_PARAMETERS_MESSAGE =
      "All of filter[name], filter[isxn], filter[subject] and filter[publisher] cannot be missing.";
    private static final String INVALID_KEYWORD_SEARCH_PARAMETERS_MESSAGE =
      "Value of required parameter filter[name], filter[isxn], filter[subject] or filter[publisher] is missing.";

    void validate() {
      if (isEmpty(this.filterTags) && isEmpty(this.filterAccessType)) {
        validateSort();

        if (this.recordType == RecordType.PROVIDER) {
          validateQuery();
        } else if (this.recordType == RecordType.PACKAGE) {
          validateQuery();
          validatePackageFilterType();
          validateFilterCustom();
          validateFilterSelected();
          validateProviderId();
        } else if (this.recordType == RecordType.RESOURCE) {
          validatePackageId();
          validateTitleFilterType();
          validateFilterSelected();
          validateKeywordSearch(true);
        } else if (this.recordType == RecordType.TITLE) {
          validateTitleFilterType();
          validateFilterSelected();
          validateKeywordSearch(false);
        }
      }
    }

    private void validateKeywordSearch(boolean allowNullFilters) {
      List<String> searchParameters = asList(this.filterName, this.filterIsxn, this.filterSubject, this.filterPublisher);

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

    private void validateFilterSelected() {
      if (this.filterSelected != null && !FILTER_SELECTED_MAPPING.containsKey(this.filterSelected)) {
        throw new ValidationException(INVALID_FILTER_SELECTED_PARAMETER_MESSAGE);
      }
    }

    private void validatePackageFilterType() {
      if (this.filterType != null &&
        !SUPPORTED_PACKAGE_FILTER_TYPE_VALUES.contains(this.filterType)) {
        throw new ValidationException(INVALID_FILTER_TYPE_PARAMETER_MESSAGE);
      }
    }

    private void validateTitleFilterType() {
      if (this.filterType != null &&
        !SUPPORTED_TITLE_FILTER_TYPE_VALUES.contains(this.filterType)) {
        throw new ValidationException(INVALID_FILTER_TYPE_PARAMETER_MESSAGE);
      }
    }

    private void validateFilterCustom() {
      if (this.filterCustom != null && !Boolean.parseBoolean(this.filterCustom)) {
        if (this.filterCustom.equalsIgnoreCase("false")) {
          throw new ValidationException(INVALID_FILTER_CUSTOM_WITH_FALSE_PARAMETER_MESSAGE);
        } else {
          throw new ValidationException(INVALID_FILTER_CUSTOM_PARAMETER_MESSAGE);
        }
      }
    }

    private void validateSort() {
      if (!Sort.contains(this.sort.toUpperCase())) {
        throw new ValidationException(INVALID_SORT_PARAMETER_MESSAGE);
      }
    }

    private void validateQuery() {
      if ("".equals(this.query)) {
        throw new ValidationException(INVALID_QUERY_PARAMETER_MESSAGE);
      }
    }

    private void validateProviderId() {
      if (this.providerId != null) {
        IdParser.parseProviderId(this.providerId);
      }
    }

    private void validatePackageId() {
      if (this.packageId != null) {
        IdParser.parsePackageId(this.packageId);
      }
    }
  }
}
