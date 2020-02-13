package org.folio.repository.accesstypes;

import lombok.Builder;
import lombok.Value;

import org.folio.repository.RecordType;

@Value
@Builder(toBuilder = true)
public class AccessTypeMapping {

  private String id;
  private String recordId;
  private RecordType recordType;
  private String accessTypeId;

}
