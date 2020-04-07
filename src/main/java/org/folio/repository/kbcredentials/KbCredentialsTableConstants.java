package org.folio.repository.kbcredentials;

import static org.folio.repository.SqlQueryHelper.insertQuery;
import static org.folio.repository.SqlQueryHelper.limitQuery;
import static org.folio.repository.SqlQueryHelper.selectQuery;
import static org.folio.repository.SqlQueryHelper.updateOnConflictedIdQuery;
import static org.folio.repository.SqlQueryHelper.whereQuery;

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
  public static final String SELECT_CREDENTIALS_BY_ID_QUERY;
  public static final String INSERT_CREDENTIALS_QUERY;
  public static final String UPSERT_CREDENTIALS_QUERY;


  static {
    String[] allColumns = new String[] {
      ID_COLUMN, URL_COLUMN, NAME_COLUMN, API_KEY_COLUMN, CUSTOMER_ID_COLUMN, CREATED_DATE_COLUMN, UPDATED_DATE_COLUMN,
      CREATED_BY_USER_ID_COLUMN, UPDATED_BY_USER_ID_COLUMN, CREATED_BY_USER_NAME_COLUMN, UPDATED_BY_USER_NAME_COLUMN
    };

    String[] insertColumns = new String[] {
      ID_COLUMN, URL_COLUMN, NAME_COLUMN, API_KEY_COLUMN, CUSTOMER_ID_COLUMN, CREATED_DATE_COLUMN,
      CREATED_BY_USER_ID_COLUMN, CREATED_BY_USER_NAME_COLUMN
    };

    SELECT_CREDENTIALS_QUERY = selectQuery() + ";";
    SELECT_CREDENTIALS_BY_ID_QUERY = selectQuery() + " " + whereQuery(ID_COLUMN) + " " + limitQuery(1) + ";";
    INSERT_CREDENTIALS_QUERY = insertQuery(insertColumns) + ";";
    UPSERT_CREDENTIALS_QUERY = insertQuery(allColumns) + " " + updateOnConflictedIdQuery(ID_COLUMN, allColumns) + ";";
  }

  private KbCredentialsTableConstants() {

  }

}
