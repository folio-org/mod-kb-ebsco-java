package org.folio.util;

import static org.folio.db.RowSetUtils.mapItems;
import static org.folio.db.RowSetUtils.toUUID;
import static org.folio.repository.DbUtil.prepareQuery;
import static org.folio.repository.SqlQueryHelper.insertQuery;
import static org.folio.repository.SqlQueryHelper.selectQuery;
import static org.folio.repository.packages.PackageTableConstants.CONTENT_TYPE_COLUMN;
import static org.folio.repository.packages.PackageTableConstants.CREDENTIALS_ID_COLUMN;
import static org.folio.repository.packages.PackageTableConstants.ID_COLUMN;
import static org.folio.repository.packages.PackageTableConstants.NAME_COLUMN;
import static org.folio.repository.packages.PackageTableConstants.PACKAGES_TABLE_NAME;
import static org.folio.util.RmApiConstants.STUB_PACKAGE_CONTENT_TYPE;
import static org.folio.util.TestUtil.STUB_TENANT;

import io.vertx.core.Vertx;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import lombok.experimental.UtilityClass;
import org.folio.repository.packages.DbPackage;
import org.folio.rest.jaxrs.model.PackagePutData;
import org.folio.rest.jaxrs.model.PackagePutDataAttributes;
import org.folio.rest.jaxrs.model.PackagePutRequest;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.util.IdParser;

@UtilityClass
public final class PackagesTestUtil {

  public static List<DbPackage> getPackages(Vertx vertx) {
    CompletableFuture<List<DbPackage>> future = new CompletableFuture<>();
    String query = prepareQuery(selectQuery(), packageTestTable());
    PostgresClient.getInstance(vertx, STUB_TENANT)
      .select(query, event -> future.complete(mapItems(event.result(), PackagesTestUtil::mapDbPackage)));
    return future.join();
  }

  public static void savePackage(DbPackage dbPackage, Vertx vertx) {
    CompletableFuture<Void> future = new CompletableFuture<>();
    String query = prepareQuery(
      insertQuery(ID_COLUMN, CREDENTIALS_ID_COLUMN, NAME_COLUMN, CONTENT_TYPE_COLUMN),
      packageTestTable()
    );
    Tuple params = Tuple.of(
      IdParser.packageIdToString(dbPackage.getId()),
      dbPackage.getCredentialsId(),
      dbPackage.getName(),
      dbPackage.getContentType()
    );
    PostgresClient.getInstance(vertx, STUB_TENANT).execute(query, params, event -> future.complete(null));
    future.join();
  }

  public static DbPackage buildDbPackage(String id, String credentialsId, String name) {
    return buildDbPackage(id, toUUID(credentialsId), name, STUB_PACKAGE_CONTENT_TYPE);
  }

  public static DbPackage buildDbPackage(String id, UUID credentialsId, String name, String contentType) {
    return DbPackage.builder()
      .id(IdParser.parsePackageId(id))
      .credentialsId(credentialsId)
      .name(name)
      .contentType(contentType)
      .build();
  }

  public static PackagePutRequest getPackagePutRequest(PackagePutDataAttributes attributes) {
    return new PackagePutRequest()
      .withData(new PackagePutData()
        .withType(PackagePutData.Type.PACKAGES)
        .withAttributes(attributes));
  }

  private static DbPackage mapDbPackage(Row row) {
    return buildDbPackage(
      row.getString(ID_COLUMN),
      row.getUUID(CREDENTIALS_ID_COLUMN),
      row.getString(NAME_COLUMN),
      row.getString(CONTENT_TYPE_COLUMN)
    );
  }

  private static String packageTestTable() {
    return PostgresClient.convertToPsqlStandard(STUB_TENANT) + "." + PACKAGES_TABLE_NAME;
  }
}
