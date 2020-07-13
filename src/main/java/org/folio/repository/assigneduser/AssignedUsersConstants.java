package org.folio.repository.assigneduser;

import static org.folio.repository.SqlQueryHelper.count;
import static org.folio.repository.SqlQueryHelper.deleteQuery;
import static org.folio.repository.SqlQueryHelper.insertQuery;
import static org.folio.repository.SqlQueryHelper.leftJoinQuery;
import static org.folio.repository.SqlQueryHelper.selectQuery;
import static org.folio.repository.SqlQueryHelper.whereQuery;
import static org.folio.repository.users.UsersTableConstants.FIRST_NAME_COLUMN;
import static org.folio.repository.users.UsersTableConstants.LAST_NAME_COLUMN;
import static org.folio.repository.users.UsersTableConstants.MIDDLE_NAME_COLUMN;
import static org.folio.repository.users.UsersTableConstants.PATRON_GROUP_COLUMN;
import static org.folio.repository.users.UsersTableConstants.USER_NAME_COLUMN;

import org.folio.repository.users.UsersTableConstants;

public class AssignedUsersConstants {

  public static final String ASSIGNED_USERS_TABLE_NAME = "assigned_users";

  public static final String ID_COLUMN = "user_id";
  public static final String CREDENTIALS_ID_COLUMN = "credentials_id";

  public static final String SELECT_ASSIGNED_USERS_QUERY;
  public static final String SELECT_ASSIGNED_USERS_BY_CREDENTIALS_ID_QUERY;
  public static final String SELECT_COUNT_BY_CREDENTIALS_ID_QUERY;
  public static final String INSERT_ASSIGNED_USER_QUERY;
  public static final String DELETE_ASSIGNED_USER_QUERY;

  static {
    String[] allColumns = new String[] {
      ID_COLUMN,
      CREDENTIALS_ID_COLUMN,
      USER_NAME_COLUMN,
      FIRST_NAME_COLUMN,
      MIDDLE_NAME_COLUMN,
      LAST_NAME_COLUMN,
      PATRON_GROUP_COLUMN
    };

    SELECT_ASSIGNED_USERS_QUERY = selectQuery(allColumns) + " "
      + leftJoinQuery("%s", ID_COLUMN, UsersTableConstants.ID_COLUMN);
    SELECT_ASSIGNED_USERS_BY_CREDENTIALS_ID_QUERY =
      SELECT_ASSIGNED_USERS_QUERY + " " + whereQuery(CREDENTIALS_ID_COLUMN) + ";";
    SELECT_COUNT_BY_CREDENTIALS_ID_QUERY = selectQuery(count()) + " " + whereQuery(CREDENTIALS_ID_COLUMN);

    INSERT_ASSIGNED_USER_QUERY = insertQuery(ID_COLUMN, CREDENTIALS_ID_COLUMN) + ";";
    DELETE_ASSIGNED_USER_QUERY = deleteQuery() + " " + whereQuery(CREDENTIALS_ID_COLUMN, ID_COLUMN) + ";";
  }

  private AssignedUsersConstants() {
  }
}
