package org.folio.rest.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.EqualToJsonPattern;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import com.github.tomakehurst.wiremock.matching.RegexPattern;
import com.github.tomakehurst.wiremock.matching.UrlPathPattern;
import io.restassured.RestAssured;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.apache.http.HttpStatus;
import org.folio.rest.jaxrs.model.*;
import org.folio.rest.jaxrs.model.Package;
import org.folio.rmapi.model.CoverageDates;
import org.folio.rmapi.model.PackageByIdData;
import org.folio.rmapi.model.PackageData;
import org.folio.util.TestUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.JSONAssert;

import java.io.IOException;
import java.net.URISyntaxException;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.folio.rest.util.RestConstants.PACKAGES_TYPE;
import static org.folio.util.TestUtil.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;

@RunWith(VertxUnitRunner.class)
public class EholdingsPackagesTest extends WireMockTestBase {

  private static final String PACKAGE_STUB_FILE = "responses/rmapi/packages/get-package-by-id-response.json";
  private static final String CUSTOM_PACKAGE_STUB_FILE = "responses/rmapi/packages/get-custom-package-by-id-response.json";
  private static final String CONFIGURATION_STUB_FILE = "responses/configuration/get-configuration.json";
  private static final int STUB_PACKAGE_ID = 3964;
  private static final int STUB_VENDOR_ID = 111111;
  private static final String PACKAGED_UPDATED_STATE = "Packaged updated";
  private static final String GET_PACKAGE_SCENARIO = "Get package";

  @Test
  public void shouldReturnPackagesOnGet() throws IOException, URISyntaxException {
    String stubResponseFile = "responses/rmapi/packages/get-packages-response.json";

    mockConfiguration(CONFIGURATION_STUB_FILE, getWiremockUrl());

    stubFor(
      get(
        new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/packages.*"),
          true))
        .willReturn(new ResponseDefinitionBuilder()
          .withBody(readFile(stubResponseFile))));

    PackageCollection packages = RestAssured.given()
      .spec(getRequestSpecification())
      .when()
      .get("eholdings/packages?q=American&filter[type]=abstractandindex&count=5")
      .then()
      .statusCode(HttpStatus.SC_OK).extract().as(PackageCollection.class);

