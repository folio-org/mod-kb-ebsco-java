package org.folio.util;

import static java.util.UUID.randomUUID;

import static org.folio.common.ListUtils.createPlaceholders;
import static org.folio.db.RowSetUtils.mapFirstItem;
import static org.folio.db.RowSetUtils.toJsonObject;
import static org.folio.db.RowSetUtils.toUUID;
import static org.folio.repository.holdings.status.HoldingsLoadingStatusFactory.getStatusNotStarted;
import static org.folio.repository.holdings.status.HoldingsStatusTableConstants.getHoldingsStatusById;
import static org.folio.repository.holdings.status.HoldingsStatusTableConstants.HOLDINGS_STATUS_TABLE;
import static org.folio.repository.holdings.status.HoldingsStatusTableConstants.insertLoadingStatus;
import static org.folio.repository.holdings.status.HoldingsStatusTableConstants.JSONB_COLUMN;
import static org.folio.test.util.TestUtil.STUB_TENANT;

import java.time.OffsetDateTime;
import java.util.concurrent.CompletableFuture;

import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;

import org.folio.repository.DbUtil;
import org.folio.rest.jaxrs.model.HoldingsLoadingStatus;
import org.folio.rest.persist.PostgresClient;

public class HoldingsStatusUtil {

  public static final String PROCESS_ID = "926223cc-bd21-4fe2-af75-b8e82cfecad3";

  public static HoldingsLoadingStatus saveStatusNotStarted(String credentialsId, Vertx vertx) {
    return saveStatus(credentialsId, getStatusNotStarted(), PROCESS_ID, vertx);
  }

  public static HoldingsLoadingStatus saveStatus(String credentialsId, HoldingsLoadingStatus status, String processId,
                                                 OffsetDateTime startedTime, Vertx vertx) {
    CompletableFuture<HoldingsLoadingStatus> future = new CompletableFuture<>();
    String query = DbUtil.prepareQuery(insertLoadingStatus(), holdingsStatusTestTable(), createPlaceholders(4));
    Tuple params = Tuple.of(randomUUID(), toUUID(credentialsId), toJsonObject(status), toUUID(processId));
    PostgresClient.getInstance(vertx, STUB_TENANT).execute(query, params, event -> future.complete(null));
    return future.join();
  }

  public static HoldingsLoadingStatus saveStatus(String credentialsId, HoldingsLoadingStatus status, String processId,
                                                 Vertx vertx) {
    return saveStatus(credentialsId, status, processId, OffsetDateTime.now(), vertx);
  }

  public static HoldingsLoadingStatus getStatus(String credentialsId, Vertx vertx) {
    CompletableFuture<HoldingsLoadingStatus> future = new CompletableFuture<>();
    String query = DbUtil.prepareQuery(getHoldingsStatusById(), holdingsStatusTestTable());
    Tuple params = Tuple.of(toUUID(credentialsId));
    PostgresClient.getInstance(vertx, STUB_TENANT)
      .select(query, params, event -> future.complete(mapStatus(event.result())));
    return future.join();
  }

  private static HoldingsLoadingStatus mapStatus(RowSet<Row> rowSet) {
    return mapFirstItem(rowSet, row -> Json.decodeValue(row.getValue(JSONB_COLUMN).toString(), HoldingsLoadingStatus.class));
  }

  private static String holdingsStatusTestTable() {
    return PostgresClient.convertToPsqlStandard(STUB_TENANT) + "." + HOLDINGS_STATUS_TABLE;
  }
}
