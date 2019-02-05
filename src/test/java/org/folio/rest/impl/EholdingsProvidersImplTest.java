package org.folio.rest.impl;

import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.folio.rest.util.RestConstants.PROVIDERS_TYPE;
import static org.folio.util.TestUtil.STUB_TENANT;
import static org.folio.util.TestUtil.getFile;
import static org.folio.util.TestUtil.mockConfiguration;
import static org.folio.util.TestUtil.mockGet;
import static org.folio.util.TestUtil.readFile;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.http.HttpStatus;
import org.folio.rest.jaxrs.model.JsonapiError;
import org.folio.rest.jaxrs.model.MetaDataIncluded;
import org.folio.rest.jaxrs.model.PackageCollectionItem;
import org.folio.rest.jaxrs.model.Packages;
import org.folio.rest.jaxrs.model.Provider;
import org.folio.rest.jaxrs.model.ProviderData;
import org.folio.rest.jaxrs.model.ProviderDataAttributes;
import org.folio.rest.jaxrs.model.ProviderPutRequest;
import org.folio.rest.jaxrs.model.Proxy;
import org.folio.rest.jaxrs.model.Relationships;
import org.folio.rest.jaxrs.model.Token;
import org.folio.rest.persist.PostgresClient;
import org.folio.tag.RecordType;
import org.folio.tag.repository.TagTableConstants;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.JSONAssert;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.matching.RegexPattern;
import com.github.tomakehurst.wiremock.matching.UrlPathPattern;

import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;
import io.vertx.ext.unit.junit.VertxUnitRunner;


@RunWith(VertxUnitRunner.class)
public class EholdingsProvidersImplTest extends WireMockTestBase {
  private static final String STUB_VENDOR_ID = "19";
  private static final String STUB_TAG_VALUE = "tag one";

