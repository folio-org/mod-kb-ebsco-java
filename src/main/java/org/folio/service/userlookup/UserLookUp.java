package org.folio.service.userlookup;

import lombok.Builder;
import lombok.Getter;

/**
 * Retrieves user information from mod-users /users/{userId} endpoint.
 */
@Builder
@Getter
public class UserLookUp {
  private String userName;
  private String firstName;
  private String middleName;
  private String lastName;

  @Override
  public String toString() {
    return "UserInfo [userName=" + userName
      + ", firstName=" + firstName + ", middleName=" + middleName + ", lastName=" + lastName + ']';
  }
}
