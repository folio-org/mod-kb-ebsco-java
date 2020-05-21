package org.folio.util;

import static org.folio.repository.holdings.status.retry.RetryStatusTableConstants.RETRY_STATUS_FIELD_LIST;
import static org.folio.repository.holdings.status.retry.RetryStatusTableConstants.RETRY_STATUS_TABLE;
import static org.folio.test.util.TestUtil.STUB_TENANT;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;

import org.folio.repository.holdings.status.retry.RetryStatus;
import org.folio.rest.persist.PostgresClient;

public class HoldingsRetryStatusTestUtil {

  public static RetryStatus insertRetryStatus(String credentialsId, Vertx vertx) {
    CompletableFuture<RetryStatus> future = new CompletableFuture<>();
    PostgresClient.getInstance(vertx)
      .execute("INSERT INTO " + holdingsStatusAuditTestTable() + " (" + RETRY_STATUS_FIELD_LIST + ") VALUES (?,?,?,?)",
        new JsonArray((Arrays.asList(UUID.randomUUID().toString(), credentialsId, 2, null))),
        event -> future.complete(null));
    return future.join();
  };

  private static String holdingsStatusAuditTestTable() {
    return PostgresClient.convertToPsqlStandard(STUB_TENANT) + "." + RETRY_STATUS_TABLE;
  }
}
