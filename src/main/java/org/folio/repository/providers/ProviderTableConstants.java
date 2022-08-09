package org.folio.repository.providers;

import static org.folio.common.ListUtils.createPlaceholders;
import static org.folio.repository.DbUtil.getProviderTableName;
import static org.folio.repository.DbUtil.getTagsTableName;
import static org.folio.repository.DbUtil.prepareQuery;
import static org.folio.repository.SqlQueryHelper.joinWithComma;

import java.util.List;

public final class ProviderTableConstants {

  public static final String PROVIDERS_TABLE_NAME = "providers";
  public static final String ID_COLUMN = "id";
  public static final String CREDENTIALS_ID_COLUMN = "credentials_id";
  public static final String NAME_COLUMN = "name";
  public static final String PROVIDER_FIELD_LIST = joinWithComma(ID_COLUMN, CREDENTIALS_ID_COLUMN, NAME_COLUMN);

  private ProviderTableConstants() {
  }

  public static String insertOrUpdateProviderStatement(String tenantId) {
    return prepareQuery(insertOrUpdateProviderStatementPart(), getProviderTableName(tenantId));
  }

  public static String deleteProviderStatement(String tenantId) {
    return prepareQuery(deleteProviderStatementPart(), getProviderTableName(tenantId));
  }

  public static String selectTaggedProviders(String tenantId, List<String> tags) {
    return prepareQuery(selectTaggedProviders(), getProviderTableName(tenantId),
      getTagsTableName(tenantId), createPlaceholders(tags.size()));
  }

  private static String selectTaggedProviders() {
    return "SELECT DISTINCT providers.id as id, providers.name "
      + "FROM %s "
      + "INNER JOIN %s as tags ON "
      + "tags.record_id = providers.id "
      + "AND tags.record_type = 'provider' "
      + "WHERE tags.tag IN (%s) "
      + "AND " + CREDENTIALS_ID_COLUMN + "=? "
      + "ORDER BY providers.name "
      + "OFFSET ? "
      + "LIMIT ?";
  }

  private static String insertOrUpdateProviderStatementPart() {
    return "INSERT INTO %s (" + PROVIDER_FIELD_LIST + ") VALUES (?, ?, ?) "
      + "ON CONFLICT (" + ID_COLUMN + ", " + CREDENTIALS_ID_COLUMN + ") DO UPDATE "
      + "SET " + NAME_COLUMN + " = ?;";
  }

  private static String deleteProviderStatementPart() {
    return "DELETE FROM %s "
      + "WHERE " + ID_COLUMN + "=? "
      + "AND " + CREDENTIALS_ID_COLUMN + "=?";
  }

}
