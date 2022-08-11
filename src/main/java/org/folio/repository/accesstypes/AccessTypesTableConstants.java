package org.folio.repository.accesstypes;

import static org.folio.repository.DbUtil.getAccessTypesMappingTableName;
import static org.folio.repository.DbUtil.getAccessTypesTableName;
import static org.folio.repository.DbUtil.getAccessTypesViewName;
import static org.folio.repository.DbUtil.prepareQuery;
import static org.folio.repository.SqlQueryHelper.count;
import static org.folio.repository.SqlQueryHelper.deleteQuery;
import static org.folio.repository.SqlQueryHelper.equalCondition;
import static org.folio.repository.SqlQueryHelper.inCondition;
import static org.folio.repository.SqlQueryHelper.insertQuery;
import static org.folio.repository.SqlQueryHelper.leftJoinQuery;
import static org.folio.repository.SqlQueryHelper.limitQuery;
import static org.folio.repository.SqlQueryHelper.selectQuery;
import static org.folio.repository.SqlQueryHelper.updateOnConflictedIdQuery;
import static org.folio.repository.SqlQueryHelper.whereConditionsQuery;
import static org.folio.repository.SqlQueryHelper.whereQuery;
import static org.folio.repository.accesstypes.AccessTypeMappingsTableConstants.ACCESS_TYPES_MAPPING_TABLE_NAME;
import static org.folio.repository.accesstypes.AccessTypeMappingsTableConstants.ACCESS_TYPE_ID_COLUMN;
import static org.folio.repository.accesstypes.AccessTypeMappingsTableConstants.RECORD_ID_COLUMN;
import static org.folio.repository.accesstypes.AccessTypeMappingsTableConstants.RECORD_TYPE_COLUMN;

import java.util.Collection;
import java.util.List;
import org.folio.common.ListUtils;

public final class AccessTypesTableConstants {

  public static final String ACCESS_TYPES_TABLE_NAME = "access_types";
  public static final String ACCESS_TYPES_VIEW_NAME = "access_types_view";

  public static final String ID_COLUMN = "id";
  public static final String CREDENTIALS_ID_COLUMN = "credentials_id";
  public static final String NAME_COLUMN = "name";
  public static final String DESCRIPTION_COLUMN = "description";
  public static final String CREATED_DATE_COLUMN = "created_date";
  public static final String CREATED_BY_USER_ID_COLUMN = "created_by_user_id";
  public static final String UPDATED_DATE_COLUMN = "updated_date";
  public static final String UPDATED_BY_USER_ID_COLUMN = "updated_by_user_id";
  public static final String USAGE_NUMBER_COLUMN = "usage_number";

  private static final String[] INSERT_COLUMNS =
    new String[] {ID_COLUMN, CREDENTIALS_ID_COLUMN, NAME_COLUMN, DESCRIPTION_COLUMN, CREATED_DATE_COLUMN,
                  CREATED_BY_USER_ID_COLUMN, UPDATED_DATE_COLUMN, UPDATED_BY_USER_ID_COLUMN};

  private AccessTypesTableConstants() {
  }

  public static String selectByCredentialsIdWithCountQuery(String tenantId) {
    return prepareQuery(selectByCredentialsIdWithCountQueryPart(), getAccessTypesViewName(tenantId));
  }

  public static String selectByCredentialsAndAccessTypeIdQuery(String tenantId) {
    return prepareQuery(selectByCredentialsAndAccessTypeIdQueryPart(), getAccessTypesViewName(tenantId));
  }

  public static String selectByCredentialsAndNamesQuery(Collection<String> accessTypeNames, String tenantId) {
    return prepareQuery(selectByCredentialsAndNamesQueryPart(), getAccessTypesTableName(tenantId),
      ListUtils.createPlaceholders(accessTypeNames.size()));
  }

  public static String selectCountByCredentialsIdQuery(String tenantId) {
    return prepareQuery(selectCountByCredentialsIdQueryPart(), getAccessTypesTableName(tenantId));
  }

  public static String selectByCredentialsAndRecordQuery(String tenantId) {
    return prepareQuery(selectByCredentialsAndRecordQueryPart(), getAccessTypesTableName(tenantId),
      getAccessTypesMappingTableName(tenantId));
  }

  public static String selectByCredentialsAndRecordIdsQuery(List<String> recordIds, String tenantId) {
    return prepareQuery(selectByCredentialsAndRecordIdsQueryPart(), getAccessTypesTableName(tenantId),
      ListUtils.createPlaceholders(recordIds.size()));
  }

  public static String deleteByCredentialsAndAccessTypeIdQuery(String tenantId) {
    return prepareQuery(deleteByCredentialsAndAccessTypeIdQueryPart(), getAccessTypesTableName(tenantId));
  }

  public static String upsertAccessTypeQuery(String tenantId) {
    return prepareQuery(upsertAccessTypeQuery(), getAccessTypesTableName(tenantId));
  }

  public static String upsertAccessTypeQuery() {
    return insertQuery(INSERT_COLUMNS) + " " + updateOnConflictedIdQuery(ID_COLUMN, INSERT_COLUMNS) + ";";
  }

  public static String selectIdsByCredentialsIdQuery() {
    return selectQuery(ID_COLUMN) + " " + whereQuery(CREDENTIALS_ID_COLUMN);
  }

  private static String selectByCredentialsIdWithCountQueryPart() {
    return selectQuery() + " " + whereQuery(CREDENTIALS_ID_COLUMN) + ";";
  }

  private static String selectByCredentialsAndAccessTypeIdQueryPart() {
    return selectQuery() + " " + whereQuery(ID_COLUMN, CREDENTIALS_ID_COLUMN) + " " + limitQuery(1) + ";";
  }

  private static String selectByCredentialsAndNamesQueryPart() {
    return selectQuery() + " " + whereConditionsQuery(equalCondition(CREDENTIALS_ID_COLUMN), inCondition(NAME_COLUMN))
      + ";";
  }

  private static String selectCountByCredentialsIdQueryPart() {
    return selectQuery(count()) + " " + whereQuery(CREDENTIALS_ID_COLUMN);
  }

  private static String selectByCredentialsAndRecordQueryPart() {
    return selectQuery() + " " + whereConditionsQuery(equalCondition(CREDENTIALS_ID_COLUMN),
      inCondition(ID_COLUMN, AccessTypeMappingsTableConstants.selectAccessTypeIdsByRecordQuery())) + " "
      + limitQuery(1) + ";";
  }

  private static String selectByCredentialsAndRecordIdsQueryPart() {
    String accessTypesColumns = "t1.*";
    String accessTypesMappingsColumns = "t2." + RECORD_ID_COLUMN;
    return selectQuery(accessTypesColumns, accessTypesMappingsColumns) + " "
      + leftJoinQuery(ACCESS_TYPES_MAPPING_TABLE_NAME, ID_COLUMN, ACCESS_TYPE_ID_COLUMN) + " "
      + whereConditionsQuery(equalCondition(CREDENTIALS_ID_COLUMN), equalCondition(RECORD_TYPE_COLUMN),
      inCondition(RECORD_ID_COLUMN));
  }

  private static String deleteByCredentialsAndAccessTypeIdQueryPart() {
    return deleteQuery() + " " + whereQuery(ID_COLUMN, CREDENTIALS_ID_COLUMN) + ";";
  }
}
