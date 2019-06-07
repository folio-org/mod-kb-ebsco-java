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

  public static final String SELECT_RESOURCE_IDS_BY_TAG =
    "SELECT resources.id FROM %s " +
    "LEFT JOIN %s as tags ON tags.record_id = resources.id " +
    "WHERE tags.tag IN (%s) and resources.id LIKE ?";

  public static final String SELECT_RESOURCES_WITH_TAGS =
    "SELECT resources.id as id, resources.name as name, tags.tag as tag FROM %s " +
      "LEFT JOIN %s as tags ON " +
      "tags.record_id = resources.id " +
      "AND tags.record_type = 'resource' " +
      "WHERE resources.id IN (%s) " +
      "ORDER BY resources.name " +
      "OFFSET ? " +
      "LIMIT ?";
}
