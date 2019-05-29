package org.folio.util;

import static org.folio.repository.holdings.HoldingsRepositoryImpl.HOLDINGS_TABLE;
import static org.folio.util.TestUtil.STUB_TENANT;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.Vertx;

import org.folio.holdingsiq.model.Holding;
import org.folio.rest.persist.PostgresClient;

public class HoldingsTestUtil {

  private static final String JSONB_COLUMN = "jsonb";

  public HoldingsTestUtil() {}

  public static List<Holding> getHoldings(Vertx vertx) {
    ObjectMapper mapper = new ObjectMapper();
    CompletableFuture<List<Holding>> future = new CompletableFuture<>();
    PostgresClient.getInstance(vertx)
      .select("SELECT *  FROM " + holdingsTestTable(),
        event -> future.complete(event.result().getRows().stream()
          .map(row -> row.getString(JSONB_COLUMN))
          .map(json -> parseHolding(mapper, json))
          .collect(Collectors.toList()))
      );
    return future.join();
  }

  private static String holdingsTestTable() {
    return PostgresClient.convertToPsqlStandard(STUB_TENANT) + "." + HOLDINGS_TABLE;
  }

  private static Holding parseHolding(ObjectMapper mapper, String json) {
    try {
      return mapper.readValue(json, Holding.class);
    } catch (IOException e) {
      e.printStackTrace();
      throw new IllegalArgumentException("Can't parse holding", e);
    }
  }
}
