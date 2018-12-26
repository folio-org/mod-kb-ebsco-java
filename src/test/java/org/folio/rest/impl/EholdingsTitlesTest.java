package org.folio.rest.impl;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.folio.rest.util.RestConstants.TITLES_TYPE;
import static org.folio.util.TestUtil.mockConfiguration;
import static org.folio.util.TestUtil.mockGet;
import static org.folio.util.TestUtil.readFile;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.matching.EqualToJsonPattern;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import com.github.tomakehurst.wiremock.matching.RegexPattern;
import com.github.tomakehurst.wiremock.matching.UrlPathPattern;
import io.restassured.RestAssured;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import java.io.IOException;
import java.net.URISyntaxException;
import org.apache.http.HttpStatus;
import org.folio.rest.jaxrs.model.JsonapiError;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.JSONAssert;

@RunWith(VertxUnitRunner.class)
public class EholdingsTitlesTest extends WireMockTestBase {
  private static final String STUB_TITLE_ID = "985846";
  private static final int STUB_PACKAGE_ID = 3964;
  private static final int STUB_VENDOR_ID = 111111;

  @Test
  public void shouldReturnTitlesOnGet() throws IOException, URISyntaxException {
    String stubResponseFile = "responses/rmapi/titles/searchTitles.json";

    mockConfiguration(CONFIGURATION_STUB_FILE, getWiremockUrl());
    stubFor(
      get(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/titles.*"), true))
        .willReturn(new ResponseDefinitionBuilder()
          .withBody(readFile(stubResponseFile))));

    RestAssured.given()
      .spec(getRequestSpecification())
      .when()
      .get("eholdings/titles?page=1&filter[name]=Mind&sort=name")
      .then()
      .statusCode(200)

      .body("meta.totalResults", equalTo(8766))
      .body("data[0].type", equalTo(TITLES_TYPE))
      .body("data[0].id", equalTo("1175655"))
      .body("data[0].attributes.name", equalTo("The $1 Million Reason to Change Your Mind"))
      .body("data[0].attributes.publisherName", isEmptyOrNullString())
      .body("data[0].attributes.isTitleCustom", equalTo(false))
      .body("data[0].attributes.subjects[0].type", equalTo("BISAC"))
      .body("data[0].attributes.subjects[0].subject", equalTo("BUSINESS & ECONOMICS / Small Business"))

      .body("data[0].attributes.identifiers[0].id", equalTo("7209484"))
      .body("data[0].attributes.identifiers[1].id", equalTo("978-1-74216-894-4"))
      .body("data[0].attributes.identifiers[2].id", equalTo("978-0-7303-7792-4"))
      /* List of identifiers returned below from RM API get filtered and sorted to only support types ISSN/ISBN and subtypes Print/Online */
      .body("data[0].attributes.identifiers[0].type", equalTo("ISSN"))
      .body("data[0].attributes.identifiers[1].type", equalTo("ISBN"))
      .body("data[0].attributes.identifiers[2].type", equalTo("ISBN"))

      .body("data[0].attributes.identifiers[0].subtype", equalTo("Print"))
      .body("data[0].attributes.identifiers[1].subtype", equalTo("Print"))
      .body("data[0].attributes.identifiers[2].subtype", equalTo("Online"))

      .body("data[0].attributes.publicationType", equalTo("Book"))

      .body("data[0].relationships.resources.meta.included", equalTo(false));
  }

  @Test
  public void shouldReturn400IfCountOutOfRange() {
    RestAssured.given()
      .spec(getRequestSpecification())
      .when()
      .get("eholdings/titles?count=1000&page=1&filter[name]=Mind&sort=name")
      .then()
      .statusCode(400);
  }

  @Test
  public void shouldReturn500WhenRMApiReturns500Error() throws IOException, URISyntaxException {
    mockConfiguration(CONFIGURATION_STUB_FILE, getWiremockUrl());
    stubFor(
      get(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/titles.*"), true))
        .willReturn(new ResponseDefinitionBuilder()
          .withStatus(500)));

    RestAssured.given()
      .spec(getRequestSpecification())
      .when()
      .get("eholdings/titles?filter[name]=news")
      .then()
      .statusCode(500);
  }

  @Test
  public void shouldReturnTitleWhenValidId() throws IOException, URISyntaxException {
    String stubResponseFile = "responses/rmapi/titles/get-title-by-id-response.json";

    mockConfiguration(CONFIGURATION_STUB_FILE, getWiremockUrl());
    stubFor(
        get(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/titles.*"), true))
        .willReturn(new ResponseDefinitionBuilder()
            .withBody(readFile(stubResponseFile))));

    String titleByIdEndpoint = "eholdings/titles/" + STUB_TITLE_ID;

    String actualResponse = RestAssured.given()
      .spec(getRequestSpecification())
      .when()
      .get(titleByIdEndpoint)
      .then()
      .statusCode(200)
      .extract().asString();

    JSONAssert.assertEquals(
      readFile("responses/kb-ebsco/titles/expected-title-by-id.json"), actualResponse, false);
  }

  @Test
  public void shouldReturn404WhenRMAPINotFoundOnTitleGet() throws IOException, URISyntaxException {
    String stubResponseFile = "responses/rmapi/titles/get-title-by-id-not-found-response.json";

    mockConfiguration(CONFIGURATION_STUB_FILE, getWiremockUrl());
    stubFor(
        get(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/titles.*"), true))
        .willReturn(new ResponseDefinitionBuilder()
            .withBody(readFile(stubResponseFile))
            .withStatus(404)));

    String titleByIdEndpoint = "eholdings/titles/" + STUB_TITLE_ID;

    JsonapiError error = RestAssured.given()
      .spec(getRequestSpecification())
      .when()
      .get(titleByIdEndpoint)
      .then()
      .statusCode(404)
      .extract().as(JsonapiError.class);

    assertThat(error.getErrors().get(0).getTitle(), equalTo("Title not found"));
  }

  @Test
  public void shouldReturn400WhenValidationErrorOnTitleGet() throws IOException, URISyntaxException {
    mockConfiguration(CONFIGURATION_STUB_FILE, getWiremockUrl());

    String titleByIdEndpoint = "eholdings/titles/12345aaa";

    JsonapiError error = RestAssured.given()
      .spec(getRequestSpecification())
      .when()
      .get(titleByIdEndpoint)
      .then()
      .statusCode(400)
      .extract().as(JsonapiError.class);

    assertThat(error.getErrors().get(0).getTitle(), equalTo("Title id is invalid - 12345aaa"));
  }

  @Test
  public void shouldReturn500WhenRMApiReturns500ErrorOnTitleGet() throws IOException, URISyntaxException {
    mockConfiguration(CONFIGURATION_STUB_FILE, getWiremockUrl());
    stubFor(
      get(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/titles.*"), true))
        .willReturn(new ResponseDefinitionBuilder()
          .withStatus(500)));

    String titleByIdEndpoint = "eholdings/titles/" + STUB_TITLE_ID;

    RestAssured.given()
      .spec(getRequestSpecification())
      .when()
      .when()
      .get(titleByIdEndpoint)
      .then()
      .statusCode(500);
  }

  @Test
  public void shouldReturnTitleWithResourcesWhenIncludeResources() throws IOException, URISyntaxException {
    String rmapiResponseFile = "responses/rmapi/titles/get-title-by-id-response.json";
    String rmapiUrl = "/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/titles.*";

    mockConfiguration(CONFIGURATION_STUB_FILE, getWiremockUrl());
    mockGet(new RegexPattern(rmapiUrl), rmapiResponseFile);
    String titleByIdEndpoint = "eholdings/titles/" + STUB_TITLE_ID + "?include=resources";

    String actual = getResponseWithStatus(titleByIdEndpoint, HttpStatus.SC_OK).asString();
    String expected = readFile("responses/kb-ebsco/titles/get-title-by-id-include-resources-response.json");

    JSONAssert.assertEquals(expected, actual, false);
  }

  @Test
  public void shouldReturnTitleWithoutResourcesWhenInvalidInclude() throws IOException, URISyntaxException {
    String rmapiResponseFile = "responses/rmapi/titles/get-title-by-id-response.json";
    String rmapiUrl = "/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/titles.*";

    mockConfiguration(CONFIGURATION_STUB_FILE, getWiremockUrl());
    mockGet(new RegexPattern(rmapiUrl), rmapiResponseFile);
    String titleByIdEndpoint = "eholdings/titles/" + STUB_TITLE_ID + "?include=badValue";

    String actual = getResponseWithStatus(titleByIdEndpoint, HttpStatus.SC_OK).asString();
    String expected = readFile("responses/kb-ebsco/titles/get-title-by-id-invalid-include-response.json");

    JSONAssert.assertEquals(expected, actual, false);
  }

  @Test
  public void shouldReturnTitleWhenValidPostRequest() throws IOException, URISyntaxException {

    String titleCreatedIdStubResponseFile = "responses/rmapi/titles/post-title-response.json";
    String titlePostStubRequestFile = "requests/kb-ebsco/title/post-title-request.json";
    String getTitleByTitleIdStubFile = "responses/rmapi/titles/get-title-by-id-for-post-request.json";

    mockConfiguration(CONFIGURATION_STUB_FILE, getWiremockUrl());
    EqualToJsonPattern postBodyPattern = new EqualToJsonPattern("{\n  \"titleName\" : \"Test Title\",\n  \"edition\" : \"Test edition\",\n  \"publisherName\" : \"Test publisher\",\n  \"pubType\" : \"thesisdissertation\",\n  \"description\" : \"Lorem ipsum dolor sit amet, consectetuer adipiscing elit.\",\n  \"isPeerReviewed\" : true,\n  \"identifiersList\" : [ {\n    \"id\" : \"1111-2222-3333\",\n    \"subtype\" : 2,\n    \"type\" : 0\n  } ],\n  \"contributorsList\" : [ {\n    \"type\" : \"Author\",\n    \"contributor\" : \"smith, john\"\n  }, {\n    \"type\" : \"Illustrator\",\n    \"contributor\" : \"smith, ralph\"\n  } ],\n  \"peerReviewed\" : true\n}", true, true);

    stubFor(
      post(new UrlPathPattern(new EqualToPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors/"
        + STUB_VENDOR_ID + "/packages/" + STUB_PACKAGE_ID + "/titles"), false))
        .withRequestBody(postBodyPattern)
        .willReturn(new ResponseDefinitionBuilder()
          .withBody(readFile(titleCreatedIdStubResponseFile))
          .withStatus(org.apache.http.HttpStatus.SC_OK)));

    stubFor(
      get(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/titles/" + STUB_TITLE_ID), true))
        .willReturn(new ResponseDefinitionBuilder()
          .withBody(readFile(getTitleByTitleIdStubFile))
          .withStatus(HttpStatus.SC_OK)));

    String actual = postResponseWithStatus("eholdings/titles", org.apache.http.HttpStatus.SC_OK, titlePostStubRequestFile).asString();
    String expected = readFile("responses/kb-ebsco/titles/get-created-title-response.json");

    JSONAssert.assertEquals(expected, actual, false);
  }

  @Test
  public void shouldReturn400WhenInvalidPostRequest() throws URISyntaxException, IOException {

    String errorResponse = "responses/rmapi/packages/post-package-400-error-response.json";
    String titlePostStubRequestFile = "requests/kb-ebsco/title/post-title-request.json";
    EqualToJsonPattern postBodyPattern = new EqualToJsonPattern("{\n  \"titleName\" : \"Test Title\",\n  \"edition\" : \"Test edition\",\n  \"publisherName\" : \"Test publisher\",\n  \"pubType\" : \"thesisdissertation\",\n  \"description\" : \"Lorem ipsum dolor sit amet, consectetuer adipiscing elit.\",\n  \"isPeerReviewed\" : true,\n  \"identifiersList\" : [ {\n    \"id\" : \"1111-2222-3333\",\n    \"subtype\" : 2,\n    \"type\" : 0\n  } ],\n  \"contributorsList\" : [ {\n    \"type\" : \"Author\",\n    \"contributor\" : \"smith, john\"\n  }, {\n    \"type\" : \"Illustrator\",\n    \"contributor\" : \"smith, ralph\"\n  } ],\n  \"peerReviewed\" : true\n}", true, true);

    mockConfiguration(CONFIGURATION_STUB_FILE, getWiremockUrl());

    stubFor(
      post(new UrlPathPattern(new EqualToPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors/"
        + STUB_VENDOR_ID + "/packages/" + STUB_PACKAGE_ID + "/titles"), false))
        .withRequestBody(postBodyPattern)
        .willReturn(new ResponseDefinitionBuilder()
          .withBody(readFile(errorResponse))
          .withStatus(HttpStatus.SC_BAD_REQUEST)));

    postResponseWithStatus("eholdings/titles", HttpStatus.SC_BAD_REQUEST, titlePostStubRequestFile);

  }

  private ExtractableResponse<Response> postResponseWithStatus(String resourcePath, int expectedStatus, String body)
    throws IOException, URISyntaxException {
    return RestAssured.given()
      .spec(getRequestSpecification())
      .header("Content-type","application/vnd.api+json")
      .body(readFile(body))
      .when()
      .post(resourcePath)
      .then()
      .statusCode(expectedStatus).extract();
  }
}
