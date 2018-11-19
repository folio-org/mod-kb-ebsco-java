package org.folio.rest.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.RegexPattern;
import com.github.tomakehurst.wiremock.matching.UrlPathPattern;
import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.http.HttpStatus;
import org.folio.rest.jaxrs.model.JsonapiError;
import org.folio.rest.jaxrs.model.MetaDataIncluded;
import org.folio.rest.jaxrs.model.Packages;
import org.folio.rest.jaxrs.model.Provider;
import org.folio.rest.jaxrs.model.ProviderData;
import org.folio.rest.jaxrs.model.ProviderDataAttributes;
import org.folio.rest.jaxrs.model.ProviderPutRequest;
import org.folio.rest.jaxrs.model.Proxy;
import org.folio.rest.jaxrs.model.Relationships;
import org.folio.rest.jaxrs.model.Token;
import org.folio.util.TestUtil;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.URISyntaxException;

import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;


@RunWith(VertxUnitRunner.class)
public class EholdingsProvidersImplTest extends WireMockTestBase {
  private static final String STUB_VENDOR_ID = "19";

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

    String wiremockUrl = getWiremockUrl();
    TestUtil.mockConfiguration(CONFIGURATION_STUB_FILE, wiremockUrl);
    WireMock.stubFor(
      WireMock.get(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors.*"), true))
        .willReturn(new ResponseDefinitionBuilder()
          .withBody(TestUtil.readFile(stubResponseFile))));

    RequestSpecification requestSpecification = getRequestSpecification();

    RestAssured.given()
      .spec(requestSpecification)
      .when()
      .get("eholdings/providers?q=e&page=1&sort=name")
      .then()
      .statusCode(200)
      .body("meta.totalResults", equalTo(expectedTotalResults))
      .body("data[0].type", equalTo("providers"))
      .body("data[0].id", equalTo(id))
      .body("data[0].attributes.name", equalTo(name))
      .body("data[0].attributes.packagesTotal", equalTo(packagesTotal))
      .body("data[0].attributes.packagesSelected", equalTo(packagesSelected))
      .body("data[0].attributes.supportsCustomPackages", equalTo(supportsCustomPackages))
      .body("data[0].attributes.providerToken.value", equalTo(token));
  }

  @Test
  public void shouldReturnErrorIfParameterInvalid() {
    RequestSpecification requestSpecification = getRequestSpecification();
    RestAssured.given()
      .spec(requestSpecification)
      .when()
      .get("eholdings/providers?q=e&count=1000")
      .then()
      .statusCode(400)
      .body("errors.first.title", notNullValue());
  }

