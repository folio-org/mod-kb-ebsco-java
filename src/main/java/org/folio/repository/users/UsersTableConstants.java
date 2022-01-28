package org.folio.repository.users;

import static org.folio.repository.DbUtil.getUsersTableName;
import static org.folio.repository.DbUtil.prepareQuery;
import static org.folio.repository.SqlQueryHelper.insertQuery;
import static org.folio.repository.SqlQueryHelper.selectQuery;
import static org.folio.repository.SqlQueryHelper.updateOnConflictedIdQuery;
import static org.folio.repository.SqlQueryHelper.updateQuery;
import static org.folio.repository.SqlQueryHelper.whereQuery;

public final class UsersTableConstants {

  public static final String USERS_TABLE_NAME = "kb_users";
  public static final String ID_COLUMN = "id";
  public static final String USER_NAME_COLUMN = "user_name";
  public static final String FIRST_NAME_COLUMN = "first_name";
  public static final String MIDDLE_NAME_COLUMN = "middle_name";
  public static final String LAST_NAME_COLUMN = "last_name";
  public static final String PATRON_GROUP_COLUMN = "patron_group";

  private static final String[] ALL_COLUMNS = new String[]{
    ID_COLUMN, USER_NAME_COLUMN, FIRST_NAME_COLUMN, MIDDLE_NAME_COLUMN, LAST_NAME_COLUMN, PATRON_GROUP_COLUMN
  };
  private static final String[] UPDATE_COLUMNS = new String[]{
    USER_NAME_COLUMN, FIRST_NAME_COLUMN, MIDDLE_NAME_COLUMN, LAST_NAME_COLUMN, PATRON_GROUP_COLUMN
  };

  private UsersTableConstants() {
  }

  public static String saveUserQuery() {
    return insertQuery(ALL_COLUMNS) + " " + updateOnConflictedIdQuery(ID_COLUMN, UPDATE_COLUMNS) + ";";
  }

  public static String saveUserQuery(String tenantId) {
    return prepareQuery(saveUserQuery(), getUsersTableName(tenantId));
  }

  public static String selectByIdQuery(String tenantId) {
    return prepareQuery(selectByIdQuery(), getUsersTableName(tenantId));
  }

  public static String updateUserQuery(String tenantId) {
    return prepareQuery(updateUserQuery(), getUsersTableName(tenantId));
  }

  private static String selectByIdQuery() {
    return selectQuery() + " " + whereQuery(ID_COLUMN) + ";";
  }

  private static String updateUserQuery() {
    return updateQuery(UPDATE_COLUMNS) + " " + whereQuery(ID_COLUMN) + ";";
  }

}
