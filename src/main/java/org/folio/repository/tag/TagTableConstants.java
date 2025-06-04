package org.folio.repository.tag;

import static org.folio.repository.DbUtil.getTagsTableName;
import static org.folio.repository.DbUtil.prepareQuery;

@SuppressWarnings("squid:S1192")
public final class TagTableConstants {

  public static final String TAGS_TABLE_NAME = "tags";
  public static final String TAG_COLUMN = "tag";
  public static final String ID_COLUMN = "id";
  public static final String RECORD_ID_COLUMN = "record_id";
  public static final String RECORD_TYPE_COLUMN = "record_type";
  public static final String COUNT_COLUMN = "count";
  public static final String TAG_FIELD_LIST = String.format("%s, %s, %s, %s",
    ID_COLUMN, RECORD_ID_COLUMN, RECORD_TYPE_COLUMN, TAG_COLUMN);

  private TagTableConstants() {
  }

  public static String selectAllTags(String tenantId) {
    return prepareQuery(selectAllTagsQuery(), getTagsTableName(tenantId));
  }

  public static String selectTagsByRecordIdAndRecordType(String tenantId) {
    return prepareQuery(selectTagsByRecordIdAndRecordTypeQuery(), getTagsTableName(tenantId));
  }

  public static String selectTagsByRecordTypes(String tenantId, String placeholders) {
    return prepareQuery(selectTagsByRecordTypesQuery(), getTagsTableName(tenantId), placeholders);
  }

  public static String getCountRecordsByTagValueAndTypeAndRecordIdPrefix(String tenantId, String values) {
    return prepareQuery(getCountRecordsByTagValueAndTypeAndRecordIdPrefixQuery(), getTagsTableName(tenantId), values);
  }

  public static String updateInsertStatementForProvider(String tenantId, String updatedValues) {
    return prepareQuery(updateInsertStatementForProviderQuery(), getTagsTableName(tenantId), updatedValues);
  }

  public static String deleteTagRecord(String tenantId) {
    return prepareQuery(deleteTagRecordQuery(), getTagsTableName(tenantId));
  }

  public static String selectTagsByResourceIds(String tenantId, String placeholders) {
    return prepareQuery(selectTagsByResourceIdsQuery(), getTagsTableName(tenantId), placeholders);
  }

  public static String selectAllDistinctTags(String tenantId) {
    return prepareQuery(selectAllDistinctTagsQuery(), getTagsTableName(tenantId));
  }

  public static String selectDistinctTagsByRecordTypes(String tenantId, String placeholders) {
    return prepareQuery(selectDistinctTagsByRecordTypesQuery(), getTagsTableName(tenantId), placeholders);
  }

  private static String selectAllTagsQuery() {
    return "SELECT " + TAG_FIELD_LIST + " FROM %s ORDER BY " + TAG_COLUMN;
  }

  private static String selectTagsByRecordIdAndRecordTypeQuery() {
    return "SELECT " + TAG_FIELD_LIST + " FROM %s "
      + "WHERE " + RECORD_ID_COLUMN + "=? AND " + RECORD_TYPE_COLUMN + "=? ORDER BY " + TAG_COLUMN;
  }

  private static String selectTagsByRecordTypesQuery() {
    return "SELECT " + TAG_FIELD_LIST + " FROM %s "
      + "WHERE " + RECORD_TYPE_COLUMN + " IN (%s) ORDER BY " + TAG_COLUMN;
  }

  private static String getCountRecordsByTagValueAndTypeAndRecordIdPrefixQuery() {
    return "SELECT COUNT(DISTINCT " + RECORD_ID_COLUMN + ") AS " + COUNT_COLUMN + " FROM %s "
      + "WHERE " + TAG_COLUMN + " IN (%s) AND " + RECORD_TYPE_COLUMN + "=? AND " + RECORD_ID_COLUMN + " LIKE ?";
  }

  private static String updateInsertStatementForProviderQuery() {
    return "INSERT INTO %s (" + TAG_FIELD_LIST + ") VALUES " + "%s;";
  }

  private static String deleteTagRecordQuery() {
    return "DELETE FROM %s WHERE " + RECORD_ID_COLUMN + "=? AND " + RECORD_TYPE_COLUMN + "=?";
  }

  private static String selectTagsByResourceIdsQuery() {
    return "SELECT " + TAG_FIELD_LIST + " FROM %s WHERE "
      + RECORD_ID_COLUMN + " IN (%s) AND " + RECORD_TYPE_COLUMN + "=?";
  }

  private static String selectAllDistinctTagsQuery() {
    return "SELECT DISTINCT " + TAG_COLUMN + " FROM %s";
  }

  private static String selectDistinctTagsByRecordTypesQuery() {
    return "SELECT DISTINCT tag FROM %s WHERE " + RECORD_TYPE_COLUMN + " IN (%s) ORDER BY " + TAG_COLUMN;
  }
}
