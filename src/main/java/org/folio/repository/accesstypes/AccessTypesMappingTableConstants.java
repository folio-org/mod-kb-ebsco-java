package org.folio.repository.accesstypes;

public class AccessTypesMappingTableConstants {

  public static final String ID_COLUMN = "id";
  public static final String RECORD_ID_COLUMN = "record_id";
  public static final String RECORD_TYPE_COLUMN = "record_type";
  public static final String ACCESS_TYPE_ID_COLUMN = "access_type_id";
  public static final String ACCESS_TYPES_MAPPING_TABLE_NAME = "access_types_mapping";

  public static final String ACCESS_TYPES_MAPPING_FIELD_LIST = String.format("%s, %s, %s, %s",
    ID_COLUMN, RECORD_ID_COLUMN, RECORD_TYPE_COLUMN, ACCESS_TYPE_ID_COLUMN);

  public static final String INSERT_MAPPING =
    "INSERT INTO %s (" + ACCESS_TYPES_MAPPING_FIELD_LIST + ") VALUES (?,?,?,?)";

  public static final String UPDATE_MAPPING =
    "UPDATE %s SET " + ACCESS_TYPE_ID_COLUMN + " = ? WHERE " + ID_COLUMN + " = ?;";

  private static final String CONDITION_BY_RECORD_ID_AND_RECORD_TYPE =
    String.format("%s = ? AND %s = ?", RECORD_ID_COLUMN, RECORD_TYPE_COLUMN);

  public static final String DELETE_MAPPING_BY_RECORD_ID_AND_RECORD_TYPE =
    "DELETE FROM %s WHERE " + CONDITION_BY_RECORD_ID_AND_RECORD_TYPE + ";";

  public static final String SELECT_MAPPING_BY_RECORD_ID_AND_RECORD_TYPE =
    "SELECT * FROM %s WHERE " + CONDITION_BY_RECORD_ID_AND_RECORD_TYPE + " LIMIT 1;";

  private AccessTypesMappingTableConstants() {
  }

}
