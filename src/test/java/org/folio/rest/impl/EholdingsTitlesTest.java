package org.folio.rest.impl;

import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.junit.Assert.assertTrue;

import static org.folio.rest.util.RestConstants.TITLES_TYPE;
import static org.folio.util.TestUtil.mockConfiguration;
import static org.folio.util.TestUtil.mockGet;
import static org.folio.util.TestUtil.readFile;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.EqualToJsonPattern;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import com.github.tomakehurst.wiremock.matching.RegexPattern;
import com.github.tomakehurst.wiremock.matching.UrlPathPattern;

import io.restassured.RestAssured;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import io.vertx.ext.unit.junit.VertxUnitRunner;

import org.apache.http.HttpStatus;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.JSONAssert;

import org.folio.rest.jaxrs.model.JsonapiError;
import org.folio.rest.jaxrs.model.Tags;
import org.folio.rest.jaxrs.model.Title;
import org.folio.rest.jaxrs.model.TitlePostRequest;
import org.folio.rest.jaxrs.model.TitlePutRequest;
import org.folio.tag.RecordType;

@RunWith(VertxUnitRunner.class)
public class EholdingsTitlesTest extends WireMockTestBase {
  private static final String STUB_TITLE_ID = "985846";
  private static final int STUB_PACKAGE_ID = 3964;
  private static final int STUB_VENDOR_ID = 111111;
  private static final String STUB_TAG_VALUE = "test tag";
  private static final String STUB_TAG_VALUE2 = "test tag 2";

