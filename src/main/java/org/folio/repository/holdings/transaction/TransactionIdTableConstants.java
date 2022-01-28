package org.folio.repository.holdings.transaction;

import io.vertx.sqlclient.Tuple;

import static org.folio.common.ListUtils.createPlaceholders;
import static org.folio.repository.DbUtil.getTransactionIdTableName;
import static org.folio.repository.DbUtil.prepareQuery;
import static org.folio.repository.SqlQueryHelper.joinWithComma;

public final class TransactionIdTableConstants {

  private TransactionIdTableConstants() {
  }

  public static final String TRANSACTION_ID_TABLE = "transaction_ids";
  public static final String CREDENTIALS_ID_COLUMN = "credentials_id";
  public static final String TRANSACTION_ID_COLUMN = "transaction_id";
  public static final String CREATED_TIME_COLUMN = "created_at";
  public static final String TRANSACTIONS_FIELD_LIST = joinWithComma(CREDENTIALS_ID_COLUMN, TRANSACTION_ID_COLUMN);
  public static final String TRANSACTIONS_FIELD_LIST_FULL = joinWithComma(CREDENTIALS_ID_COLUMN, TRANSACTION_ID_COLUMN, CREATED_TIME_COLUMN);

  public static String getLastTransactionIdByCredentials(String tenantId) {
    return prepareQuery(getLastTransactionIdByCredentials(), getTransactionIdTableName(tenantId));
  }

  public static String insertTransactionId(String tenantId, Tuple params) {
    return prepareQuery(insertTransactionId(),
      getTransactionIdTableName(tenantId),
      createPlaceholders(params.size())
    );
  }

  private static String getLastTransactionIdByCredentials() {
    return "SELECT " + TRANSACTIONS_FIELD_LIST_FULL + " FROM %s " +
      "WHERE " + CREDENTIALS_ID_COLUMN + "=? ORDER BY " + CREATED_TIME_COLUMN + " DESC LIMIT 1;";
  }

  private static String insertTransactionId() {
    return "INSERT INTO %s (" + TRANSACTIONS_FIELD_LIST + ") VALUES (%s);";
  }

}
