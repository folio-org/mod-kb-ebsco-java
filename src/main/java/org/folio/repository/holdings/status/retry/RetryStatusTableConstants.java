package org.folio.repository.holdings.status.retry;

import static org.folio.repository.DbUtil.getRetryStatusTableName;
import static org.folio.repository.DbUtil.prepareQuery;
import static org.folio.repository.SqlQueryHelper.joinWithComma;

public final class RetryStatusTableConstants {

  private RetryStatusTableConstants() {
  }

  public static final String RETRY_STATUS_TABLE = "retry_status";
  public static final String ID_COLUMN = "id";
  public static final String CREDENTIALS_ID_COLUMN = "credentials_id";
  public static final String ATTEMPTS_LEFT_COLUMN = "attempts_left";
  public static final String TIMER_ID_COLUMN = "timer_id";
  public static final String RETRY_STATUS_FIELD_LIST = joinWithComma(ID_COLUMN, CREDENTIALS_ID_COLUMN, ATTEMPTS_LEFT_COLUMN, TIMER_ID_COLUMN);

  public static String getRetryStatusByCredentials() {
    return "SELECT " + RETRY_STATUS_FIELD_LIST + " from %s WHERE " + CREDENTIALS_ID_COLUMN + "=?;";
  }

  public static String getRetryStatusByCredentials(String tenantId) {
    return prepareQuery(getRetryStatusByCredentials(), getRetryStatusTableName(tenantId));
  }

  public static String updateRetryStatus(String tenantId) {
    return prepareQuery(updateRetryStatus(), getRetryStatusTableName(tenantId));
  }

  public static String insertRetryStatus(String tenantId) {
    return prepareQuery(insertRetryStatus(), getRetryStatusTableName(tenantId));
  }

  public static String deleteRetryStatus(String tenantId) {
    return prepareQuery(deleteRetryStatus(), getRetryStatusTableName(tenantId));
  }

  private static String updateRetryStatus() {
    return "UPDATE %s SET " + ATTEMPTS_LEFT_COLUMN + "=?, " + TIMER_ID_COLUMN + "=? WHERE " + CREDENTIALS_ID_COLUMN + "=?;";
  }

  private static String insertRetryStatus() {
    return "INSERT INTO %s (" + RETRY_STATUS_FIELD_LIST + ") VALUES (?, ?, ?, ?);";
  }

  private static String deleteRetryStatus() {
    return "DELETE FROM %s WHERE " + CREDENTIALS_ID_COLUMN + "=?;";
  }
}
