package org.folio.rest.impl;

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
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import static org.folio.util.TestUtil.getFile;
import static org.folio.util.TestUtil.mockConfiguration;
import static org.folio.util.TestUtil.mockGet;
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
import com.github.tomakehurst.wiremock.matching.EqualToJsonPattern;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import com.github.tomakehurst.wiremock.matching.RegexPattern;
import com.github.tomakehurst.wiremock.matching.UrlPathPattern;
import io.restassured.RestAssured;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.apache.http.HttpStatus;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.JSONAssert;

import org.folio.rest.jaxrs.model.ContentType;
import org.folio.rest.jaxrs.model.JsonapiError;
import org.folio.rest.jaxrs.model.Package;
import org.folio.rest.jaxrs.model.PackagePutRequest;
import org.folio.rest.jaxrs.model.ResourceCollection;
import org.folio.rest.jaxrs.model.Tags;
import org.folio.rmapi.model.CoverageDates;
import org.folio.rmapi.model.PackageByIdData;
import org.folio.rmapi.model.PackageData;
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
  private static final int STUB_VENDOR_ID = 111111;
  private static final String PACKAGES_PATH = "eholdings/packages/" + STUB_VENDOR_ID + "-" + STUB_PACKAGE_ID;
  private static final String PACKAGE_BY_ID_URL = "/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors/" + STUB_VENDOR_ID + "/packages/" + STUB_PACKAGE_ID;
  private static final String RESOURCES_BY_PACKAGE_ID_URL = PACKAGE_BY_ID_URL + "/titles";
  private static final String PACKAGED_UPDATED_STATE = "Packaged updated";
  private static final String GET_PACKAGE_SCENARIO = "Get package";
  private static final String STUB_TAG = "test tag";
  private static final String STUB_TAG_VALUE = "tag one";
  private static final String STUB_TAG_VALUE_2 = "tag 2";

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
      String packageId = STUB_VENDOR_ID + "-" + STUB_PACKAGE_ID;
      TagsTestUtil.insertTag(vertx, packageId, RecordType.PACKAGE, STUB_TAG);
      mockConfiguration("responses/kb-ebsco/configuration/get-configuration.json", getWiremockUrl());

      mockGet(new RegexPattern(PACKAGE_BY_ID_URL), CUSTOM_PACKAGE_STUB_FILE);

      Package packageData = getOkResponse("eholdings/packages/" + packageId).as(Package.class);

      assertTrue(packageData.getData().getAttributes().getTags().getTagList().contains(STUB_TAG));
    }
    finally {
      TagsTestUtil.clearTags(vertx);
    }
  }

  @Test
  public void shouldDeleteAndAddPackageTagsOnPut() throws IOException, URISyntaxException {
    try {
      String packageId = STUB_VENDOR_ID + "-" + STUB_PACKAGE_ID;
      TagsTestUtil.insertTag(vertx, packageId, RecordType.PACKAGE, "test one");
      List<String> newTags = Arrays.asList(STUB_TAG_VALUE, STUB_TAG_VALUE_2);
      sendPutWithTags(Arrays.asList(STUB_TAG_VALUE, STUB_TAG_VALUE_2));
      List<String> tagsAfterRequest = TagsTestUtil.getTags(vertx, RecordType.PACKAGE);
      assertThat(tagsAfterRequest, containsInAnyOrder(newTags.toArray()));
    } finally {
      TagsTestUtil.clearTags(vertx);
    }
  }

  @Test
  public void shouldAddPackageTagsOnPutWhenPackageAlreadyHasTags() throws IOException, URISyntaxException {
    try {
      String packageId = STUB_VENDOR_ID + "-" + STUB_PACKAGE_ID;
      TagsTestUtil.insertTag(vertx, packageId, RecordType.PACKAGE, STUB_TAG_VALUE);
      List<String> newTags = Arrays.asList(STUB_TAG_VALUE, STUB_TAG_VALUE_2);
      sendPutWithTags(Arrays.asList(STUB_TAG_VALUE, STUB_TAG_VALUE_2));
      List<String> tagsAfterRequest = TagsTestUtil.getTags(vertx, RecordType.PACKAGE);
      assertThat(tagsAfterRequest, containsInAnyOrder(newTags.toArray()));
    } finally {
      TagsTestUtil.clearTags(vertx);
    }
  }

  @Test
  public void shouldAddPackageTagsOnPostWhenPackageAlreadyHasTags() throws IOException, URISyntaxException {
    try {
      List<String> newTags = Arrays.asList(STUB_TAG_VALUE, STUB_TAG_VALUE_2);
      sendPutWithTags(Arrays.asList(STUB_TAG_VALUE, STUB_TAG_VALUE_2));
      List<String> tagsAfterRequest = TagsTestUtil.getTags(vertx, RecordType.PACKAGE);
      assertThat(tagsAfterRequest, containsInAnyOrder(newTags.toArray()));
    } finally {
      TagsTestUtil.clearTags(vertx);
    }
  }

  @Test
  public void shouldDeleteAllPackageTagsOnPutWhenRequestHasEmptyListOfTags() throws IOException, URISyntaxException {
      String packageId = STUB_VENDOR_ID + "-" + STUB_PACKAGE_ID;
      TagsTestUtil.insertTag(vertx, packageId, RecordType.PACKAGE, "test one");
      sendPutWithTags(Collections.emptyList());
      List<String> tagsAfterRequest = TagsTestUtil.getTags(vertx, RecordType.PACKAGE);
      assertThat(tagsAfterRequest, empty());
  }

  @Test
  public void shouldDoNothingOnPutWhenRequestHasNotTags() throws IOException, URISyntaxException {
    sendPutWithTags(null);
    List<String> tagsAfterRequest = TagsTestUtil.getTags(vertx, RecordType.PACKAGE);
    assertThat(tagsAfterRequest, empty());
  }

  @Test
  public void shouldDeletePackageTagsOnDelete() throws IOException, URISyntaxException {
      String packageId = STUB_VENDOR_ID + "-" + STUB_PACKAGE_ID;
      TagsTestUtil.insertTag(vertx, packageId, RecordType.PACKAGE, "test one");

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

      List<String> tagsAfterRequest = TagsTestUtil.getTags(vertx, RecordType.PACKAGE);
      assertThat(tagsAfterRequest, empty());
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
    getResponseWithStatus("eholdings/packages?q=American&filter[type]=abstractandindex&count=500",
      HttpStatus.SC_BAD_REQUEST);
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
  public void shouldReturn422WhenPackageIsNotSelectedAndIsHiddenIsTrue() throws URISyntaxException, IOException {
    mockConfiguration(CONFIGURATION_STUB_FILE, getWiremockUrl());

    mockGet(new EqualToPattern(PACKAGE_BY_ID_URL), PACKAGE_STUB_FILE);

    RestAssured.given()
      .spec(getRequestSpecification())
      .header(CONTENT_TYPE_HEADER)
      .when()
      .body(readFile("requests/kb-ebsco/package/put-package-not-selected-non-empty-fields.json"))
      .put(PACKAGES_PATH)
      .then()
      .statusCode(HttpStatus.SC_UNPROCESSABLE_ENTITY);
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
    String providerStubResponseFile = "responses/rmapi/packages/get-package-provider-by-id.json";
    String packagePostStubRequestFile = "requests/kb-ebsco/package/post-package-request.json";
    String packageCreatedIdStubResponseFile = "responses/rmapi/packages/post-package-response.json";
    String packageByIdStubResponseFile = "responses/rmapi/packages/get-package-by-id-response.json";
    String packagePostStubResponseFile = "responses/kb-ebsco/packages/get-created-package-response.json";

    mockConfiguration(CONFIGURATION_STUB_FILE, getWiremockUrl());

    final EqualToPattern packagesUrl = new EqualToPattern(
      "/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors/" + STUB_VENDOR_ID + "/packages");
    UrlPathPattern postPackagePattern = new UrlPathPattern(packagesUrl, false);

    EqualToJsonPattern postBodyPattern = new EqualToJsonPattern("{\n  \"contentType\" : 1,\n  \"packageName\" : \"TEST_NAME\",\n  \"customCoverage\" : {\n    \"beginCoverage\" : \"2017-12-23\",\n    \"endCoverage\" : \"2018-03-30\"\n  }\n}", false, true);

    mockGet(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/"), providerStubResponseFile);
    mockPost(packagesUrl, postBodyPattern, packageCreatedIdStubResponseFile, HttpStatus.SC_OK);
    mockGet(new EqualToPattern(PACKAGE_BY_ID_URL), packageByIdStubResponseFile);

    String actual = RestAssured.given()
      .spec(getRequestSpecification())
      .header(CONTENT_TYPE_HEADER)
      .body(readFile(packagePostStubRequestFile))
      .when()
      .post("eholdings/packages")
      .then()
      .statusCode(HttpStatus.SC_OK).extract().body().asString();

    String expected = readFile(packagePostStubResponseFile);

    JSONAssert.assertEquals(expected, actual, false);
    verify(1, postRequestedFor(postPackagePattern).withRequestBody(postBodyPattern));
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
  }

  @Test
  public void shouldReturnUnauthorizedOnGetWithResourcesWhenRMAPI403() throws IOException, URISyntaxException {
    mockConfiguration(CONFIGURATION_STUB_FILE, getWiremockUrl());

    mockGet(new RegexPattern(PACKAGE_BY_ID_URL + "/titles.*" ), HttpStatus.SC_FORBIDDEN);

    JsonapiError error = getResponseWithStatus("/eholdings/packages/" + STUB_VENDOR_ID + "-" + STUB_PACKAGE_ID + "/resources",
      HttpStatus.SC_FORBIDDEN).as(JsonapiError.class);
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
    ObjectMapper mapper = new ObjectMapper();

    mockConfiguration(CONFIGURATION_STUB_FILE, getWiremockUrl());

    UrlPathPattern urlPattern = new UrlPathPattern(new EqualToPattern(PACKAGE_BY_ID_URL), false);
    PackageByIdData packageData = mapper.readValue(getFile(PACKAGE_STUB_FILE), PackageByIdData.class);
    String initialPackageValue = mapper.writeValueAsString(packageData);
    String updatedPackageValue = mapper.writeValueAsString(packageData);
    mockUpdateScenario(urlPattern, initialPackageValue, updatedPackageValue);

    PackagePutRequest packageToBeUpdated = mapper.readValue(getFile("requests/kb-ebsco/package/put-package-selected.json"), PackagePutRequest.class);

    if(newTags != null) {
      packageToBeUpdated.getData().getAttributes().setTags(new Tags()
        .withTagList(newTags));
    }

    sendPutRequestAndRetrieveResponse(PACKAGES_PATH, mapper.writeValueAsString(packageToBeUpdated), Package.class);
  }
}
