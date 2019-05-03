package org.folio.rest.impl;

import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import static org.folio.util.TestUtil.getFile;
import static org.folio.util.TestUtil.mockConfiguration;
import static org.folio.util.TestUtil.mockGet;
import static org.folio.util.TestUtil.mockGetWithBody;
import static org.folio.util.TestUtil.mockPost;
import static org.folio.util.TestUtil.mockPut;
import static org.folio.util.TestUtil.readFile;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.AnythingPattern;
import com.github.tomakehurst.wiremock.matching.EqualToJsonPattern;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import com.github.tomakehurst.wiremock.matching.RegexPattern;
import com.github.tomakehurst.wiremock.matching.UrlPathPattern;

import io.restassured.RestAssured;
import io.vertx.core.json.Json;
import io.vertx.ext.unit.junit.VertxUnitRunner;

import org.apache.http.HttpStatus;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.JSONAssert;

import org.folio.holdingsiq.model.CoverageDates;
import org.folio.holdingsiq.model.PackageByIdData;
import org.folio.holdingsiq.model.PackageData;
import org.folio.rest.jaxrs.model.ContentType;
import org.folio.rest.jaxrs.model.JsonapiError;
import org.folio.rest.jaxrs.model.Package;
import org.folio.rest.jaxrs.model.PackageCollection;
import org.folio.rest.jaxrs.model.PackageCollectionItem;
import org.folio.rest.jaxrs.model.PackagePostRequest;
import org.folio.rest.jaxrs.model.PackagePutRequest;
import org.folio.rest.jaxrs.model.ResourceCollection;
import org.folio.rest.jaxrs.model.Tags;
import org.folio.tag.RecordType;

@RunWith(VertxUnitRunner.class)
public class EholdingsPackagesTest extends WireMockTestBase {

  private static final String PACKAGE_STUB_FILE = "responses/rmapi/packages/get-package-by-id-response.json";
  private static final String CUSTOM_PACKAGE_STUB_FILE = "responses/rmapi/packages/get-custom-package-by-id-response.json";
  private static final String CONFIGURATION_STUB_FILE = "responses/kb-ebsco/configuration/get-configuration.json";
  private static final String RESOURCES_BY_PACKAGE_ID_STUB_FILE = "responses/rmapi/resources/get-resources-by-package-id-response.json";
  private static final String EXPECTED_PACKAGE_BY_ID_STUB_FILE = "responses/kb-ebsco/packages/expected-package-by-id.json";
  private static final String EXPECTED_RESOURCES_STUB_FILE = "responses/kb-ebsco/resources/get-resources-by-package-id-response.json";
  private static final String VENDOR_BY_PACKAGE_ID_STUB_FILE = "responses/rmapi/vendors/get-vendor-by-id-for-package.json";

  private static final int STUB_PACKAGE_ID = 3964;
  private static final int STUB_PACKAGE_ID_2 = 13964;
  private static final int STUB_PACKAGE_ID_3 = 23964;

  private static final String STUB_PACKAGE_NAME = "EBSCO Biotechnology Collection: India";
  private static final String STUB_PACKAGE_NAME_2 = "package 2";
  private static final String STUB_PACKAGE_NAME_3 = "package 3";
  private static final String STUB_PACKAGE_CONTENT_TYPE = "AggregatedFullText";
  private static final int STUB_VENDOR_ID = 111111;
  public static final String PACKAGES_STUB_URL = "/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors/" + STUB_VENDOR_ID + "/packages";
  public static final String FULL_PACKAGE_ID = STUB_VENDOR_ID + "-" + STUB_PACKAGE_ID;
  public static final String FULL_PACKAGE_ID_2 = STUB_VENDOR_ID + "-" + STUB_PACKAGE_ID_2;
  public static final String FULL_PACKAGE_ID_3 = STUB_VENDOR_ID + "-" + STUB_PACKAGE_ID_3;

  private static final String PACKAGES_PATH = "eholdings/packages/" + STUB_VENDOR_ID + "-" + STUB_PACKAGE_ID;
  private static final String PACKAGE_BY_ID_URL = "/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors/" + STUB_VENDOR_ID + "/packages/" + STUB_PACKAGE_ID;
  private static final String RESOURCES_BY_PACKAGE_ID_URL = PACKAGE_BY_ID_URL + "/titles";
  private static final String PACKAGED_UPDATED_STATE = "Packaged updated";
  private static final String GET_PACKAGE_SCENARIO = "Get package";
  private static final String STUB_TAG = "test tag";
  private static final String STUB_TAG_VALUE = "tag one";
  private static final String STUB_TAG_VALUE_2 = "tag 2";
  private static final String STUB_TAG_VALUE_3 = "tag 3";

