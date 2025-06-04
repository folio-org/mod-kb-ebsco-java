package org.folio.repository.holdings;

import static org.folio.common.ListUtils.createPlaceholders;
import static org.folio.repository.DbUtil.getHoldingsTableName;
import static org.folio.repository.DbUtil.prepareQuery;
import static org.folio.repository.SqlQueryHelper.deleteQuery;
import static org.folio.repository.SqlQueryHelper.equalCondition;
import static org.folio.repository.SqlQueryHelper.joinWithComma;
import static org.folio.repository.SqlQueryHelper.lessThanCondition;
import static org.folio.repository.SqlQueryHelper.selectQuery;
import static org.folio.repository.SqlQueryHelper.updateOnConflictedIdQuery;
import static org.folio.repository.SqlQueryHelper.whereConditionsQuery;
import static org.folio.repository.SqlQueryHelper.whereQuery;

import java.util.List;

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

  private static final String PK_HOLDINGS = joinWithComma(CREDENTIALS_ID_COLUMN, ID_COLUMN);
  private static final String ALL_COLUMNS = joinWithComma(PK_HOLDINGS, VENDOR_ID_COLUMN,
    PACKAGE_ID_COLUMN, TITLE_ID_COLUMN, RESOURCE_TYPE_COLUMN, PUBLISHER_NAME_COLUMN, PUBLICATION_TITLE_COLUMN,
    UPDATED_AT_COLUMN);

  private static final String[] EXCLUDE_COLUMNS = new String[] {
    VENDOR_ID_COLUMN, PACKAGE_ID_COLUMN, TITLE_ID_COLUMN,
    RESOURCE_TYPE_COLUMN, PUBLISHER_NAME_COLUMN, PUBLICATION_TITLE_COLUMN, UPDATED_AT_COLUMN};

  private HoldingsTableConstants() {
  }

  public static String deleteByPkHoldings(String tenantId, List<HoldingsId> holdings) {
    return prepareQuery(deleteByPkHoldingsQuery(),
      getHoldingsTableName(tenantId),
      createPlaceholders(2, holdings.size())
    );
  }

  public static String selectByPkHoldings(String tenantId, List<String> resourceIds) {
    return prepareQuery(selectByPkHoldingsQuery(),
      getHoldingsTableName(tenantId),
      createPlaceholders(2, resourceIds.size())
    );
  }

  public static String selectByPackageIdAndCredentials(String tenantId) {
    return prepareQuery(selectByPackageIdAndCredentialsQuery(), getHoldingsTableName(tenantId));
  }

  public static String deleteOldRecordsByCredentialsId(String tenantId) {
    return prepareQuery(deleteOldRecordsByCredentialsIdQuery(), getHoldingsTableName(tenantId));
  }

  public static String insertOrUpdateHoldings(String tenantId, List<DbHoldingInfo> holdings) {
    return prepareQuery(insertOrUpdateHoldings(),
      getHoldingsTableName(tenantId),
      createPlaceholders(9, holdings.size())
    );
  }

  public static String insertOrUpdateHoldings() {
    return "INSERT INTO %s (" + ALL_COLUMNS + ") VALUES %s "
      + updateOnConflictedIdQuery(PK_HOLDINGS, EXCLUDE_COLUMNS);
  }

  private static String deleteByPkHoldingsQuery() {
    return "DELETE FROM %s WHERE (" + PK_HOLDINGS + ") IN (%s);";
  }

  private static String selectByPkHoldingsQuery() {
    return "SELECT * FROM %s WHERE (" + PK_HOLDINGS + ") IN (%s);";
  }

  private static String selectByPackageIdAndCredentialsQuery() {
    return selectQuery() + " " + whereQuery(PACKAGE_ID_COLUMN, CREDENTIALS_ID_COLUMN) + ";";
  }

  private static String deleteOldRecordsByCredentialsIdQuery() {
    return deleteQuery() + " "
      + whereConditionsQuery(equalCondition(CREDENTIALS_ID_COLUMN), lessThanCondition(UPDATED_AT_COLUMN)) + ";";
  }
}
