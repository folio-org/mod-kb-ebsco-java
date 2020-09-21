package org.folio.util;

import static org.folio.db.RowSetUtils.mapItems;
import static org.folio.db.RowSetUtils.toUUID;
import static org.folio.repository.DbUtil.prepareQuery;
import static org.folio.repository.SqlQueryHelper.insertQuery;
import static org.folio.repository.SqlQueryHelper.selectQuery;
import static org.folio.repository.providers.ProviderTableConstants.CREDENTIALS_ID_COLUMN;
import static org.folio.repository.providers.ProviderTableConstants.ID_COLUMN;
import static org.folio.repository.providers.ProviderTableConstants.NAME_COLUMN;
import static org.folio.repository.providers.ProviderTableConstants.PROVIDERS_TABLE_NAME;
import static org.folio.test.util.TestUtil.STUB_TENANT;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import io.vertx.core.Vertx;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;

import org.folio.repository.providers.DbProvider;
import org.folio.rest.persist.PostgresClient;

public class ProvidersTestUtil {

  public ProvidersTestUtil() {
  }

  public static List<DbProvider> getProviders(Vertx vertx) {
    CompletableFuture<List<DbProvider>> future = new CompletableFuture<>();
    String query = prepareQuery(selectQuery(), providerTestTable());
    PostgresClient.getInstance(vertx, STUB_TENANT)
      .select(query, event -> future.complete(mapItems(event.result(), ProvidersTestUtil::mapDbProvider)));
    return future.join();
  }

  public static void saveProvider(DbProvider provider, Vertx vertx) {
    CompletableFuture<Void> future = new CompletableFuture<>();
    String query = prepareQuery(insertQuery(ID_COLUMN, CREDENTIALS_ID_COLUMN, NAME_COLUMN), providerTestTable());
    Tuple params = Tuple.of(provider.getId(), provider.getCredentialsId(), provider.getName());
    PostgresClient.getInstance(vertx, STUB_TENANT).execute(query, params, event -> future.complete(null));
    future.join();
  }

  public static DbProvider buildDbProvider(String id, String credentialsId, String name) {
    return buildDbProvider(id, toUUID(credentialsId), name);
  }

  private static DbProvider buildDbProvider(String id, UUID credentialsId, String name) {
    return DbProvider.builder()
      .id(id)
      .credentialsId(credentialsId)
      .name(name)
      .build();
  }

  private static DbProvider mapDbProvider(Row row) {
    return buildDbProvider(row.getString(ID_COLUMN), row.getUUID(CREDENTIALS_ID_COLUMN), row.getString(NAME_COLUMN));
  }

  private static String providerTestTable() {
    return PostgresClient.convertToPsqlStandard(STUB_TENANT) + "." + PROVIDERS_TABLE_NAME;
  }
}
