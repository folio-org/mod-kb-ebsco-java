package org.folio.repository.users;

import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class DbUser {

  private final UUID id;
  private final String username;
  private final String firstName;
  private final String middleName;
  private final String lastName;
  private final String patronGroup;
}
