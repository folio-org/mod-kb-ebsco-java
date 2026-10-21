package org.folio.rest.model.filter;

import static java.util.Arrays.asList;

import java.util.List;
import lombok.Builder;
import lombok.Value;
import org.folio.holdingsiq.model.FilterSelected;
import org.folio.holdingsiq.model.PackageId;
import org.folio.holdingsiq.model.TitleFilterType;
import org.folio.repository.RecordType;
import org.folio.rest.util.IdParser;

@Value
@Builder
public class ResourceFilter implements Filter {

  private static final FilterValidator<ResourceFilter> VALIDATOR = new ResourceFilterValidator();

  String packageId;
  String query;
  String queryField;

  String filterSelected;
  String filterType;
  String filterAccess;
  String sort;
  int page;
  int count;
  List<String> filterTags;
  List<String> filterAccessType;

  @Override
  public RecordType getRecordType() {
    return RecordType.RESOURCE;
  }

  @Override
  public List<String> searchCriteriaValues() {
    return asList(query, filterSelected);
  }

  public PackageId parsePackageId() {
    return IdParser.parsePackageId(packageId);
  }

  public FilterSelected resolveFilterSelected() {
    return Filter.mapFilterSelected(filterSelected);
  }

  public org.folio.holdingsiq.model.ResourceFilter toClientFilter() {
    var search = TitleSearch.of(query, queryField);
    return org.folio.holdingsiq.model.ResourceFilter.builder()
      .query(search.query())
      .searchField(search.searchField())
      .filterSelected(resolveFilterSelected())
      .filterType(TitleFilterType.fromValue(filterType))
      .filterPackageFreeAccess(Filter.mapFilterAccess(filterAccess))
      .build();
  }

  public static ResourceFilterBuilder builder() {
    return new ResourceFilterBuilder() {
      @Override
      public ResourceFilter build() {
        ResourceFilter filter = super.build();
        VALIDATOR.validate(filter);
        return filter;
      }
    };
  }
}
