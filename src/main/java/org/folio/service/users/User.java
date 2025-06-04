package org.folio.service.users;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class User {

  private final String id;
  private final String userName;
  private final String firstName;
  private final String middleName;
  private final String lastName;
  private final String patronGroup;
}
