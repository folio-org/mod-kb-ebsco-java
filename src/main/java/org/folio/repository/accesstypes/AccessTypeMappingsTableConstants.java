package org.folio.repository.accesstypes;

import static org.folio.repository.SqlQueryHelper.count;
import static org.folio.repository.SqlQueryHelper.deleteQuery;
import static org.folio.repository.SqlQueryHelper.equalCondition;
import static org.folio.repository.SqlQueryHelper.groupByQuery;
import static org.folio.repository.SqlQueryHelper.inCondition;
import static org.folio.repository.SqlQueryHelper.insertQuery;
import static org.folio.repository.SqlQueryHelper.likeCondition;
import static org.folio.repository.SqlQueryHelper.limitQuery;
import static org.folio.repository.SqlQueryHelper.offsetQuery;
import static org.folio.repository.SqlQueryHelper.orderByQuery;
import static org.folio.repository.SqlQueryHelper.selectQuery;
import static org.folio.repository.SqlQueryHelper.updateOnConflictedIdQuery;
import static org.folio.repository.SqlQueryHelper.whereConditionsQuery;
import static org.folio.repository.SqlQueryHelper.whereQuery;

public class AccessTypeMappingsTableConstants {

  public static final String ACCESS_TYPES_MAPPING_TABLE_NAME = "access_types_mappings";

  public static final String ID_COLUMN = "id";
  public static final String COUNT_COLUMN = "count";
  public static final String RECORD_ID_COLUMN = "record_id";
  public static final String RECORD_TYPE_COLUMN = "record_type";
  public static final String ACCESS_TYPE_ID_COLUMN = "access_type_id";

  public static final String ACCESS_TYPES_MAPPING_FIELD_LIST = String.format("%s, %s, %s, %s",
    ID_COLUMN, RECORD_ID_COLUMN, RECORD_TYPE_COLUMN, ACCESS_TYPE_ID_COLUMN);

  public static final String UPSERT_QUERY;
  public static final String DELETE_BY_RECORD_ID_AND_RECORD_TYPE_QUERY;

  public static final String SELECT_BY_ACCESS_TYPE_ID_QUERY;
  public static final String SELECT_BY_RECORD_ID_AND_RECORD_TYPE_QUERY;
  public static final String SELECT_BY_ACCESS_TYPE_IDS_AND_RECORD_TYPE_QUERY;
  public static final String SELECT_ACCESS_TYPE_IDS_BY_RECORD_QUERY;

  public static final String COUNT_BY_ACCESS_TYPE_ID_QUERY;
  public static final String COUNT_BY_RECORD_ID_PREFIX_QUERY;


  static {
    String[] allColumns = new String[] {ID_COLUMN, RECORD_ID_COLUMN, RECORD_TYPE_COLUMN, ACCESS_TYPE_ID_COLUMN};
    String whereRecordAndAccessIds = whereConditionsQuery(
      equalCondition(RECORD_ID_COLUMN),
      equalCondition(RECORD_TYPE_COLUMN),
      inCondition(ACCESS_TYPE_ID_COLUMN, AccessTypesTableConstants.SELECT_IDS_BY_CREDENTIALS_ID_QUERY)
    );

    UPSERT_QUERY = insertQuery(allColumns) + " " + updateOnConflictedIdQuery(ID_COLUMN, ACCESS_TYPE_ID_COLUMN) + ";";

    SELECT_ACCESS_TYPE_IDS_BY_RECORD_QUERY = selectQuery(ACCESS_TYPE_ID_COLUMN) + " " +
      whereQuery(RECORD_ID_COLUMN, RECORD_TYPE_COLUMN);
    SELECT_BY_ACCESS_TYPE_ID_QUERY = selectQuery() + " " + whereQuery(ACCESS_TYPE_ID_COLUMN) + ";";
    SELECT_BY_RECORD_ID_AND_RECORD_TYPE_QUERY = selectQuery() + " " + whereRecordAndAccessIds + " " + limitQuery(1) + ";";
    SELECT_BY_ACCESS_TYPE_IDS_AND_RECORD_TYPE_QUERY = selectQuery() + " " + whereConditionsQuery(
      inCondition(ACCESS_TYPE_ID_COLUMN), equalCondition(RECORD_TYPE_COLUMN), likeCondition(RECORD_ID_COLUMN)
    ) + " " + orderByQuery(RECORD_ID_COLUMN) + " " + offsetQuery() + " " + limitQuery() + ";";

    COUNT_BY_ACCESS_TYPE_ID_QUERY = selectQuery(ACCESS_TYPE_ID_COLUMN, count()) + groupByQuery(ACCESS_TYPE_ID_COLUMN) + ";";
    COUNT_BY_RECORD_ID_PREFIX_QUERY = selectQuery(ACCESS_TYPE_ID_COLUMN, count()) + " " + whereConditionsQuery(
      likeCondition(RECORD_ID_COLUMN),
      equalCondition(RECORD_TYPE_COLUMN),
      inCondition(ACCESS_TYPE_ID_COLUMN, AccessTypesTableConstants.SELECT_IDS_BY_CREDENTIALS_ID_QUERY)
    ) + " " + groupByQuery(ACCESS_TYPE_ID_COLUMN) + ";";

    DELETE_BY_RECORD_ID_AND_RECORD_TYPE_QUERY = deleteQuery() + " " + whereRecordAndAccessIds + ";";
  }


  private AccessTypeMappingsTableConstants() {
  }

}