  @Test
  public void shouldReturn500IfRMApiReturnsError() throws IOException, URISyntaxException {


    String wiremockUrl = getWiremockUrl();
    TestUtil.mockConfiguration(CONFIGURATION_STUB_FILE, wiremockUrl);
    WireMock.stubFor(
      WireMock.get(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors.*"), true))
        .willReturn(new ResponseDefinitionBuilder()
          .withStatus(500)));

    RequestSpecification requestSpecification = getRequestSpecification();
    RestAssured.given()
      .spec(requestSpecification)
      .when()
      .get("eholdings/providers?q=e&count=1")
      .then()
      .statusCode(500)
      .body("errors.first.title", notNullValue());
  }

  @Test
  public void shouldReturnErrorIfSortParameterInvalid() {

    RequestSpecification requestSpecification = getRequestSpecification();
    RestAssured.given()
      .spec(requestSpecification)
      .when()
      .get("eholdings/providers?q=e&count=10&sort=abc")
      .then()
      .statusCode(400)
      .body("errors.first.title", notNullValue());
  }

  @Test
  public void shouldReturnProviderWhenValidId() throws IOException, URISyntaxException {
    String stubResponseFile = "responses/rmapi/vendors/get-vendor-by-id-response.json";

    String wiremockUrl = getWiremockUrl();
    TestUtil.mockConfiguration(CONFIGURATION_STUB_FILE, wiremockUrl);
    WireMock.stubFor(
      WireMock.get(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors.*"), true))
        .willReturn(new ResponseDefinitionBuilder()
          .withBody(TestUtil.readFile(stubResponseFile))));

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
  public void shouldReturn404WhenProviderIdNotFound() throws IOException, URISyntaxException {

    String wiremockUrl = getWiremockUrl();
    TestUtil.mockConfiguration(CONFIGURATION_STUB_FILE, wiremockUrl);
    WireMock.stubFor(
      WireMock.get(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors.*"), true))
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
    String wiremockUrl = getWiremockUrl();
    TestUtil.mockConfiguration(CONFIGURATION_STUB_FILE, wiremockUrl);

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

    String wiremockUrl = getWiremockUrl();
    TestUtil.mockConfiguration(CONFIGURATION_STUB_FILE, wiremockUrl);

    WireMock.stubFor(
      WireMock.get(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors.*"), true))
        .willReturn(new ResponseDefinitionBuilder().withBody(TestUtil.readFile(stubResponseFile))));

    WireMock.stubFor(
      WireMock.put(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors.*"), true))
        .willReturn(new ResponseDefinitionBuilder().withStatus(204)));

    ObjectMapper mapper = new ObjectMapper();
    ProviderPutRequest providerToBeUpdated = mapper.readValue(TestUtil.getFile("requests/kb-ebsco/put-provider.json"),
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

    WireMock.verify(1, putRequestedFor(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors.*"), true))
      .withRequestBody(equalToJson(TestUtil.readFile("requests/rmapi/vendors/put-vendor-token-proxy.json"))));
  }

  @Test
  public void shouldReturn400WhenRMAPIErrorOnPut() throws IOException, URISyntaxException {
    String stubResponseFile = "responses/rmapi/vendors/put-vendor-token-not-allowed-response.json";

    String wiremockUrl = getWiremockUrl();
    TestUtil.mockConfiguration(CONFIGURATION_STUB_FILE, wiremockUrl);

    WireMock.stubFor(
      WireMock.put(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors.*"), true))
        .willReturn(new ResponseDefinitionBuilder().withBody(TestUtil.readFile(stubResponseFile)).withStatus(400)));

    ObjectMapper mapper = new ObjectMapper();
    ProviderPutRequest providerToBeUpdated = mapper.readValue(TestUtil.getFile("requests/kb-ebsco/put-provider.json"),
      ProviderPutRequest.class);

    RequestSpecification requestSpecification = getRequestSpecification();

    String providerByIdEndpoint = "eholdings/providers/" + STUB_VENDOR_ID;

    JsonapiError error = RestAssured
      .given()
      .spec(requestSpecification)
      .header(CONTENT_TYPE_HEADER)
      .body(mapper.writeValueAsString(providerToBeUpdated))
      .when()
      .put(providerByIdEndpoint)
      .then()
      .statusCode(HttpStatus.SC_BAD_REQUEST)
      .extract().as(JsonapiError.class);

    assertThat(error.getErrors().get(0).getTitle(), equalTo("Vendor does not allow token"));

  }

  @Test
  public void shouldReturn422WhenBodyInputInvalidOnPut() throws IOException, URISyntaxException {
    String wiremockUrl = getWiremockUrl();
    TestUtil.mockConfiguration(CONFIGURATION_STUB_FILE, wiremockUrl);

    ObjectMapper mapper = new ObjectMapper();
    ProviderPutRequest providerToBeUpdated = mapper.readValue(TestUtil.getFile("requests/kb-ebsco/put-provider.json"),
      ProviderPutRequest.class);

    Token providerToken = new Token();
    providerToken.setValue(RandomStringUtils.randomAlphanumeric(501));

    providerToBeUpdated.getData().getAttributes().setProviderToken(providerToken);

    RequestSpecification requestSpecification = getRequestSpecification();

    String providerByIdEndpoint = "eholdings/providers/" + STUB_VENDOR_ID;

    JsonapiError error = RestAssured
      .given()
      .spec(requestSpecification)
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

  private Provider getExpectedProvider() {

    return new Provider()
      .withData(new ProviderData()
        .withType("providers")
        .withId(STUB_VENDOR_ID)
        .withAttributes(new ProviderDataAttributes()
          .withName("EBSCO")
          .withPackagesTotal(625)
          .withPackagesSelected(11)
          .withSupportsCustomPackages(false)
          .withProviderToken(null)
          .withProxy(new Proxy().withId("<n>").withInherited(true)))
        .withRelationships(new Relationships()
          .withPackages(new Packages()
            .withMeta(new MetaDataIncluded()
              .withIncluded(false)))));
  }

  private Provider getUpdatedProvider() {

    return new Provider()
      .withData(new ProviderData()
        .withType("providers")
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
}
