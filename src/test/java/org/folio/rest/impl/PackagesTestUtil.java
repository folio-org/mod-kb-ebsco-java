package org.folio.rest.impl;

import static org.folio.common.ListUtils.mapItems;
import static org.folio.tag.repository.packages.PackageTableConstants.CONTENT_TYPE_COLUMN;
import static org.folio.tag.repository.packages.PackageTableConstants.ID_COLUMN;
import static org.folio.tag.repository.packages.PackageTableConstants.NAME_COLUMN;
import static org.folio.tag.repository.packages.PackageTableConstants.TABLE_NAME;
import static org.folio.util.TestUtil.STUB_TENANT;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import io.vertx.core.Vertx;
import lombok.Builder;
import lombok.Value;

import org.folio.rest.persist.PostgresClient;

public class PackagesTestUtil {

  private PackagesTestUtil() {}

  public static List<DbPackage> getPackages(Vertx vertx) {
    CompletableFuture<List<DbPackage>> future = new CompletableFuture<>();
    PostgresClient.getInstance(vertx).select(
      "SELECT " + NAME_COLUMN + ", " + CONTENT_TYPE_COLUMN + ", " + ID_COLUMN + " FROM " + packageTestTable(),
      event -> future.complete(mapItems(event.result().getRows(),
        row ->
          DbPackage.builder()
          .id(row.getString(ID_COLUMN))
          .name(row.getString(NAME_COLUMN))
          .contentType(row.getString(CONTENT_TYPE_COLUMN))
          .build()
      )));
    return future.join();
  }

  public static void clearPackages(Vertx vertx) {
    CompletableFuture<Void> future = new CompletableFuture<>();
    PostgresClient.getInstance(vertx).execute(
      "DELETE FROM " + packageTestTable(),
      event -> future.complete(null));
    future.join();
  }

  private static String packageTestTable() {
    return PostgresClient.convertToPsqlStandard(STUB_TENANT) + "." + TABLE_NAME;
  }

  @Value
  @Builder(toBuilder = true)
  static class DbPackage {
    private String id;
    private String name;
    private String contentType;
  }
}
