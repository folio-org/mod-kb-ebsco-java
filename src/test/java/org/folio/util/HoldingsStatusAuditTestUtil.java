package org.folio.util;

import static org.folio.repository.holdings.status.HoldingsStatusAuditTableConstants.HOLDINGS_STATUS_AUDIT_TABLE;
import static org.folio.repository.holdings.status.HoldingsStatusAuditTableConstants.JSONB_COLUMN;
import static org.folio.repository.holdings.status.HoldingsStatusAuditTableConstants.OPERATION_COLUMN;
import static org.folio.repository.holdings.status.HoldingsStatusAuditTableConstants.UPDATED_AT_COLUMN;
import static org.folio.test.util.TestUtil.STUB_TENANT;

import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;

import org.folio.rest.jaxrs.model.HoldingsLoadingStatus;
import org.folio.rest.persist.PostgresClient;

public class HoldingsStatusAuditTestUtil {

  public static List<HoldingsLoadingStatus> getRecords(Vertx vertx) {
    ObjectMapper mapper = new ObjectMapper();
    CompletableFuture<List<HoldingsLoadingStatus>> future = new CompletableFuture<>();
    PostgresClient.getInstance(vertx)
      .select("SELECT * FROM " + holdingsStatusAuditTestTable() + " ORDER BY " + UPDATED_AT_COLUMN,
        event -> future.complete(event.result().getRows().stream()
          .map(row -> row.getString(JSONB_COLUMN))
          .map(json -> parseStatus(mapper, json))
          .collect(Collectors.toList()))
      );

    return future.join();
  }

  public static HoldingsLoadingStatus insertStatus(Vertx vertx, HoldingsLoadingStatus status, Instant updatedAt) {
    CompletableFuture<HoldingsLoadingStatus> future = new CompletableFuture<>();
    PostgresClient.getInstance(vertx)
      .execute("INSERT INTO " + holdingsStatusAuditTestTable() + " (" + JSONB_COLUMN + ", " + OPERATION_COLUMN + ", "+ UPDATED_AT_COLUMN +") VALUES (?,?,?)",
        new JsonArray(Arrays.asList(Json.encode(status),"UPDATE", updatedAt)),
        event -> future.complete(null));
    return future.join();
  }
  private static String holdingsStatusAuditTestTable() {
    return PostgresClient.convertToPsqlStandard(STUB_TENANT) + "." + HOLDINGS_STATUS_AUDIT_TABLE;
  }

  private static HoldingsLoadingStatus parseStatus(ObjectMapper mapper, String json) {
    try {
      return mapper.readValue(json, HoldingsLoadingStatus.class);
    } catch (IOException e) {
      e.printStackTrace();
      throw new IllegalArgumentException("Can't parse holdings status", e);
    }
  }
}
