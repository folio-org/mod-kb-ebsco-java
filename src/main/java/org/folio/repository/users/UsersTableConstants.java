package org.folio.repository.users;

import static org.folio.repository.SqlQueryHelper.insertQuery;
import static org.folio.repository.SqlQueryHelper.selectQuery;
import static org.folio.repository.SqlQueryHelper.updateOnConflictedIdQuery;
import static org.folio.repository.SqlQueryHelper.updateQuery;
import static org.folio.repository.SqlQueryHelper.whereQuery;

public class UsersTableConstants {

  public static final String USERS_TABLE_NAME = "kb_users";

  public static final String ID_COLUMN = "id";
  public static final String USER_NAME_COLUMN = "user_name";
  public static final String FIRST_NAME_COLUMN = "first_name";
  public static final String MIDDLE_NAME_COLUMN = "middle_name";
  public static final String LAST_NAME_COLUMN = "last_name";
  public static final String PATRON_GROUP_COLUMN = "patron_group";

  public static final String SELECT_BY_ID_QUERY;
  public static final String SAVE_USER_QUERY;
  public static final String UPDATE_USER_QUERY;

  static {
    String[] allColumns = new String[] {
      ID_COLUMN, USER_NAME_COLUMN, FIRST_NAME_COLUMN, MIDDLE_NAME_COLUMN, LAST_NAME_COLUMN, PATRON_GROUP_COLUMN
    };
    String[] updateColumns = new String[] {
      USER_NAME_COLUMN, FIRST_NAME_COLUMN, MIDDLE_NAME_COLUMN, LAST_NAME_COLUMN, PATRON_GROUP_COLUMN
    };

    SELECT_BY_ID_QUERY = selectQuery() + " " + whereQuery(ID_COLUMN) + ";";
    SAVE_USER_QUERY = insertQuery(allColumns) + " " + updateOnConflictedIdQuery(ID_COLUMN, updateColumns) + ";";
    UPDATE_USER_QUERY = updateQuery(updateColumns) + " " + whereQuery(ID_COLUMN) + ";";
  }

  private UsersTableConstants() {
  }
}
