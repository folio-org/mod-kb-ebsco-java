package org.folio.repository.packages;

public class PackageTableConstants {
  private PackageTableConstants() {}

  public static final String PACKAGES_TABLE_NAME = "packages";
  public static final String ID_COLUMN = "id";
  public static final String NAME_COLUMN = "name";
  public static final String CONTENT_TYPE_COLUMN = "content_type";
  public static final String PACKAGE_FIELD_LIST = String.format("%s, %s, %s",
    ID_COLUMN, NAME_COLUMN, CONTENT_TYPE_COLUMN);

  public static final String INSERT_OR_UPDATE_STATEMENT =
    "INSERT INTO %s(" + PACKAGE_FIELD_LIST + ") VALUES (?, ?, ?) " +
      "ON CONFLICT (" + ID_COLUMN + ") DO UPDATE " +
      "SET " + NAME_COLUMN + " = ?, " + CONTENT_TYPE_COLUMN + " = ?";

  public static final String DELETE_STATEMENT =
    "DELETE FROM %s " +
      "WHERE " + ID_COLUMN + "=?";

  public static final String SELECT_PACKAGE_IDS_BY_TAG =
    "SELECT packages.id FROM %s " +
    "LEFT JOIN %s as tags ON tags.record_id = packages.id " +
    "WHERE tags.tag IN (%s) and packages.id LIKE ?";

  public static final String SELECT_PACKAGES_WITH_TAGS =
    "SELECT packages.id as id, packages.name as name, packages.content_type as content_type, tags.tag as tag FROM %s " +
      "LEFT JOIN %s as tags ON " +
      "tags.record_id = packages.id " +
      "AND tags.record_type = 'package' " +
      "WHERE packages.id IN (%s) " +
      "ORDER BY packages.name " +
      "OFFSET ? " +
      "LIMIT ?";

  public static final String SELECT_PACKAGES_WITH_TAGS_BY_IDS =
    "SELECT packages.id as id, packages.name as name, packages.content_type as content_type, tags.tag as tag FROM %s " +
      "LEFT JOIN %s as tags ON " +
      "tags.record_id = packages.id " +
      "AND tags.record_type = 'package' " +
      "WHERE packages.id IN (%s)";
}
