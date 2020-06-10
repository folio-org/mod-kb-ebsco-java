package org.folio.util;

import static org.folio.db.RowSetUtils.mapItems;
import static org.folio.db.RowSetUtils.toJsonObject;
import static org.folio.db.RowSetUtils.toUUID;
import static org.folio.repository.DbUtil.prepareQuery;
import static org.folio.repository.SqlQueryHelper.insertQuery;
import static org.folio.repository.SqlQueryHelper.orderByQuery;
import static org.folio.repository.SqlQueryHelper.selectQuery;
import static org.folio.repository.holdings.status.audit.HoldingsStatusAuditTableConstants.CREDENTIALS_COLUMN;
import static org.folio.repository.holdings.status.audit.HoldingsStatusAuditTableConstants.HOLDINGS_STATUS_AUDIT_TABLE;
import static org.folio.repository.holdings.status.audit.HoldingsStatusAuditTableConstants.JSONB_COLUMN;
import static org.folio.repository.holdings.status.audit.HoldingsStatusAuditTableConstants.OPERATION_COLUMN;
import static org.folio.repository.holdings.status.audit.HoldingsStatusAuditTableConstants.UPDATED_AT_COLUMN;
import static org.folio.test.util.TestUtil.STUB_TENANT;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import io.vertx.core.Vertx;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;

import org.folio.rest.jaxrs.model.HoldingsLoadingStatus;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.tools.utils.ObjectMapperTool;

public class HoldingsStatusAuditTestUtil {

  public static List<HoldingsLoadingStatus> getRecords(Vertx vertx) {
    CompletableFuture<List<HoldingsLoadingStatus>> future = new CompletableFuture<>();
    String query = prepareQuery(
      selectQuery() + " " + orderByQuery(UPDATED_AT_COLUMN),
      holdingsStatusAuditTestTable()
    );
    PostgresClient.getInstance(vertx)
      .select(query,
        event -> future.complete(mapItems(event.result(), HoldingsStatusAuditTestUtil::mapHoldingsLoadingStatus))
      );
    return future.join();
  }

  public static HoldingsLoadingStatus saveStatusAudit(String credentialsId, HoldingsLoadingStatus status,
                                                      OffsetDateTime updatedAt, Vertx vertx) {
    CompletableFuture<HoldingsLoadingStatus> future = new CompletableFuture<>();
    String query = prepareQuery(insertQuery(CREDENTIALS_COLUMN, JSONB_COLUMN, OPERATION_COLUMN, UPDATED_AT_COLUMN));
    Tuple params = Tuple.of(toUUID(credentialsId), toJsonObject(status), "UPDATE", updatedAt);
    PostgresClient.getInstance(vertx).execute(query, params, event -> future.complete(null));
    return future.join();
  }

  private static HoldingsLoadingStatus mapHoldingsLoadingStatus(Row row) {
    try {
      return ObjectMapperTool.getMapper().readValue(row.getValue(JSONB_COLUMN).toString(), HoldingsLoadingStatus.class);
    } catch (IOException e) {
      e.printStackTrace();
      throw new IllegalArgumentException("Can't parse holdings status", e);
    }
  }

  private static String holdingsStatusAuditTestTable() {
    return PostgresClient.convertToPsqlStandard(STUB_TENANT) + "." + HOLDINGS_STATUS_AUDIT_TABLE;
  }

}
