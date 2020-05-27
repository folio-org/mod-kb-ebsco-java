package org.folio.repository.holdings.transaction;

public class TransactionIdTableConstants {

  private TransactionIdTableConstants() { }

  public static final String TRANSACTION_ID_TABLE = "transaction_ids";
  public static final String CREDENTIALS_ID_COLUMN = "credentials_id";
  public static final String TRANSACTION_ID_COLUMN = "transaction_id";
  public static final String CREATED_TIME_COLUMN = "created_at";
  public static final String TRANSACTIONS_FIELD_LIST = String.format("%s, %s", CREDENTIALS_ID_COLUMN, TRANSACTION_ID_COLUMN);
  public static final String TRANSACTIONS_FIELD_LIST_FULL = String.format("%s, %s, %s", CREDENTIALS_ID_COLUMN, TRANSACTION_ID_COLUMN, CREATED_TIME_COLUMN);
  public static final String GET_LAST_TRANSACTION_ID_BY_CREDENTIALS = "SELECT " + TRANSACTIONS_FIELD_LIST_FULL + " FROM %s " +
    "WHERE "+ CREDENTIALS_ID_COLUMN + "=? ORDER BY " + CREATED_TIME_COLUMN + " DESC LIMIT 1;";
  public static final String INSERT_TRANSACTION_ID = "INSERT INTO %s (" + TRANSACTIONS_FIELD_LIST + ") VALUES (%s);";
}
