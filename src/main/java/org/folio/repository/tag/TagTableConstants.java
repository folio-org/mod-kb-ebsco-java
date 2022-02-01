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
    return prepareQuery(selectAllTags(), getTagsTableName(tenantId));
  }

  public static String selectTagsByRecordIdAndRecordType(String tenantId) {
    return prepareQuery(selectTagsByRecordIdAndRecordType(), getTagsTableName(tenantId));
  }

  public static String selectTagsByRecordTypes(String tenantId, String placeholders) {
    return prepareQuery(selectTagsByRecordTypes(), getTagsTableName(tenantId), placeholders);
  }

  public static String getCountRecordsByTagValueAndTypeAndRecordIdPrefix(String tenantId, String values) {
    return prepareQuery(getCountRecordsByTagValueAndTypeAndRecordIdPrefix(), getTagsTableName(tenantId), values);
  }

  public static String updateInsertStatementForProvider(String tenantId, String updatedValues) {
    return prepareQuery(updateInsertStatementForProvider(), getTagsTableName(tenantId), updatedValues);
  }

  public static String deleteTagRecord(String tenantId) {
    return prepareQuery(deleteTagRecord(), getTagsTableName(tenantId));
  }

  public static String selectTagsByResourceIds(String tenantId, String placeholders) {
    return prepareQuery(selectTagsByResourceIds(), getTagsTableName(tenantId), placeholders);
  }

  public static String selectAllDistinctTags(String tenantId) {
    return prepareQuery(selectAllDistinctTags(), getTagsTableName(tenantId));
  }

  public static String selectDistinctTagsByRecordTypes(String tenantId, String placeholders) {
    return prepareQuery(selectDistinctTagsByRecordTypes(), getTagsTableName(tenantId), placeholders);
  }

  private static String selectAllTags() {
    return "SELECT " + TAG_FIELD_LIST + " FROM %s ORDER BY " + TAG_COLUMN;
  }

  private static String selectTagsByRecordIdAndRecordType() {
    return "SELECT " + TAG_FIELD_LIST + " FROM %s "
      + "WHERE " + RECORD_ID_COLUMN + "=? AND " + RECORD_TYPE_COLUMN + "=? ORDER BY " + TAG_COLUMN;
  }

  private static String selectTagsByRecordTypes() {
    return "SELECT " + TAG_FIELD_LIST + " FROM %s "
      + "WHERE " + RECORD_TYPE_COLUMN + " IN (%s) ORDER BY " + TAG_COLUMN;
  }

  private static String getCountRecordsByTagValueAndTypeAndRecordIdPrefix() {
    return "SELECT COUNT(DISTINCT " + RECORD_ID_COLUMN + ") AS " + COUNT_COLUMN + " FROM %s "
      + "WHERE " + TAG_COLUMN + " IN (%s) AND " + RECORD_TYPE_COLUMN + "=? AND " + RECORD_ID_COLUMN + " LIKE ?";
  }

  private static String updateInsertStatementForProvider() {
    return "INSERT INTO %s (" + TAG_FIELD_LIST + ") VALUES " +
      "%s;";
  }

  private static String deleteTagRecord() {
    return "DELETE FROM %s WHERE " + RECORD_ID_COLUMN + "=? AND " + RECORD_TYPE_COLUMN + "=?";
  }

  private static String selectTagsByResourceIds() {
    return "SELECT " + TAG_FIELD_LIST + " FROM %s WHERE "
      + RECORD_ID_COLUMN + " IN (%s) AND " + RECORD_TYPE_COLUMN + "=?";
  }

  private static String selectAllDistinctTags() {
    return "SELECT DISTINCT tag FROM %s";
  }

  private static String selectDistinctTagsByRecordTypes() {
    return "SELECT DISTINCT tag FROM %s WHERE " + RECORD_TYPE_COLUMN + " IN (%s) ORDER BY " + TAG_COLUMN;
  }

}