  private static final String STUB_CUSTOM_VENDOR_ID = "123356";
  private static final String STUB_CUSTOM_PACKAGE_ID = "3157070";
  private static final String STUB_CUSTOM_TITLE_ID = "19412030";
  private static final String CUSTOM_TITLE_ENDPOINT = "/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/titles/" + STUB_CUSTOM_TITLE_ID;
  private static final String CUSTOM_RESOURCE_ENDPOINT = "/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors/" + STUB_CUSTOM_VENDOR_ID + "/packages/" + STUB_CUSTOM_PACKAGE_ID + "/titles/" + STUB_CUSTOM_TITLE_ID;

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
  public void shouldReturnTitleTagsWhenValidId() throws IOException, URISyntaxException {
    try {
      String stubResponseFile = "responses/rmapi/titles/get-title-by-id-response.json";
      TagsTestUtil.insertTag(vertx, STUB_TITLE_ID, RecordType.TITLE, STUB_TAG_VALUE);
      mockConfiguration(CONFIGURATION_STUB_FILE, getWiremockUrl());
      stubFor(
        get(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/titles.*"), true))
          .willReturn(new ResponseDefinitionBuilder()
            .withBody(readFile(stubResponseFile))));

      String titleByIdEndpoint = "eholdings/titles/" + STUB_TITLE_ID;

      Title actualResponse = RestAssured.given()
        .spec(getRequestSpecification())
        .when()
        .get(titleByIdEndpoint)
        .then()
        .statusCode(200)
        .extract().as(Title.class);

      assertTrue(actualResponse.getData().getAttributes().getTags().getTagList().contains(STUB_TAG_VALUE));
    }
    finally {
      TagsTestUtil.clearTags(vertx);
    }
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
    String actual = postTitle(Collections.emptyList()).asString();
    String expected = readFile("responses/kb-ebsco/titles/get-created-title-response.json");

    JSONAssert.assertEquals(expected, actual, false);
  }

  @Test
  public void shouldUpdateTagsWhenValidPostRequest() throws IOException, URISyntaxException {
    try {
      List<String> tagList = Arrays.asList(STUB_TAG_VALUE, STUB_TAG_VALUE2);
      Title actual = postTitle(tagList).as(Title.class);
      List<String> tagsFromDB = TagsTestUtil.getTags(vertx);
      assertThat(actual.getData().getAttributes().getTags().getTagList(), containsInAnyOrder(tagList.toArray()));
      assertThat(tagsFromDB, containsInAnyOrder(tagList.toArray()));
    }
    finally {
      TagsTestUtil.clearTags(vertx);
    }
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

    postResponseWithStatus("eholdings/titles", HttpStatus.SC_BAD_REQUEST, readFile(titlePostStubRequestFile));

  }

  @Test
  public void shouldReturnUpdatedValuesForCustomTitleOnSuccessfulPut() throws IOException, URISyntaxException {
    String stubResponseFile = "responses/rmapi/resources/get-custom-resource-updated-response.json";
    String expectedTitleFile = "responses/kb-ebsco/titles/expected-updated-title.json";
    String requestBody = readFile("requests/kb-ebsco/title/put-title.json");
    String actualResponse = putTitle(stubResponseFile, requestBody);

    JSONAssert.assertEquals(
      readFile(expectedTitleFile), actualResponse, false);

    verify(1, putRequestedFor(new UrlPathPattern(new RegexPattern(CUSTOM_RESOURCE_ENDPOINT), true))
      .withRequestBody(equalToJson(readFile("requests/rmapi/resources/put-custom-resource-is-selected-multiple-attributes.json"))));
  }

  @Test
  public void shouldUpdateTitleTagsOnSuccessfulPut() throws IOException, URISyntaxException {
    try {
      String udpatedResourceResponse = "responses/rmapi/resources/get-custom-resource-updated-response.json";
      ObjectMapper mapper = new ObjectMapper();
      TitlePutRequest request = mapper.readValue(readFile("requests/kb-ebsco/title/put-title.json"), TitlePutRequest.class);
      List<String> newTags = Arrays.asList(STUB_TAG_VALUE, STUB_TAG_VALUE2);
      request.getData().getAttributes().setTags(new Tags().withTagList(newTags));

      putTitle(udpatedResourceResponse, mapper.writeValueAsString(request));

      List<String> tags = TagsTestUtil.getTags(vertx);
      assertThat(tags, containsInAnyOrder(newTags.toArray()));
    } finally {
      TagsTestUtil.clearTags(vertx);
    }
  }

  @Test
  public void shouldUpdateOnlyTagsOnPutForNonCustomTitle() throws IOException, URISyntaxException {
    try {
      String resourceResponse = "responses/rmapi/resources/get-managed-resource-updated-response.json";
      ObjectMapper mapper = new ObjectMapper();
      TitlePutRequest request = mapper.readValue(readFile("requests/kb-ebsco/title/put-title.json"), TitlePutRequest.class);
      List<String> newTags = Arrays.asList(STUB_TAG_VALUE, STUB_TAG_VALUE2);
      request.getData().getAttributes().setTags(new Tags().withTagList(newTags));

      mockConfiguration(CONFIGURATION_STUB_FILE, getWiremockUrl());

      stubFor(
        get(new UrlPathPattern(new RegexPattern(CUSTOM_TITLE_ENDPOINT), false))
          .willReturn(new ResponseDefinitionBuilder().withBody(readFile(resourceResponse))));

      RestAssured
        .given()
        .spec(getRequestSpecification())
        .header(CONTENT_TYPE_HEADER)
        .body(mapper.writeValueAsString(request))
        .when()
        .put("eholdings/titles/" + STUB_CUSTOM_TITLE_ID)
        .then()
        .statusCode(HttpStatus.SC_OK)
        .extract().asString();

      List<String> tags = TagsTestUtil.getTags(vertx);
      assertThat(tags, containsInAnyOrder(newTags.toArray()));
      WireMock.verify(0, putRequestedFor(anyUrl()));
    } finally {
      TagsTestUtil.clearTags(vertx);
    }
  }

  @Test
  public void shouldReturn422WhenNameIsNotProvided() throws URISyntaxException, IOException {
    mockConfiguration(CONFIGURATION_STUB_FILE, getWiremockUrl());
    RestAssured.given()
      .spec(getRequestSpecification())
      .header(CONTENT_TYPE_HEADER)
      .when()
      .body(readFile("requests/kb-ebsco/title/put-title-null-name.json"))
      .put("eholdings/titles/" + STUB_TITLE_ID)
      .then()
      .statusCode(HttpStatus.SC_UNPROCESSABLE_ENTITY);
  }

  private String putTitle(String updatedResourceResponse, String requestBody) throws IOException, URISyntaxException {
    mockConfiguration(CONFIGURATION_STUB_FILE, getWiremockUrl());

    stubFor(
      get(new UrlPathPattern(new RegexPattern(CUSTOM_TITLE_ENDPOINT), false))
        .willReturn(new ResponseDefinitionBuilder().withBody(readFile(updatedResourceResponse))));

    stubFor(
      put(new UrlPathPattern(new RegexPattern(CUSTOM_RESOURCE_ENDPOINT), true))
        .willReturn(new ResponseDefinitionBuilder().withStatus(HttpStatus.SC_NO_CONTENT)));

    String updateTitleEndpoint = "eholdings/titles/" + STUB_CUSTOM_TITLE_ID;

    return RestAssured
      .given()
      .spec(getRequestSpecification())
      .header(CONTENT_TYPE_HEADER)
      .body(requestBody)
      .when()
      .put(updateTitleEndpoint)
      .then()
      .statusCode(HttpStatus.SC_OK)
      .extract().asString();
  }

  private ExtractableResponse<Response> postResponseWithStatus(String resourcePath, int expectedStatus, String requestBody) {
    return RestAssured.given()
      .spec(getRequestSpecification())
      .header("Content-type","application/vnd.api+json")
      .body(requestBody)
      .when()
      .post(resourcePath)
      .then()
      .statusCode(expectedStatus).extract();
  }

  private ExtractableResponse<Response> postTitle(List<String> tags) throws IOException, URISyntaxException {
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
          .withStatus(HttpStatus.SC_OK)));

    stubFor(
      get(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/titles/" + STUB_TITLE_ID), true))
        .willReturn(new ResponseDefinitionBuilder()
          .withBody(readFile(getTitleByTitleIdStubFile))
          .withStatus(HttpStatus.SC_OK)));

    ObjectMapper mapper = new ObjectMapper();
    TitlePostRequest request = mapper.readValue(readFile(titlePostStubRequestFile), TitlePostRequest.class);
    request.getData().getAttributes().setTags(new Tags());
    request.getData().getAttributes().getTags().setTagList(tags);

    return postResponseWithStatus("eholdings/titles", HttpStatus.SC_OK, mapper.writeValueAsString(request));
  }
}
