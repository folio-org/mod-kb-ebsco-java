package org.folio.repository.accesstypes;

import static org.folio.repository.DbMetadataUtil.CREATED_BY_USER_ID_COLUMN;
import static org.folio.repository.DbMetadataUtil.CREATED_DATE_COLUMN;
import static org.folio.repository.DbMetadataUtil.UPDATED_BY_USER_ID_COLUMN;
import static org.folio.repository.DbMetadataUtil.UPDATED_DATE_COLUMN;
import static org.folio.repository.SqlQueryHelper.count;
import static org.folio.repository.SqlQueryHelper.deleteQuery;
import static org.folio.repository.SqlQueryHelper.equalCondition;
import static org.folio.repository.SqlQueryHelper.inCondition;
import static org.folio.repository.SqlQueryHelper.insertQuery;
import static org.folio.repository.SqlQueryHelper.limitQuery;
import static org.folio.repository.SqlQueryHelper.selectQuery;
import static org.folio.repository.SqlQueryHelper.updateOnConflictedIdQuery;
import static org.folio.repository.SqlQueryHelper.whereConditionsQuery;
import static org.folio.repository.SqlQueryHelper.whereQuery;

public class AccessTypesTableConstants {

  public static final String ACCESS_TYPES_TABLE_NAME = "access_types";
  public static final String ACCESS_TYPES_VIEW_NAME = "access_types_view";

  public static final String ID_COLUMN = "id";
  public static final String CREDENTIALS_ID_COLUMN = "credentials_id";
  public static final String NAME_COLUMN = "name";
  public static final String DESCRIPTION_COLUMN = "description";
  public static final String CREATED_BY_LAST_NAME_COLUMN = "created_by_last_name";
  public static final String CREATED_BY_FIRST_NAME_COLUMN = "created_by_first_name";
  public static final String CREATED_BY_MIDDLE_NAME_COLUMN = "created_by_middle_name";
  public static final String UPDATED_BY_LAST_NAME_COLUMN = "updated_by_last_name";
  public static final String UPDATED_BY_FIRST_NAME_COLUMN = "updated_by_first_name";
  public static final String UPDATED_BY_MIDDLE_NAME_COLUMN = "updated_by_middle_name";
  public static final String USAGE_NUMBER_COLUMN = "usage_number";

  public static final String UPSERT_ACCESS_TYPE_QUERY;
  public static final String SELECT_IDS_BY_CREDENTIALS_ID_QUERY;
  public static final String SELECT_BY_CREDENTIALS_ID_WITH_COUNT_QUERY;
  public static final String SELECT_BY_CREDENTIALS_AND_ACCESS_TYPE_ID_QUERY;
  public static final String SELECT_BY_CREDENTIALS_AND_RECORD_QUERY;
  public static final String SELECT_BY_CREDENTIALS_AND_NAMES_QUERY;
  public static final String SELECT_COUNT_BY_CREDENTIALS_ID_QUERY;
  public static final String DELETE_BY_CREDENTIALS_AND_ACCESS_TYPE_ID_QUERY;

  static {
    String[] insertColumns = new String[] {
      ID_COLUMN, CREDENTIALS_ID_COLUMN, NAME_COLUMN, DESCRIPTION_COLUMN, CREATED_DATE_COLUMN,
      CREATED_BY_USER_ID_COLUMN, UPDATED_DATE_COLUMN, UPDATED_BY_USER_ID_COLUMN
    };

    SELECT_IDS_BY_CREDENTIALS_ID_QUERY = selectQuery(ID_COLUMN) + " " + whereQuery(CREDENTIALS_ID_COLUMN);
    SELECT_BY_CREDENTIALS_ID_WITH_COUNT_QUERY = selectQuery() + " " + whereQuery(CREDENTIALS_ID_COLUMN) + ";";
    SELECT_BY_CREDENTIALS_AND_ACCESS_TYPE_ID_QUERY = selectQuery() + " " +
      whereQuery(ID_COLUMN, CREDENTIALS_ID_COLUMN) + " " + limitQuery(1) + ";";
    SELECT_BY_CREDENTIALS_AND_NAMES_QUERY = selectQuery() + " " + whereConditionsQuery(
      equalCondition(CREDENTIALS_ID_COLUMN), inCondition(NAME_COLUMN)) + ";";
    SELECT_COUNT_BY_CREDENTIALS_ID_QUERY = selectQuery(count()) + " " + whereQuery(CREDENTIALS_ID_COLUMN);
    SELECT_BY_CREDENTIALS_AND_RECORD_QUERY = selectQuery() + " " + whereConditionsQuery(
      equalCondition(CREDENTIALS_ID_COLUMN),
      inCondition(ID_COLUMN, AccessTypeMappingsTableConstants.SELECT_ACCESS_TYPE_IDS_BY_RECORD_QUERY)
    ) + " " + limitQuery(1) + ";";

    UPSERT_ACCESS_TYPE_QUERY = insertQuery(insertColumns) + " " + updateOnConflictedIdQuery(ID_COLUMN, insertColumns) + ";";
    DELETE_BY_CREDENTIALS_AND_ACCESS_TYPE_ID_QUERY =
      deleteQuery() + " " + whereQuery(ID_COLUMN, CREDENTIALS_ID_COLUMN) + ";";
  }

  private AccessTypesTableConstants() {
  }
}
