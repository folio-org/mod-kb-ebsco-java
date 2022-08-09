package org.folio.repository.uc;

import static org.folio.repository.DbUtil.getUcCredentialsTableName;
import static org.folio.repository.DbUtil.prepareQuery;
import static org.folio.repository.SqlQueryHelper.deleteQuery;
import static org.folio.repository.SqlQueryHelper.insertQuery;
import static org.folio.repository.SqlQueryHelper.limitQuery;
import static org.folio.repository.SqlQueryHelper.selectQuery;

public final class UcCredentialsTableConstants {

  public static final String UC_CREDENTIALS_TABLE_NAME = "usage_consolidation_credentials";

  public static final String CLIENT_ID_COLUMN = "client_id";
  public static final String CLIENT_SECRET_COLUMN = "client_secret";

  private UcCredentialsTableConstants() {
  }

  public static String selectUcCredentials(String tenantId) {
    return prepareQuery(selectUcCredentialsQuery(), getUcCredentialsTableName(tenantId));
  }

  public static String saveUcCredentials(String tenantId) {
    return prepareQuery(saveUcCredentialsQuery(), getUcCredentialsTableName(tenantId));
  }

  public static String deleteUcCredentials(String tenantId) {
    return prepareQuery(deleteUcCredentialsQuery(), getUcCredentialsTableName(tenantId));
  }

  private static String selectUcCredentialsQuery() {
    return selectQuery() + " " + limitQuery(1) + ";";
  }

  private static String saveUcCredentialsQuery() {
    return insertQuery(CLIENT_ID_COLUMN, CLIENT_SECRET_COLUMN) + ";";
  }

  private static String deleteUcCredentialsQuery() {
    return deleteQuery() + ";";
  }

}
