package org.folio.repository.packages;

import static org.folio.common.ListUtils.createPlaceholders;
import static org.folio.repository.DbUtil.getPackagesTableName;
import static org.folio.repository.DbUtil.getTagsTableName;
import static org.folio.repository.DbUtil.prepareQuery;
import static org.folio.repository.SqlQueryHelper.joinWithComma;

import java.util.List;
import org.folio.holdingsiq.model.PackageId;

public final class PackageTableConstants {

  public static final String PACKAGES_TABLE_NAME = "packages";
  public static final String ID_COLUMN = "id";
  public static final String CREDENTIALS_ID_COLUMN = "credentials_id";
  public static final String NAME_COLUMN = "name";
  public static final String CONTENT_TYPE_COLUMN = "content_type";
  public static final String PK_PACKAGES = joinWithComma(ID_COLUMN, CREDENTIALS_ID_COLUMN);
  public static final String PACKAGE_FIELD_LIST = joinWithComma(PK_PACKAGES, NAME_COLUMN, CONTENT_TYPE_COLUMN);

  private PackageTableConstants() {
  }

  public static String insertOrUpdateStatement(String tenantId) {
    return prepareQuery(insertOrUpdateStatementQuery(), getPackagesTableName(tenantId));
  }

  public static String deleteStatement(String tenantId) {
    return prepareQuery(deleteStatementQuery(), getPackagesTableName(tenantId));
  }

  public static String selectPackagesWithTags(String tenantId, List<String> tags) {
    return prepareQuery(selectPackagesWithTagsQuery(), getPackagesTableName(tenantId),
      getTagsTableName(tenantId), createPlaceholders(tags.size()));
  }

  public static String selectPackagesWithTagsByIds(String tenantId, List<PackageId> packageIds) {
    return prepareQuery(selectPackagesWithTagsByIdsQuery(), getPackagesTableName(tenantId),
      getTagsTableName(tenantId), createPlaceholders(packageIds.size()));
  }

  private static String insertOrUpdateStatementQuery() {
    return "INSERT INTO %s(" + PACKAGE_FIELD_LIST + ") VALUES (?, ?, ?, ?) "
      + "ON CONFLICT (" + ID_COLUMN + ", " + CREDENTIALS_ID_COLUMN + ") DO UPDATE "
      + "SET " + NAME_COLUMN + " = ?, " + CONTENT_TYPE_COLUMN + " = ?";
  }

  private static String deleteStatementQuery() {
    return "DELETE FROM %s " + "WHERE " + ID_COLUMN + "=? " + "AND " + CREDENTIALS_ID_COLUMN + "=?";
  }

  private static String selectPackagesWithTagsQuery() {
    return "SELECT packages.id as id, packages.credentials_id as credentials_id, packages.name as name, "
      + "packages.content_type as content_type, array_agg(tags.tag) as tag "
      + "FROM %s "
      + "INNER JOIN %s as tags ON "
      + "tags.record_id = packages.id "
      + "AND tags.record_type = 'package' "
      + "WHERE packages.id LIKE ? "
      + "AND " + CREDENTIALS_ID_COLUMN + "=? "
      + "GROUP BY packages.id, packages.credentials_id "
      + "HAVING array_agg(tags.tag) && array[%s]::varchar[] "
      + "ORDER BY packages.name "
      + "OFFSET ? "
      + "LIMIT ?";
  }

  private static String selectPackagesWithTagsByIdsQuery() {
    return "SELECT packages.id as id, packages.credentials_id as credentials_id, packages.name as name, "
      + "packages.content_type as content_type, array_agg(tags.tag) as tag "
      + "FROM %s "
      + "LEFT JOIN %s as tags ON "
      + "tags.record_id = packages.id "
      + "AND tags.record_type = 'package' "
      + "WHERE packages.id IN (%s)"
      + "AND " + CREDENTIALS_ID_COLUMN + "=?"
      + "GROUP BY packages.id, packages.credentials_id";
  }

}
