package org.folio.rest.model.filter;

import java.util.List;
import java.util.UUID;
import lombok.Data;
import org.folio.repository.RecordType;

@Data
public class AccessTypeFilter {

  private List<String> accessTypeNames;
  private List<UUID> accessTypeIds;
  private String recordIdPrefix;
  private RecordType recordType;
  private int page;
  private int count;

  public static AccessTypeFilter from(Filter filter) {
    AccessTypeFilter accessTypeFilter = new AccessTypeFilter();
    accessTypeFilter.setAccessTypeNames(filter.getFilterAccessType());
    accessTypeFilter.setCount(filter.getCount());
    accessTypeFilter.setPage(filter.getPage());

    switch (filter) {
      case PackageRecordFilter pf -> {
        accessTypeFilter.setRecordIdPrefix(pf.getProviderId());
        accessTypeFilter.setRecordType(RecordType.PACKAGE);
      }
      case ResourceFilter rf -> {
        accessTypeFilter.setRecordIdPrefix(rf.getPackageId());
        accessTypeFilter.setRecordType(RecordType.RESOURCE);
      }
      case TitleFilter tf -> accessTypeFilter.setRecordType(RecordType.RESOURCE);
      case ProviderFilter prov -> { }
    }
    return accessTypeFilter;
  }
}
