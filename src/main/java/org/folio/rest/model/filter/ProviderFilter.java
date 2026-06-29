package org.folio.rest.model.filter;

import static java.util.Arrays.asList;

import java.util.List;
import lombok.Builder;
import lombok.Value;
import org.folio.repository.RecordType;

@Value
@Builder
public class ProviderFilter implements Filter {

  String query;
  String sort;
  int page;
  int count;
  List<String> filterTags;
  List<String> filterAccessType;

  @Override
  public RecordType getRecordType() {
    return RecordType.PROVIDER;
  }

  @Override
  public List<String> searchCriteriaValues() {
    return asList(query);
  }

  public static ProviderFilterBuilder builder() {
    return new ProviderFilterBuilder() {
      @Override
      public ProviderFilter build() {
        ProviderFilter filter = super.build();
        FilterValidators.validateProvider(filter);
        return filter;
      }
    };
  }
}
