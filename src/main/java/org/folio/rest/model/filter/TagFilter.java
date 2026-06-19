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
    RecordType recordType = filter.getRecordType();
    TagFilterBuilder builder = TagFilter.builder()
      .tags(filter.getFilterTags())
      .recordType(recordType)
      .offset((filter.getPage() - 1) * filter.getCount())
      .count(filter.getCount());

    switch (recordType) {
      case PACKAGE -> builder.recordIdPrefix(ensureTrailingDash(filter.getProviderId()));
      case RESOURCE -> builder.recordIdPrefix(ensureTrailingDash(filter.getPackageId()));
      case TITLE -> {
        builder.recordType(RecordType.RESOURCE);
        builder.recordIdPrefix("");
      }
      case null, default -> builder.recordIdPrefix("");
    }

    return builder.build();
  }

  private static String ensureTrailingDash(String id) {
    return isBlank(id) ? "" : Strings.CS.appendIfMissing(id, "-");
  }
}
