package org.folio.repository.resources;

public class ResourceTableConstants {
  private ResourceTableConstants() {}

  public static final String RESOURCES_TABLE_NAME = "resources";
  public static final String ID_COLUMN = "id";
  public static final String NAME_COLUMN = "name";
  public static final String RESOURCE_FIELD_LIST = String.format("%s, %s", ID_COLUMN, NAME_COLUMN);

  public static final String INSERT_OR_UPDATE_RESOURCE_STATEMENT = "INSERT INTO %s(" + RESOURCE_FIELD_LIST + ") VALUES (?, ?) "
    + "ON CONFLICT (" + ID_COLUMN + ") DO UPDATE " + "SET " + NAME_COLUMN + " = ?;";

  public static final String DELETE_RESOURCE_STATEMENT = "DELETE FROM %s " + "WHERE " + ID_COLUMN + "=?";
}
