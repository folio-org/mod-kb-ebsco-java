package org.folio.util;

import static java.util.Arrays.asList;

import static org.folio.repository.holdings.transaction.TransactionIdTableConstants.TRANSACTIONS_FIELD_LIST;
import static org.folio.repository.holdings.transaction.TransactionIdTableConstants.TRANSACTION_ID_TABLE;
import static org.folio.test.util.TestUtil.STUB_TENANT;

import java.util.concurrent.CompletableFuture;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;

import org.folio.rest.persist.PostgresClient;

public class TransactionIdTestUtil {

  public static void addTransactionId(String credentialsId, String transactionId, Vertx vertx) {
    CompletableFuture<Void> future = new CompletableFuture<>();
    PostgresClient.getInstance(vertx).execute(
      "INSERT INTO " + transactionIdsTestTable() +
        "(" + TRANSACTIONS_FIELD_LIST + ") VALUES(?,?)",
      new JsonArray(asList(credentialsId, transactionId)),
      event -> future.complete(null));
    future.join();
  }

  private static String transactionIdsTestTable() {
    return PostgresClient.convertToPsqlStandard(STUB_TENANT) + "." + TRANSACTION_ID_TABLE;
  }
}
