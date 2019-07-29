package org.folio.repository.holdings.status;

public class RetryStatusTableConstants {

  private RetryStatusTableConstants() { }

  public static final String RETRY_STATUS_TABLE = "retry_status";
  public static final String RETRIES_LEFT_COLUMN = "attempts_left";
  public static final String ID_COLUMN = "id";
  public static final String TIMER_ID_COLUMN = "timer_id";
  public static final String RETRY_STATUS_FIELD_LIST = String.format("%s, %s, %s", ID_COLUMN, RETRIES_LEFT_COLUMN, TIMER_ID_COLUMN);
  public static final String GET_RETRY_STATUS = "SELECT " + RETRY_STATUS_FIELD_LIST + " from %s;";
  public static final String UPDATE_RETRY_STATUS = "UPDATE %s SET " + RETRIES_LEFT_COLUMN + " = ?, " + TIMER_ID_COLUMN + "= ?;";
  public static final String INSERT_RETRY_STATUS = "INSERT INTO %s (" + RETRY_STATUS_FIELD_LIST + ") VALUES (%s);";
  public static final String DELETE_RETRY_STATUS = "DELETE FROM %s;";
}
