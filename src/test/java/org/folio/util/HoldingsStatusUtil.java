package org.folio.util;

import static org.folio.common.ListUtils.createPlaceholders;
import static org.folio.repository.holdings.status.HoldingsLoadingStatusFactory.getStatusNotStarted;
import static org.folio.repository.holdings.status.HoldingsStatusTableConstants.HOLDINGS_STATUS_TABLE;
import static org.folio.repository.holdings.status.HoldingsStatusTableConstants.INSERT_LOADING_STATUS;
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
  public static HoldingsLoadingStatus insertStatusNotStarted(String credentialsId, Vertx vertx) {
    return insertStatus(credentialsId, getStatusNotStarted(), PROCESS_ID, vertx);
  }

  public static HoldingsLoadingStatus insertStatus(String credentialsId, HoldingsLoadingStatus status, String processId, Vertx vertx) {
    CompletableFuture<HoldingsLoadingStatus> future = new CompletableFuture<>();
    PostgresClient.getInstance(vertx)
      .execute(String.format(INSERT_LOADING_STATUS, holdingsStatusTestTable(), createPlaceholders(4)),
        new JsonArray(Arrays.asList(UUID.randomUUID().toString(), credentialsId, Json.encode(status), processId)),
         event -> future.complete(null));
    return future.join();
  }
  private static String holdingsStatusTestTable() {
    return PostgresClient.convertToPsqlStandard(STUB_TENANT) + "." + HOLDINGS_STATUS_TABLE;
  }
}
