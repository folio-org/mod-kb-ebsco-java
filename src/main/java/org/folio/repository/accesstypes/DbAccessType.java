package org.folio.repository.accesstypes;


import java.time.Instant;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class DbAccessType {

  private final String id;
  private final String credentialsId;
  private final String name;
  private final String description;
  private final Integer usageNumber;
  private final Instant createdDate;
  private final String createdByUserId;
  private final String createdByUsername;
  private final String createdByLastName;
  private final String createdByFirstName;
  private final String createdByMiddleName;
  private final Instant updatedDate;
  private final String updatedByUserId;
  private final String updatedByUsername;
  private final String updatedByLastName;
  private final String updatedByFirstName;
  private final String updatedByMiddleName;
}
