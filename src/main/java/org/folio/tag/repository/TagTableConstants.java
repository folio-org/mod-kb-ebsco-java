package org.folio.tag.repository;

@SuppressWarnings("squid:S1192")
public class TagTableConstants {
  private TagTableConstants() {}

  public static final String TABLE_NAME = "tags";
  public static final String TAG_COLUMN = "tag";
  public static final String ID_COLUMN = "id";
  public static final String RECORD_ID_COLUMN = "record_id";
  public static final String RECORD_TYPE_COLUMN = "record_type";
  public static final String TAG_FIELD_LIST = String.format("%s, %s, %s, %s",
    ID_COLUMN, RECORD_ID_COLUMN, RECORD_TYPE_COLUMN, TAG_COLUMN);

  static final String SELECT_ALL_TAGS =
    "SELECT " + TAG_FIELD_LIST + " FROM %s ORDER BY " + TAG_COLUMN;

  static final String SELECT_TAGS_BY_RECORD_ID_AND_RECORD_TYPE =
    "SELECT " + TAG_FIELD_LIST + " FROM %s "
      + "WHERE " + RECORD_ID_COLUMN + "=? AND " + RECORD_TYPE_COLUMN + "=? ORDER BY " + TAG_COLUMN;

  static final String SELECT_TAGS_BY_RECORD_TYPES =
    "SELECT " + TAG_FIELD_LIST + " FROM %s "
      + "WHERE " + RECORD_TYPE_COLUMN + " IN (%s) ORDER BY " + TAG_COLUMN;

  public static final String COUNT_RECORDS_BY_TAG_VALUE_AND_TYPE =
    "SELECT COUNT(DISTINCT " + RECORD_ID_COLUMN + ") AS count FROM %s "
      + "WHERE " + TAG_COLUMN + " IN (%s) AND " + RECORD_TYPE_COLUMN + "=?";


  static  final String UPDATE_INSERT_STATEMENT_FOR_PROVIDER =
    "INSERT INTO %s (" + TAG_FIELD_LIST + ") VALUES " +
      "%s;";

  static final String DELETE_TAG_RECORD =
    "DELETE FROM %s WHERE " + RECORD_ID_COLUMN + "=? AND " + RECORD_TYPE_COLUMN + "=?";

}
