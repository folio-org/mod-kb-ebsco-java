package org.folio.repository.accesstypes;

import java.util.List;
import java.util.UUID;

import static org.folio.common.ListUtils.createPlaceholders;
import static org.folio.repository.DbUtil.getAccessTypesMappingTableName;
import static org.folio.repository.DbUtil.getAccessTypesTableName;
import static org.folio.repository.DbUtil.prepareQuery;
import static org.folio.repository.SqlQueryHelper.count;
import static org.folio.repository.SqlQueryHelper.deleteQuery;
import static org.folio.repository.SqlQueryHelper.equalCondition;
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
import static org.folio.repository.SqlQueryHelper.groupByQuery;

public final class AccessTypeMappingsTableConstants {

  public static final String ACCESS_TYPES_MAPPING_TABLE_NAME = "access_types_mappings";
  public static final String ID_COLUMN = "id";
  public static final String COUNT_COLUMN = "count";
  public static final String RECORD_ID_COLUMN = "record_id";
  public static final String RECORD_TYPE_COLUMN = "record_type";
  public static final String ACCESS_TYPE_ID_COLUMN = "access_type_id";

  private static final String[] ALL_COLUMNS =
    new String[]{ID_COLUMN, RECORD_ID_COLUMN, RECORD_TYPE_COLUMN, ACCESS_TYPE_ID_COLUMN};

  private AccessTypeMappingsTableConstants() {
  }

  public static String upsertQuery() {
    return insertQuery(ALL_COLUMNS) + " " + updateOnConflictedIdQuery(ID_COLUMN, ACCESS_TYPE_ID_COLUMN) + ";";
  }

  public static String upsertQuery(String tenantId) {
    return prepareQuery(upsertQuery(), getAccessTypesMappingTableName(tenantId));
  }

  public static String selectByAccessTypeIdsAndRecordQuery(String tenantId, List<UUID> accessTypeIds) {
    return prepareQuery(selectByAccessTypeIdsAndRecordQuery(), getAccessTypesMappingTableName(tenantId),
      createPlaceholders(accessTypeIds.size()));
  }

  public static String deleteByRecordQuery(String tenantId) {
    return prepareQuery(deleteByRecordQuery(), getAccessTypesMappingTableName(tenantId),
      getAccessTypesTableName(tenantId));
  }

  public static String selectByRecordQuery(String tenantId) {
    return prepareQuery(selectByRecordQuery(), getAccessTypesMappingTableName(tenantId),
      getAccessTypesTableName(tenantId));
  }

  public static String selectCountByRecordIdPrefixQuery(String tenantId) {
    return prepareQuery(selectCountByRecordIdPrefixQuery(), getAccessTypesMappingTableName(tenantId),
      getAccessTypesTableName(tenantId));
  }

  public static String selectAccessTypeIdsByRecordQuery() {
    return selectQuery(ACCESS_TYPE_ID_COLUMN) + " " +
      whereQuery(RECORD_ID_COLUMN, RECORD_TYPE_COLUMN);
  }

  private static String selectCountByRecordIdPrefixQuery() {
    return selectQuery(ACCESS_TYPE_ID_COLUMN, count()) + " " + whereConditionsQuery(
      likeCondition(RECORD_ID_COLUMN),
      equalCondition(RECORD_TYPE_COLUMN),
      inCondition(ACCESS_TYPE_ID_COLUMN, AccessTypesTableConstants.selectIdsByCredentialsIdQuery())
    ) + " " + groupByQuery(ACCESS_TYPE_ID_COLUMN) + ";";
  }

  private static String selectByRecordQuery() {
    return selectQuery() + " " + whereRecordAndAccessIds() + " " + limitQuery(1) + ";";
  }

  private static String deleteByRecordQuery() {
    return deleteQuery() + " " + whereRecordAndAccessIds() + ";";
  }

  private static String selectByAccessTypeIdsAndRecordQuery() {
    return selectQuery() + " " + whereConditionsQuery(
      inCondition(ACCESS_TYPE_ID_COLUMN), equalCondition(RECORD_TYPE_COLUMN), likeCondition(RECORD_ID_COLUMN)
    ) + " " + orderByQuery(RECORD_ID_COLUMN) + " " + offsetQuery() + " " + limitQuery() + ";";
  }

  private static String whereRecordAndAccessIds() {
    return whereConditionsQuery(
      equalCondition(RECORD_ID_COLUMN),
      equalCondition(RECORD_TYPE_COLUMN),
      inCondition(ACCESS_TYPE_ID_COLUMN, AccessTypesTableConstants.selectIdsByCredentialsIdQuery())
    );
  }

}
