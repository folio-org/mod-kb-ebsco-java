package org.folio.repository.accesstypes;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class DbAccessType {

  private final UUID id;
  private final String credentialsId;
  private final String name;
  private final String description;
  private final Integer usageNumber;
  private final LocalDateTime createdDate;
  private final String createdByUserId;
  private final String createdByUsername;
  private final String createdByLastName;
  private final String createdByFirstName;
  private final String createdByMiddleName;
  private final LocalDateTime updatedDate;
  private final String updatedByUserId;
  private final String updatedByUsername;
  private final String updatedByLastName;
  private final String updatedByFirstName;
  private final String updatedByMiddleName;
}
