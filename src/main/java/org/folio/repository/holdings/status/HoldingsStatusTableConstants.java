package org.folio.repository.holdings.status;

public class HoldingsStatusTableConstants {

  public static final String HOLDINGS_STATUS_TABLE = "holdings_status";
  public static final String ID_COLUMN = "id";
  public static final String JSONB_COLUMN = "jsonb";
  public static final String HOLDINGS_STATUS_FIELD_LIST = String.format("%s, %s", ID_COLUMN, JSONB_COLUMN);
  public static final String GET_HOLDINGS_STATUS = "SELECT " + HOLDINGS_STATUS_FIELD_LIST + " from %s;";
  public static final String INSERT_LOADING_STATUS = "INSERT INTO %s (" + HOLDINGS_STATUS_FIELD_LIST + ") VALUES (%s) ON CONFLICT DO NOTHING;";
  public static final String UPDATE_LOADING_STATUS = "UPDATE %s SET " + JSONB_COLUMN + " = '%s'::jsonb;";
  public static final String UPDATE_IMPORTED_COUNT =
    "UPDATE %s SET jsonb = jsonb_set(jsonb_set(jsonb, " +
      "'{data,attributes,importedCount}', ((jsonb->'data'->'attributes'->>'importedCount')::int + %s)::text::jsonb, false), " +
      "'{data,attributes,importedPages}', ((jsonb->'data'->'attributes'->>'importedPages')::int + %s)::text::jsonb, false) " +
      "WHERE " +
      "jsonb->'data'->'attributes'->>'importedCount' IS NOT NULL AND " +
      "jsonb->'data'->'attributes'->>'importedPages' IS NOT NULL ";
  public static final String DELETE_LOADING_STATUS = "DELETE FROM %s;";

  private HoldingsStatusTableConstants() { }
}
