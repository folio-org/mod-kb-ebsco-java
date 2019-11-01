package org.folio.util;

import static org.folio.repository.holdings.HoldingsTableConstants.JSONB_COLUMN;
import static org.folio.repository.holdings.status.HoldingsLoadingStatusFactory.getStatusNotStarted;
import static org.folio.repository.holdings.status.HoldingsStatusTableConstants.HOLDINGS_STATUS_TABLE;
import static org.folio.repository.holdings.status.HoldingsStatusTableConstants.ID_COLUMN;
import static org.folio.repository.holdings.status.HoldingsStatusTableConstants.PROCESS_ID_COLUMN;
import static org.folio.test.util.TestUtil.STUB_TENANT;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;

import org.folio.rest.jaxrs.model.HoldingsLoadingStatus;
import org.folio.rest.persist.PostgresClient;

public class HoldingsStatusUtil {

  public static String PROCESS_ID = "926223cc-bd21-4fe2-af75-b8e82cfecad3";
  public static HoldingsLoadingStatus insertStatusNotStarted(Vertx vertx) {
    return insertStatus(vertx, getStatusNotStarted(), PROCESS_ID );
  }

  public static HoldingsLoadingStatus insertStatus(Vertx vertx, HoldingsLoadingStatus status, String processId) {
    CompletableFuture<HoldingsLoadingStatus> future = new CompletableFuture<>();
    PostgresClient.getInstance(vertx)
      .execute("INSERT INTO " + holdingsStatusTestTable() + " (" + ID_COLUMN + ", " + JSONB_COLUMN + ", " + PROCESS_ID_COLUMN + " ) VALUES (?,?,?)",
        new JsonArray(Arrays.asList(UUID.randomUUID().toString(), Json.encode(status), processId)),
         event -> future.complete(null));
    return future.join();
  }
  private static String holdingsStatusTestTable() {
    return PostgresClient.convertToPsqlStandard(STUB_TENANT) + "." + HOLDINGS_STATUS_TABLE;
  }
}
