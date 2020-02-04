package org.folio.util;

import static org.apache.commons.lang3.StringUtils.join;

import static org.folio.repository.accesstypes.AccessTypesTableConstants.ACCESS_TYPES_TABLE_NAME;
import static org.folio.repository.accesstypes.AccessTypesTableConstants.ID_COLUMN;
import static org.folio.repository.accesstypes.AccessTypesTableConstants.JSONB_COLUMN;
import static org.folio.test.util.TestUtil.STUB_TENANT;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;

import org.jetbrains.annotations.NotNull;

import org.folio.rest.jaxrs.model.AccessTypeCollectionItem;
import org.folio.rest.jaxrs.model.AccessTypeDataAttributes;
import org.folio.rest.jaxrs.model.UserDisplayInfo;
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

  public static List<AccessTypeCollectionItem> insertAccessTypes(List<AccessTypeCollectionItem> items, Vertx vertx) {
    CompletableFuture<ResultSet> future = new CompletableFuture<>();

    String insertStatement = "INSERT INTO " + accessTypesTestTable() +
      "(" + ID_COLUMN + "," + JSONB_COLUMN + ") VALUES " + join(Collections.nCopies(items.size(), "(?,?)"), ",") +
      " RETURNING " + ID_COLUMN;
    JsonArray params = createParams(items);

    PostgresClient.getInstance(vertx).select(insertStatement, params, ar -> {
      if (ar.succeeded()) {
        future.complete(ar.result());
      } else {
        future.completeExceptionally(ar.cause());
      }
    });
    return populateAccessTypesIds(items, future.join().getRows());
  }

  private static List<AccessTypeCollectionItem> populateAccessTypesIds(List<AccessTypeCollectionItem> items,
                                                                       List<JsonObject> rows) {
    List<AccessTypeCollectionItem> result = new ArrayList<>(items.size());

    for (int i = 0; i < items.size(); i++) {
      result.add(items.get(i)
        .withId(rows.get(i).getString(ID_COLUMN)));
    }

    return result;
  }

  private static JsonArray createParams(List<AccessTypeCollectionItem> items) {
    JsonArray params = new JsonArray();

    items.forEach(item -> {
      params.add(UUID.randomUUID().toString());
      params.add(Json.encode(item));
    });

    return params;
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

  @NotNull
  public static List<AccessTypeCollectionItem> testData() {
    AccessTypeCollectionItem accessType1 = new AccessTypeCollectionItem()
      .withType(AccessTypeCollectionItem.Type.ACCESS_TYPES)
      .withAttributes(new AccessTypeDataAttributes()
        .withName("Access Type 1")
        .withDescription("Access Type description 1"))
      .withCreator(new UserDisplayInfo()
        .withFirstName("first name")
        .withLastName("last name"))
      .withUpdater(new UserDisplayInfo()
        .withFirstName("first name")
        .withLastName("last name"));

    AccessTypeCollectionItem accessType2 = new AccessTypeCollectionItem()
      .withType(AccessTypeCollectionItem.Type.ACCESS_TYPES)
      .withAttributes(new AccessTypeDataAttributes()
        .withName("Access Type 2")
        .withDescription("Access Type description 2"))
      .withCreator(new UserDisplayInfo()
        .withFirstName("first name")
        .withLastName("last name"))
      .withUpdater(new UserDisplayInfo()
        .withFirstName("first name")
        .withLastName("last name"));

    AccessTypeCollectionItem accessType3 = new AccessTypeCollectionItem()
      .withType(AccessTypeCollectionItem.Type.ACCESS_TYPES)
      .withAttributes(new AccessTypeDataAttributes()
        .withName("Access Type 3")
        .withDescription("Access Type description 3"))
      .withCreator(new UserDisplayInfo()
        .withFirstName("first name")
        .withLastName("last name"))
      .withUpdater(new UserDisplayInfo()
        .withFirstName("first name")
        .withLastName("last name"));

    return Arrays.asList(accessType1, accessType2, accessType3);
  }
}
