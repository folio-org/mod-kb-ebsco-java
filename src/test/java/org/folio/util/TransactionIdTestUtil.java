package org.folio.util;

import static org.folio.repository.holdings.status.TransactionIdTableConstants.TRANSACTION_ID_TABLE;
import static org.folio.test.util.TestUtil.STUB_TENANT;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;

import org.folio.rest.persist.PostgresClient;

public class TransactionIdTestUtil {
  public static void addTransactionId(Vertx vertx, String id) {
    CompletableFuture<Void> future = new CompletableFuture<>();
    PostgresClient.getInstance(vertx).execute(
      "INSERT INTO " + transactionIdsTestTable() +
        "(transaction_id) VALUES(?)",
      new JsonArray(Collections.singletonList(id)),
      event -> future.complete(null));
    future.join();
  }

  private static String transactionIdsTestTable() {
    return PostgresClient.convertToPsqlStandard(STUB_TENANT) + "." + TRANSACTION_ID_TABLE;
  }
}
