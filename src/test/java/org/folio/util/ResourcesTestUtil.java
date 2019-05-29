package org.folio.util;

import static org.folio.common.ListUtils.mapItems;
import static org.folio.tag.repository.packages.PackageTableConstants.ID_COLUMN;
import static org.folio.tag.repository.packages.PackageTableConstants.NAME_COLUMN;
import static org.folio.tag.repository.resources.ResourceTableConstants.RESOURCES_TABLE_NAME;
import static org.folio.util.TestUtil.STUB_TENANT;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import lombok.Builder;
import lombok.Value;

import org.folio.rest.persist.PostgresClient;

public class ResourcesTestUtil {

  public ResourcesTestUtil() {
  }

  public static List<ResourcesTestUtil.DbResources> getResources(Vertx vertx) {
    CompletableFuture<List<ResourcesTestUtil.DbResources>> future = new CompletableFuture<>();
    PostgresClient.getInstance(vertx).select(
      "SELECT " + NAME_COLUMN + ", " + ID_COLUMN + " FROM " + resourceTestTable(),
      event -> future.complete(mapItems(event.result().getRows(),
        row ->
          ResourcesTestUtil.DbResources.builder()
            .id(row.getString(ID_COLUMN))
            .name(row.getString(NAME_COLUMN))
            .build()
      )));
    return future.join();
  }

  public static void addResource(Vertx vertx, DbResources dbResource) {
    CompletableFuture<Void> future = new CompletableFuture<>();
    PostgresClient.getInstance(vertx).execute(
      "INSERT INTO " + resourceTestTable() +
        "(" + ID_COLUMN + ", " + NAME_COLUMN + ") VALUES(?,?)",
      new JsonArray(Arrays.asList(dbResource.getId(), dbResource.getName() )),
      event -> future.complete(null));
    future.join();
  }

  private static String resourceTestTable() {
    return PostgresClient.convertToPsqlStandard(STUB_TENANT) + "." + RESOURCES_TABLE_NAME;
  }

  @Value
  @Builder(toBuilder = true)
  public static class DbResources {
    private String id;
    private String name;
  }
}
