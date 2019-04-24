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
}
