package org.folio.repository.tag;

import java.util.UUID;
import lombok.Builder;
import lombok.Value;
import org.folio.repository.RecordType;

@Value
@Builder(toBuilder = true)
public class DbTag {

  UUID id;
  String recordId;
  RecordType recordType;
  String value;

}
