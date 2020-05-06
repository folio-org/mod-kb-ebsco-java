package org.folio.service.userlookup;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class UserLookUp {

  private final String userId;
  private final String username;
  private final String firstName;
  private final String middleName;
  private final String lastName;

}
