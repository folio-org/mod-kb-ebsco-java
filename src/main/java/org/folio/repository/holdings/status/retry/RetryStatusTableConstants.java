package org.folio.repository.holdings.status.retry;

import static org.folio.repository.SqlQueryHelper.joinWithComma;

public final class RetryStatusTableConstants {

  private RetryStatusTableConstants() { }

  public static final String RETRY_STATUS_TABLE = "retry_status";

  public static final String ID_COLUMN = "id";
  public static final String CREDENTIALS_ID_COLUMN = "credentials_id";
  public static final String ATTEMPTS_LEFT_COLUMN = "attempts_left";
  public static final String TIMER_ID_COLUMN = "timer_id";
  public static final String RETRY_STATUS_FIELD_LIST = joinWithComma(ID_COLUMN, CREDENTIALS_ID_COLUMN, ATTEMPTS_LEFT_COLUMN, TIMER_ID_COLUMN);

  public static final String GET_RETRY_STATUS_BY_CREDENTIALS = "SELECT " + RETRY_STATUS_FIELD_LIST + " from %s WHERE " + CREDENTIALS_ID_COLUMN + "=?;";
  public static final String UPDATE_RETRY_STATUS = "UPDATE %s SET " + CREDENTIALS_ID_COLUMN + "=?, " + ATTEMPTS_LEFT_COLUMN + "=?, " + TIMER_ID_COLUMN + "=?;";
  public static final String INSERT_RETRY_STATUS = "INSERT INTO %s (" + RETRY_STATUS_FIELD_LIST + ") VALUES (%s);";
  public static final String DELETE_RETRY_STATUS = "DELETE FROM %s WHERE " + CREDENTIALS_ID_COLUMN + "=?;";
}
