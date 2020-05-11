package org.folio.repository.accesstypes;

import lombok.Builder;
import lombok.Value;

import org.folio.repository.RecordType;

@Value
@Builder(toBuilder = true)
public class AccessTypeMapping {

  String id;
  String recordId;
  RecordType recordType;
  String accessTypeId;

}
