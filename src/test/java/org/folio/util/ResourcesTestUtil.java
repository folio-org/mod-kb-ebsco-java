package org.folio.util;

import static org.folio.db.RowSetUtils.mapItems;
import static org.folio.db.RowSetUtils.toUUID;
import static org.folio.repository.DbUtil.prepareQuery;
import static org.folio.repository.SqlQueryHelper.insertQuery;
import static org.folio.repository.SqlQueryHelper.selectQuery;
import static org.folio.repository.resources.ResourceTableConstants.CREDENTIALS_ID_COLUMN;
import static org.folio.repository.resources.ResourceTableConstants.ID_COLUMN;
import static org.folio.repository.resources.ResourceTableConstants.NAME_COLUMN;
import static org.folio.repository.resources.ResourceTableConstants.RESOURCES_TABLE_NAME;
import static org.folio.rest.util.IdParser.resourceIdToString;
import static org.folio.test.util.TestUtil.STUB_TENANT;

import io.vertx.core.Vertx;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.folio.repository.resources.DbResource;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.util.IdParser;

public class ResourcesTestUtil {

  public ResourcesTestUtil() {
  }

  public static List<DbResource> getResources(Vertx vertx) {
    CompletableFuture<List<DbResource>> future = new CompletableFuture<>();
    String query = prepareQuery(selectQuery(), resourceTestTable());
    PostgresClient.getInstance(vertx, STUB_TENANT)
      .select(query, event -> future.complete(mapItems(event.result(), ResourcesTestUtil::mapDbResource)));
    return future.join();
  }

  public static void saveResource(DbResource dbResource, Vertx vertx) {
    CompletableFuture<Void> future = new CompletableFuture<>();
    String query = prepareQuery(insertQuery(ID_COLUMN, CREDENTIALS_ID_COLUMN, NAME_COLUMN), resourceTestTable());
    Tuple params =
      Tuple.of(resourceIdToString(dbResource.getId()), dbResource.getCredentialsId(), dbResource.getName());
    PostgresClient.getInstance(vertx, STUB_TENANT).execute(query, params, event -> future.complete(null));
    future.join();
  }

  public static DbResource buildResource(String id, String credentialsId, String name) {
    return buildResource(id, toUUID(credentialsId), name);
  }

  private static DbResource buildResource(String id, UUID credentialsId, String name) {
    return DbResource.builder().id(IdParser.parseResourceId(id)).credentialsId(credentialsId).name(name).build();
  }

  private static DbResource mapDbResource(Row row) {
    return buildResource(row.getString(ID_COLUMN), row.getUUID(CREDENTIALS_ID_COLUMN), row.getString(NAME_COLUMN));
  }

  private static String resourceTestTable() {
    return PostgresClient.convertToPsqlStandard(STUB_TENANT) + "." + RESOURCES_TABLE_NAME;
  }
}
