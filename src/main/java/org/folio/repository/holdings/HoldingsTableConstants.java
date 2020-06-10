package org.folio.repository.holdings;

import static org.folio.repository.SqlQueryHelper.joinWithComma;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

public final class HoldingsTableConstants {
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
  private static final String PK_HOLDINGS;

  public static final String INSERT_OR_UPDATE_HOLDINGS;
  public static final String GET_BY_PK_HOLDINGS;
  public static final String DELETE_BY_PK_HOLDINGS;
  public static final String DELETE_OLD_RECORDS_BY_CREDENTIALS_ID;
  private static final String EXCLUDED_DELIMITER = "= EXCLUDED.";


  private HoldingsTableConstants() { }

  static {
    PK_HOLDINGS = joinWithComma(CREDENTIALS_ID_COLUMN, ID_COLUMN);

    CharSequence[] excludedColumns = new CharSequence[] {
      VENDOR_ID_COLUMN, PACKAGE_ID_COLUMN, TITLE_ID_COLUMN,
      RESOURCE_TYPE_COLUMN, PUBLISHER_NAME_COLUMN, PUBLICATION_TITLE_COLUMN, UPDATED_AT_COLUMN
    };

    String allColumns = joinWithComma(PK_HOLDINGS, VENDOR_ID_COLUMN, PACKAGE_ID_COLUMN, TITLE_ID_COLUMN,
      RESOURCE_TYPE_COLUMN, PUBLISHER_NAME_COLUMN, PUBLICATION_TITLE_COLUMN, UPDATED_AT_COLUMN);

    DELETE_BY_PK_HOLDINGS = "DELETE FROM %s WHERE (" + PK_HOLDINGS + ") IN (%s);";
    GET_BY_PK_HOLDINGS =  "SELECT * FROM %s WHERE (" + PK_HOLDINGS + ") IN (%s);";

    DELETE_OLD_RECORDS_BY_CREDENTIALS_ID = "DELETE FROM %s WHERE " + CREDENTIALS_ID_COLUMN + "=? AND "
      + UPDATED_AT_COLUMN + " < ?;";

    INSERT_OR_UPDATE_HOLDINGS = "INSERT INTO %s (" + allColumns + ") VALUES %s " +
      "ON CONFLICT (" + PK_HOLDINGS + ") " +
      "DO UPDATE SET " + getExcludedCause(excludedColumns) + ";";
  }

  @NotNull
  private static String getExcludedCause(CharSequence ... excludedColumns) {
    return Arrays.stream(excludedColumns)
      .map(column -> column + EXCLUDED_DELIMITER + column)
      .collect(Collectors.joining(","));
  }
}
