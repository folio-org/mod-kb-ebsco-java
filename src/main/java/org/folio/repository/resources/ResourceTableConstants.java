package org.folio.repository.resources;

import static org.folio.common.ListUtils.createPlaceholders;
import static org.folio.repository.DbUtil.getResourcesTableName;
import static org.folio.repository.DbUtil.getTagsTableName;
import static org.folio.repository.DbUtil.prepareQuery;
import static org.folio.repository.SqlQueryHelper.joinWithComma;

import java.util.List;

public final class ResourceTableConstants {

  public static final String RESOURCES_TABLE_NAME = "resources";
  public static final String ID_COLUMN = "id";
  public static final String CREDENTIALS_ID_COLUMN = "credentials_id";
  public static final String NAME_COLUMN = "name";
  public static final String RESOURCE_FIELD_LIST = joinWithComma(ID_COLUMN, CREDENTIALS_ID_COLUMN, NAME_COLUMN);

  private ResourceTableConstants() {
  }

  public static String insertOrUpdateResourceStatement(String tenantId) {
    return prepareQuery(insertOrUpdateResourceStatementPart(), getResourcesTableName(tenantId));
  }

  public static String deleteResourceStatement(String tenantId) {
    return prepareQuery(deleteResourceStatementPart(), getResourcesTableName(tenantId));
  }

  public static String selectResourcesWithTags(String tenantId, List<String> tags) {
    return prepareQuery(selectResourcesWithTagsQuery(), getResourcesTableName(tenantId),
      getTagsTableName(tenantId), createPlaceholders(tags.size()));
  }

  private static String insertOrUpdateResourceStatementPart() {
    return "INSERT INTO %s(" + RESOURCE_FIELD_LIST + ") VALUES (?, ?, ?) "
      + "ON CONFLICT (" + ID_COLUMN + ", " + CREDENTIALS_ID_COLUMN + ") DO UPDATE "
      + "SET " + NAME_COLUMN + " = ?;";
  }

  private static String deleteResourceStatementPart() {
    return "DELETE FROM %s "
      + "WHERE " + ID_COLUMN + "=? "
      + "AND " + CREDENTIALS_ID_COLUMN + "=?";
  }

  private static String selectResourcesWithTagsQuery() {
    return "SELECT resources.id as id, resources.credentials_id as credentials_id, "
      + "resources.name as name, array_agg(tags.tag) as tag "
      + "FROM %s "
      + "INNER JOIN %s as tags ON "
      + "tags.record_id = resources.id "
      + "AND tags.record_type = 'resource' "
      + "WHERE resources.id LIKE ? "
      + "AND " + CREDENTIALS_ID_COLUMN + "=? "
      + "GROUP BY resources.id, resources.credentials_id "
      + "HAVING array_agg(tags.tag) && array[%s]::varchar[] "
      + "ORDER BY resources.name "
      + "OFFSET ? "
      + "LIMIT ?";
  }
}
