package org.folio.rest.model.filter;

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.collections4.IterableUtils.matchesAll;
import static org.folio.rest.util.RestConstants.FILTER_SELECTED_MAPPING;

import java.util.List;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.folio.holdingsiq.model.Pageable;
import org.folio.holdingsiq.model.Sort;
import org.folio.repository.RecordType;

public sealed interface Filter permits ProviderFilter, PackageRecordFilter, ResourceFilter, TitleFilter {

  String getSort();

  int getPage();

  int getCount();

  List<String> getFilterTags();

  List<String> getFilterAccessType();

  RecordType getRecordType();

  // Each impl returns its own search-criteria field values (used by isTagsFilter/isAccessTypeFilter)
  List<String> searchCriteriaValues();

  // Default methods
  default Sort resolveSort() {
    return Sort.valueOf(getSort().toUpperCase());
  }

  default boolean isTagsFilter() {
    return isFilteredBy(getFilterTags(), getFilterAccessType());
  }

  default boolean isAccessTypeFilter() {
    return isFilteredBy(getFilterAccessType(), getFilterTags());
  }

  default Pageable toPageable() {
    return new Pageable(getPage(), getCount(), resolveSort());
  }

  // Private helper
  private boolean isFilteredBy(List<String> primary, List<String> other) {
    return isNotEmpty(primary)
           && matchesAll(primary, StringUtils::isNotBlank)
           && CollectionUtils.isEmpty(other)
           && matchesAll(searchCriteriaValues(), StringUtils::isBlank);
  }

  // Shared utility for resolveFilterSelected (used by 3 implementations)
  static String mapFilterSelected(String filterSelected) {
    return filterSelected == null ? null : FILTER_SELECTED_MAPPING.get(filterSelected).getValue();
  }
}
