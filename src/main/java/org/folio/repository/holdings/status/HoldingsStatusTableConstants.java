package org.folio.repository.holdings.status;

import static org.folio.repository.SqlQueryHelper.joinWithComma;

public final class HoldingsStatusTableConstants {

  public static final String HOLDINGS_STATUS_TABLE = "holdings_status";
  public static final String ID_COLUMN = "id";
  public static final String CREDENTIALS_COLUMN = "credentials_id";
  public static final String JSONB_COLUMN = "jsonb";
  public static final String PROCESS_ID_COLUMN = "process_id";
  private static final String HOLDINGS_STATUS_FIELD_LIST_FULL = joinWithComma(ID_COLUMN, CREDENTIALS_COLUMN, JSONB_COLUMN, PROCESS_ID_COLUMN);
  public static final String GET_HOLDINGS_STATUSES = "SELECT " + HOLDINGS_STATUS_FIELD_LIST_FULL + " from %s;";
  public static final String GET_HOLDINGS_STATUS_BY_ID = "SELECT " + HOLDINGS_STATUS_FIELD_LIST_FULL + " from %s WHERE " + CREDENTIALS_COLUMN + "=?;";
  public static final String INSERT_LOADING_STATUS = "INSERT INTO %s (" + HOLDINGS_STATUS_FIELD_LIST_FULL + ") VALUES (%s) ON CONFLICT DO NOTHING;";
  public static final String UPDATE_LOADING_STATUS = "UPDATE %s SET " + JSONB_COLUMN + " = ? WHERE process_id=? AND " + CREDENTIALS_COLUMN + "=?;";
  public static final String UPDATE_IMPORTED_COUNT =
    "UPDATE %s SET jsonb = jsonb_set(jsonb_set(jsonb, " +
      "'{data,attributes,importedCount}', ((jsonb->'data'->'attributes'->>'importedCount')::int + %s)::text::jsonb, false), " +
      "'{data,attributes,importedPages}', ((jsonb->'data'->'attributes'->>'importedPages')::int + %s)::text::jsonb, false) " +
      "WHERE " +
      "jsonb->'data'->'attributes'->>'importedCount' IS NOT NULL AND " +
      "jsonb->'data'->'attributes'->>'importedPages' IS NOT NULL AND " +
      "process_id=? AND " + CREDENTIALS_COLUMN + "=?;";
  public static final String DELETE_LOADING_STATUS = "DELETE FROM %s WHERE " + CREDENTIALS_COLUMN + "=?;";

  private HoldingsStatusTableConstants() { }
}
