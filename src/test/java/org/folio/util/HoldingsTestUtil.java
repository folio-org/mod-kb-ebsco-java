package org.folio.util;

import static org.folio.common.ListUtils.createInsertPlaceholders;
import static org.folio.repository.holdings.HoldingsTableConstants.HOLDINGS_TABLE;
import static org.folio.repository.holdings.HoldingsTableConstants.INSERT_OR_UPDATE_HOLDINGS;
import static org.folio.test.util.TestUtil.STUB_TENANT;
import static org.folio.test.util.TestUtil.readFile;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import org.folio.repository.holdings.HoldingInfoInDB;
import org.folio.rest.persist.PostgresClient;

public class HoldingsTestUtil {

  public HoldingsTestUtil() {
  }

  public static List<HoldingInfoInDB> getHoldings(Vertx vertx) {
    ObjectMapper mapper = new ObjectMapper();
    CompletableFuture<List<HoldingInfoInDB>> future = new CompletableFuture<>();
    PostgresClient.getInstance(vertx)
      .select("SELECT * FROM " + holdingsTestTable(),
        event -> future.complete(event.result().getRows().stream()
          .map(row -> parseHolding(mapper, row))
          .collect(Collectors.toList()))
      );
    return future.join();
  }

  public static void addHolding(String credentialsId, HoldingInfoInDB holding, Instant updatedAt, Vertx vertx) {
    CompletableFuture<Void> future = new CompletableFuture<>();
    final String sql = String.format(INSERT_OR_UPDATE_HOLDINGS, holdingsTestTable(), createInsertPlaceholders(9, 1));
    final JsonArray params = getHoldingsInsertParams(credentialsId, holding, updatedAt);
    PostgresClient.getInstance(vertx).execute(sql, params, event -> future.complete(null));
    future.join();
  }

  private static JsonArray getHoldingsInsertParams(String credentialsId, HoldingInfoInDB holding, Instant updatedAt) {
    return new JsonArray(Arrays.asList(
      getHoldingsId(holding),
      credentialsId,
      holding.getVendorId(),
      holding.getPackageId(),
      holding.getTitleId(),
      holding.getResourceType(),
      holding.getPublisherName(),
      holding.getPublicationTitle(),
      updatedAt));
  }

  private static String holdingsTestTable() {
    return PostgresClient.convertToPsqlStandard(STUB_TENANT) + "." + HOLDINGS_TABLE;
  }

  private static HoldingInfoInDB parseHolding(ObjectMapper mapper, JsonObject json) {
    try {
      return mapper.readValue(json.toString(), HoldingInfoInDB.class);
    } catch (IOException e) {
      e.printStackTrace();
      throw new IllegalArgumentException("Can't parse holding", e);
    }
  }

  private static String getHoldingsId(HoldingInfoInDB holding) {
    return holding.getVendorId() + "-" + holding.getPackageId() + "-" + holding.getTitleId();
  }

  public static HoldingInfoInDB getHolding() throws IOException, URISyntaxException {
    return Json.decodeValue(readFile("responses/kb-ebsco/holdings/custom-holding.json"), HoldingInfoInDB.class);
  }
}
