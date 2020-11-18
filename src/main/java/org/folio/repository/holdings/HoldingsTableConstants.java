package org.folio.repository.holdings;

import static org.folio.repository.SqlQueryHelper.deleteQuery;
import static org.folio.repository.SqlQueryHelper.equalCondition;
import static org.folio.repository.SqlQueryHelper.joinWithComma;
import static org.folio.repository.SqlQueryHelper.lessThanCondition;
import static org.folio.repository.SqlQueryHelper.selectQuery;
import static org.folio.repository.SqlQueryHelper.updateOnConflictedIdQuery;
import static org.folio.repository.SqlQueryHelper.whereConditionsQuery;
import static org.folio.repository.SqlQueryHelper.whereQuery;

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

  public static final String INSERT_OR_UPDATE_HOLDINGS;
  public static final String GET_BY_PK_HOLDINGS;
  public static final String GET_BY_PACKAGE_ID_AND_CREDENTIALS;
  public static final String DELETE_BY_PK_HOLDINGS;
  public static final String DELETE_OLD_RECORDS_BY_CREDENTIALS_ID;

  private static final String PK_HOLDINGS;

  static {
    PK_HOLDINGS = joinWithComma(CREDENTIALS_ID_COLUMN, ID_COLUMN);

    String[] excludedColumns = new String[] {
      VENDOR_ID_COLUMN, PACKAGE_ID_COLUMN, TITLE_ID_COLUMN,
      RESOURCE_TYPE_COLUMN, PUBLISHER_NAME_COLUMN, PUBLICATION_TITLE_COLUMN, UPDATED_AT_COLUMN
    };

    String allColumns = joinWithComma(PK_HOLDINGS, VENDOR_ID_COLUMN, PACKAGE_ID_COLUMN, TITLE_ID_COLUMN,
      RESOURCE_TYPE_COLUMN, PUBLISHER_NAME_COLUMN, PUBLICATION_TITLE_COLUMN, UPDATED_AT_COLUMN);

    DELETE_BY_PK_HOLDINGS = "DELETE FROM %s WHERE (" + PK_HOLDINGS + ") IN (%s);";
    GET_BY_PK_HOLDINGS = "SELECT * FROM %s WHERE (" + PK_HOLDINGS + ") IN (%s);";
    GET_BY_PACKAGE_ID_AND_CREDENTIALS = selectQuery() + " " + whereQuery(PACKAGE_ID_COLUMN, CREDENTIALS_ID_COLUMN) + ";";

    DELETE_OLD_RECORDS_BY_CREDENTIALS_ID = deleteQuery() + " " +
      whereConditionsQuery(equalCondition(CREDENTIALS_ID_COLUMN), lessThanCondition(UPDATED_AT_COLUMN)) + ";";

    INSERT_OR_UPDATE_HOLDINGS = "INSERT INTO %s (" + allColumns + ") VALUES %s " +
      updateOnConflictedIdQuery(PK_HOLDINGS, excludedColumns);
  }

  private HoldingsTableConstants() { }

}
