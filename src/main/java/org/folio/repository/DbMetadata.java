package org.folio.repository;

import java.time.OffsetDateTime;
import java.util.UUID;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder(toBuilder = true)
public class DbMetadata {

  OffsetDateTime createdDate;
  OffsetDateTime updatedDate;
  UUID createdByUserId;
  UUID updatedByUserId;
  String createdByUserName;
  String updatedByUserName;
}
