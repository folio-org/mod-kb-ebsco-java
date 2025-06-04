package org.folio.repository.holdings.status;

import static org.folio.common.ListUtils.createPlaceholders;
import static org.folio.repository.DbUtil.getHoldingsStatusTableName;
import static org.folio.repository.DbUtil.prepareQuery;
import static org.folio.repository.SqlQueryHelper.joinWithComma;

import io.vertx.sqlclient.Tuple;

public final class HoldingsStatusTableConstants {

  public static final String HOLDINGS_STATUS_TABLE = "holdings_status";
  public static final String ID_COLUMN = "id";
  public static final String CREDENTIALS_COLUMN = "credentials_id";
  public static final String JSONB_COLUMN = "jsonb";
  public static final String PROCESS_ID_COLUMN = "process_id";

  private static final String HOLDINGS_STATUS_FIELD_LIST_FULL =
    joinWithComma(ID_COLUMN, CREDENTIALS_COLUMN, JSONB_COLUMN, PROCESS_ID_COLUMN);

  private HoldingsStatusTableConstants() {
  }

  public static String getHoldingsStatusById() {
    return "SELECT " + HOLDINGS_STATUS_FIELD_LIST_FULL + " from %s WHERE " + CREDENTIALS_COLUMN + "=?;";
  }

  public static String getHoldingsStatusById(String tenantId) {
    return prepareQuery(getHoldingsStatusById(), getHoldingsStatusTableName(tenantId));
  }

  public static String deleteLoadingStatus(String tenantId) {
    return prepareQuery(deleteLoadingStatusQuery(), getHoldingsStatusTableName(tenantId));
  }

  public static String updateImportedCount(String tenantId) {
    return prepareQuery(updateImportedCountQuery(), getHoldingsStatusTableName(tenantId));
  }

  public static String updateLoadingStatus(String tenantId) {
    return prepareQuery(updateLoadingStatusQuery(), getHoldingsStatusTableName(tenantId));
  }

  public static String getHoldingsStatuses(String tenantId) {
    return prepareQuery(getHoldingsStatusesQuery(), getHoldingsStatusTableName(tenantId));
  }

  public static String insertLoadingStatus(String tenantId, Tuple params) {
    return prepareQuery(insertLoadingStatus(),
      getHoldingsStatusTableName(tenantId),
      createPlaceholders(params.size()));
  }

  public static String insertLoadingStatus() {
    return "INSERT INTO %s (" + HOLDINGS_STATUS_FIELD_LIST_FULL + ") VALUES (%s) ON CONFLICT DO NOTHING;";
  }

  private static String deleteLoadingStatusQuery() {
    return "DELETE FROM %s WHERE " + CREDENTIALS_COLUMN + "=?;";
  }

  private static String updateImportedCountQuery() {
    return "UPDATE %s SET jsonb = jsonb_set(jsonb_set(jsonb, "
      + "'{data,attributes,importedCount}', "
      + "((jsonb->'data'->'attributes'->>'importedCount')::int + ?)::text::jsonb, false), "
      + "'{data,attributes,importedPages}', "
      + "((jsonb->'data'->'attributes'->>'importedPages')::int + ?)::text::jsonb, false) "
      + "WHERE "
      + "jsonb->'data'->'attributes'->>'importedCount' IS NOT NULL AND "
      + "jsonb->'data'->'attributes'->>'importedPages' IS NOT NULL AND "
      + "process_id=? AND " + CREDENTIALS_COLUMN + "=?;";
  }

  private static String updateLoadingStatusQuery() {
    return "UPDATE %s SET " + JSONB_COLUMN + " = ? WHERE process_id=? AND " + CREDENTIALS_COLUMN + "=?;";
  }

  private static String getHoldingsStatusesQuery() {
    return "SELECT " + HOLDINGS_STATUS_FIELD_LIST_FULL + " from %s;";
  }
}
