package org.folio.util;

import io.vertx.core.Vertx;
import lombok.Builder;
import lombok.Value;
import org.folio.rest.persist.PostgresClient;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.folio.common.ListUtils.mapItems;
import static org.folio.tag.repository.packages.PackageTableConstants.ID_COLUMN;
import static org.folio.tag.repository.packages.PackageTableConstants.NAME_COLUMN;
import static org.folio.tag.repository.providers.ProviderTableConstants.PROVIDERS_TABLE_NAME;
import static org.folio.util.TestUtil.STUB_TENANT;

public class ProvidersTestUtil {

  public ProvidersTestUtil() {
  }

  public static List<DbProviders> getProviders(Vertx vertx) {
    CompletableFuture<List<ProvidersTestUtil.DbProviders>> future = new CompletableFuture<>();
    PostgresClient.getInstance(vertx).select(
      "SELECT " + NAME_COLUMN + ", " + ID_COLUMN + " FROM " + providerTestTable(),
      event -> future.complete(mapItems(event.result().getRows(),
        row ->
          ProvidersTestUtil.DbProviders.builder()
            .id(row.getString(ID_COLUMN))
            .name(row.getString(NAME_COLUMN))
            .build()
      )));
    return future.join();
  }

  private static String providerTestTable() {
    return PostgresClient.convertToPsqlStandard(STUB_TENANT) + "." + PROVIDERS_TABLE_NAME;
  }

  @Value
  @Builder(toBuilder = true)
 public static class DbProviders {
    private String id;
    private String name;
  }
}
