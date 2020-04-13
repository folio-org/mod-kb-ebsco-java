package org.folio.repository.assigneduser;

import static org.folio.repository.SqlQueryHelper.selectQuery;
import static org.folio.repository.SqlQueryHelper.whereQuery;

public class AssignedUsersConstants {
  private AssignedUsersConstants() {
  }
  public static final String ASSIGNED_USERS_TABLE_NAME = "assigned_users";

  public static final String ID_COLUMN = "id";
  public static final String CREDENTIALS_ID = "credentialsId";
  public static final String USER_NAME = "username";
  public static final String FIRST_NAME = "firstName";
  public static final String MIDDLE_NAME = "middleName";
  public static final String LAST_NAME = "lastName";
  public static final String PATRON_GROUP = "patronGroup";

  public static final String SELECT_ASSIGNED_USERS_BY_CREDENTIALS_ID_QUERY;

  static {
    String[] allColumns = new String[] {
      ID_COLUMN, CREDENTIALS_ID, USER_NAME, FIRST_NAME, MIDDLE_NAME, LAST_NAME, PATRON_GROUP
    };

    SELECT_ASSIGNED_USERS_BY_CREDENTIALS_ID_QUERY = selectQuery() + " " + whereQuery(CREDENTIALS_ID) + ";";
  }
}
