package org.folio.repository.holdings.status;

public class TransactionIdTableConstants {
  private TransactionIdTableConstants() { }

  public static final String TRANSACTION_ID_TABLE = "transaction_ids";
  public static final String TRANSACTION_ID_COLUMN = "transaction_id";
  public static final String GET_LAST_TRANSACTION_ID = "SELECT created_at, transaction_id FROM %s ORDER BY created_at DESC LIMIT 1;";
  public static final String INSERT_TRANSACTION_ID = "INSERT INTO %s (transaction_id) VALUES (%s);";
}
