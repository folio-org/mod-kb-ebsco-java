package org.folio.repository.accesstypes;

public class AccessTypesTableConstants {

  public static final String ID_COLUMN = "id";
  public static final String RECORD_ID_COLUMN = "record_id";
  public static final String RECORD_TYPE_COLUMN = "record_type";
  public static final String ACCESS_TYPE_ID_COLUMN = "access_type_id";
  public static final String ACCESS_TYPES_TABLE_NAME = "access_types";
  public static final String ACCESS_TYPES_MAPPING_TABLE_NAME = "access_types_mapping";
  public static final String ACCESS_TYPES_TYPE = "accessTypes";
  public static final String JSONB_COLUMN = "jsonb";

  public static final String SELECT_ALL_ACCESS_TYPES = "SELECT *  FROM %s ;";
  public static final String SELECT_COUNT_ACCESS_TYPES = "SELECT COUNT(*)  FROM %s ;";
  public static final String SELECT_COUNT_ACCESS_TYPES_BY_ID = "SELECT COUNT(*) FROM %s WHERE " + ID_COLUMN + " = (?);";

  public static final String TAG_FIELD_LIST = String.format("%s, %s, %s, %s",
    ID_COLUMN, RECORD_ID_COLUMN, RECORD_TYPE_COLUMN, ACCESS_TYPE_ID_COLUMN);

  public static final String SELECT_ACCESS_TYPE_MAPPING = "SELECT *  FROM %s;";
  public static final String SELECT_ACCESS_TYPE_MAPPING_BY_RECORD_ID =
    "SELECT *  FROM %s WHERE " + RECORD_ID_COLUMN + "=? AND " + RECORD_TYPE_COLUMN + "=?;";
  public static final String INSERT_ACCESS_TYPE_MAPPING = "INSERT INTO %s (" + TAG_FIELD_LIST + ") VALUES (?,?,?,?)";


  private AccessTypesTableConstants() {
  }

}
