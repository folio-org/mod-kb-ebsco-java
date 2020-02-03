package org.folio.util;

import static org.folio.repository.accessTypes.AccessTypesTableConstants.ACCESS_TYPES_TABLE_NAME;
import static org.folio.repository.accessTypes.AccessTypesTableConstants.JSONB_COLUMN;
import static org.folio.test.util.TestUtil.STUB_TENANT;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.Vertx;

import org.folio.rest.jaxrs.model.AccessTypeCollectionItem;
import org.folio.rest.persist.PostgresClient;

public class AccessTypesTestUtil {


  public static List<AccessTypeCollectionItem> getAccessTypes(Vertx vertx) {
    ObjectMapper mapper = new ObjectMapper();
    CompletableFuture<List<AccessTypeCollectionItem>> future = new CompletableFuture<>();
    PostgresClient.getInstance(vertx).select("SELECT * FROM " + accessTypesTestTable(),
      event -> future.complete(event.result().getRows().stream()
        .map(row -> row.getString(JSONB_COLUMN))
        .map(json -> parseAccessType(mapper, json))
        .collect(Collectors.toList())));
    return future.join();
  }

  public static void clearAccessTypes(Vertx vertx) {
    CompletableFuture<Void> future = new CompletableFuture<>();
    PostgresClient.getInstance(vertx).execute(
      "DELETE FROM " + accessTypesTestTable(),
      event -> future.complete(null));
    future.join();
  }

  private static AccessTypeCollectionItem parseAccessType(ObjectMapper mapper, String json) {
    try {
      return mapper.readValue(json, AccessTypeCollectionItem.class);
    } catch (IOException e) {
      e.printStackTrace();
      throw new IllegalArgumentException("Can't parse note type", e);
    }
  }

  private static String accessTypesTestTable() {
    return PostgresClient.convertToPsqlStandard(STUB_TENANT) + "." + ACCESS_TYPES_TABLE_NAME;
  }

}
