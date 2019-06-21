package org.folio.tag.repository.resources;

public class HoldingsTableConstants {
  public static final String HOLDINGS_TABLE = "holdings";
  public static final String ID_COLUMN = "id";
  public static final String JSONB_COLUMN = "jsonb";
  public static final String UPDATED_AT_COLUMN = "updated_at";
  public static final String HOLDINGS_FIELD_LIST = String.format("%s, %s, %s", ID_COLUMN, JSONB_COLUMN, UPDATED_AT_COLUMN);
  public static final String INSERT_OR_UPDATE_HOLDINGS_STATEMENT =
    "INSERT INTO %s(" + HOLDINGS_FIELD_LIST + ") VALUES %s" +
    "ON CONFLICT (" + ID_COLUMN + ") DO UPDATE SET " + JSONB_COLUMN + " = EXCLUDED." + JSONB_COLUMN + ","
      + UPDATED_AT_COLUMN + "= EXCLUDED." + UPDATED_AT_COLUMN + ";";
  public static final String REMOVE_FROM_HOLDINGS = "DELETE FROM %s WHERE " + UPDATED_AT_COLUMN + " != timestamp with time zone '%s';";

  public static final String GET_HOLDINGS_BY_IDS = "SELECT * from %s WHERE id IN (%s);";

  private HoldingsTableConstants() { }
}
