package org.folio.repository.accesstypes;

import java.util.UUID;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

import org.folio.repository.DbMetadata;

@Getter
@SuperBuilder(toBuilder = true)
public class DbAccessType extends DbMetadata {

  private final UUID id;
  private final UUID credentialsId;
  private final String name;
  private final String description;
  private final Integer usageNumber;
  private final String createdByLastName;
  private final String createdByFirstName;
  private final String createdByMiddleName;
  private final String updatedByLastName;
  private final String updatedByFirstName;
  private final String updatedByMiddleName;
}
