package org.folio.tag.repository.packages;

public class PackageTableConstants {
  private PackageTableConstants() {}

  public static final String TABLE_NAME = "packages";
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

  public static final String SELECT_TAGGED_PACKAGES =
    "SELECT DISTINCT packages.id as id, packages.name FROM %s " +
      "LEFT JOIN tags ON " +
      "tags.record_id = packages.id " +
      "AND tags.record_type = 'package' " +
      "WHERE tags.tag IN (%s) " +
      "ORDER BY packages.name " +
      "OFFSET ? " +
      "LIMIT ?";
}
