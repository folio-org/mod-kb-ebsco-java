package org.folio.repository.packages;

import static org.folio.repository.SqlQueryHelper.joinWithComma;

public final class PackageTableConstants {
  private PackageTableConstants() { }

  public static final String PACKAGES_TABLE_NAME = "packages";
  public static final String ID_COLUMN = "id";
  public static final String CREDENTIALS_ID_COLUMN = "credentials_id";
  public static final String NAME_COLUMN = "name";
  public static final String CONTENT_TYPE_COLUMN = "content_type";
  public static final String PK_PACKAGES = joinWithComma(ID_COLUMN, CREDENTIALS_ID_COLUMN);
  public static final String PACKAGE_FIELD_LIST = joinWithComma(PK_PACKAGES, NAME_COLUMN, CONTENT_TYPE_COLUMN);

  public static final String INSERT_OR_UPDATE_STATEMENT =
    "INSERT INTO %s(" + PACKAGE_FIELD_LIST + ") VALUES (?, ?, ?, ?) " +
      "ON CONFLICT (" + ID_COLUMN + ", " +  CREDENTIALS_ID_COLUMN + ") DO UPDATE " +
      "SET " + NAME_COLUMN + " = ?, " + CONTENT_TYPE_COLUMN + " = ?";

  public static final String DELETE_STATEMENT =
    "DELETE FROM %s " +
      "WHERE " + ID_COLUMN + "=? " +
      "AND " + CREDENTIALS_ID_COLUMN + "=?";

  public static final String SELECT_PACKAGES_WITH_TAGS =
    "SELECT packages.id as id, packages.credentials_id as credentials_id, packages.name as name, " +
        "packages.content_type as content_type, array_agg(tags.tag) as tag " +
      "FROM %s " +
      "INNER JOIN %s as tags ON " +
      "tags.record_id = packages.id " +
      "AND tags.record_type = 'package' " +
      "WHERE packages.id LIKE ? " +
      "AND " + CREDENTIALS_ID_COLUMN + "=? " +
      "GROUP BY packages.id, packages.credentials_id " +
      "HAVING array_agg(tags.tag) && array[%s]::varchar[] " +
      "ORDER BY packages.name " +
      "OFFSET ? " +
      "LIMIT ?";

  public static final String SELECT_PACKAGES_WITH_TAGS_BY_IDS =
    "SELECT packages.id as id, packages.credentials_id as credentials_id, packages.name as name, " +
        "packages.content_type as content_type, array_agg(tags.tag) as tag " +
      "FROM %s " +
      "LEFT JOIN %s as tags ON " +
      "tags.record_id = packages.id " +
      "AND tags.record_type = 'package' " +
      "WHERE packages.id IN (%s)" +
      "AND " + CREDENTIALS_ID_COLUMN + "=?" +
      "GROUP BY packages.id, packages.credentials_id";
}
