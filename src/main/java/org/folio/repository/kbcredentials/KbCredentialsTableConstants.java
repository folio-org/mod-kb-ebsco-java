package org.folio.repository.kbcredentials;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.folio.common.ListUtils;

public final class KbCredentialsTableConstants {

  public static final String KB_CREDENTIALS_TABLE_NAME = "kb_credentials";

  public static final String ID_COLUMN = "id";
  public static final String URL_COLUMN = "url";
  public static final String NAME_COLUMN = "name";
  public static final String API_KEY_COLUMN = "api_key";
  public static final String CUSTOMER_ID_COLUMN = "customer_id";
  public static final String CREATED_DATE_COLUMN = "created_date";
  public static final String UPDATED_DATE_COLUMN = "updated_date";
  public static final String CREATED_BY_USER_ID_COLUMN = "created_by_user_id";
  public static final String UPDATED_BY_USER_ID_COLUMN = "updated_by_user_id";
  public static final String CREATED_BY_USER_NAME_COLUMN = "created_by_user_name";
  public static final String UPDATED_BY_USER_NAME_COLUMN = "updated_by_user_name";

  public static final String SELECT_CREDENTIALS_QUERY;
  public static final String UPSERT_CREDENTIALS_QUERY;


  static {
    String[] columns = new String[] {
      ID_COLUMN, URL_COLUMN, NAME_COLUMN, API_KEY_COLUMN, CUSTOMER_ID_COLUMN, CREATED_DATE_COLUMN, UPDATED_DATE_COLUMN,
      CREATED_BY_USER_ID_COLUMN, UPDATED_BY_USER_ID_COLUMN, CREATED_BY_USER_NAME_COLUMN, UPDATED_BY_USER_NAME_COLUMN
    };

    SELECT_CREDENTIALS_QUERY = selectQuery() + ";";
    UPSERT_CREDENTIALS_QUERY = insertQuery(columns) + " " + updateOnConflictedIdQuery(columns) + ";";
  }

  private KbCredentialsTableConstants() {

  }

  private static String selectQuery(String... columns) {
    if (columns.length == 0) {
      return "SELECT * FROM %s";
    } else {
      return "SELECT " + String.join(", ", columns) + " FROM %s";
    }
  }

  private static String insertQuery(String[] columns) {
    return "INSERT INTO %s (" + String.join(", ", columns) + ") VALUES "
      + ListUtils.createInsertPlaceholders(columns.length, 1);
  }

  private static String updateOnConflictedIdQuery(String[] columns) {
    String updateColumns = Arrays.stream(columns)
      .filter(columnName -> !columnName.equals(ID_COLUMN))
      .map(KbCredentialsTableConstants::assignExcludedColumn)
      .collect(Collectors.joining(", "));
    return "ON CONFLICT(" + ID_COLUMN + ") DO UPDATE SET " + updateColumns;
  }

  private static String assignExcludedColumn(String column) {
    return column + "= EXCLUDED." + column;
  }

}
