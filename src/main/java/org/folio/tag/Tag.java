package org.folio.tag;

import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class Tag {

  private String id;
  private String recordId;
  private RecordType recordType;
  private String value;

}
