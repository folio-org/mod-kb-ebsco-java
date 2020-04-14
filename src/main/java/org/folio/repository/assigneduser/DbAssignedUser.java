package org.folio.repository.assigneduser;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class DbAssignedUser {
  private final String id;
  private final String credentialsId;
  private final String username;
  private final String firstName;
  private final String middleName;
  private final String lastName;
  private final String patronGroup;
}
