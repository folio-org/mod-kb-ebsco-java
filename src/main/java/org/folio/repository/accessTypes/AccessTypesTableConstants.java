package org.folio.repository.accessTypes;

public class AccessTypesTableConstants {
  private AccessTypesTableConstants() {}

  public static final String ACCESS_TYPES_TABLE_NAME = "access_types";
  public static final String ACCESS_TYPES_TYPE = "accessTypes";
  public static final String JSONB_COLUMN = "jsonb";

  static final String SELECT_ALL_ACCESS_TYPES = "SELECT *  FROM %s ;";
  static final String SELECT_COUNT_ACCESS_TYPES = "SELECT COUNT(*)  FROM %s ;";

}
