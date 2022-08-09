package org.folio.repository.holdings.transaction;

import static org.folio.common.ListUtils.createPlaceholders;
import static org.folio.repository.DbUtil.getTransactionIdTableName;
import static org.folio.repository.DbUtil.prepareQuery;
import static org.folio.repository.SqlQueryHelper.joinWithComma;

import io.vertx.sqlclient.Tuple;

public final class TransactionIdTableConstants {

  public static final String TRANSACTION_ID_TABLE = "transaction_ids";
  public static final String CREDENTIALS_ID_COLUMN = "credentials_id";
  public static final String TRANSACTION_ID_COLUMN = "transaction_id";
  public static final String CREATED_TIME_COLUMN = "created_at";
  public static final String TRANSACTIONS_FIELD_LIST = joinWithComma(CREDENTIALS_ID_COLUMN, TRANSACTION_ID_COLUMN);
  public static final String TRANSACTIONS_FIELD_LIST_FULL =
    joinWithComma(CREDENTIALS_ID_COLUMN, TRANSACTION_ID_COLUMN, CREATED_TIME_COLUMN);

  private TransactionIdTableConstants() {
  }

  public static String getLastTransactionIdByCredentials(String tenantId) {
    return prepareQuery(getLastTransactionIdByCredentialsQuery(), getTransactionIdTableName(tenantId));
  }

  public static String insertTransactionId(String tenantId, Tuple params) {
    return prepareQuery(insertTransactionIdQuery(),
      getTransactionIdTableName(tenantId),
      createPlaceholders(params.size())
    );
  }

  private static String getLastTransactionIdByCredentialsQuery() {
    return "SELECT " + TRANSACTIONS_FIELD_LIST_FULL + " FROM %s "
      + "WHERE " + CREDENTIALS_ID_COLUMN + "=? ORDER BY " + CREATED_TIME_COLUMN + " DESC LIMIT 1;";
  }

  private static String insertTransactionIdQuery() {
    return "INSERT INTO %s (" + TRANSACTIONS_FIELD_LIST + ") VALUES (%s);";
  }

}
