package org.folio.rest.model.filter;

import static java.util.Arrays.asList;

import java.util.List;
import lombok.Builder;
import lombok.Value;
import org.folio.holdingsiq.model.Pageable;
import org.folio.repository.RecordType;
import org.folio.rest.model.query.PackageSearchParams;
import org.folio.rest.util.IdParser;

@Value
@Builder
public class PackageRecordFilter implements Filter {

  String query;
  String providerId;
  String filterCustom;
  String filterSelected;
  String filterType;
  String sort;
  int page;
  int count;
  List<String> filterTags;
  List<String> filterAccessType;
  PackageSearchParams packageSearchParams;
  PackageFilterParams packageFilterParams;

  @Override
  public RecordType getRecordType() {
    return RecordType.PACKAGE;
  }

  @Override
  public List<String> searchCriteriaValues() {
    return asList(query, filterCustom, filterSelected);
  }

  public Integer parseProviderId() {
    return IdParser.parseProviderId(providerId);
  }

  public Boolean resolveFilterCustom() {
    return filterCustom == null ? null : Boolean.parseBoolean(filterCustom);
  }

  public String resolveFilterSelected() {
    return Filter.mapFilterSelected(filterSelected);
  }

  public static PackageRecordFilter create(PackageSearchParams searchParams,
                                           PackageFilterParams filterParams,
                                           Pageable pageable) {
    return PackageRecordFilter.builder()
        .query(searchParams.query())
        .filterTags(filterParams.filterTags())
        .filterCustom(filterParams.filterCustom())
        .filterAccessType(filterParams.filterAccessType())
        .filterSelected(filterParams.filterSelected())
        .filterType(filterParams.filterType())
        .packageFilterParams(filterParams)
        .packageSearchParams(searchParams)
        .sort(pageable.sort().getValue())
        .page(pageable.page())
        .count(pageable.count())
        .build();
  }

  public static PackageRecordFilterBuilder builder() {
    return new PackageRecordFilterBuilder() {
      @Override
      public PackageRecordFilter build() {
        PackageRecordFilter filter = super.build();
        FilterValidators.validatePackage(filter);
        return filter;
      }
    };
  }
}
