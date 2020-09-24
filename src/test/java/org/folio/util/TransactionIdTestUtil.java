package org.folio.util;

import static org.folio.repository.DbUtil.prepareQuery;
import static org.folio.repository.SqlQueryHelper.insertQuery;
import static org.folio.repository.holdings.transaction.TransactionIdTableConstants.CREDENTIALS_ID_COLUMN;
import static org.folio.repository.holdings.transaction.TransactionIdTableConstants.TRANSACTION_ID_COLUMN;
import static org.folio.repository.holdings.transaction.TransactionIdTableConstants.TRANSACTION_ID_TABLE;
import static org.folio.test.util.TestUtil.STUB_TENANT;

import java.util.concurrent.CompletableFuture;

import io.vertx.core.Vertx;
import io.vertx.sqlclient.Tuple;

import org.folio.db.RowSetUtils;
import org.folio.rest.persist.PostgresClient;

public class TransactionIdTestUtil {

  public static void addTransactionId(String credentialsId, String transactionId, Vertx vertx) {
    CompletableFuture<Void> future = new CompletableFuture<>();
    String query = prepareQuery(insertQuery(CREDENTIALS_ID_COLUMN, TRANSACTION_ID_COLUMN), transactionIdsTestTable());
    Tuple params = Tuple.of(RowSetUtils.toUUID(credentialsId), transactionId);
    PostgresClient.getInstance(vertx, STUB_TENANT).execute(query, params,
      event -> future.complete(null)
    );
    future.join();
  }

  private static String transactionIdsTestTable() {
    return PostgresClient.convertToPsqlStandard(STUB_TENANT) + "." + TRANSACTION_ID_TABLE;
  }
}