  @Test
  public void shouldReturnPackagesOnGet() throws IOException, URISyntaxException {
    String stubResponseFile = "responses/rmapi/packages/get-packages-response.json";

    mockConfiguration(CONFIGURATION_STUB_FILE, getWiremockUrl());
    mockGet(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/packages.*"), stubResponseFile);

    String packages = getOkResponse("eholdings/packages?q=American&filter[type]=abstractandindex&count=5")
      .asString();
    JSONAssert.assertEquals(readFile("responses/kb-ebsco/packages/expected-package-collection-with-five-elements.json"),
      packages, false);
  }


  @Test
  public void shouldReturnPackagesOnSearchByTagsOnly() throws IOException, URISyntaxException {
    try {
      TagsTestUtil.insertTag(vertx, FULL_PACKAGE_ID, RecordType.PACKAGE, STUB_TAG_VALUE);
      TagsTestUtil.insertTag(vertx, FULL_PACKAGE_ID_2, RecordType.PACKAGE, STUB_TAG_VALUE);
      TagsTestUtil.insertTag(vertx, FULL_PACKAGE_ID_2, RecordType.PACKAGE, STUB_TAG_VALUE_2);
      TagsTestUtil.insertTag(vertx, FULL_PACKAGE_ID_3, RecordType.PACKAGE, STUB_TAG_VALUE_3);

      setUpTaggedPackages();

      PackageCollection packageCollection = RestAssured.given(getRequestSpecification())
        .when()
        .get("eholdings/packages?filter[tags]=" + STUB_TAG_VALUE + "," + STUB_TAG_VALUE_2)
        .then()
        .statusCode(HttpStatus.SC_OK).extract().as(PackageCollection.class);
      List<PackageCollectionItem> packages = packageCollection.getData();

      assertEquals(2, (int) packageCollection.getMeta().getTotalResults());
      assertEquals(2, packages.size());
      assertEquals(STUB_PACKAGE_NAME, packages.get(0).getAttributes().getName());
      assertEquals(STUB_PACKAGE_NAME_2, packages.get(1).getAttributes().getName());
    } finally {
      TagsTestUtil.clearTags(vertx);
      PackagesTestUtil.clearPackages(vertx);
    }
  }

  @Test
  public void shouldReturnPackagesOnSearchWithPagination() throws IOException, URISyntaxException {
    try {
      TagsTestUtil.insertTag(vertx, FULL_PACKAGE_ID, RecordType.PACKAGE, STUB_TAG_VALUE);
      TagsTestUtil.insertTag(vertx, FULL_PACKAGE_ID_2, RecordType.PACKAGE, STUB_TAG_VALUE);
      TagsTestUtil.insertTag(vertx, FULL_PACKAGE_ID_3, RecordType.PACKAGE, STUB_TAG_VALUE);

      setUpTaggedPackages();

      PackageCollection packageCollection = RestAssured.given(getRequestSpecification())
        .when()
        .get("eholdings/packages?page=2&count=1&filter[tags]=" + STUB_TAG_VALUE)
        .then()
        .statusCode(HttpStatus.SC_OK).extract().as(PackageCollection.class);
      List<PackageCollectionItem> packages = packageCollection.getData();

      assertEquals(3, (int) packageCollection.getMeta().getTotalResults());
      assertEquals(1, packages.size());
      assertEquals(STUB_PACKAGE_NAME_2, packages.get(0).getAttributes().getName());
    } finally {
      TagsTestUtil.clearTags(vertx);
      PackagesTestUtil.clearPackages(vertx);
    }
  }

  @Test
  public void shouldReturnPackagesOnGetWithPackageId() throws IOException, URISyntaxException {
    String packagesStubResponseFile = "responses/rmapi/packages/get-packages-by-provider-id.json";
    String providerIdByCustIdStubResponseFile = "responses/rmapi/proxiescustomlabels/get-root-proxy-custom-labels-success-response.json";

    mockConfiguration(CONFIGURATION_STUB_FILE, getWiremockUrl());

    mockGet(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/"), providerIdByCustIdStubResponseFile);
    mockGet(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors/" + STUB_VENDOR_ID + "/packages.*")
      , packagesStubResponseFile);

    String packages = getOkResponse("eholdings/packages?q=a&count=5&page=1&filter[custom]=true")
      .asString();

    JSONAssert.assertEquals(readFile("responses/kb-ebsco/packages/expected-package-collection-with-one-element.json"),
      packages, false);
}

  @Test
  public void shouldReturnPackagesOnGetById() throws IOException, URISyntaxException {
    mockConfiguration("responses/kb-ebsco/configuration/get-configuration.json", getWiremockUrl());

    mockGet(new RegexPattern(PACKAGE_BY_ID_URL), CUSTOM_PACKAGE_STUB_FILE);

    String packageData = getOkResponse(PACKAGES_PATH).asString();

    JSONAssert.assertEquals(readFile(EXPECTED_PACKAGE_BY_ID_STUB_FILE), packageData, false);
  }

  @Test
  public void shouldReturnPackageWithTagOnGetById() throws IOException, URISyntaxException {
    try {
      String packageId = FULL_PACKAGE_ID;
      TagsTestUtil.insertTag(vertx, packageId, RecordType.PACKAGE, STUB_TAG);
      mockConfiguration("responses/kb-ebsco/configuration/get-configuration.json", getWiremockUrl());

      mockGet(new RegexPattern(PACKAGE_BY_ID_URL), CUSTOM_PACKAGE_STUB_FILE);

      Package packageData = getOkResponse("eholdings/packages/" + packageId).as(Package.class);

      assertTrue(packageData.getData().getAttributes().getTags().getTagList().contains(STUB_TAG));
    }
    finally {
      TagsTestUtil.clearTags(vertx);
      PackagesTestUtil.clearPackages(vertx);
    }
  }

  @Test
  public void shouldDeleteAndAddPackageTagsOnPut() throws IOException, URISyntaxException {
    try {
      TagsTestUtil.insertTag(vertx, FULL_PACKAGE_ID, RecordType.PACKAGE, "test one");
      List<String> newTags = Arrays.asList(STUB_TAG_VALUE, STUB_TAG_VALUE_2);
      sendPutWithTags(Arrays.asList(STUB_TAG_VALUE, STUB_TAG_VALUE_2));
      List<String> tagsAfterRequest = TagsTestUtil.getTagsForRecordType(vertx, RecordType.PACKAGE);
      assertThat(tagsAfterRequest, containsInAnyOrder(newTags.toArray()));
    } finally {
      TagsTestUtil.clearTags(vertx);
      PackagesTestUtil.clearPackages(vertx);
    }
  }

  @Test
  public void shouldAddPackageTagsOnPutWhenPackageAlreadyHasTags() throws IOException, URISyntaxException {
    try {
      TagsTestUtil.insertTag(vertx, FULL_PACKAGE_ID, RecordType.PACKAGE, STUB_TAG_VALUE);
      List<String> newTags = Arrays.asList(STUB_TAG_VALUE, STUB_TAG_VALUE_2);
      sendPutWithTags(Arrays.asList(STUB_TAG_VALUE, STUB_TAG_VALUE_2));
      List<String> tagsAfterRequest = TagsTestUtil.getTagsForRecordType(vertx, RecordType.PACKAGE);
      assertThat(tagsAfterRequest, containsInAnyOrder(newTags.toArray()));
    } finally {
      TagsTestUtil.clearTags(vertx);
      PackagesTestUtil.clearPackages(vertx);
    }
  }

  @Test
  public void shouldAddPackageTagsOnPostWhenPackageAlreadyHasTags() throws IOException, URISyntaxException {
    try {
      List<String> newTags = Arrays.asList(STUB_TAG_VALUE, STUB_TAG_VALUE_2);
      sendPutWithTags(Arrays.asList(STUB_TAG_VALUE, STUB_TAG_VALUE_2));
      List<String> tagsAfterRequest = TagsTestUtil.getTagsForRecordType(vertx, RecordType.PACKAGE);
      assertThat(tagsAfterRequest, containsInAnyOrder(newTags.toArray()));
    } finally {
      TagsTestUtil.clearTags(vertx);
      PackagesTestUtil.clearPackages(vertx);
    }
  }

  @Test
  public void shouldAddPackageDataOnPut() throws IOException, URISyntaxException {
    try {
      List<String> tags = Collections.singletonList(STUB_TAG_VALUE);
      sendPutWithTags(tags);
      List<PackagesTestUtil.DbPackage> packages = PackagesTestUtil.getPackages(vertx);
      assertEquals(1, packages.size());
      assertEquals(FULL_PACKAGE_ID, packages.get(0).getId());
      assertEquals(STUB_PACKAGE_NAME, packages.get(0).getName());
      assertEquals(STUB_PACKAGE_CONTENT_TYPE, packages.get(0).getContentType());
    } finally {
      TagsTestUtil.clearTags(vertx);
      PackagesTestUtil.clearPackages(vertx);
    }
  }

  @Test
  public void shouldUpdatePackageDataOnSecondPut() throws IOException, URISyntaxException {
    try {
      String newName = "new name";
      String newType = "newType";

      List<String> tags = Collections.singletonList(STUB_TAG_VALUE);
      sendPutWithTags(tags);

      ObjectMapper mapper = new ObjectMapper();
      PackageByIdData updatedPackage = mapper.readValue(readFile(PACKAGE_STUB_FILE), PackageByIdData.class)
        .toByIdBuilder().packageName(newName).contentType(newType).build();
      sendPutWithTags(updatedPackage, tags);

      List<PackagesTestUtil.DbPackage> packages = PackagesTestUtil.getPackages(vertx);
      assertEquals(1, packages.size());
      assertEquals(FULL_PACKAGE_ID, packages.get(0).getId());
      assertEquals(newName, packages.get(0).getName());
      assertEquals(newType, packages.get(0).getContentType());
    } finally {
      TagsTestUtil.clearTags(vertx);
      PackagesTestUtil.clearPackages(vertx);
    }
  }

  @Test
  public void shouldDeletePackageDataOnPutWithEmptyTagList() throws IOException, URISyntaxException {
    List<String> tags = Collections.singletonList(STUB_TAG_VALUE);
    sendPutWithTags(tags);
    sendPutWithTags(Collections.emptyList());

    List<PackagesTestUtil.DbPackage> packages = PackagesTestUtil.getPackages(vertx);
    assertThat(packages, is(empty()));
  }

  @Test
  public void shouldAddPackageDataOnPost() throws URISyntaxException, IOException {
    try {
      String packagePostStubRequestFile = "requests/kb-ebsco/package/post-package-request.json";
      sendPostWithTags(readFile(packagePostStubRequestFile), Collections.singletonList(STUB_TAG_VALUE));
      List<PackagesTestUtil.DbPackage> packages = PackagesTestUtil.getPackages(vertx);
      assertEquals(1, packages.size());
      assertEquals(FULL_PACKAGE_ID, packages.get(0).getId());
      assertEquals(STUB_PACKAGE_NAME, packages.get(0).getName());
      assertEquals(STUB_PACKAGE_CONTENT_TYPE, packages.get(0).getContentType());
    } finally {
      TagsTestUtil.clearTags(vertx);
      PackagesTestUtil.clearPackages(vertx);
    }
  }


  @Test
  public void shouldDeleteAllPackageTagsOnPutWhenRequestHasEmptyListOfTags() throws IOException, URISyntaxException {
    TagsTestUtil.insertTag(vertx, FULL_PACKAGE_ID, RecordType.PACKAGE, "test one");
      sendPutWithTags(Collections.emptyList());
      List<String> tagsAfterRequest = TagsTestUtil.getTagsForRecordType(vertx, RecordType.PACKAGE);
      assertThat(tagsAfterRequest, empty());
  }

  @Test
  public void shouldDoNothingOnPutWhenRequestHasNotTags() throws IOException, URISyntaxException {
    sendPutWithTags(null);
    sendPutWithTags(null);
    List<String> tagsAfterRequest = TagsTestUtil.getTagsForRecordType(vertx, RecordType.PACKAGE);
    assertThat(tagsAfterRequest, empty());
  }

  @Test
  public void shouldDeletePackageTagsOnDelete() throws IOException, URISyntaxException {
    TagsTestUtil.insertTag(vertx, FULL_PACKAGE_ID, RecordType.PACKAGE, "test one");

      mockConfiguration(CONFIGURATION_STUB_FILE, getWiremockUrl());
      mockGet(new EqualToPattern(PACKAGE_BY_ID_URL), CUSTOM_PACKAGE_STUB_FILE);

      EqualToJsonPattern putBodyPattern = new EqualToJsonPattern("{\"isSelected\":false}", true, true);
      mockPut(new EqualToPattern(PACKAGE_BY_ID_URL), putBodyPattern, HttpStatus.SC_NO_CONTENT);

      RestAssured.given()
        .spec(getRequestSpecification())
        .when()
        .delete(PACKAGES_PATH)
        .then()
        .statusCode(HttpStatus.SC_NO_CONTENT);

      List<String> tagsAfterRequest = TagsTestUtil.getTagsForRecordType(vertx, RecordType.PACKAGE);
      assertThat(tagsAfterRequest, empty());
  }

  @Test
  public void shouldDeletePackageOnDeleteRequest() throws IOException, URISyntaxException {
    sendPostWithTags(readFile("requests/kb-ebsco/package/post-package-request.json"), Collections.singletonList(STUB_TAG_VALUE));

    mockGet(new EqualToPattern(PACKAGE_BY_ID_URL), CUSTOM_PACKAGE_STUB_FILE);
    mockPut(new EqualToPattern(PACKAGE_BY_ID_URL), new AnythingPattern(), HttpStatus.SC_NO_CONTENT);

    RestAssured.given()
      .spec(getRequestSpecification())
      .when()
      .delete(PACKAGES_PATH)
      .then()
      .statusCode(HttpStatus.SC_NO_CONTENT);

    List<PackagesTestUtil.DbPackage> packages = PackagesTestUtil.getPackages(vertx);
    assertThat(packages, is(empty()));
  }

  @Test
  public void shouldReturn404WhenPackageIsNotFoundOnRMAPI() throws IOException, URISyntaxException {
    mockConfiguration("responses/kb-ebsco/configuration/get-configuration.json", getWiremockUrl());

    mockGet(new RegexPattern(PACKAGE_BY_ID_URL), HttpStatus.SC_NOT_FOUND);

    getResponse(PACKAGES_PATH).statusCode(HttpStatus.SC_NOT_FOUND);
  }

  @Test
  public void shouldReturnResourcesWhenIncludedFlagIsSetToResources() throws IOException, URISyntaxException {
    mockConfiguration("responses/kb-ebsco/configuration/get-configuration.json", getWiremockUrl());
    mockGet(new RegexPattern(PACKAGE_BY_ID_URL), CUSTOM_PACKAGE_STUB_FILE);
    mockResourceById(RESOURCES_BY_PACKAGE_ID_STUB_FILE);

    Package packageData = getOkResponse(PACKAGES_PATH + "?include=resources")
      .as(Package.class);

    ObjectMapper mapper = new ObjectMapper();
    Package expectedPackage = mapper.readValue(readFile(EXPECTED_PACKAGE_BY_ID_STUB_FILE), Package.class);
    ResourceCollection expectedResources = mapper.readValue(readFile(EXPECTED_RESOURCES_STUB_FILE), ResourceCollection.class);
    expectedPackage.getIncluded().addAll(expectedResources.getData());

    JSONAssert.assertEquals(mapper.writeValueAsString(expectedPackage), mapper.writeValueAsString(packageData), false);
  }

  @Test
  public void shouldReturnProviderWhenIncludedFlagIsSetToProvider() throws IOException, URISyntaxException {
    mockConfiguration("responses/kb-ebsco/configuration/get-configuration.json", getWiremockUrl());

    mockGet(new RegexPattern(PACKAGE_BY_ID_URL), CUSTOM_PACKAGE_STUB_FILE);
    mockGet(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors/" + STUB_VENDOR_ID),
      VENDOR_BY_PACKAGE_ID_STUB_FILE);

    String actual = getOkResponse(PACKAGES_PATH + "?include=provider")
      .asString();

    String expected = readFile("responses/kb-ebsco/packages/expected-package-by-id-with-provider.json");
    JSONAssert.assertEquals(expected, actual, false);
  }

  @Test
  public void shouldReturn400WhenCountInvalid() {
    checkResponseNotEmptyWhenStatusIs400("eholdings/packages?q=American&filter[type]=abstractandindex&count=500");
  }

  @Test
  public void shouldSendDeleteRequestForPackage() throws IOException, URISyntaxException {

    mockConfiguration(CONFIGURATION_STUB_FILE, getWiremockUrl());

    UrlPathPattern packageUrlPattern = new UrlPathPattern(new EqualToPattern(PACKAGE_BY_ID_URL), false);
    EqualToJsonPattern putBodyPattern = new EqualToJsonPattern("{\"isSelected\":false}", true, true);

    mockGet(new EqualToPattern(PACKAGE_BY_ID_URL), CUSTOM_PACKAGE_STUB_FILE);

    mockPut(new EqualToPattern(PACKAGE_BY_ID_URL), putBodyPattern, HttpStatus.SC_NO_CONTENT);

    RestAssured.given()
      .spec(getRequestSpecification())
      .when()
      .delete(PACKAGES_PATH)
      .then()
      .statusCode(HttpStatus.SC_NO_CONTENT);

    verify(1, putRequestedFor(packageUrlPattern)
      .withRequestBody(putBodyPattern));
  }

  @Test
  public void shouldReturn400WhenPackageIdIsInvalid() {
    RestAssured.given()
      .spec(getRequestSpecification())
      .when()
      .delete("eholdings/packages/abc-def")
      .then()
      .statusCode(HttpStatus.SC_BAD_REQUEST);
  }

  @Test
  public void shouldReturn400WhenPackageIsNotCustom() throws URISyntaxException, IOException {
    mockConfiguration(CONFIGURATION_STUB_FILE, getWiremockUrl());

    ObjectMapper mapper = new ObjectMapper();
    PackageData packageData = mapper.readValue(getFile(CUSTOM_PACKAGE_STUB_FILE), PackageData.class)
      .toBuilder().isCustom(false).build();

    stubFor(
      get(new UrlPathPattern(new EqualToPattern(PACKAGE_BY_ID_URL), false))
        .willReturn(new ResponseDefinitionBuilder()
          .withBody(mapper.writeValueAsString(packageData))));

    RestAssured.given()
      .spec(getRequestSpecification())
      .when()
      .delete(PACKAGES_PATH)
      .then()
      .statusCode(HttpStatus.SC_BAD_REQUEST);
  }

  @Test
  public void shouldReturn200WhenSelectingPackage() throws URISyntaxException, IOException {
    boolean updatedIsSelected = true;

    mockConfiguration(CONFIGURATION_STUB_FILE, getWiremockUrl());

    UrlPathPattern urlPattern = new UrlPathPattern(new EqualToPattern(PACKAGE_BY_ID_URL), false);
    EqualToJsonPattern putBodyPattern = new EqualToJsonPattern(readFile("requests/rmapi/packages/put-package-is-selected.json"), true, true);

    ObjectMapper mapper = new ObjectMapper();
    PackageByIdData packageData = mapper.readValue(getFile(PACKAGE_STUB_FILE), PackageByIdData.class);
    String initialPackageValue = mapper.writeValueAsString(packageData);
    packageData = packageData.toByIdBuilder().isSelected(updatedIsSelected).build();
    String updatedPackageValue = mapper.writeValueAsString(packageData);
    mockUpdateScenario(urlPattern, initialPackageValue, updatedPackageValue);

    Package aPackage = sendPutRequestAndRetrieveResponse(
      PACKAGES_PATH,
      readFile("requests/kb-ebsco/package/put-package-selected.json"),
      Package.class);

    assertEquals(updatedIsSelected, aPackage.getData().getAttributes().getIsSelected());

    verify(putRequestedFor(urlPattern)
      .withRequestBody(putBodyPattern));
  }

  @Test
  public void shouldUpdateAllAttributesInSelectedPackage() throws URISyntaxException, IOException {
    boolean updatedSelected = true;
    boolean updatedAllowEbscoToAddTitles = true;
    boolean updatedHidden = true;
    String updatedBeginCoverage = "2003-01-01";
    String updatedEndCoverage = "2004-01-01";

    mockConfiguration(CONFIGURATION_STUB_FILE, getWiremockUrl());

    UrlPathPattern urlPattern = new UrlPathPattern(new EqualToPattern(PACKAGE_BY_ID_URL), false);
    EqualToJsonPattern putBodyPattern = new EqualToJsonPattern(readFile("requests/rmapi/packages/put-package-is-selected-multiple-attributes.json"), true, true);

    ObjectMapper mapper = new ObjectMapper();
    PackageByIdData packageData = mapper.readValue(getFile(PACKAGE_STUB_FILE), PackageByIdData.class);
    String initialPackageValue = mapper.writeValueAsString(packageData);

    packageData = packageData.toByIdBuilder()
      .isSelected(updatedSelected)
      .customCoverage(CoverageDates.builder()
        .beginCoverage(updatedBeginCoverage)
        .endCoverage(updatedEndCoverage)
        .build())
      .allowEbscoToAddTitles(updatedAllowEbscoToAddTitles)
      .visibilityData(packageData.getVisibilityData().toBuilder().isHidden(updatedHidden).build())
      .build();

    String updatedPackageValue = mapper.writeValueAsString(packageData);
    mockUpdateScenario(urlPattern, initialPackageValue, updatedPackageValue);

    Package aPackage = sendPutRequestAndRetrieveResponse(
      PACKAGES_PATH,
      readFile("requests/kb-ebsco/package/put-package-selected-multiple-attributes"),
      Package.class);

    verify(putRequestedFor(urlPattern)
      .withRequestBody(putBodyPattern));

    assertEquals(updatedSelected, aPackage.getData().getAttributes().getIsSelected());
    assertEquals(updatedAllowEbscoToAddTitles, aPackage.getData().getAttributes().getAllowKbToAddTitles());
    assertEquals(updatedHidden, aPackage.getData().getAttributes().getVisibilityData().getIsHidden());
    assertEquals(updatedBeginCoverage, aPackage.getData().getAttributes().getCustomCoverage().getBeginCoverage());
    assertEquals(updatedEndCoverage, aPackage.getData().getAttributes().getCustomCoverage().getEndCoverage());
  }

  @Test
  public void shouldUpdateAllAttributesInCustomPackage() throws URISyntaxException, IOException {
    boolean updatedSelected = true;
    boolean updatedHidden = true;
    String updatedBeginCoverage = "2003-01-01";
    String updatedEndCoverage = "2004-01-01";
    String updatedPackageName = "name of the ages forever and ever";

    mockConfiguration(CONFIGURATION_STUB_FILE, getWiremockUrl());

    UrlPathPattern urlPattern = new UrlPathPattern(new EqualToPattern(PACKAGE_BY_ID_URL), false);
    EqualToJsonPattern putBodyPattern = new EqualToJsonPattern(readFile("requests/rmapi/packages/put-package-custom.json"), true, true);

    ObjectMapper mapper = new ObjectMapper();
    PackageByIdData packageData = mapper.readValue(getFile(CUSTOM_PACKAGE_STUB_FILE), PackageByIdData.class);
    String initialPackageValue = mapper.writeValueAsString(packageData);

    packageData = packageData.toByIdBuilder()
      .isSelected(updatedSelected)
      .visibilityData(packageData.getVisibilityData().toBuilder().isHidden(updatedHidden).build())
      .customCoverage(CoverageDates.builder()
        .beginCoverage(updatedBeginCoverage)
        .endCoverage(updatedEndCoverage)
        .build())
      .packageName(updatedPackageName)
      .contentType("AggregatedFullText").build();

    String updatedPackageValue = mapper.writeValueAsString(packageData);
    mockUpdateScenario(urlPattern, initialPackageValue, updatedPackageValue);

    Package aPackage = sendPutRequestAndRetrieveResponse(
      PACKAGES_PATH,
      readFile("requests/kb-ebsco/package/put-package-custom-multiple-attributes"),
      Package.class);

    verify(putRequestedFor(urlPattern)
      .withRequestBody(putBodyPattern));

    assertEquals(updatedSelected, aPackage.getData().getAttributes().getIsSelected());
    assertEquals(updatedHidden, aPackage.getData().getAttributes().getVisibilityData().getIsHidden());
    assertEquals(updatedBeginCoverage, aPackage.getData().getAttributes().getCustomCoverage().getBeginCoverage());
    assertEquals(updatedEndCoverage, aPackage.getData().getAttributes().getCustomCoverage().getEndCoverage());
    assertEquals(updatedPackageName, aPackage.getData().getAttributes().getName());
    assertEquals(ContentType.AGGREGATED_FULL_TEXT, aPackage.getData().getAttributes().getContentType());
  }

  @Test
  public void shouldUpdateOnlyTagsWhenPackageIsNotSelected() throws URISyntaxException, IOException {
    try {
      mockConfiguration(CONFIGURATION_STUB_FILE, getWiremockUrl());
      ObjectMapper mapper = new ObjectMapper();
      PackagePutRequest request = mapper.readValue(
        readFile("requests/kb-ebsco/package/put-package-not-selected-non-empty-fields.json"), PackagePutRequest.class);
      List<String> addedTags = Arrays.asList(STUB_TAG_VALUE, STUB_TAG_VALUE_2);
      request.getData().getAttributes().withTags(new Tags().withTagList(addedTags));

      mockGet(new EqualToPattern(PACKAGE_BY_ID_URL), PACKAGE_STUB_FILE);

      RestAssured.given()
        .spec(getRequestSpecification())
        .header(CONTENT_TYPE_HEADER)
        .when()
        .body(mapper.writeValueAsString(request))
        .put(PACKAGES_PATH)
        .then()
        .statusCode(HttpStatus.SC_OK);

      List<String> tags = TagsTestUtil.getTagsForRecordType(vertx, RecordType.PACKAGE);
      assertThat(tags, containsInAnyOrder(addedTags.toArray()));
      WireMock.verify(0, putRequestedFor(anyUrl()));
    } finally {
      TagsTestUtil.clearTags(vertx);
      PackagesTestUtil.clearPackages(vertx);
    }
  }

  @Test
  public void shouldReturn400WhenRMAPIReturns400() throws URISyntaxException, IOException {
    EqualToPattern urlPattern = new EqualToPattern(
      "/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors/" + STUB_VENDOR_ID + "/packages/"
        + STUB_PACKAGE_ID);

    mockConfiguration(CONFIGURATION_STUB_FILE, getWiremockUrl());

    mockGet(urlPattern, PACKAGE_STUB_FILE);

    mockPut(urlPattern, HttpStatus.SC_BAD_REQUEST);

    RestAssured.given()
      .spec(getRequestSpecification())
      .header(CONTENT_TYPE_HEADER)
      .when()
      .body(readFile("requests/kb-ebsco/package/put-package-selected.json"))
      .put(PACKAGES_PATH)
      .then()
      .statusCode(HttpStatus.SC_BAD_REQUEST);
  }

  @Test
  public void shouldReturn200WhenPackagePostIsValid() throws URISyntaxException, IOException {
    String packagePostStubRequestFile = "requests/kb-ebsco/package/post-package-request.json";
    String packagePostRMAPIRequestFile = "requests/rmapi/packages/post-package.json";
    String actual = sendPostWithTags(readFile(packagePostStubRequestFile), Collections.emptyList());

    String packagePostStubResponseFile = "responses/kb-ebsco/packages/get-created-package-response.json";
    String expected = readFile(packagePostStubResponseFile);

    JSONAssert.assertEquals(expected, actual, false);
    EqualToJsonPattern postBodyPattern = new EqualToJsonPattern(readFile(packagePostRMAPIRequestFile), false, true);
    verify(1, postRequestedFor(new UrlPathPattern(new EqualToPattern(PACKAGES_STUB_URL), false))
      .withRequestBody(postBodyPattern));
  }

  @Test
  public void shouldReturn400WhenPackagePostDataIsInvalid()throws URISyntaxException, IOException{
    String providerStubResponseFile = "responses/rmapi/packages/get-package-provider-by-id.json";
    String packagePostStubRequestFile = "requests/kb-ebsco/package/post-package-request.json";
    String response = "responses/rmapi/packages/post-package-400-error-response.json";

    mockConfiguration(CONFIGURATION_STUB_FILE, getWiremockUrl());

    EqualToJsonPattern postBodyPattern = new EqualToJsonPattern("{\n  \"contentType\" : 1,\n  \"packageName\" : \"TEST_NAME\",\n  \"customCoverage\" : {\n    \"beginCoverage\" : \"2017-12-23\",\n    \"endCoverage\" : \"2018-03-30\"\n  }\n}", false, true);

    mockGet(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors.*"), providerStubResponseFile);
    mockPost(new EqualToPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors/"+ STUB_VENDOR_ID + "/packages"),
      postBodyPattern, response, HttpStatus.SC_BAD_REQUEST);

    RestAssured.given()
      .spec(getRequestSpecification())
      .body(readFile(packagePostStubRequestFile))
      .when()
      .post("eholdings/packages")
      .then()
      .statusCode(HttpStatus.SC_BAD_REQUEST);
  }

  @Test
  public void shouldReturnDefaultResourcesOnGetWithResources() throws IOException, URISyntaxException {

    String packageResourcesUrl = "/eholdings/packages/" + STUB_VENDOR_ID + "-" + STUB_PACKAGE_ID + "/resources";
    String query = "?searchfield=titlename&selection=all&resourcetype=all&searchtype=advanced&search=&offset=1&count=25&orderby=titlename";
    shouldReturnResourcesOnGetWithResources(packageResourcesUrl, query);
  }

  @Test
  public void shouldReturnResourcesWithPagingOnGetWithResources() throws IOException, URISyntaxException {

    String packageResourcesUrl = "/eholdings/packages/" + STUB_VENDOR_ID + "-" + STUB_PACKAGE_ID + "/resources?page=2";
    String query = "?searchfield=titlename&selection=all&resourcetype=all&searchtype=advanced&search=&offset=2&count=25&orderby=titlename";
    shouldReturnResourcesOnGetWithResources(packageResourcesUrl, query);
  }

  @Test
  public void shouldReturn404OnGetWithResourcesWhenPackageNotFound() throws IOException, URISyntaxException {
    mockConfiguration(CONFIGURATION_STUB_FILE, getWiremockUrl());

    mockGet(new RegexPattern(RESOURCES_BY_PACKAGE_ID_URL + ".*"), HttpStatus.SC_NOT_FOUND);

    JsonapiError error = getResponseWithStatus("/eholdings/packages/" + STUB_VENDOR_ID + "-" + STUB_PACKAGE_ID + "/resources",
      HttpStatus.SC_NOT_FOUND).as(JsonapiError.class);

    assertThat(error.getErrors().get(0).getTitle(), is("Package not found"));
  }

  @Test
  public void shouldReturn400OnGetWithResourcesWhenCountOutOfRange() throws IOException, URISyntaxException {
    mockConfiguration(CONFIGURATION_STUB_FILE, getWiremockUrl());

    String packageResourcesUrl = "/eholdings/packages/" + STUB_VENDOR_ID + "-" + STUB_PACKAGE_ID + "/resources?count=500";

    getResponse(packageResourcesUrl).statusCode(HttpStatus.SC_BAD_REQUEST);
  }

  @Test
  public void shouldReturn400OnGetWithResourcesWhenRMAPI400() throws IOException, URISyntaxException {
    String stubResponseFile = "responses/rmapi/packages/get-package-resources-400-response.json";

    mockConfiguration(CONFIGURATION_STUB_FILE, getWiremockUrl());

    stubFor(
      get(
        new UrlPathPattern(new RegexPattern(
          PACKAGE_BY_ID_URL + "/titles.*" ),
          true))
        .willReturn(new ResponseDefinitionBuilder()
          .withBody(readFile(stubResponseFile))
          .withStatus(HttpStatus.SC_BAD_REQUEST)));

    JsonapiError error = getResponseWithStatus("/eholdings/packages/" + STUB_VENDOR_ID + "-" + STUB_PACKAGE_ID + "/resources",
      HttpStatus.SC_BAD_REQUEST).as(JsonapiError.class);

    assertThat(error.getErrors().get(0).getTitle(), is("Parameter Count is outside the range 1-100."));
  }


  @Test
  public void shouldReturnUnauthorizedOnGetWithResourcesWhenRMAPI401() throws IOException, URISyntaxException {
    mockConfiguration(CONFIGURATION_STUB_FILE, getWiremockUrl());

    mockGet(new RegexPattern(PACKAGE_BY_ID_URL + "/titles.*" ), HttpStatus.SC_UNAUTHORIZED);

    JsonapiError error = getResponseWithStatus("/eholdings/packages/" + STUB_VENDOR_ID + "-" + STUB_PACKAGE_ID + "/resources",
      HttpStatus.SC_FORBIDDEN).as(JsonapiError.class);
    assertThat(error.getErrors().get(0).getTitle(), containsString("Unauthorized Access"));
  }

  @Test
  public void shouldReturnUnauthorizedOnGetWithResourcesWhenRMAPI403() throws IOException, URISyntaxException {
    mockConfiguration(CONFIGURATION_STUB_FILE, getWiremockUrl());

    mockGet(new RegexPattern(PACKAGE_BY_ID_URL + "/titles.*" ), HttpStatus.SC_FORBIDDEN);

    JsonapiError error = getResponseWithStatus("/eholdings/packages/" + STUB_VENDOR_ID + "-" + STUB_PACKAGE_ID + "/resources",
      HttpStatus.SC_FORBIDDEN).as(JsonapiError.class);
    assertThat(error.getErrors().get(0).getTitle(), containsString("Unauthorized Access"));
  }

  private void shouldReturnResourcesOnGetWithResources(String getURL, String rmAPIQuery) throws IOException, URISyntaxException {

    mockConfiguration(CONFIGURATION_STUB_FILE, getWiremockUrl());

    mockResourceById(RESOURCES_BY_PACKAGE_ID_STUB_FILE);

    String actual = getResponseWithStatus(getURL, 200).asString();
    String expected = readFile(EXPECTED_RESOURCES_STUB_FILE);

    JSONAssert.assertEquals(expected, actual, false);

    verify(1, getRequestedFor(urlEqualTo(RESOURCES_BY_PACKAGE_ID_URL + rmAPIQuery)));
  }

  private void mockResourceById(String stubFile) throws IOException, URISyntaxException {
    mockGet(new RegexPattern(RESOURCES_BY_PACKAGE_ID_URL + ".*" ), stubFile);
  }

  private void mockUpdateScenario(UrlPathPattern urlPattern, String initialPackage, String updatedPackage){
    stubFor(
      get(urlPattern)
        .inScenario(GET_PACKAGE_SCENARIO)
        .willReturn(new ResponseDefinitionBuilder()
          .withBody(initialPackage)));

    stubFor(
      put(urlPattern)
        .inScenario(GET_PACKAGE_SCENARIO)
        .willReturn(new ResponseDefinitionBuilder()
          .withStatus(HttpStatus.SC_NO_CONTENT))
        .willSetStateTo(PACKAGED_UPDATED_STATE));

    stubFor(
      get(urlPattern)
        .inScenario(GET_PACKAGE_SCENARIO)
        .whenScenarioStateIs(PACKAGED_UPDATED_STATE)
        .willReturn(new ResponseDefinitionBuilder()
          .withBody(updatedPackage)));
  }

  private void sendPutWithTags(List<String> newTags) throws IOException, URISyntaxException {
    sendPutWithTags(Json.decodeValue(readFile(PACKAGE_STUB_FILE), PackageByIdData.class), newTags);
  }

  private void sendPutWithTags(PackageByIdData mockUpdatedPackage, List<String> newTags) throws IOException, URISyntaxException {
    ObjectMapper mapper = new ObjectMapper();

    mockConfiguration(CONFIGURATION_STUB_FILE, getWiremockUrl());

    UrlPathPattern urlPattern = new UrlPathPattern(new EqualToPattern(PACKAGE_BY_ID_URL), false);
    PackageByIdData packageData = mapper.readValue(getFile(PACKAGE_STUB_FILE), PackageByIdData.class);
    String initialPackageValue = mapper.writeValueAsString(packageData);
    String updatedPackageValue = mapper.writeValueAsString(mockUpdatedPackage);
    mockUpdateScenario(urlPattern, initialPackageValue, updatedPackageValue);

    PackagePutRequest packageToBeUpdated = mapper.readValue(getFile("requests/kb-ebsco/package/put-package-selected.json"), PackagePutRequest.class);

    if(newTags != null) {
      packageToBeUpdated.getData().getAttributes().setTags(new Tags()
        .withTagList(newTags));
    }

    sendPutRequestAndRetrieveResponse(PACKAGES_PATH, mapper.writeValueAsString(packageToBeUpdated), Package.class);
  }

  private String sendPostWithTags(String requestBody, List<String> tags) throws IOException, URISyntaxException {
    String providerStubResponseFile = "responses/rmapi/packages/get-package-provider-by-id.json";
    String packageCreatedIdStubResponseFile = "responses/rmapi/packages/post-package-response.json";
    String packageByIdStubResponseFile = "responses/rmapi/packages/get-package-by-id-response.json";

    mockConfiguration(CONFIGURATION_STUB_FILE, getWiremockUrl());

    mockGet(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/"), providerStubResponseFile);
    mockPost(new EqualToPattern(PACKAGES_STUB_URL), new AnythingPattern(), packageCreatedIdStubResponseFile, HttpStatus.SC_OK);
    mockGet(new EqualToPattern(PACKAGE_BY_ID_URL), packageByIdStubResponseFile);

    ObjectMapper mapper = new ObjectMapper();
    PackagePostRequest request = mapper.readValue(requestBody, PackagePostRequest.class);
    request.getData().getAttributes()
      .withTags(new Tags().withTagList(tags));
    return RestAssured.given()
      .spec(getRequestSpecification())
      .header(CONTENT_TYPE_HEADER)
      .body(mapper.writeValueAsString(request))
      .when()
      .post("eholdings/packages")
      .then()
      .statusCode(HttpStatus.SC_OK).extract().body().asString();
  }

  private void mockPackageWithName(int stubPackageId, String stubPackageName) throws IOException, URISyntaxException {
    mockGetWithBody(new RegexPattern(".*vendors/"+STUB_VENDOR_ID+"/packages/" + stubPackageId),
      getPackageResponse(stubPackageName, stubPackageId));
  }

  private String getPackageResponse(String packageName, int packageId) throws IOException, URISyntaxException {
    PackageByIdData packageData = Json.decodeValue(readFile("responses/rmapi/packages/get-package-by-id-response.json"), PackageByIdData.class);
    return Json.encode(packageData.toByIdBuilder()
      .packageName(packageName)
      .packageId(packageId)
      .vendorId(STUB_VENDOR_ID)
      .build());
  }

  private PackagesTestUtil.DbPackage buildDbPackage(String id, String name) {
    return PackagesTestUtil.DbPackage.builder()
      .id(String.valueOf(id))
      .name(name)
      .contentType(STUB_PACKAGE_CONTENT_TYPE).build();
  }

  private void setUpTaggedPackages() throws IOException, URISyntaxException {
    PackagesTestUtil.addPackage(vertx, buildDbPackage(FULL_PACKAGE_ID, STUB_PACKAGE_NAME));
    PackagesTestUtil.addPackage(vertx, buildDbPackage(FULL_PACKAGE_ID_2, STUB_PACKAGE_NAME_2));
    PackagesTestUtil.addPackage(vertx, buildDbPackage(FULL_PACKAGE_ID_3, STUB_PACKAGE_NAME_3));

    mockConfiguration(CONFIGURATION_STUB_FILE, getWiremockUrl());

    mockPackageWithName(STUB_PACKAGE_ID, STUB_PACKAGE_NAME);
    mockPackageWithName(STUB_PACKAGE_ID_2, STUB_PACKAGE_NAME_2);
    mockPackageWithName(STUB_PACKAGE_ID_3, STUB_PACKAGE_NAME_3);
  }
}
