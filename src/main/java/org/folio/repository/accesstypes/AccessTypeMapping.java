package org.folio.repository.accesstypes;

import java.util.UUID;

import lombok.Builder;
import lombok.Value;

import org.folio.repository.RecordType;

@Value
@Builder(toBuilder = true)
public class AccessTypeMapping {

  UUID id;
  String recordId;
  RecordType recordType;
  UUID accessTypeId;

}
