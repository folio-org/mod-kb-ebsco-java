package org.folio.rest.model.filter;

import static java.util.Arrays.asList;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.collections4.IterableUtils.matchesAll;
import static org.folio.rest.util.RestConstants.FILTER_SELECTED_MAPPING;

import java.util.List;
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
  List<String> filterPackageIds;

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
        Filter filter = super.build();
        FilterValidators.validate(filter);
        return filter;
      }
    };
  }

  public static Filter getSortableFilter(Filter.FilterBuilder filterBuilder, String sort, int page, int count) {
    return filterBuilder
      .sort(sort)
      .page(page)
      .count(count)
      .build();
  }

  public boolean isTagsFilter() {
    return isCheckedFilter(filterTags, filterAccessType);
  }

  public boolean isAccessTypeFilter() {
    return isCheckedFilter(filterAccessType, filterTags);
  }

  public FilterQuery createFilterQuery() {
    return FilterQuery.builder()
      .type(filterType)
      .name(filterName)
      .isxn(filterIsxn)
      .subject(filterSubject)
      .publisher(filterPublisher)
      .selected(resolveFilterSelected())
      .packageIds(resolveFilterPackageIds())
      .build();
  }

  public Integer parseProviderId() {
    return IdParser.parseProviderId(providerId);
  }

  public PackageId parsePackageId() {
    return IdParser.parsePackageId(packageId);
  }

  public String resolveFilterSelected() {
    return filterSelected == null ? null : FILTER_SELECTED_MAPPING.get(filterSelected);
  }

  public List<Integer> resolveFilterPackageIds() {
    return filterPackageIds == null ? null : filterPackageIds.stream()
      .map(Integer::parseInt)
      .toList();
  }

  public Boolean resolveFilterCustom() {
    return filterCustom == null ? null : Boolean.parseBoolean(filterCustom);
  }

  public Sort resolveSort() {
    return Sort.valueOf(sort.toUpperCase());
  }

  private boolean isCheckedFilter(List<String> checkedFilter, List<String> otherFilter) {
    return isNotEmpty(checkedFilter)
           && matchesAll(checkedFilter, StringUtils::isNotBlank)
           && CollectionUtils.isEmpty(otherFilter)
           && matchesAll(
      asList(query, filterIsxn, filterName, filterPublisher, filterSubject, filterCustom, filterSelected),
      StringUtils::isBlank);
  }
}
