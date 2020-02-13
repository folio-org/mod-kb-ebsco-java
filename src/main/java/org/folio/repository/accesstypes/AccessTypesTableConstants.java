package org.folio.repository.accesstypes;

public class AccessTypesTableConstants {

  public static final String ID_COLUMN = "id";
  public static final String ACCESS_TYPES_TABLE_NAME = "access_types";
  public static final String JSONB_COLUMN = "jsonb";

  public static final String SELECT_ALL_ACCESS_TYPES = "SELECT *  FROM %s ;";
  public static final String SELECT_COUNT_ACCESS_TYPES = "SELECT COUNT(*)  FROM %s ;";

  private AccessTypesTableConstants() {
  }

}
