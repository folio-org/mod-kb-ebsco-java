package org.folio.util;

import static org.folio.repository.holdings.status.retry.RetryStatusTableConstants.GET_RETRY_STATUS_BY_CREDENTIALS;
import static org.folio.repository.holdings.status.retry.RetryStatusTableConstants.RETRIES_LEFT_COLUMN;
import static org.folio.repository.holdings.status.retry.RetryStatusTableConstants.RETRY_STATUS_FIELD_LIST;
import static org.folio.repository.holdings.status.retry.RetryStatusTableConstants.RETRY_STATUS_TABLE;
import static org.folio.repository.holdings.status.retry.RetryStatusTableConstants.TIMER_ID_COLUMN;
import static org.folio.test.util.TestUtil.STUB_TENANT;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

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
  }

  public static RetryStatus getRetryStatus(String credentialsId, Vertx vertx) {
    CompletableFuture<RetryStatus> future = new CompletableFuture<>();
    String sql = String.format(GET_RETRY_STATUS_BY_CREDENTIALS, holdingsStatusAuditTestTable());
    PostgresClient.getInstance(vertx)
      .select(sql, new JsonArray().add(credentialsId),
        event -> future.complete(event.result().getRows().stream()
          .map(json -> parseRetryStatus(json))
          .collect(Collectors.toList()).get(0)));
    return future.join();
  }

  private static RetryStatus parseRetryStatus(JsonObject row) {
    return new RetryStatus(
      row.getInteger(RETRIES_LEFT_COLUMN),
      row.getLong(TIMER_ID_COLUMN));
  }

  private static String holdingsStatusAuditTestTable() {
    return PostgresClient.convertToPsqlStandard(STUB_TENANT) + "." + RETRY_STATUS_TABLE;
  }
}
