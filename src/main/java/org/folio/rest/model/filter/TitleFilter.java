package org.folio.rest.model.filter;

import static java.util.Arrays.asList;

import java.util.List;
import java.util.Optional;
import lombok.Builder;
import lombok.Value;
import org.folio.holdingsiq.model.FilterSelected;
import org.folio.holdingsiq.model.SearchType;
import org.folio.holdingsiq.model.TitleFilterType;
import org.folio.properties.common.SearchProperties;
import org.folio.repository.RecordType;

@Value
@Builder
public class TitleFilter implements Filter {

  private static final FilterValidator<TitleFilter> VALIDATOR = new TitleFilterValidator();

  String query;
  String queryField;
  String queryType;

  String filterSelected;
  String filterType;
  String filterAccess;
  List<String> filterPackageIds;
  String sort;
  int page;
  int count;
  List<String> filterTags;
  List<String> filterAccessType;

  @Override
  public RecordType getRecordType() {
    return RecordType.TITLE;
  }

  @Override
  public List<String> searchCriteriaValues() {
    return asList(query, filterSelected);
  }

  public FilterSelected resolveFilterSelected() {
    return Filter.mapFilterSelected(filterSelected);
  }

  public List<Integer> resolveFilterPackageIds() {
    return filterPackageIds == null ? null : filterPackageIds.stream()
        .map(Integer::parseInt)
        .toList();
  }

  public Optional<SearchType> getSearchType() {
    return Optional.ofNullable(queryType).map(SearchType::fromValue);
  }

  public org.folio.holdingsiq.model.TitleFilter toClientFilter(SearchProperties searchProperties) {
    var search = TitleSearch.of(query, queryField);
    return org.folio.holdingsiq.model.TitleFilter.builder()
      .query(search.query())
      .searchField(search.searchField())
      .searchType(getSearchType().orElse(SearchType.fromValue(searchProperties.titlesSearchType())))
      .filterSelected(resolveFilterSelected())
      .filterType(TitleFilterType.fromValue(filterType))
      .filterPackageIds(resolveFilterPackageIds())
      .filterPackageFreeAccess(Filter.mapFilterAccess(filterAccess))
      .build();
  }

  public static TitleFilterBuilder builder() {
    return new TitleFilterBuilder() {
      @Override
      public TitleFilter build() {
        TitleFilter filter = super.build();
        VALIDATOR.validate(filter);
        return filter;
      }
    };
  }
}
