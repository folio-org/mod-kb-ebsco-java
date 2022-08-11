package org.folio.repository.uc;

import java.util.UUID;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import org.folio.repository.DbMetadata;

@Getter
@SuperBuilder(toBuilder = true)
public class DbUcSettings extends DbMetadata {

  UUID id;
  UUID kbCredentialsId;
  String customerKey;
  String startMonth;
  String currency;
  String platformType;
}
