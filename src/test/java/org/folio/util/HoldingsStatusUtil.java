package org.folio.util;

import static org.folio.common.ListUtils.createPlaceholders;
import static org.folio.repository.holdings.status.HoldingsLoadingStatusFactory.getStatusNotStarted;
import static org.folio.repository.holdings.status.HoldingsStatusTableConstants.GET_HOLDINGS_STATUS_BY_ID;
import static org.folio.repository.holdings.status.HoldingsStatusTableConstants.HOLDINGS_STATUS_TABLE;
import static org.folio.repository.holdings.status.HoldingsStatusTableConstants.INSERT_LOADING_STATUS;
import static org.folio.test.util.TestUtil.STUB_TENANT;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;

import org.folio.db.RowSetUtils;
import org.folio.rest.jaxrs.model.HoldingsLoadingStatus;
import org.folio.rest.persist.PostgresClient;

public class HoldingsStatusUtil {

  public static String PROCESS_ID = "926223cc-bd21-4fe2-af75-b8e82cfecad3";

  public static HoldingsLoadingStatus insertStatusNotStarted(String credentialsId, Vertx vertx) {
    return insertStatus(credentialsId, getStatusNotStarted(), PROCESS_ID, vertx);
  }

  public static HoldingsLoadingStatus insertStatus(String credentialsId, HoldingsLoadingStatus status, String processId,
                                                   Vertx vertx) {
    CompletableFuture<HoldingsLoadingStatus> future = new CompletableFuture<>();
    PostgresClient.getInstance(vertx)
      .execute(String.format(INSERT_LOADING_STATUS, holdingsStatusTestTable(), createPlaceholders(4)),
        Tuple.of(UUID.randomUUID().toString(), credentialsId, Json.encode(status), processId),
        event -> future.complete(null));
    return future.join();
  }

  public static HoldingsLoadingStatus getStatus(String credentialsId, Vertx vertx) {
    ObjectMapper mapper = new ObjectMapper();
    CompletableFuture<HoldingsLoadingStatus> future = new CompletableFuture<>();
    String sql = String.format(GET_HOLDINGS_STATUS_BY_ID, holdingsStatusTestTable());
    PostgresClient.getInstance(vertx)
      .select(sql, Tuple.of(credentialsId),
        event -> future.complete(RowSetUtils.mapFirstItem(event.result(), row -> parseStatus(mapper, row))));
    return future.join();
  }

  private static HoldingsLoadingStatus parseStatus(ObjectMapper mapper, Row row) {
    try {
      return mapper.readValue(row.getString("jsonb"), HoldingsLoadingStatus.class);
    } catch (IOException e) {
      throw new IllegalArgumentException("Can't parse status", e);
    }
  }

  private static String holdingsStatusTestTable() {
    return PostgresClient.convertToPsqlStandard(STUB_TENANT) + "." + HOLDINGS_STATUS_TABLE;
  }
}
