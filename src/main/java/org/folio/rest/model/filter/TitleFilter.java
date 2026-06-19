package org.folio.rest.model.filter;

import static java.util.Arrays.asList;

import java.util.List;
import lombok.Builder;
import lombok.Value;
import org.folio.holdingsiq.model.FilterQuery;
import org.folio.repository.RecordType;

@Value
@Builder
public class TitleFilter implements Filter {

  String filterSelected;
  String filterType;
  String filterName;
  String filterIsxn;
  String filterSubject;
  String filterPublisher;
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
    return asList(filterIsxn, filterName, filterPublisher, filterSubject, filterSelected);
  }

  public String resolveFilterSelected() {
    return Filter.mapFilterSelected(filterSelected);
  }

  public List<Integer> resolveFilterPackageIds() {
    return filterPackageIds == null ? null : filterPackageIds.stream()
        .map(Integer::parseInt)
        .toList();
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

  public static TitleFilterBuilder builder() {
    return new TitleFilterBuilder() {
      @Override
      public TitleFilter build() {
        TitleFilter filter = super.build();
        FilterValidators.validateTitle(filter);
        return filter;
      }
    };
  }
}