  @Test
  public void shouldReturnProvidersOnGet() throws IOException, URISyntaxException {
    String stubResponseFile = "responses/rmapi/vendors/get-vendors-response.json";
    int expectedTotalResults = 115;
    String id = "131872";
    String name = "Editions de L'Universite de Bruxelles";
    int packagesTotal = 1;
    int packagesSelected = 0;
    boolean supportsCustomPackages = false;
    String token = "sampleToken";

    mockConfiguration(CONFIGURATION_STUB_FILE, getWiremockUrl());
    stubFor(
      get(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors.*"), true))
        .willReturn(new ResponseDefinitionBuilder()
          .withBody(readFile(stubResponseFile))));

    RestAssured.given()
      .spec(getRequestSpecification())
      .when()
      .get("eholdings/providers?q=e&page=1&sort=name")
      .then()
      .statusCode(200)
      .body("meta.totalResults", equalTo(expectedTotalResults))
      .body("data[0].type", equalTo(PROVIDERS_TYPE))
      .body("data[0].id", equalTo(id))
      .body("data[0].attributes.name", equalTo(name))
      .body("data[0].attributes.packagesTotal", equalTo(packagesTotal))
      .body("data[0].attributes.packagesSelected", equalTo(packagesSelected))
      .body("data[0].attributes.supportsCustomPackages", equalTo(supportsCustomPackages))
      .body("data[0].attributes.providerToken.value", equalTo(token));
  }

  @Test
  public void shouldReturnProvidersOnGetWithPackages() throws IOException, URISyntaxException {
    String stubResponseFile = "responses/rmapi/vendors/get-vendor-by-id-response.json";
    String stubPackagesResponseFile = "responses/rmapi/packages/get-packages-by-provider-id.json";

    mockConfiguration(CONFIGURATION_STUB_FILE, getWiremockUrl());
    stubFor(
      get(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors.*"), true))
        .willReturn(new ResponseDefinitionBuilder()
          .withBody(readFile(stubResponseFile))));

    stubFor(
      get(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors/" + STUB_VENDOR_ID + "/packages.*"), true))
        .willReturn(new ResponseDefinitionBuilder()
          .withBody(readFile(stubPackagesResponseFile))));

    Provider expectedProvider = getExpectedProvider(PackagesTestData.getExpectedPackageCollection().getData());
    RequestSpecification requestSpecification = getRequestSpecification();
    String providerByIdEndpoint = "eholdings/providers/" + STUB_VENDOR_ID + "?include=packages";
    Provider actualProvider = RestAssured.given(requestSpecification)
      .when()
      .get(providerByIdEndpoint)
      .then()
      .statusCode(HttpStatus.SC_OK).extract().as(Provider.class);
    compareProviders(actualProvider, expectedProvider);
  }

  @Test
  public void shouldReturnErrorIfParameterInvalid(){

    RestAssured.given()
      .spec(getRequestSpecification())
      .when()
      .get("eholdings/providers?q=e&count=1000")
      .then()
      .statusCode(400)
      .body("errors.first.title", notNullValue());
  }

  @Test
  public void shouldReturn500IfRMApiReturnsError() throws IOException, URISyntaxException {


    String wiremockUrl = getWiremockUrl();
    mockConfiguration(CONFIGURATION_STUB_FILE, wiremockUrl);
    stubFor(
      get(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors.*"), true))
        .willReturn(new ResponseDefinitionBuilder()
          .withStatus(500)));

    RestAssured.given()
      .spec(getRequestSpecification())
      .when()
      .get("eholdings/providers?q=e&count=1")
      .then()
      .statusCode(500)
      .body("errors.first.title", notNullValue());
  }

  @Test
  public void shouldReturnErrorIfSortParameterInvalid() {

    RestAssured.given()
      .spec(getRequestSpecification())
      .when()
      .get("eholdings/providers?q=e&count=10&sort=abc")
      .then()
      .statusCode(400)
      .body("errors.first.title", notNullValue());
  }

  @Test
  public void shouldReturnProviderWhenValidId() throws IOException, URISyntaxException {
    String stubResponseFile = "responses/rmapi/vendors/get-vendor-by-id-response.json";

    mockConfiguration(CONFIGURATION_STUB_FILE, getWiremockUrl());
    stubFor(
      get(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors.*"), true))
        .willReturn(new ResponseDefinitionBuilder()
          .withBody(readFile(stubResponseFile))));

    Provider expected = getExpectedProvider();
    String providerByIdEndpoint = "eholdings/providers/" + STUB_VENDOR_ID;
    RequestSpecification requestSpecification = getRequestSpecification();

    Provider provider = RestAssured.given(requestSpecification)
      .when()
      .get(providerByIdEndpoint)
      .then()
      .statusCode(HttpStatus.SC_OK).extract().as(Provider.class);

    compareProviders(provider, expected);

  }

  @Test
  public void shouldReturnProviderWithTagWhenValidId() throws IOException, URISyntaxException {
    try {
      TagsTestUtil.insertTag(vertx, STUB_VENDOR_ID, RecordType.PROVIDER, STUB_TAG_VALUE);

      String stubResponseFile = "responses/rmapi/vendors/get-vendor-by-id-response.json";

      mockConfiguration(CONFIGURATION_STUB_FILE, getWiremockUrl());
      stubFor(
        get(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors.*"), true))
          .willReturn(new ResponseDefinitionBuilder()
            .withBody(readFile(stubResponseFile))));

      String providerByIdEndpoint = "eholdings/providers/" + STUB_VENDOR_ID;
      RequestSpecification requestSpecification = getRequestSpecification();

      Provider provider = RestAssured.given(requestSpecification)
        .when()
        .get(providerByIdEndpoint)
        .then()
        .statusCode(HttpStatus.SC_OK).extract().as(Provider.class);

      assertTrue(provider.getData().getAttributes().getTags().getTagList().contains(STUB_TAG_VALUE));
    }
    finally {
      TagsTestUtil.clearTags(vertx);
    }
  }

  @Test
  public void shouldReturn404WhenProviderIdNotFound() throws IOException, URISyntaxException {

    mockConfiguration(CONFIGURATION_STUB_FILE, getWiremockUrl());
    stubFor(
      get(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors.*"), true))
        .willReturn(new ResponseDefinitionBuilder()
          .withStatus(HttpStatus.SC_NOT_FOUND)));

    RequestSpecification requestSpecification = getRequestSpecification();
    JsonapiError error = RestAssured.given()
      .spec(requestSpecification)
      .when()
      .get("eholdings/providers/191919")
      .then()
      .statusCode(HttpStatus.SC_NOT_FOUND)
      .extract().as(JsonapiError.class);

    assertThat(error.getErrors().get(0).getTitle(), is("Provider not found"));
  }

  @Test
  public void shouldReturn400WhenInvalidProviderId() throws IOException, URISyntaxException {
    mockConfiguration(CONFIGURATION_STUB_FILE, getWiremockUrl());

    RequestSpecification requestSpecification = getRequestSpecification();
    JsonapiError error = RestAssured.given()
      .spec(requestSpecification)
      .when()
      .get("eholdings/providers/19191919as")
      .then()
      .statusCode(HttpStatus.SC_BAD_REQUEST)
      .extract().as(JsonapiError.class);

    assertThat(error.getErrors().get(0).getTitle(), notNullValue());
  }

  @Test
  public void shouldUpdateAndReturnProviderOnPut() throws IOException, URISyntaxException {
    String stubResponseFile = "responses/rmapi/vendors/get-vendor-updated-response.json";

    mockConfiguration(CONFIGURATION_STUB_FILE, getWiremockUrl());

    stubFor(
      get(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors.*"), true))
        .willReturn(new ResponseDefinitionBuilder().withBody(readFile(stubResponseFile))));

    stubFor(
      put(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors.*"), true))
        .willReturn(new ResponseDefinitionBuilder().withStatus(204)));

    ObjectMapper mapper = new ObjectMapper();
    ProviderPutRequest providerToBeUpdated = mapper.readValue(getFile("requests/kb-ebsco/put-provider.json"),
      ProviderPutRequest.class);

    Provider expected = getUpdatedProvider();
    RequestSpecification requestSpecification = getRequestSpecification();

    String providerByIdEndpoint = "eholdings/providers/" + STUB_VENDOR_ID;

    Provider provider = RestAssured
      .given()
      .spec(requestSpecification)
      .header(CONTENT_TYPE_HEADER)
      .body(mapper.writeValueAsString(providerToBeUpdated))
      .when()
      .put(providerByIdEndpoint)
      .then()
      .statusCode(HttpStatus.SC_OK)
      .extract().as(Provider.class);

    compareProviders(provider, expected);

    verify(1, putRequestedFor(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors.*"), true))
      .withRequestBody(equalToJson(readFile("requests/rmapi/vendors/put-vendor-token-proxy.json"))));
  }

  @Test
  public void shouldReturn400WhenRMAPIErrorOnPut() throws IOException, URISyntaxException {
    String stubResponseFile = "responses/rmapi/vendors/put-vendor-token-not-allowed-response.json";

    mockConfiguration(CONFIGURATION_STUB_FILE, getWiremockUrl());

    stubFor(
      put(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors.*"), true))
        .willReturn(new ResponseDefinitionBuilder().withBody(readFile(stubResponseFile)).withStatus(400)));

    ObjectMapper mapper = new ObjectMapper();
    ProviderPutRequest providerToBeUpdated = mapper.readValue(getFile("requests/kb-ebsco/put-provider.json"),
      ProviderPutRequest.class);

    String providerByIdEndpoint = "eholdings/providers/" + STUB_VENDOR_ID;

    JsonapiError error = RestAssured
      .given()
      .spec(getRequestSpecification())
      .header(CONTENT_TYPE_HEADER)
      .body(mapper.writeValueAsString(providerToBeUpdated))
      .when()
      .put(providerByIdEndpoint)
      .then()
      .statusCode(HttpStatus.SC_BAD_REQUEST)
      .extract().as(JsonapiError.class);

    assertThat(error.getErrors().get(0).getTitle(), equalTo("Provider does not allow token"));

  }

  @Test
  public void shouldReturn422WhenBodyInputInvalidOnPut() throws IOException, URISyntaxException {
    mockConfiguration(CONFIGURATION_STUB_FILE, getWiremockUrl());

    ObjectMapper mapper = new ObjectMapper();
    ProviderPutRequest providerToBeUpdated = mapper.readValue(getFile("requests/kb-ebsco/put-provider.json"),
      ProviderPutRequest.class);

    Token providerToken = new Token();
    providerToken.setValue(RandomStringUtils.randomAlphanumeric(501));

    providerToBeUpdated.getData().getAttributes().setProviderToken(providerToken);

    String providerByIdEndpoint = "eholdings/providers/" + STUB_VENDOR_ID;

    JsonapiError error = RestAssured
      .given()
      .spec(getRequestSpecification())
      .header(CONTENT_TYPE_HEADER)
      .body(mapper.writeValueAsString(providerToBeUpdated))
      .when()
      .put(providerByIdEndpoint)
      .then()
      .statusCode(HttpStatus.SC_UNPROCESSABLE_ENTITY)
      .extract().as(JsonapiError.class);

    assertThat(error.getErrors().get(0).getTitle(), equalTo("Invalid value"));
    assertThat(error.getErrors().get(0).getDetail(), equalTo("Value is too long (maximum is 500 characters)"));

  }

  @Test
  public void shouldReturnProviderPackagesWhenValidId() throws IOException, URISyntaxException {
    String rmapiProviderPackagesUrl = "/rm/rmaccounts.*" + STUB_CUSTOMER_ID + "/vendors/"
      + STUB_VENDOR_ID + "/packages.*";
    String providerPackagesUrl = "eholdings/providers/" + STUB_VENDOR_ID + "/packages";
    String packageStubResponseFile = "responses/rmapi/packages/get-packages-by-provider-id.json";

    mockConfiguration(CONFIGURATION_STUB_FILE, getWiremockUrl());
    mockGet(new RegexPattern(rmapiProviderPackagesUrl), packageStubResponseFile);

    String actual = getResponseWithStatus(providerPackagesUrl, 200).asString();
    String expected = readFile("responses/kb-ebsco/packages/get-packages-by-provider-id-response.json");

    JSONAssert.assertEquals(expected, actual, false);
  }

  @Test
  public void shouldReturn400IfProviderIdInvalid() {
    errorTitleIsNotEmptyWith400Status("eholdings/providers/invalid/packages");
  }

  @Test
  public void shouldReturn400IfCountOutOfRange() {
    errorTitleIsNotEmptyWith400Status("eholdings/providers/" + STUB_VENDOR_ID + "/packages?count=120");
  }

  @Test
  public void shouldReturn400IfFilterTypeInvalid() {
    errorTitleIsNotEmptyWith400Status("eholdings/providers/" + STUB_VENDOR_ID +
      "/packages?q=Search&filter[selected]=true&filter[type]=unsupported");
  }

  @Test
  public void shouldReturn400IfFilterSelectedInvalid() {
    errorTitleIsNotEmptyWith400Status("eholdings/providers/" + STUB_VENDOR_ID +
      "/packages?q=Search&filter[selected]=invalid");
  }

  @Test
  public void shouldReturn400IfPageOffsetInvalid() {
    getResponseWithStatus("eholdings/providers/" + STUB_VENDOR_ID + "/packages?q=Search&count=5&page=abc",
      HttpStatus.SC_BAD_REQUEST);
  }

  @Test
  public void shouldReturn400IfSortInvalid() {
    errorTitleIsNotEmptyWith400Status("eholdings/providers/" +
      STUB_VENDOR_ID + "/packages?q=Search&sort=invalid");
  }

  @Test
  public void shouldReturn400IfQueryParamInvalid() {
    errorTitleIsNotEmptyWith400Status("/eholdings/providers/" + STUB_VENDOR_ID + "/packages?q=");
  }

  @Test
  public void shouldReturn404WhenNonProviderIdNotFound() throws IOException, URISyntaxException {
    String rmapiInvalidProviderIdUrl = "/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors/191919/packages";

    mockConfiguration(CONFIGURATION_STUB_FILE, getWiremockUrl());

    mockGet(new RegexPattern(rmapiInvalidProviderIdUrl), HttpStatus.SC_NOT_FOUND);

    JsonapiError error = getResponseWithStatus("/eholdings/providers/191919/packages",
      HttpStatus.SC_NOT_FOUND).as(JsonapiError.class);

    assertThat(error.getErrors().get(0).getTitle(), is("Provider not found"));
  }

  private void compareProviders(Provider actual, Provider expected) {
    assertThat(actual.getData().getType(), equalTo(expected.getData().getType()));
    assertThat(actual.getData().getId(), equalTo(expected.getData().getId()));
    assertThat(actual.getData().getAttributes().getName(), equalTo(expected.getData().getAttributes().getName()));
    assertThat(actual.getData().getAttributes().getPackagesTotal(),
      equalTo(expected.getData().getAttributes().getPackagesTotal()));
    assertThat(actual.getData().getAttributes().getPackagesSelected(),
      equalTo(expected.getData().getAttributes().getPackagesSelected()));
    assertThat(actual.getData().getAttributes().getSupportsCustomPackages(),
      equalTo(expected.getData().getAttributes().getSupportsCustomPackages()));
    if (expected.getData().getAttributes().getProviderToken() != null) {
      assertThat(actual.getData().getAttributes().getProviderToken().getFactName(),
        equalTo(expected.getData().getAttributes().getProviderToken().getFactName()));
      assertThat(actual.getData().getAttributes().getProviderToken().getHelpText(),
        equalTo(expected.getData().getAttributes().getProviderToken().getHelpText()));
      assertThat(actual.getData().getAttributes().getProviderToken().getPrompt(),
        equalTo(expected.getData().getAttributes().getProviderToken().getPrompt()));
      assertThat(actual.getData().getAttributes().getProviderToken().getValue(),
        equalTo(expected.getData().getAttributes().getProviderToken().getValue()));
    }
    assertThat(actual.getData().getAttributes().getProxy().getId(),
      equalTo(expected.getData().getAttributes().getProxy().getId()));
    assertThat(actual.getData().getAttributes().getProxy().getInherited(),
      equalTo(expected.getData().getAttributes().getProxy().getInherited()));
  }

  private Provider getExpectedProvider(List<PackageCollectionItem> packages) {
    return getExpectedProvider()
      .withIncluded(packages);
  }

  private Provider getExpectedProvider() {

    return new Provider()
      .withData(new ProviderData()
        .withType(PROVIDERS_TYPE)
        .withId(STUB_VENDOR_ID)
        .withAttributes(new ProviderDataAttributes()
          .withName("EBSCO")
          .withPackagesTotal(625)
          .withPackagesSelected(11)
          .withSupportsCustomPackages(false)
          .withProviderToken(new Token()
            .withValue("sampleToken"))
          .withProxy(new Proxy().withId("<n>").withInherited(true)))
        .withRelationships(new Relationships()
          .withPackages(new Packages()
            .withMeta(new MetaDataIncluded()
              .withIncluded(false)))));
  }

  private Provider getUpdatedProvider() {

    return new Provider()
      .withData(new ProviderData()
        .withType(PROVIDERS_TYPE)
        .withId(STUB_VENDOR_ID)
        .withAttributes(new ProviderDataAttributes()
          .withName("EBSCO")
          .withPackagesTotal(625)
          .withPackagesSelected(11)
          .withSupportsCustomPackages(false)
          .withProviderToken(new Token()
            .withFactName("[[galesiteid]]")
            .withHelpText("<ul><li>Enter site id</li></ul>")
            .withPrompt("/itweb/")
            .withValue("My Test Token"))
          .withProxy(new Proxy().withId("<n>").withInherited(false)))
        .withRelationships(new Relationships()
          .withPackages(new Packages()
            .withMeta(new MetaDataIncluded()
              .withIncluded(false)))));
  }

  private void errorTitleIsNotEmptyWith400Status(String resourcePath) {
    RequestSpecification requestSpecification = getRequestSpecification();
    RestAssured.given()
      .spec(requestSpecification)
      .when()
      .get(resourcePath)
      .then()
      .statusCode(HttpStatus.SC_BAD_REQUEST)
      .body("errors.first.title", notNullValue());
  }
}
