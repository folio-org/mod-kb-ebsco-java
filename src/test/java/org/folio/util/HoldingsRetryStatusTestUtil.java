package org.folio.util;

import static org.folio.repository.holdings.status.retry.RetryStatusTableConstants.GET_RETRY_STATUS_BY_CREDENTIALS;
import static org.folio.repository.holdings.status.retry.RetryStatusTableConstants.RETRIES_LEFT_COLUMN;
import static org.folio.repository.holdings.status.retry.RetryStatusTableConstants.RETRY_STATUS_FIELD_LIST;
import static org.folio.repository.holdings.status.retry.RetryStatusTableConstants.RETRY_STATUS_TABLE;
import static org.folio.repository.holdings.status.retry.RetryStatusTableConstants.TIMER_ID_COLUMN;
import static org.folio.test.util.TestUtil.STUB_TENANT;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import io.vertx.core.Vertx;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;

import org.folio.db.RowSetUtils;
import org.folio.repository.holdings.status.retry.RetryStatus;
import org.folio.rest.persist.PostgresClient;

public class HoldingsRetryStatusTestUtil {

  public static RetryStatus insertRetryStatus(String credentialsId, Vertx vertx) {
    CompletableFuture<RetryStatus> future = new CompletableFuture<>();
    PostgresClient.getInstance(vertx)
      .execute("INSERT INTO " + holdingsStatusAuditTestTable() + " (" + RETRY_STATUS_FIELD_LIST + ") VALUES (?,?,?,?)",
        Tuple.of(UUID.randomUUID().toString(), credentialsId, 2, null),
        event -> future.complete(null));
    return future.join();
  }

  public static RetryStatus getRetryStatus(String credentialsId, Vertx vertx) {
    CompletableFuture<RetryStatus> future = new CompletableFuture<>();
    String sql = String.format(GET_RETRY_STATUS_BY_CREDENTIALS, holdingsStatusAuditTestTable());
    PostgresClient.getInstance(vertx)
      .select(sql, Tuple.of(credentialsId),
        event -> future.complete(RowSetUtils.mapFirstItem(event.result(), HoldingsRetryStatusTestUtil::parseRetryStatus)));
    return future.join();
  }

  private static RetryStatus parseRetryStatus(Row row) {
    return new RetryStatus(
      row.getInteger(RETRIES_LEFT_COLUMN),
      row.getLong(TIMER_ID_COLUMN));
  }

  private static String holdingsStatusAuditTestTable() {
    return PostgresClient.convertToPsqlStandard(STUB_TENANT) + "." + RETRY_STATUS_TABLE;
  }
}
