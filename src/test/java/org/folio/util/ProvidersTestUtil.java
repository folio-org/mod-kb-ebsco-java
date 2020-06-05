package org.folio.util;

import static org.folio.repository.providers.ProviderTableConstants.CREDENTIALS_ID_COLUMN;
import static org.folio.repository.providers.ProviderTableConstants.ID_COLUMN;
import static org.folio.repository.providers.ProviderTableConstants.NAME_COLUMN;
import static org.folio.repository.providers.ProviderTableConstants.PROVIDERS_TABLE_NAME;
import static org.folio.test.util.TestUtil.STUB_TENANT;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import io.vertx.core.Vertx;
import io.vertx.sqlclient.Tuple;
import lombok.Builder;
import lombok.Value;

import org.folio.db.RowSetUtils;
import org.folio.rest.persist.PostgresClient;

public class ProvidersTestUtil {

  public ProvidersTestUtil() {
  }

  public static List<DbProviders> getProviders(Vertx vertx) {
    CompletableFuture<List<ProvidersTestUtil.DbProviders>> future = new CompletableFuture<>();
    PostgresClient.getInstance(vertx).select(
      "SELECT " + NAME_COLUMN + ", " + CREDENTIALS_ID_COLUMN + ", " + ID_COLUMN + " FROM " + providerTestTable(),
      event -> future.complete(RowSetUtils.mapItems(event.result(),
        row ->
          DbProviders.builder()
            .id(row.getString(ID_COLUMN))
            .credentialsId(row.getString(CREDENTIALS_ID_COLUMN))
            .name(row.getString(NAME_COLUMN))
            .build()
      )));
    return future.join();
  }

  public static void addProvider(Vertx vertx, DbProviders provider) {
    CompletableFuture<Void> future = new CompletableFuture<>();
    PostgresClient.getInstance(vertx).execute(
      "INSERT INTO " + providerTestTable() +
        "(" + ID_COLUMN + ", " + CREDENTIALS_ID_COLUMN + ", " + NAME_COLUMN + ") " +
        "VALUES(?,?,?)",
      Tuple.of(provider.getId(), provider.getCredentialsId(), provider.getName()),
      event -> future.complete(null));
    future.join();
  }

  private static String providerTestTable() {
    return PostgresClient.convertToPsqlStandard(STUB_TENANT) + "." + PROVIDERS_TABLE_NAME;
  }

  @Value
  @Builder(toBuilder = true)
  public static class DbProviders {

    String id;
    String credentialsId;
    String name;
  }
}
