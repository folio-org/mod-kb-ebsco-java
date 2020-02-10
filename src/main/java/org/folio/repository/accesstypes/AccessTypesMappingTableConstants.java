package org.folio.repository.accesstypes;

public class AccessTypesMappingTableConstants {

  public static final String ID_COLUMN = "id";
  public static final String RECORD_ID_COLUMN = "record_id";
  public static final String RECORD_TYPE_COLUMN = "record_type";
  public static final String ACCESS_TYPE_ID_COLUMN = "access_type_id";
  public static final String ACCESS_TYPES_MAPPING_TABLE_NAME = "access_types_mapping";

  public static final String ACCESS_TYPES_MAPPING_FIELD_LIST = String.format("%s, %s, %s, %s",
    ID_COLUMN, RECORD_ID_COLUMN, RECORD_TYPE_COLUMN, ACCESS_TYPE_ID_COLUMN);

  public static final String INSERT_ACCESS_TYPE_MAPPING =
    "INSERT INTO %s (" + ACCESS_TYPES_MAPPING_FIELD_LIST + ") VALUES (?,?,?,?)";


  private AccessTypesMappingTableConstants() {
  }

}
