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

    RecordType recordType = filter.getRecordType();
    if (recordType == RecordType.PACKAGE) {
      accessTypeFilter.setRecordIdPrefix(filter.getProviderId());
      accessTypeFilter.setRecordType(RecordType.PACKAGE);
    } else if (recordType == RecordType.RESOURCE) {
      accessTypeFilter.setRecordIdPrefix(filter.getPackageId());
      accessTypeFilter.setRecordType(RecordType.RESOURCE);
    } else if (recordType == RecordType.TITLE) {
      accessTypeFilter.setRecordType(RecordType.RESOURCE);
    }
    return accessTypeFilter;
  }
}
