package org.folio.repository.tag;

import lombok.Builder;
import lombok.Value;

import org.folio.repository.RecordType;

@Value
@Builder(toBuilder = true)
public class Tag {

  private String id;
  private String recordId;
  private RecordType recordType;
  private String value;

}
