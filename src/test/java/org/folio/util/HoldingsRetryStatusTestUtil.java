package org.folio.util;

import static org.folio.db.RowSetUtils.mapFirstItem;
import static org.folio.db.RowSetUtils.toUUID;
import static org.folio.repository.DbUtil.prepareQuery;
import static org.folio.repository.SqlQueryHelper.insertQuery;
import static org.folio.repository.holdings.status.retry.RetryStatusTableConstants.ATTEMPTS_LEFT_COLUMN;
import static org.folio.repository.holdings.status.retry.RetryStatusTableConstants.CREDENTIALS_ID_COLUMN;
import static org.folio.repository.holdings.status.retry.RetryStatusTableConstants.getRetryStatusByCredentials;
import static org.folio.repository.holdings.status.retry.RetryStatusTableConstants.ID_COLUMN;
import static org.folio.repository.holdings.status.retry.RetryStatusTableConstants.RETRY_STATUS_TABLE;
import static org.folio.repository.holdings.status.retry.RetryStatusTableConstants.TIMER_ID_COLUMN;
import static org.folio.test.util.TestUtil.STUB_TENANT;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import io.vertx.core.Vertx;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;

import org.folio.repository.holdings.status.retry.RetryStatus;
import org.folio.rest.persist.PostgresClient;

public class HoldingsRetryStatusTestUtil {

  public static RetryStatus insertRetryStatus(String credentialsId, Vertx vertx) {
    CompletableFuture<RetryStatus> future = new CompletableFuture<>();
    String query = prepareQuery(
      insertQuery(ID_COLUMN, CREDENTIALS_ID_COLUMN, ATTEMPTS_LEFT_COLUMN, TIMER_ID_COLUMN),
      holdingsStatusAuditTestTable()
    );
    Tuple params = Tuple.of(UUID.randomUUID(), toUUID(credentialsId), 2, null);
    PostgresClient.getInstance(vertx, STUB_TENANT).execute(query, params, event -> future.complete(null));
    return future.join();
  }

  public static RetryStatus getRetryStatus(String credentialsId, Vertx vertx) {
    CompletableFuture<RetryStatus> future = new CompletableFuture<>();
    String query = prepareQuery(getRetryStatusByCredentials(), holdingsStatusAuditTestTable());
    Tuple params = Tuple.of(toUUID(credentialsId));
    PostgresClient.getInstance(vertx, STUB_TENANT)
      .select(query, params,
        event -> future.complete(mapFirstItem(event.result(), HoldingsRetryStatusTestUtil::parseRetryStatus))
      );
    return future.join();
  }

  private static RetryStatus parseRetryStatus(Row row) {
    return new RetryStatus(row.getInteger(ATTEMPTS_LEFT_COLUMN), row.getLong(TIMER_ID_COLUMN));
  }

  private static String holdingsStatusAuditTestTable() {
    return PostgresClient.convertToPsqlStandard(STUB_TENANT) + "." + RETRY_STATUS_TABLE;
  }
}
