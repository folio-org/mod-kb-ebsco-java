package org.folio.repository.accesstypes;

import java.time.OffsetDateTime;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class DbAccessType {

  private final UUID id;
  private final UUID credentialsId;
  private final String name;
  private final String description;
  private final Integer usageNumber;
  private final OffsetDateTime createdDate;
  private final UUID createdByUserId;
  private final String createdByUsername;
  private final String createdByLastName;
  private final String createdByFirstName;
  private final String createdByMiddleName;
  private final OffsetDateTime updatedDate;
  private final UUID updatedByUserId;
  private final String updatedByUsername;
  private final String updatedByLastName;
  private final String updatedByFirstName;
  private final String updatedByMiddleName;
}
