package org.folio.util;

import static org.folio.common.ListUtils.createInsertPlaceholders;
import static org.folio.repository.holdings.HoldingsTableConstants.HOLDINGS_TABLE;
import static org.folio.repository.holdings.HoldingsTableConstants.INSERT_OR_UPDATE_HOLDINGS;
import static org.folio.test.util.TestUtil.STUB_TENANT;
import static org.folio.test.util.TestUtil.readFile;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;

import org.folio.db.RowSetUtils;
import org.folio.repository.holdings.DbHoldingInfo;
import org.folio.rest.persist.PostgresClient;

public class HoldingsTestUtil {

  public HoldingsTestUtil() {
  }

  public static List<DbHoldingInfo> getHoldings(Vertx vertx) {
    ObjectMapper mapper = new ObjectMapper();
    CompletableFuture<List<DbHoldingInfo>> future = new CompletableFuture<>();
    PostgresClient.getInstance(vertx)
      .select("SELECT * FROM " + holdingsTestTable(),
        event -> future.complete(RowSetUtils.mapItems(event.result(), row -> parseHolding(mapper, row)))
      );
    return future.join();
  }

  public static void addHolding(String credentialsId, DbHoldingInfo holding, Instant updatedAt, Vertx vertx) {
    CompletableFuture<Void> future = new CompletableFuture<>();
    final String sql = String.format(INSERT_OR_UPDATE_HOLDINGS, holdingsTestTable(), createInsertPlaceholders(9, 1));
    final Tuple params = getHoldingsInsertParams(credentialsId, holding, updatedAt);
    PostgresClient.getInstance(vertx).execute(sql, params, event -> future.complete(null));
    future.join();
  }

  private static Tuple getHoldingsInsertParams(String credentialsId, DbHoldingInfo holding, Instant updatedAt) {
    return Tuple.of(
      getHoldingsId(holding),
      credentialsId,
      holding.getVendorId(),
      holding.getPackageId(),
      holding.getTitleId(),
      holding.getResourceType(),
      holding.getPublisherName(),
      holding.getPublicationTitle(),
      updatedAt
    );
  }

  private static String holdingsTestTable() {
    return PostgresClient.convertToPsqlStandard(STUB_TENANT) + "." + HOLDINGS_TABLE;
  }

  private static DbHoldingInfo parseHolding(ObjectMapper mapper, Row json) {
    try {
      return mapper.readValue(json.toString(), DbHoldingInfo.class);
    } catch (IOException e) {
      e.printStackTrace();
      throw new IllegalArgumentException("Can't parse holding", e);
    }
  }

  private static String getHoldingsId(DbHoldingInfo holding) {
    return holding.getVendorId() + "-" + holding.getPackageId() + "-" + holding.getTitleId();
  }

  public static DbHoldingInfo getHolding() throws IOException, URISyntaxException {
    return Json.decodeValue(readFile("responses/kb-ebsco/holdings/custom-holding.json"), DbHoldingInfo.class);
  }
}
