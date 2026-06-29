package org.folio.rest.model.filter;

import static java.util.Arrays.asList;

import java.util.List;
import lombok.Builder;
import lombok.Value;
import org.folio.holdingsiq.model.FilterQuery;
import org.folio.holdingsiq.model.PackageId;
import org.folio.repository.RecordType;
import org.folio.rest.util.IdParser;

@Value
@Builder
public class ResourceFilter implements Filter {

  String packageId;
  String filterSelected;
  String filterType;
  String filterName;
  String filterIsxn;
  String filterSubject;
  String filterPublisher;
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
    return asList(filterIsxn, filterName, filterPublisher, filterSubject, filterSelected);
  }

  public PackageId parsePackageId() {
    return IdParser.parsePackageId(packageId);
  }

  public String resolveFilterSelected() {
    return Filter.mapFilterSelected(filterSelected);
  }

  public FilterQuery createFilterQuery() {
    return FilterQuery.builder()
        .type(filterType)
        .name(filterName)
        .isxn(filterIsxn)
        .subject(filterSubject)
        .publisher(filterPublisher)
        .selected(resolveFilterSelected())
        .build();
  }

  public static ResourceFilterBuilder builder() {
    return new ResourceFilterBuilder() {
      @Override
      public ResourceFilter build() {
        ResourceFilter filter = super.build();
        FilterValidators.validateResource(filter);
        return filter;
      }
    };
  }
}
