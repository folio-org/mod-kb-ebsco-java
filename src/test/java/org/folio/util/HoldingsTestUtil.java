package org.folio.util;

import static org.folio.common.ListUtils.createPlaceholders;
import static org.folio.db.RowSetUtils.mapItems;
import static org.folio.db.RowSetUtils.toUUID;
import static org.folio.repository.DbUtil.prepareQuery;
import static org.folio.repository.holdings.HoldingsTableConstants.HOLDINGS_TABLE;
import static org.folio.repository.holdings.HoldingsTableConstants.INSERT_OR_UPDATE_HOLDINGS;
import static org.folio.repository.holdings.HoldingsTableConstants.PACKAGE_ID_COLUMN;
import static org.folio.repository.holdings.HoldingsTableConstants.PUBLICATION_TITLE_COLUMN;
import static org.folio.repository.holdings.HoldingsTableConstants.PUBLISHER_NAME_COLUMN;
import static org.folio.repository.holdings.HoldingsTableConstants.RESOURCE_TYPE_COLUMN;
import static org.folio.repository.holdings.HoldingsTableConstants.TITLE_ID_COLUMN;
import static org.folio.repository.holdings.HoldingsTableConstants.VENDOR_ID_COLUMN;
import static org.folio.test.util.TestUtil.STUB_TENANT;
import static org.folio.test.util.TestUtil.readJsonFile;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import io.vertx.core.Vertx;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;

import org.folio.repository.SqlQueryHelper;
import org.folio.repository.holdings.DbHoldingInfo;
import org.folio.rest.persist.PostgresClient;

public class HoldingsTestUtil {

  public static List<DbHoldingInfo> getHoldings(Vertx vertx) {
    CompletableFuture<List<DbHoldingInfo>> future = new CompletableFuture<>();
    String query = prepareQuery(SqlQueryHelper.selectQuery(), holdingsTestTable());
    PostgresClient.getInstance(vertx, STUB_TENANT)
      .select(query, event -> future.complete(mapItems(event.result(), HoldingsTestUtil::mapHoldingInfo)));
    return future.join();
  }

  public static void saveHolding(String credentialsId, DbHoldingInfo holding, OffsetDateTime updatedAt, Vertx vertx) {
    CompletableFuture<Void> future = new CompletableFuture<>();
    String query = prepareQuery(INSERT_OR_UPDATE_HOLDINGS, holdingsTestTable(), createPlaceholders(9, 1));
    Tuple params = getHoldingsInsertParams(credentialsId, holding, updatedAt);
    PostgresClient.getInstance(vertx, STUB_TENANT).execute(query, params, event -> future.complete(null));
    future.join();
  }

  private static Tuple getHoldingsInsertParams(String credentialsId, DbHoldingInfo holding, OffsetDateTime updatedAt) {
    return Tuple.of(
      toUUID(credentialsId),
      getHoldingsId(holding),
      holding.getVendorId(),
      holding.getPackageId(),
      holding.getTitleId(),
      holding.getResourceType(),
      holding.getPublisherName(),
      holding.getPublicationTitle(),
      updatedAt
    );
  }

  public static DbHoldingInfo getStubHolding() throws IOException, URISyntaxException {
    return readJsonFile("responses/kb-ebsco/holdings/custom-holding.json", DbHoldingInfo.class);
  }

  private static String holdingsTestTable() {
    return PostgresClient.convertToPsqlStandard(STUB_TENANT) + "." + HOLDINGS_TABLE;
  }

  private static DbHoldingInfo mapHoldingInfo(Row row) {
    return DbHoldingInfo.builder()
      .titleId(row.getInteger(TITLE_ID_COLUMN))
      .packageId(row.getInteger(PACKAGE_ID_COLUMN))
      .vendorId(row.getInteger(VENDOR_ID_COLUMN))
      .publicationTitle(row.getString(PUBLICATION_TITLE_COLUMN))
      .publisherName(row.getString(PUBLISHER_NAME_COLUMN))
      .resourceType(row.getString(RESOURCE_TYPE_COLUMN))
      .build();
  }

  private static String getHoldingsId(DbHoldingInfo holding) {
    return holding.getVendorId() + "-" + holding.getPackageId() + "-" + holding.getTitleId();
  }
}
