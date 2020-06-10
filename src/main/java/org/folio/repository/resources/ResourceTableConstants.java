package org.folio.repository.resources;

import static org.folio.repository.SqlQueryHelper.joinWithComma;

public class ResourceTableConstants {
  private ResourceTableConstants() {}

  public static final String RESOURCES_TABLE_NAME = "resources";
  public static final String ID_COLUMN = "id";
  public static final String CREDENTIALS_ID_COLUMN = "credentials_id";
  public static final String NAME_COLUMN = "name";
  public static final String RESOURCE_FIELD_LIST = joinWithComma(ID_COLUMN, CREDENTIALS_ID_COLUMN, NAME_COLUMN);

  public static final String INSERT_OR_UPDATE_RESOURCE_STATEMENT =
    "INSERT INTO %s(" + RESOURCE_FIELD_LIST + ") VALUES (?, ?, ?) " +
      "ON CONFLICT (" + ID_COLUMN + ", " + CREDENTIALS_ID_COLUMN + ") DO UPDATE " +
      "SET " + NAME_COLUMN + " = ?;";

  public static final String DELETE_RESOURCE_STATEMENT =
    "DELETE FROM %s " +
      "WHERE " + ID_COLUMN + "=? " +
      "AND " + CREDENTIALS_ID_COLUMN + "=?";

  public static final String SELECT_RESOURCES_WITH_TAGS =
    "SELECT resources.id as id, resources.credentials_id as credentials_id, " +
          "resources.name as name, tags.tag as tag " +
      "FROM %s " +
      "INNER JOIN %s as tags ON " +
      "tags.record_id = resources.id " +
      "AND tags.record_type = 'resource' " +
      "AND tags.tag IN (%s) " +
      "WHERE resources.id LIKE ? " +
      "AND " + CREDENTIALS_ID_COLUMN + "=? " +
      "ORDER BY resources.name " +
      "OFFSET ? " +
      "LIMIT ?";
}
