package org.folio.tag.repository;

public class TagTableConstants {
  private TagTableConstants() {}

  public static final String TABLE_NAME = "tags";
  public static final String TAG_COLUMN = "tag";
  public static final String ID_COLUMN = "id";
  public static final String RECORD_ID_COLUMN = "record_id";
  public static final String RECORD_TYPE_COLUMN = "record_type";

  public static final String SELECT_TAG_VALUES_BY_ID_AND_TYPE =
    "SELECT " + TAG_COLUMN + " FROM %s "
      + "WHERE " + RECORD_ID_COLUMN + "=? AND " + RECORD_TYPE_COLUMN + "=?";

  public static  final String UPDATE_INSERT_STATEMENT_FOR_PROVIDER =
    "INSERT INTO %s (id, record_id, record_type, tag) VALUES " +
      "%s;";


  public static final String DELETE_TAG_RECORD =
    "DELETE FROM %s WHERE " + RECORD_ID_COLUMN + "=? AND " + RECORD_TYPE_COLUMN + "=?";

}
