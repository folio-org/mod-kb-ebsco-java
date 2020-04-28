package org.folio.repository.accesstypes;

import static org.folio.repository.SqlQueryHelper.count;
import static org.folio.repository.SqlQueryHelper.groupByQuery;
import static org.folio.repository.SqlQueryHelper.insertQuery;
import static org.folio.repository.SqlQueryHelper.leftJoinQuery;
import static org.folio.repository.SqlQueryHelper.limitQuery;
import static org.folio.repository.SqlQueryHelper.selectQuery;
import static org.folio.repository.SqlQueryHelper.updateOnConflictedIdQuery;
import static org.folio.repository.SqlQueryHelper.whereQuery;
import static org.folio.repository.accesstypes.AccessTypeMappingsTableConstants.ACCESS_TYPE_ID_COLUMN;

public class AccessTypesTableConstants {

  public static final String ACCESS_TYPES_TABLE_NAME_OLD = "access_types_old";
  public static final String ACCESS_TYPES_TABLE_NAME = "access_types";

  public static final String ID_COLUMN = "id";
  public static final String CREDENTIALS_ID_COLUMN = "credentials_id";
  public static final String NAME_COLUMN = "name";
  public static final String DESCRIPTION_COLUMN = "description";
  public static final String CREATED_DATE_COLUMN = "created_date";
  public static final String CREATED_BY_USER_ID_COLUMN = "created_by_user_id";
  public static final String CREATED_BY_USERNAME_COLUMN = "created_by_username";
  public static final String CREATED_BY_LAST_NAME_COLUMN = "created_by_last_name";
  public static final String CREATED_BY_FIRST_NAME_COLUMN = "created_by_first_name";
  public static final String CREATED_BY_MIDDLE_NAME_COLUMN = "created_by_middle_name";
  public static final String UPDATED_DATE_COLUMN = "updated_date";
  public static final String UPDATED_BY_USER_ID_COLUMN = "updated_by_user_id";
  public static final String UPDATED_BY_USERNAME_COLUMN = "updated_by_username";
  public static final String UPDATED_BY_LAST_NAME_COLUMN = "updated_by_last_name";
  public static final String UPDATED_BY_FIRST_NAME_COLUMN = "updated_by_first_name";
  public static final String UPDATED_BY_MIDDLE_NAME_COLUMN = "updated_by_middle_name";

  public static final String USAGE_NUMBER_COLUMN = "usage_number";

  public static final String UPSERT_ACCESS_TYPE_QUERY;
  public static final String SELECT_BY_CREDENTIALS_ID_QUERY;
  public static final String SELECT_BY_CREDENTIALS_AND_ACCESS_TYPE_ID_QUERY;
  public static final String SELECT_COUNT_BY_CREDENTIALS_ID_QUERY;

  static {
    String[] allColumns = new String[] {
      ID_COLUMN, CREDENTIALS_ID_COLUMN, NAME_COLUMN, DESCRIPTION_COLUMN, CREATED_DATE_COLUMN, CREATED_BY_USER_ID_COLUMN,
      CREATED_BY_USERNAME_COLUMN, CREATED_BY_LAST_NAME_COLUMN, CREATED_BY_FIRST_NAME_COLUMN, CREATED_BY_MIDDLE_NAME_COLUMN,
      UPDATED_DATE_COLUMN, UPDATED_BY_USER_ID_COLUMN, UPDATED_BY_USERNAME_COLUMN, UPDATED_BY_LAST_NAME_COLUMN,
      UPDATED_BY_FIRST_NAME_COLUMN, UPDATED_BY_MIDDLE_NAME_COLUMN
    };

    String selectWithCountUsage = selectQuery() + " " +
      leftJoinQuery(
        selectQuery(ACCESS_TYPE_ID_COLUMN, count(ACCESS_TYPE_ID_COLUMN, USAGE_NUMBER_COLUMN)) + " " +
          groupByQuery(ACCESS_TYPE_ID_COLUMN), ID_COLUMN, ACCESS_TYPE_ID_COLUMN
      );
    SELECT_BY_CREDENTIALS_ID_QUERY = selectWithCountUsage + " " + whereQuery(CREDENTIALS_ID_COLUMN) + ";";
    SELECT_BY_CREDENTIALS_AND_ACCESS_TYPE_ID_QUERY = selectWithCountUsage + " " +
      whereQuery(ID_COLUMN, CREDENTIALS_ID_COLUMN) + " " + limitQuery(1) + ";";
    SELECT_COUNT_BY_CREDENTIALS_ID_QUERY = selectQuery(count()) + " " + whereQuery(CREDENTIALS_ID_COLUMN);
    UPSERT_ACCESS_TYPE_QUERY = insertQuery(allColumns) + " " + updateOnConflictedIdQuery(ID_COLUMN, allColumns) + ";";
  }

  private AccessTypesTableConstants() {
  }
}
