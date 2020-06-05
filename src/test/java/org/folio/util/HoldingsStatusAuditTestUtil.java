package org.folio.util;

import static org.folio.repository.holdings.status.HoldingsStatusTableConstants.CREDENTIALS_COLUMN;
import static org.folio.repository.holdings.status.audit.HoldingsStatusAuditTableConstants.HOLDINGS_STATUS_AUDIT_TABLE;
import static org.folio.repository.holdings.status.audit.HoldingsStatusAuditTableConstants.JSONB_COLUMN;
import static org.folio.repository.holdings.status.audit.HoldingsStatusAuditTableConstants.OPERATION_COLUMN;
import static org.folio.repository.holdings.status.audit.HoldingsStatusAuditTableConstants.UPDATED_AT_COLUMN;
import static org.folio.test.util.TestUtil.STUB_TENANT;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Tuple;

import org.folio.db.RowSetUtils;
import org.folio.rest.jaxrs.model.HoldingsLoadingStatus;
import org.folio.rest.persist.PostgresClient;

public class HoldingsStatusAuditTestUtil {

  public static List<HoldingsLoadingStatus> getRecords(Vertx vertx) {
    ObjectMapper mapper = new ObjectMapper();
    CompletableFuture<List<HoldingsLoadingStatus>> future = new CompletableFuture<>();
    PostgresClient.getInstance(vertx)
      .select("SELECT * FROM " + holdingsStatusAuditTestTable() + " ORDER BY " + UPDATED_AT_COLUMN,
        event -> future
          .complete(RowSetUtils.mapItems(event.result(), row -> parseStatus(mapper, row.getString(JSONB_COLUMN))))
      );
    return future.join();
  }

  public static HoldingsLoadingStatus insertStatus(String credentialsId, HoldingsLoadingStatus status, Instant updatedAt,
                                                   Vertx vertx) {
    CompletableFuture<HoldingsLoadingStatus> future = new CompletableFuture<>();
    PostgresClient.getInstance(vertx)
      .execute("INSERT INTO " + holdingsStatusAuditTestTable() + " (" + CREDENTIALS_COLUMN + ", " + JSONB_COLUMN + ", "
          + OPERATION_COLUMN + ", " + UPDATED_AT_COLUMN + ") VALUES (?,?,?,?)",
        Tuple.of(credentialsId, JsonObject.mapFrom(status), "UPDATE", updatedAt),
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
