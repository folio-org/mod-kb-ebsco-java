package org.folio.repository;

import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class RecordKey {

  private String recordId;
  private RecordType recordType;
}
