package org.folio.repository.holdings;

public class HoldingsTableConstants {
  public static final String HOLDINGS_TABLE = "holdings";
  public static final String ID_COLUMN = "id";
  public static final String CREDENTIALS_ID_COLUMN = "credentials_id";
  public static final String TITLE_ID_COLUMN = "title_id";
  public static final String VENDOR_ID_COLUMN = "vendor_id";
  public static final String PACKAGE_ID_COLUMN = "package_id";
  public static final String RESOURCE_TYPE_COLUMN = "resource_type";
  public static final String PUBLISHER_NAME_COLUMN = "publisher_name";
  public static final String PUBLICATION_TITLE_COLUMN = "publication_title";
  public static final String UPDATED_AT_COLUMN = "updated_at";
  public static final String PK_HOLDINGS;

  public static final String INSERT_OR_UPDATE_HOLDINGS;
  public static final String GET_BY_PK_HOLDINGS;
  public static final String DELETE_BY_PK_HOLDINGS;
  public static final String DELETE_OLD_RECORDS_BY_CREDENTIALS_ID;


  private HoldingsTableConstants() { }

  static {
    String allColumns = String.join(", ", new String[] {
      ID_COLUMN, CREDENTIALS_ID_COLUMN, VENDOR_ID_COLUMN, PACKAGE_ID_COLUMN, TITLE_ID_COLUMN,
      RESOURCE_TYPE_COLUMN, PUBLISHER_NAME_COLUMN, PUBLICATION_TITLE_COLUMN, UPDATED_AT_COLUMN
    });

    PK_HOLDINGS = String.join(", ", new String[] {CREDENTIALS_ID_COLUMN, ID_COLUMN});
    DELETE_BY_PK_HOLDINGS = "DELETE FROM %s WHERE (" + PK_HOLDINGS + ") IN (%s);";
    GET_BY_PK_HOLDINGS =  "SELECT * FROM %s WHERE (" + PK_HOLDINGS + ") IN (%s);";

    DELETE_OLD_RECORDS_BY_CREDENTIALS_ID = "DELETE FROM %s WHERE " + CREDENTIALS_ID_COLUMN + "=? AND "
      + UPDATED_AT_COLUMN + " < timestamp with time zone '%s';";

    INSERT_OR_UPDATE_HOLDINGS = "INSERT INTO %s (" + allColumns + ") VALUES %s " +
      "ON CONFLICT (" + ID_COLUMN + ", " + CREDENTIALS_ID_COLUMN + ") " +
      "DO UPDATE SET " + TITLE_ID_COLUMN + " = EXCLUDED." + TITLE_ID_COLUMN + ", "
      + VENDOR_ID_COLUMN + "= EXCLUDED." + VENDOR_ID_COLUMN + ", "
      + PACKAGE_ID_COLUMN + "= EXCLUDED." + PACKAGE_ID_COLUMN + ", "
      + RESOURCE_TYPE_COLUMN + "= EXCLUDED." + RESOURCE_TYPE_COLUMN + ", "
      + PUBLISHER_NAME_COLUMN + "= EXCLUDED." + PUBLISHER_NAME_COLUMN + ", "
      + PUBLICATION_TITLE_COLUMN + "= EXCLUDED." + PUBLICATION_TITLE_COLUMN + ", "
      + UPDATED_AT_COLUMN + "= EXCLUDED." + UPDATED_AT_COLUMN + ";";
  }
}
