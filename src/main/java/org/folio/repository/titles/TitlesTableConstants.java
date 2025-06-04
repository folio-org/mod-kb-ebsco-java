package org.folio.repository.titles;

import static org.folio.common.ListUtils.createPlaceholders;
import static org.folio.repository.DbUtil.getHoldingsTableName;
import static org.folio.repository.DbUtil.getResourcesTableName;
import static org.folio.repository.DbUtil.getTagsTableName;
import static org.folio.repository.DbUtil.getTitlesTableName;
import static org.folio.repository.DbUtil.prepareQuery;
import static org.folio.repository.tag.TagTableConstants.TAG_COLUMN;

import java.util.List;

public final class TitlesTableConstants {

  public static final String TITLES_TABLE_NAME = "titles";
  public static final String ID_COLUMN = "id";
  public static final String CREDENTIALS_ID_COLUMN = "credentials_id";
  public static final String NAME_COLUMN = "name";
  public static final String HOLDINGS_ID_COLUMN = "h_id";
  public static final String COUNT_COLUMN = "count";
  public static final String TITLE_FIELD_LIST = String.format("%s, %s, %s", ID_COLUMN, CREDENTIALS_ID_COLUMN,
    NAME_COLUMN);

  private TitlesTableConstants() {
  }

  public static String insertOrUpdateTitleStatement(String tenantId) {
    return prepareQuery(insertOrUpdateTitleStatementPart(), getTitlesTableName(tenantId));
  }

  public static String deleteTitleStatement(String tenantId) {
    return prepareQuery(deleteTitleStatementPart(), getTitlesTableName(tenantId));
  }

  public static String getCountTitlesByResourceTags(String tenantId, List<String> tags) {
    return prepareQuery(getCountTitlesByResourceTagsStatementPart(),
      getResourcesTableName(tenantId), getTagsTableName(tenantId), createPlaceholders(tags.size()));
  }

  public static String selectTitlesByResourceTags(String tenantId, List<String> tags) {
    return prepareQuery(selectTitlesByResourceTagsStatementPart(),
      getResourcesTableName(tenantId), getTagsTableName(tenantId), getHoldingsTableName(tenantId),
      createPlaceholders(tags.size()));
  }

  private static String insertOrUpdateTitleStatementPart() {
    return "INSERT INTO %s (" + TITLE_FIELD_LIST + ") VALUES (?, ?, ?) "
      + "ON CONFLICT (" + ID_COLUMN + ", " + CREDENTIALS_ID_COLUMN + ") DO UPDATE "
      + "SET " + NAME_COLUMN + " = ?;";
  }

  private static String deleteTitleStatementPart() {
    return "DELETE FROM %s "
      + "WHERE " + ID_COLUMN + "=? "
      + "AND " + CREDENTIALS_ID_COLUMN + "=?";
  }

  private static String getCountTitlesByResourceTagsStatementPart() {
    return "SELECT COUNT(DISTINCT (regexp_split_to_array(resources.id, '-'))[3]) AS " + COUNT_COLUMN + " "
      + "FROM %s as resources "
      + "INNER JOIN %s as tags ON "
      + "tags.record_id = resources.id "
      + "AND tags.record_type = 'resource' "
      + "WHERE tags." + TAG_COLUMN + " IN (%s) "
      + "AND resources." + CREDENTIALS_ID_COLUMN + "=?";
  }

  private static String selectTitlesByResourceTagsStatementPart() {
    return "SELECT DISTINCT (regexp_split_to_array(resources.id, '-'))[3] as id, "
      + "resources.credentials_id as credentials_id, resources.name as name, "
      + "holdings.id as " + HOLDINGS_ID_COLUMN + ", holdings.vendor_id, holdings.package_id, holdings.title_id, "
      + "holdings.resource_type, holdings.publisher_name, holdings.publication_title "
      + "FROM %s as resources "
      + "INNER JOIN %s as tags ON "
      + "tags.record_id = resources.id "
      + "AND tags.record_type = 'resource' "
      + "LEFT JOIN %s as holdings ON "
      + "holdings.id = resources.id "
      + "WHERE tags.tag IN (%s) "
      + "AND resources." + CREDENTIALS_ID_COLUMN + "=? "
      + "ORDER BY resources.name "
      + "OFFSET ? "
      + "LIMIT ? ";
  }
}
