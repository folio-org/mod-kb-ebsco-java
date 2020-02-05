package org.folio.service.userlookup;

import lombok.Builder;
import lombok.Value;

/**
 * Retrieves user information from mod-users /users/{userId} endpoint.
 */
@Value
@Builder(toBuilder = true)
public class UserLookUp {

  private String userName;
  private String firstName;
  private String middleName;
  private String lastName;

}