    comparePackages(packages, PackagesTestData.getExpectedCollectionPackageItem());
  }

  @Test
  public void shouldReturnPackagesOnGetWithPackageId() throws IOException, URISyntaxException {
    String packagesStubResponseFile = "responses/rmapi/packages/get-packages-by-provider-id.json";
    String providerByCustIdStubResponseFile = "responses/rmapi/packages/get-package-provider-by-id.json";

    mockConfiguration(CONFIGURATION_STUB_FILE, getWiremockUrl());

    UrlPathPattern vendorsPattern = new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors.*"), true);
    UrlPathPattern packagesByProviderIdPattern = new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors/" + STUB_VENDOR_ID + "/packages.*"), true);

    stubFor(
      get(vendorsPattern)
        .willReturn(new ResponseDefinitionBuilder()
          .withBody(readFile(providerByCustIdStubResponseFile))));

    stubFor(
      get(packagesByProviderIdPattern)
        .willReturn(new ResponseDefinitionBuilder()
          .withBody(readFile(packagesStubResponseFile))));

    PackageCollection packages = RestAssured.given()
      .spec(getRequestSpecification())
      .when()
      .get("eholdings/packages?q=a&count=5&page=1&filter[custom]=true")
      .then()
      .statusCode(HttpStatus.SC_OK).extract().as(PackageCollection.class);

    comparePackages(packages, PackagesTestData.getExpectedPackageCollection());
  }

  @Test
  public void shouldReturnPackagesOnGetById() throws IOException, URISyntaxException {
    mockConfiguration("responses/configuration/get-configuration.json", getWiremockUrl());

    stubFor(
      get(
        new UrlPathPattern(new RegexPattern(
          "/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors/" + STUB_VENDOR_ID + "/packages/" + STUB_PACKAGE_ID),
          true))
        .willReturn(new ResponseDefinitionBuilder()
          .withBody(readFile(CUSTOM_PACKAGE_STUB_FILE))));

    Package packageData = RestAssured.given()
      .spec(getRequestSpecification())
      .when()
      .get("eholdings/packages/" + STUB_VENDOR_ID + "-" + STUB_PACKAGE_ID)
      .then()
      .statusCode(HttpStatus.SC_OK).extract().as(Package.class);

    comparePackage(packageData, PackagesTestData.getExpectedPackage());
  }

  @Test
  public void shouldReturn404WhenPackageIsNotFoundOnRMAPI() throws IOException, URISyntaxException {
    mockConfiguration("responses/configuration/get-configuration.json", getWiremockUrl());

    stubFor(
      get(
        new UrlPathPattern(new RegexPattern(
          "/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors/" + STUB_VENDOR_ID + "/packages/" + STUB_PACKAGE_ID),
          true))
        .willReturn(new ResponseDefinitionBuilder()
          .withStatus(HttpStatus.SC_NOT_FOUND)));

    RestAssured.given()
      .spec(getRequestSpecification())
      .when()
      .get("eholdings/packages/" + STUB_VENDOR_ID + "-" + STUB_PACKAGE_ID)
      .then()
      .statusCode(HttpStatus.SC_NOT_FOUND);
  }

  @Test
  public void shouldReturn400WhenCountInvalid() {
    RestAssured.given()
      .spec(getRequestSpecification())
      .when()
      .get("eholdings/packages?q=American&filter[type]=abstractandindex&count=500")
      .then()
      .statusCode(HttpStatus.SC_BAD_REQUEST);
  }

  @Test
  public void shouldSendDeleteRequestForPackage() throws IOException, URISyntaxException {

    mockConfiguration(CONFIGURATION_STUB_FILE, getWiremockUrl());

    UrlPathPattern packageUrlPattern = new UrlPathPattern(new EqualToPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors/" + STUB_VENDOR_ID + "/packages/" + STUB_PACKAGE_ID), false);
    EqualToJsonPattern putBodyPattern = new EqualToJsonPattern("{\"isSelected\":false}", true, true);

    stubFor(
      get(packageUrlPattern)
        .willReturn(new ResponseDefinitionBuilder()
          .withBody(readFile(CUSTOM_PACKAGE_STUB_FILE))));

    stubFor(
      put(packageUrlPattern)
        .withRequestBody(putBodyPattern)
        .willReturn(new ResponseDefinitionBuilder()
          .withStatus(HttpStatus.SC_NO_CONTENT)));

    RestAssured.given()
      .spec(getRequestSpecification())
      .when()
      .delete("eholdings/packages/" + STUB_VENDOR_ID + "-" + STUB_PACKAGE_ID)
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
      get(new UrlPathPattern(new EqualToPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors/" + STUB_VENDOR_ID + "/packages/" + STUB_PACKAGE_ID), false))
        .willReturn(new ResponseDefinitionBuilder()
          .withBody(mapper.writeValueAsString(packageData))));

    RestAssured.given()
      .spec(getRequestSpecification())
      .when()
      .delete("eholdings/packages/" + STUB_VENDOR_ID + "-" + STUB_PACKAGE_ID)
      .then()
      .statusCode(HttpStatus.SC_BAD_REQUEST);
  }

  @Test
  public void shouldReturn200WhenSelectingPackage() throws URISyntaxException, IOException {
    boolean updatedIsSelected = true;

    mockConfiguration(CONFIGURATION_STUB_FILE, getWiremockUrl());

    UrlPathPattern urlPattern = new UrlPathPattern(new EqualToPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors/" + STUB_VENDOR_ID + "/packages/" + STUB_PACKAGE_ID), false);
    EqualToJsonPattern putBodyPattern = new EqualToJsonPattern(readFile("requests/rmapi/packages/put-package-is-selected.json"), true, true);

    ObjectMapper mapper = new ObjectMapper();
    PackageByIdData packageData = mapper.readValue(getFile(PACKAGE_STUB_FILE), PackageByIdData.class);
    String initialPackageValue = mapper.writeValueAsString(packageData);
    packageData = packageData.toByIdBuilder().isSelected(updatedIsSelected).build();
    String updatedPackageValue = mapper.writeValueAsString(packageData);
    mockUpdateScenario(urlPattern, initialPackageValue, updatedPackageValue);

    Package aPackage = RestAssured.given()
      .spec(getRequestSpecification())
      .header(CONTENT_TYPE_HEADER)
      .when()
      .body(readFile("requests/kb-ebsco/package/put-package-selected.json"))
      .put("eholdings/packages/" + STUB_VENDOR_ID + "-" + STUB_PACKAGE_ID)
      .then()
      .statusCode(HttpStatus.SC_OK)
      .extract().as(Package.class);

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

    UrlPathPattern urlPattern = new UrlPathPattern(new EqualToPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors/" + STUB_VENDOR_ID + "/packages/" + STUB_PACKAGE_ID), false);
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

    Package aPackage = RestAssured.given()
      .spec(getRequestSpecification())
      .header(CONTENT_TYPE_HEADER)
      .when()
      .body(readFile("requests/kb-ebsco/package/put-package-selected-multiple-attributes"))
      .put("eholdings/packages/" + STUB_VENDOR_ID + "-" + STUB_PACKAGE_ID)
      .then()
      .statusCode(HttpStatus.SC_OK)
      .extract().as(Package.class);

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

    UrlPathPattern urlPattern = new UrlPathPattern(new EqualToPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors/" + STUB_VENDOR_ID + "/packages/" + STUB_PACKAGE_ID), false);
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

    Package aPackage = RestAssured.given()
      .spec(getRequestSpecification())
      .header(CONTENT_TYPE_HEADER)
      .when()
      .body(readFile("requests/kb-ebsco/package/put-package-custom-multiple-attributes"))
      .put("eholdings/packages/" + STUB_VENDOR_ID + "-" + STUB_PACKAGE_ID)
      .then()
      .statusCode(HttpStatus.SC_OK)
      .extract().as(Package.class);

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

    stubFor(
      get(new UrlPathPattern(new EqualToPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors/" + STUB_VENDOR_ID + "/packages/" + STUB_PACKAGE_ID), false))
        .willReturn(new ResponseDefinitionBuilder()
          .withBody(readFile(PACKAGE_STUB_FILE))));

    RestAssured.given()
      .spec(getRequestSpecification())
      .header(CONTENT_TYPE_HEADER)
      .when()
      .body(readFile("requests/kb-ebsco/package/put-package-not-selected-non-empty-fields.json"))
      .put("eholdings/packages/" + STUB_VENDOR_ID + "-" + STUB_PACKAGE_ID)
      .then()
      .statusCode(HttpStatus.SC_UNPROCESSABLE_ENTITY);
  }

  @Test
  public void shouldReturn400WhenRMAPIReturns400() throws URISyntaxException, IOException {
    UrlPathPattern urlPattern = new UrlPathPattern(new EqualToPattern(
      "/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors/" + STUB_VENDOR_ID + "/packages/"
        + STUB_PACKAGE_ID), false);

    mockConfiguration(CONFIGURATION_STUB_FILE, getWiremockUrl());

    stubFor(
      get(urlPattern)
        .willReturn(new ResponseDefinitionBuilder()
          .withBody(readFile(PACKAGE_STUB_FILE))));

    stubFor(
      put(urlPattern)
        .willReturn(new ResponseDefinitionBuilder()
          .withStatus(HttpStatus.SC_BAD_REQUEST)));

    RestAssured.given()
      .spec(getRequestSpecification())
      .header(CONTENT_TYPE_HEADER)
      .when()
      .body(readFile("requests/kb-ebsco/package/put-package-selected.json"))
      .put("eholdings/packages/" + STUB_VENDOR_ID + "-" + STUB_PACKAGE_ID)
      .then()
      .statusCode(HttpStatus.SC_BAD_REQUEST);
  }

  public void shouldReturn200WhenPackagePostIsValid() throws URISyntaxException, IOException {
    String providerStubResponseFile = "responses/rmapi/packages/get-package-provider-by-id.json";
    String packagePostStubRequestFile = "requests/kb-ebsco/package/post-package-request.json";
    String packageCreatedIdStubResponseFile = "responses/rmapi/packages/post-package-response.json";
    String packageByIdStubResponseFile = "responses/rmapi/packages/get-package-by-id-response.json";
    String packagePostStubResponseFile = "responses/kb-ebsco/package/get-created-package-response.json";

    mockConfiguration(CONFIGURATION_STUB_FILE, getWiremockUrl());

    UrlPathPattern vendorsPattern = new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors.*"), true);
    UrlPathPattern postPackagePattern = new UrlPathPattern(new EqualToPattern(
      "/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors/"+ STUB_VENDOR_ID + "/packages"), false);
    UrlPathPattern packagesByProviderIdPattern = new UrlPathPattern(new EqualToPattern(
      "/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors/" + STUB_VENDOR_ID + "/packages/" + STUB_PACKAGE_ID), false);

    EqualToJsonPattern postBodyPattern = new EqualToJsonPattern("{\n  \"contentType\" : 4,\n  \"packageName\" : \"TEST_NAME\",\n  \"customCoverage\" : {\n    \"beginCoverage\" : \"2017-12-23\",\n    \"endCoverage\" : \"2018-03-30\"\n  }\n}", false, true);

    stubFor(
      get(vendorsPattern)
        .willReturn(new ResponseDefinitionBuilder().withBody(readFile(providerStubResponseFile))));

    stubFor(
      post(postPackagePattern)
        .withRequestBody(postBodyPattern)
        .willReturn(new ResponseDefinitionBuilder()
          .withBody(readFile(packageCreatedIdStubResponseFile))
          .withStatus(HttpStatus.SC_OK)));

    stubFor(
      get(packagesByProviderIdPattern)
        .willReturn(new ResponseDefinitionBuilder()
          .withBody(readFile(packageByIdStubResponseFile))
          .withStatus(HttpStatus.SC_OK)));

    String actual = RestAssured.given()
      .spec(getRequestSpecification())
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

    UrlPathPattern vendorsPattern = new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors.*"), true);
    UrlPathPattern postPackagePattern = new UrlPathPattern(new EqualToPattern(
      "/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors/"+ STUB_VENDOR_ID + "/packages"), false);
    EqualToJsonPattern postBodyPattern = new EqualToJsonPattern("{\n  \"contentType\" : 1,\n  \"packageName\" : \"TEST_NAME\",\n  \"customCoverage\" : {\n    \"beginCoverage\" : \"2017-12-23\",\n    \"endCoverage\" : \"2018-03-30\"\n  }\n}", false, true);

    stubFor(
      get(vendorsPattern)
        .willReturn(new ResponseDefinitionBuilder().withBody(readFile(providerStubResponseFile))));

    stubFor(
      post(postPackagePattern)
        .withRequestBody(postBodyPattern)
        .willReturn(new ResponseDefinitionBuilder()
          .withBody(readFile(response)).withStatus(HttpStatus.SC_BAD_REQUEST)));

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
    String query = "/titles?searchfield=titlename&selection=all&resourcetype=all&searchtype=advanced&search=&offset=1&count=25&orderby=titlename";
    shouldReturnResourcesOnGetWithResources(packageResourcesUrl, query);
  }

  @Test
  public void shouldReturnResourcesWithPagingOnGetWithResources() throws IOException, URISyntaxException {

    String packageResourcesUrl = "/eholdings/packages/" + STUB_VENDOR_ID + "-" + STUB_PACKAGE_ID + "/resources?page=2";
    String query = "/titles?searchfield=titlename&selection=all&resourcetype=all&searchtype=advanced&search=&offset=2&count=25&orderby=titlename";
    shouldReturnResourcesOnGetWithResources(packageResourcesUrl, query);
  }


  @Test
  public void shouldReturn404OnGetWithResourcesWhenPackageNotFound() throws IOException, URISyntaxException {

    TestUtil.mockConfiguration(CONFIGURATION_STUB_FILE, getWiremockUrl());

    stubFor(
      get(
        new UrlPathPattern(new RegexPattern(
          "/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors/" + STUB_VENDOR_ID + "/packages/" + STUB_PACKAGE_ID + "/titles.*" ),
          true))
        .willReturn(new ResponseDefinitionBuilder()
          .withStatus(HttpStatus.SC_NOT_FOUND)));

    JsonapiError error = getResponseWithStatus("/eholdings/packages/" + STUB_VENDOR_ID + "-" + STUB_PACKAGE_ID + "/resources",
      HttpStatus.SC_NOT_FOUND).as(JsonapiError.class);

    assertThat(error.getErrors().get(0).getTitle(), is("Package not found"));
  }

  @Test
  public void shouldReturn400OnGetWithResourcesWhenCountOutOfRange() throws IOException, URISyntaxException {
    TestUtil.mockConfiguration(CONFIGURATION_STUB_FILE, getWiremockUrl());

    String packageResourcesUrl = "/eholdings/packages/" + STUB_VENDOR_ID + "-" + STUB_PACKAGE_ID + "/resources?count=500";

    RestAssured.given()
      .spec(getRequestSpecification())
      .when()
      .get(packageResourcesUrl)
      .then()
      .statusCode(HttpStatus.SC_BAD_REQUEST);

  }

  @Test
  public void shouldReturn400OnGetWithResourcesWhenRMAPI400() throws IOException, URISyntaxException {
    String stubResponseFile = "responses/rmapi/packages/get-package-resources-400-response.json";

    TestUtil.mockConfiguration(CONFIGURATION_STUB_FILE, getWiremockUrl());

    stubFor(
      get(
        new UrlPathPattern(new RegexPattern(
          "/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors/" + STUB_VENDOR_ID + "/packages/" + STUB_PACKAGE_ID + "/titles.*" ),
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
    TestUtil.mockConfiguration(CONFIGURATION_STUB_FILE, getWiremockUrl());
    stubFor(
      get(
        new UrlPathPattern(new RegexPattern(
          "/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors/" + STUB_VENDOR_ID + "/packages/" + STUB_PACKAGE_ID + "/titles.*" ),
          true))
        .willReturn(new ResponseDefinitionBuilder()
          .withStatus(HttpStatus.SC_UNAUTHORIZED)));

    JsonapiError error = getResponseWithStatus("/eholdings/packages/" + STUB_VENDOR_ID + "-" + STUB_PACKAGE_ID + "/resources",
      HttpStatus.SC_FORBIDDEN).as(JsonapiError.class);
  }

  @Test
  public void shouldReturnUnauthorizedOnGetWithResourcesWhenRMAPI403() throws IOException, URISyntaxException {
    TestUtil.mockConfiguration(CONFIGURATION_STUB_FILE, getWiremockUrl());
    stubFor(
      get(
        new UrlPathPattern(new RegexPattern(
          "/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors/" + STUB_VENDOR_ID + "/packages/" + STUB_PACKAGE_ID + "/titles.*" ),
          true))
        .willReturn(new ResponseDefinitionBuilder()
          .withStatus(HttpStatus.SC_FORBIDDEN)));

    JsonapiError error = getResponseWithStatus("/eholdings/packages/" + STUB_VENDOR_ID + "-" + STUB_PACKAGE_ID + "/resources",
      HttpStatus.SC_FORBIDDEN).as(JsonapiError.class);
  }

  private void shouldReturnResourcesOnGetWithResources(String getURL, String rmAPIQuery) throws IOException, URISyntaxException {
    String packageResourcesResponseFile = "responses/rmapi/resources/get-resources-by-package-id-response.json";

    TestUtil.mockConfiguration(CONFIGURATION_STUB_FILE, getWiremockUrl());

    String baseUrl =  "/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors/" + STUB_VENDOR_ID + "/packages/" + STUB_PACKAGE_ID;
    UrlPathPattern packagesResourcesPattern = new UrlPathPattern(new RegexPattern(baseUrl + "/titles.*" ), true);
    WireMock.stubFor(
      WireMock.get(packagesResourcesPattern)
        .willReturn(new ResponseDefinitionBuilder()
          .withBody(TestUtil.readFile(packageResourcesResponseFile))));

    String actual = getResponseWithStatus(getURL, 200).asString();
    String expected = readFile("responses/resources/get-resources-by-package-id-response.json");

    JSONAssert.assertEquals(expected, actual, false);

    verify(1, getRequestedFor(urlEqualTo(baseUrl + rmAPIQuery)));
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

  private void comparePackage(Package actual, Package expected) {
    assertThat(actual.getJsonapi().getVersion(), equalTo(expected.getJsonapi().getVersion()));
    comparePackageItem(actual.getData(), expected.getData());
  }

  private void comparePackages(PackageCollection actual, PackageCollection expected) {
    assertThat(actual.getMeta().getTotalResults(), equalTo(expected.getMeta().getTotalResults()));
    PackageCollectionItem actualItem = actual.getData().get(0);
    PackageCollectionItem expectedItem = expected.getData().get(0);
    comparePackageItem(actualItem, expectedItem);
  }

  private void comparePackageItem(PackageCollectionItem actualItem, PackageCollectionItem expectedItem) {
    assertThat(actualItem.getId(), equalTo(expectedItem.getId()));
    assertThat(actualItem.getType(), equalTo(PACKAGES_TYPE));
    assertThat(actualItem.getAttributes().getName(),
      equalTo(expectedItem.getAttributes().getName()));
    assertThat(actualItem.getAttributes().getPackageId(),
      equalTo(expectedItem.getAttributes().getPackageId()));
    assertThat(actualItem.getAttributes().getIsCustom(),
      equalTo(expectedItem.getAttributes().getIsCustom()));
    assertThat(actualItem.getAttributes().getProviderId(),
      equalTo(expectedItem.getAttributes().getProviderId()));
    assertThat(actualItem.getAttributes().getProviderName(),
      equalTo(expectedItem.getAttributes().getProviderName()));
    assertThat(actualItem.getAttributes().getTitleCount(),
      equalTo(expectedItem.getAttributes().getTitleCount()));
    assertThat(actualItem.getAttributes().getIsSelected(),
      equalTo(expectedItem.getAttributes().getIsSelected()));
    assertThat(actualItem.getAttributes().getSelectedCount(),
      equalTo(expectedItem.getAttributes().getSelectedCount()));
    assertThat(actualItem.getAttributes().getContentType().value(),
      equalTo(expectedItem.getAttributes().getContentType().value()));
    assertThat(actualItem.getAttributes().getIsCustom(),
      equalTo(expectedItem.getAttributes().getIsCustom()));
    assertThat(actualItem.getAttributes().getPackageType(),
      equalTo(expectedItem.getAttributes().getPackageType()));
    assertThat(actualItem.getAttributes().getVisibilityData().getReason(),
      equalTo(expectedItem.getAttributes().getVisibilityData().getReason()));
    assertThat(actualItem.getAttributes().getVisibilityData().getIsHidden(),
      equalTo(expectedItem.getAttributes().getVisibilityData().getIsHidden()));
    assertThat(actualItem.getAttributes().getCustomCoverage().getBeginCoverage(),
      equalTo(expectedItem.getAttributes().getCustomCoverage().getBeginCoverage()));
    assertThat(actualItem.getAttributes().getCustomCoverage().getEndCoverage(),
      equalTo(expectedItem.getAttributes().getCustomCoverage().getEndCoverage()));
    assertThat(actualItem.getAttributes().getAllowKbToAddTitles(),
      equalTo(expectedItem.getAttributes().getAllowKbToAddTitles()));
  }
}
