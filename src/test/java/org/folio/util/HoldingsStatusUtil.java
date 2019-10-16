package org.folio.util;

import static org.folio.repository.holdings.HoldingsTableConstants.JSONB_COLUMN;
import static org.folio.repository.holdings.status.HoldingsLoadingStatusFactory.getStatusNotStarted;
import static org.folio.repository.holdings.status.HoldingsStatusTableConstants.HOLDINGS_STATUS_TABLE;
import static org.folio.repository.holdings.status.HoldingsStatusTableConstants.ID_COLUMN;
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

  public static HoldingsLoadingStatus insertStatusNotStarted(Vertx vertx) {
    return insertStatus(vertx, getStatusNotStarted());
  }

  public static HoldingsLoadingStatus insertStatus(Vertx vertx, HoldingsLoadingStatus status) {
    CompletableFuture<HoldingsLoadingStatus> future = new CompletableFuture<>();
    PostgresClient.getInstance(vertx)
      .execute("INSERT INTO " + holdingsStatusTestTable() + " (" + ID_COLUMN + ", " + JSONB_COLUMN + " ) VALUES (?,?)",
        new JsonArray(Arrays.asList(UUID.randomUUID().toString(), Json.encode(status))),
         event -> future.complete(null));
    return future.join();
  }
  private static String holdingsStatusTestTable() {
    return PostgresClient.convertToPsqlStandard(STUB_TENANT) + "." + HOLDINGS_STATUS_TABLE;
  }
}
