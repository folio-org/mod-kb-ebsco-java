package org.folio.util;

import static org.folio.repository.holdings.HoldingsTableConstants.HOLDINGS_TABLE;
import static org.folio.repository.holdings.HoldingsTableConstants.ID_COLUMN;
import static org.folio.repository.holdings.HoldingsTableConstants.JSONB_COLUMN;
import static org.folio.repository.holdings.HoldingsTableConstants.UPDATED_AT_COLUMN;
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
          .map(row -> row.getString(JSONB_COLUMN))
          .map(json -> parseHolding(mapper, json))
          .collect(Collectors.toList()))
      );
    return future.join();
  }

  public static void addHolding(Vertx vertx, HoldingInfoInDB holding, Instant updatedAt) {
    CompletableFuture<Void> future = new CompletableFuture<>();
    PostgresClient.getInstance(vertx).execute(
      "INSERT INTO " + holdingsTestTable() +
        "(" + ID_COLUMN + ", " + JSONB_COLUMN + ", " + UPDATED_AT_COLUMN + ") VALUES(?,?,?)",
      new JsonArray(Arrays.asList(getHoldingsId(holding), Json.encode(holding), updatedAt)),
      event -> future.complete(null));
    future.join();
  }

  private static String holdingsTestTable() {
    return PostgresClient.convertToPsqlStandard(STUB_TENANT) + "." + HOLDINGS_TABLE;
  }

  private static HoldingInfoInDB parseHolding(ObjectMapper mapper, String json) {
    try {
      return mapper.readValue(json, HoldingInfoInDB.class);
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
