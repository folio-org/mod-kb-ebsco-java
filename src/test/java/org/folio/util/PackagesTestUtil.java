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
import static org.folio.rest.impl.PackagesTestData.STUB_PACKAGE_CONTENT_TYPE;
import static org.folio.rest.impl.PackagesTestData.STUB_PACKAGE_ID;
import static org.folio.rest.impl.PackagesTestData.STUB_PACKAGE_ID_2;
import static org.folio.rest.impl.PackagesTestData.STUB_PACKAGE_ID_3;
import static org.folio.rest.impl.PackagesTestData.STUB_PACKAGE_NAME;
import static org.folio.rest.impl.PackagesTestData.STUB_PACKAGE_NAME_2;
import static org.folio.rest.impl.PackagesTestData.STUB_PACKAGE_NAME_3;
import static org.folio.rest.impl.ProvidersTestData.STUB_VENDOR_ID;
import static org.folio.rest.impl.ProvidersTestData.STUB_VENDOR_ID_2;
import static org.folio.rest.impl.ProvidersTestData.STUB_VENDOR_ID_3;
import static org.folio.test.util.TestUtil.STUB_TENANT;
import static org.folio.test.util.TestUtil.mockGetWithBody;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import com.github.tomakehurst.wiremock.matching.RegexPattern;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;

import org.folio.holdingsiq.model.PackageByIdData;
import org.folio.repository.packages.DbPackage;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.util.IdParser;

public class PackagesTestUtil {

  private static final String STUB_PACKAGE_JSON_PATH = "responses/rmapi/packages/get-package-by-id-response.json";

  private PackagesTestUtil() {
  }

  public static List<DbPackage> getPackages(Vertx vertx) {
    CompletableFuture<List<DbPackage>> future = new CompletableFuture<>();
    String query = prepareQuery(selectQuery(), packageTestTable());
    PostgresClient.getInstance(vertx)
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
    PostgresClient.getInstance(vertx).execute(query, params, event -> future.complete(null));
    future.join();
  }

  public static String getPackageResponse(String packageName, String packageId, String providerId) {
    PackageByIdData packageData = Json.decodeValue(STUB_PACKAGE_JSON_PATH, PackageByIdData.class);
    return Json.encode(packageData.toByIdBuilder()
      .packageName(packageName)
      .packageId(Integer.parseInt(packageId))
      .vendorId(Integer.parseInt(providerId))
      .build());
  }

  public static void setUpPackages(Vertx vertx, String credentialsId) {
    setUpPackage(vertx, credentialsId, STUB_PACKAGE_ID, STUB_VENDOR_ID, STUB_PACKAGE_NAME);
    setUpPackage(vertx, credentialsId, STUB_PACKAGE_ID_2, STUB_VENDOR_ID_2, STUB_PACKAGE_NAME_2);
    setUpPackage(vertx, credentialsId, STUB_PACKAGE_ID_3, STUB_VENDOR_ID_3, STUB_PACKAGE_NAME_3);
  }

  public static void setUpPackage(Vertx vertx, String credentialsId, String packageId, String vendorId, String packageName) {
    savePackage(buildDbPackage(vendorId + "-" + packageId, credentialsId, packageName), vertx);
    mockPackageWithName(packageId, vendorId, packageName);
  }

  public static void mockPackageWithName(String stubPackageId, String stubProviderId, String stubPackageName) {
    mockGetWithBody(new RegexPattern(".*vendors/" + stubProviderId + "/packages/" + stubPackageId),
      getPackageResponse(stubPackageName, stubPackageId, stubProviderId));
  }

  public static DbPackage buildDbPackage(String id, String credentialsId, String name) {
    return buildDbPackage(id, toUUID(credentialsId), name, STUB_PACKAGE_CONTENT_TYPE);
  }

  private static DbPackage buildDbPackage(String id, UUID credentialsId, String name, String contentType) {
    return DbPackage.builder()
      .id(IdParser.parsePackageId(id))
      .credentialsId(credentialsId)
      .name(name)
      .contentType(contentType)
      .build();
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
