package org.folio.repository.assigneduser;

import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class DbAssignedUser {
  private final UUID id;
  private final UUID credentialsId;
}
