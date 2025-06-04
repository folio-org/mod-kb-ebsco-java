package org.folio.repository.kbcredentials;

import static org.folio.repository.DbMetadataUtil.CREATED_BY_USER_ID_COLUMN;
import static org.folio.repository.DbMetadataUtil.CREATED_BY_USER_NAME_COLUMN;
import static org.folio.repository.DbMetadataUtil.CREATED_DATE_COLUMN;
import static org.folio.repository.DbMetadataUtil.UPDATED_BY_USER_ID_COLUMN;
import static org.folio.repository.DbMetadataUtil.UPDATED_BY_USER_NAME_COLUMN;
import static org.folio.repository.DbMetadataUtil.UPDATED_DATE_COLUMN;
import static org.folio.repository.DbUtil.getAssignedUsersTableName;
import static org.folio.repository.DbUtil.getKbCredentialsTableName;
import static org.folio.repository.DbUtil.prepareQuery;
import static org.folio.repository.SqlQueryHelper.deleteQuery;
import static org.folio.repository.SqlQueryHelper.insertQuery;
import static org.folio.repository.SqlQueryHelper.limitQuery;
import static org.folio.repository.SqlQueryHelper.selectQuery;
import static org.folio.repository.SqlQueryHelper.updateOnConflictedIdQuery;
import static org.folio.repository.SqlQueryHelper.whereQuery;

import org.folio.repository.assigneduser.AssignedUsersConstants;

public final class KbCredentialsTableConstants {

  public static final String KB_CREDENTIALS_TABLE_NAME = "kb_credentials";
  public static final String ID_COLUMN = "id";
  public static final String URL_COLUMN = "url";
  public static final String NAME_COLUMN = "name";
  public static final String API_KEY_COLUMN = "api_key";
  public static final String CUSTOMER_ID_COLUMN = "customer_id";

  private static final String[] ALL_COLUMNS = new String[] {
    ID_COLUMN, URL_COLUMN, NAME_COLUMN, API_KEY_COLUMN, CUSTOMER_ID_COLUMN, CREATED_DATE_COLUMN,
    CREATED_BY_USER_ID_COLUMN, CREATED_BY_USER_NAME_COLUMN, UPDATED_DATE_COLUMN, UPDATED_BY_USER_ID_COLUMN,
    UPDATED_BY_USER_NAME_COLUMN};

  private KbCredentialsTableConstants() {
  }

  public static String selectCredentialsQuery() {
    return selectQuery() + ";";
  }

  public static String selectCredentialsQuery(String tenantId) {
    return prepareQuery(selectCredentialsQuery(), getKbCredentialsTableName(tenantId));
  }

  public static String selectCredentialsByIdQuery(String tenantId) {
    return prepareQuery(selectCredentialsByIdQueryPart(), getKbCredentialsTableName(tenantId));
  }

  public static String selectCredentialsByUserIdQuery(String tenantId) {
    return prepareQuery(selectCredentialsByUserIdQueryPart(), getKbCredentialsTableName(tenantId),
      getAssignedUsersTableName(tenantId));
  }

  public static String deleteCredentialsQuery(String tenantId) {
    return prepareQuery(deleteCredentialsQueryPart(), getKbCredentialsTableName(tenantId));
  }

  public static String upsertCredentialsQuery(String tenantId) {
    return prepareQuery(upsertCredentialsQuery(), getKbCredentialsTableName(tenantId));
  }

  public static String upsertCredentialsQuery() {
    return insertQuery(ALL_COLUMNS) + " " + updateOnConflictedIdQuery(ID_COLUMN, ALL_COLUMNS) + ";";
  }

  private static String selectCredentialsByIdQueryPart() {
    return selectQuery() + " " + whereQuery(ID_COLUMN) + " " + limitQuery(1) + ";";
  }

  private static String selectCredentialsByUserIdQueryPart() {
    return selectQuery() + " WHERE " + ID_COLUMN + " = ("
      + selectQuery(AssignedUsersConstants.CREDENTIALS_ID_COLUMN) + " "
      + whereQuery(AssignedUsersConstants.ID_COLUMN) + ");";
  }

  private static String deleteCredentialsQueryPart() {
    return deleteQuery() + " " + whereQuery(ID_COLUMN) + ";";
  }
}
