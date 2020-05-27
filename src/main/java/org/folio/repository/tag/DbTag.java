package org.folio.repository.tag;

import lombok.Builder;
import lombok.Value;

import org.folio.repository.RecordType;

@Value
@Builder(toBuilder = true)
public class DbTag {

  String id;
  String recordId;
  RecordType recordType;
  String value;

}
