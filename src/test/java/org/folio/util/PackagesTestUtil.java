package org.folio.util;

import static org.folio.common.ListUtils.mapItems;
import static org.folio.repository.packages.PackageTableConstants.CONTENT_TYPE_COLUMN;
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
import static org.folio.test.util.TestUtil.readFile;
import static org.folio.util.KBTestUtil.setupDefaultKBConfiguration;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.github.tomakehurst.wiremock.matching.RegexPattern;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import lombok.Builder;
import lombok.Value;

import org.folio.holdingsiq.model.PackageByIdData;
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

  public static void addPackage(Vertx vertx, DbPackage dbPackage) {
    CompletableFuture<Void> future = new CompletableFuture<>();
    PostgresClient.getInstance(vertx).execute(
      "INSERT INTO " + packageTestTable() +
        "(" + ID_COLUMN + ", " + NAME_COLUMN + ", " + CONTENT_TYPE_COLUMN + ") VALUES(?,?,?)",
      new JsonArray(Arrays.asList(dbPackage.getId(), dbPackage.getName(), dbPackage.getContentType())),
      event -> future.complete(null));
    future.join();
  }

  private static String packageTestTable() {
    return PostgresClient.convertToPsqlStandard(STUB_TENANT) + "." + PACKAGES_TABLE_NAME;
  }

  public static String getPackageResponse(String packageName, String packageId, String stubProviderId) throws IOException, URISyntaxException {
    PackageByIdData packageData = Json.decodeValue(readFile("responses/rmapi/packages/get-package-by-id-response.json"), PackageByIdData.class);
    return Json.encode(packageData.toByIdBuilder()
      .packageName(packageName)
      .packageId(Integer.parseInt(packageId))
      .vendorId(Integer.parseInt(stubProviderId))
      .build());
  }

  public static PackagesTestUtil.DbPackage buildDbPackage(String id, String name) {
    return PackagesTestUtil.DbPackage.builder()
      .id(String.valueOf(id))
      .name(name)
      .contentType(STUB_PACKAGE_CONTENT_TYPE).build();
  }

  public static void setUpPackages(Vertx vertx, String wiremockUrl) throws IOException, URISyntaxException {
    setUpPackage(vertx, STUB_PACKAGE_ID, STUB_VENDOR_ID, STUB_PACKAGE_NAME);
    setUpPackage(vertx, STUB_PACKAGE_ID_2, STUB_VENDOR_ID_2, STUB_PACKAGE_NAME_2);
    setUpPackage(vertx, STUB_PACKAGE_ID_3, STUB_VENDOR_ID_3, STUB_PACKAGE_NAME_3);
    setupDefaultKBConfiguration(wiremockUrl, vertx);
  }

  public static void setUpPackage(Vertx vertx, String stubPackageId, String stubVendorId, String stubPackageName) throws IOException, URISyntaxException {
    PackagesTestUtil.addPackage(vertx, buildDbPackage(stubVendorId + "-" + stubPackageId, stubPackageName));
    mockPackageWithName(stubPackageId, stubVendorId, stubPackageName);
  }

  public static void mockPackageWithName(String stubPackageId, String stubProviderId, String stubPackageName) throws IOException, URISyntaxException {
    mockGetWithBody(new RegexPattern(".*vendors/" + stubProviderId + "/packages/" + stubPackageId),
      getPackageResponse(stubPackageName, stubPackageId, stubProviderId));
  }

  @Value
  @Builder(toBuilder = true)
 public static class DbPackage {
    private String id;
    private String name;
    private String contentType;
  }
}
