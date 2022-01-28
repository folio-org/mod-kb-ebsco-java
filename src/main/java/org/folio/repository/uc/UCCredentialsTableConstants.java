package org.folio.repository.uc;

import static org.folio.repository.DbUtil.getUCCredentialsTableName;
import static org.folio.repository.DbUtil.prepareQuery;
import static org.folio.repository.SqlQueryHelper.deleteQuery;
import static org.folio.repository.SqlQueryHelper.insertQuery;
import static org.folio.repository.SqlQueryHelper.limitQuery;
import static org.folio.repository.SqlQueryHelper.selectQuery;

public final class UCCredentialsTableConstants {

  public static final String UC_CREDENTIALS_TABLE_NAME = "usage_consolidation_credentials";

  public static final String CLIENT_ID_COLUMN = "client_id";
  public static final String CLIENT_SECRET_COLUMN = "client_secret";

  private UCCredentialsTableConstants() {
  }

  public static String selectUcCredentials(String tenantId) {
    return prepareQuery(selectUcCredentials(), getUCCredentialsTableName(tenantId));
  }

  public static String saveUcCredentials(String tenantId) {
    return prepareQuery(saveUcCredentials(), getUCCredentialsTableName(tenantId));
  }

  public static String deleteUcCredentials(String tenantId) {
    return prepareQuery(deleteUcCredentials(), getUCCredentialsTableName(tenantId));
  }

  private static String selectUcCredentials() {
    return selectQuery() + " " + limitQuery(1) + ";";
  }

  private static String saveUcCredentials() {
    return insertQuery(CLIENT_ID_COLUMN, CLIENT_SECRET_COLUMN) + ";";
  }

  private static String deleteUcCredentials() {
    return deleteQuery() + ";";
  }

}
