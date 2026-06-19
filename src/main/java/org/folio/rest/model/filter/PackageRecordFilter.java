package org.folio.rest.model.filter;

import static java.util.Arrays.asList;

import java.util.List;
import java.util.Optional;
import lombok.Builder;
import lombok.Value;
import org.folio.holdingsiq.model.PackageFilter;
import org.folio.holdingsiq.model.PackageFilterFreeAccess;
import org.folio.holdingsiq.model.PackageFilterSelected;
import org.folio.holdingsiq.model.PackageFilterType;
import org.folio.holdingsiq.model.PackageFilterVisibility;
import org.folio.holdingsiq.model.PackageSearchField;
import org.folio.holdingsiq.model.SearchType;
import org.folio.properties.common.SearchProperties;
import org.folio.repository.RecordType;
import org.folio.rest.util.IdParser;
import org.folio.rest.util.RestConstants;

@Value
@Builder
public class PackageRecordFilter implements Filter {

  private static final FilterValidator<PackageRecordFilter> VALIDATOR = new PackageFilterValidator();

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

  public static PackageRecordFilterBuilder builder() {
    return new PackageRecordFilterBuilder() {
      @Override
      public PackageRecordFilter build() {
        PackageRecordFilter filter = super.build();
        VALIDATOR.validate(filter);
        return filter;
      }
    };
  }

  public Optional<SearchType> getSearchType() {
    return Optional.ofNullable(queryType).map(SearchType::fromValue);
  }

  public Optional<PackageSearchField> getPackageSearchField() {
    return Optional.ofNullable(queryField).map(PackageSearchField::fromValue);
  }

  public Optional<PackageFilterSelected> getPackageFilterSelected() {
    return Optional.ofNullable(filterSelected).map(RestConstants.FILTER_SELECTED_MAPPING::get);
  }

  public Optional<PackageFilterType> getPackageFilterType() {
    return Optional.ofNullable(filterType).map(PackageFilterType::fromValue);
  }

  public Optional<PackageFilterFreeAccess> getPackageFilterFreeAccess() {
    return Optional.ofNullable(filterFreeAccess).map(RestConstants.FILTER_FREE_ACCESS_MAPPING::get);
  }

  public Optional<PackageFilterVisibility> getPackageFilterVisibility() {
    return Optional.ofNullable(filterVisibility).map(RestConstants.FILTER_VISIBILITY_MAPPING::get);
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

  public PackageFilter toClientFilter(SearchProperties searchProperties) {
    return PackageFilter.builder()
      .query(query)
      .searchType(getSearchType().orElse(SearchType.fromValue(searchProperties.packagesSearchType())))
      .searchField(getPackageSearchField().orElse(null))
      .highlightTag(highlight ? searchProperties.highlightTag() : null)
      .filterSelected(getPackageFilterSelected().orElse(null))
      .filterType(getPackageFilterType().orElse(null))
      .filterPackageFreeAccess(getPackageFilterFreeAccess().orElse(null))
      .filterVisibility(getPackageFilterVisibility().orElse(null))
      .build();
  }
}
