package org.folio.repository.assigneduser;

import static org.folio.repository.DbUtil.getAssignedUsersTableName;
import static org.folio.repository.DbUtil.prepareQuery;
import static org.folio.repository.SqlQueryHelper.count;
import static org.folio.repository.SqlQueryHelper.deleteQuery;
import static org.folio.repository.SqlQueryHelper.insertQuery;
import static org.folio.repository.SqlQueryHelper.selectQuery;
import static org.folio.repository.SqlQueryHelper.whereQuery;

public final class AssignedUsersConstants {

  public static final String ASSIGNED_USERS_TABLE_NAME = "assigned_users";
  public static final String ID_COLUMN = "user_id";
  public static final String CREDENTIALS_ID_COLUMN = "credentials_id";

  private AssignedUsersConstants() {
  }

  public static String insertAssignedUserQuery() {
    return insertQuery(ID_COLUMN, CREDENTIALS_ID_COLUMN) + ";";
  }

  public static String insertAssignedUserQuery(String tenantId) {
    return prepareQuery(insertAssignedUserQuery(), getAssignedUsersTableName(tenantId));
  }

  public static String selectAssignedUsersByCredentialsIdQuery(String tenantId) {
    return prepareQuery(selectAssignedUsersByCredentialsIdQuery(), getAssignedUsersTableName(tenantId));
  }

  public static String selectCountByCredentialsIdQuery(String tenantId) {
    return prepareQuery(selectCountByCredentialsIdQuery(), getAssignedUsersTableName(tenantId));
  }

  public static String deleteAssignedUserQuery(String tenantId) {
    return prepareQuery(deleteAssignedUserQuery(), getAssignedUsersTableName(tenantId));
  }

  private static String selectAssignedUsersByCredentialsIdQuery() {
    return selectQuery() + " " + whereQuery(CREDENTIALS_ID_COLUMN) + ";";
  }

  private static String selectCountByCredentialsIdQuery() {
    return selectQuery(count()) + " " + whereQuery(CREDENTIALS_ID_COLUMN) + ";";
  }

  private static String deleteAssignedUserQuery() {
    return deleteQuery() + " " + whereQuery(CREDENTIALS_ID_COLUMN, ID_COLUMN) + ";";
  }
}
