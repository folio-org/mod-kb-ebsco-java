package org.folio.rest.model.filter;

import static java.util.Arrays.asList;

import java.util.List;
import java.util.Optional;
import lombok.Builder;
import lombok.Value;
import org.folio.holdingsiq.model.SearchType;
import org.folio.repository.RecordType;
import org.folio.rest.util.IdParser;

@Value
@Builder
public class PackageRecordFilter implements Filter {

  String query;
  String queryField;
  String queryType;
  boolean highlight;

  String providerId;
  String filterCustom;
  String filterSelected;
  String filterType;
  String filterVisibility;
  String filterFreeAccess;
  String sort;
  int page;
  int count;
  List<String> filterTags;
  List<String> filterAccessType;

  public Optional<SearchType> getSearchType() {
    return Optional.ofNullable(queryType)
      .map(SearchType::fromValue);
  }

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
