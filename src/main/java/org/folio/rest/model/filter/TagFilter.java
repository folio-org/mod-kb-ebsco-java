package org.folio.rest.model.filter;

import static org.apache.commons.lang3.StringUtils.isBlank;

import java.util.List;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.lang3.Strings;
import org.folio.repository.RecordType;

@Value
@Builder
public class TagFilter {

  List<String> tags;
  String recordIdPrefix;
  RecordType recordType;
  int offset;
  int count;

  public static TagFilter from(Filter filter) {
    TagFilterBuilder builder = TagFilter.builder()
      .tags(filter.getFilterTags())
      .recordType(filter.getRecordType())
      .offset((filter.getPage() - 1) * filter.getCount())
      .count(filter.getCount());

    switch (filter) {
      case PackageRecordFilter pf -> builder.recordIdPrefix(ensureTrailingDash(pf.getProviderId()));
      case ResourceFilter rf -> builder.recordIdPrefix(ensureTrailingDash(rf.getPackageId()));
      case TitleFilter tf -> {
        builder.recordType(RecordType.RESOURCE);
        builder.recordIdPrefix("");
      }
      case ProviderFilter prov -> builder.recordIdPrefix("");
    }

    return builder.build();
  }

  private static String ensureTrailingDash(String id) {
    return isBlank(id) ? "" : Strings.CS.appendIfMissing(id, "-");
  }
}
