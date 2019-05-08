package org.folio.rest.impl;

import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNPROCESSABLE_ENTITY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import static org.folio.rest.util.RestConstants.TITLES_TYPE;
import static org.folio.tag.repository.titles.TitlesTableConstants.TITLES_TABLE_NAME;
import static org.folio.util.TestUtil.mockDefaultConfiguration;
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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.JSONAssert;

import org.folio.rest.jaxrs.model.JsonapiError;
import org.folio.rest.jaxrs.model.Tags;
import org.folio.rest.jaxrs.model.Title;
import org.folio.rest.jaxrs.model.TitlePostRequest;
import org.folio.rest.jaxrs.model.TitlePutRequest;
import org.folio.tag.RecordType;
import org.folio.util.TagsTestUtil;
import org.folio.util.TestUtil;
import org.folio.util.TitlesTestUtil;

@RunWith(VertxUnitRunner.class)
public class EholdingsTitlesTest extends WireMockTestBase {
  private static final String STUB_TITLE_ID = "985846";
  private static final String STUB_TITLE_NAME = "Test Title";
  private static final int STUB_PACKAGE_ID = 3964;
  private static final int STUB_VENDOR_ID = 111111;
  private static final String STUB_TAG_VALUE = "test tag";
  private static final String STUB_TAG_VALUE2 = "test tag 2";

  private static final String STUB_CUSTOM_VENDOR_ID = "123356";
  private static final String STUB_CUSTOM_PACKAGE_ID = "3157070";
  private static final String STUB_CUSTOM_TITLE_ID = "19412030";
  private static final String CUSTOM_TITLE_ENDPOINT = "/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/titles/" + STUB_CUSTOM_TITLE_ID;
  private static final String CUSTOM_RESOURCE_ENDPOINT = "/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors/" + STUB_CUSTOM_VENDOR_ID + "/packages/" + STUB_CUSTOM_PACKAGE_ID + "/titles/" + STUB_CUSTOM_TITLE_ID;
  public static final String EHOLDINGS_TITLES_PATH = "eholdings/titles";

  @Test
  public void shouldReturnTitlesOnGet() throws IOException, URISyntaxException {
    String stubResponseFile = "responses/rmapi/titles/searchTitles.json";

    mockDefaultConfiguration(getWiremockUrl());
    stubFor(
      get(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/titles.*"), true))
        .willReturn(new ResponseDefinitionBuilder()
          .withBody(readFile(stubResponseFile))));

    RestAssured.given()
      .spec(getRequestSpecification())
      .when()
      .get(EHOLDINGS_TITLES_PATH + "?page=1&filter[name]=Mind&sort=name")
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
    getWithStatus(EHOLDINGS_TITLES_PATH + "?count=1000&page=1&filter[name]=Mind&sort=name", SC_BAD_REQUEST);
  }

  @Test
  public void shouldReturn500WhenRMApiReturns500Error() throws IOException, URISyntaxException {
    mockDefaultConfiguration(getWiremockUrl());
    stubFor(
      get(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/titles.*"), true))
        .willReturn(new ResponseDefinitionBuilder()
          .withStatus(500)));

    getWithStatus(EHOLDINGS_TITLES_PATH + "?filter[name]=news", SC_INTERNAL_SERVER_ERROR);
  }

  @Test
  public void shouldReturnTitleWhenValidId() throws IOException, URISyntaxException {
    String stubResponseFile = "responses/rmapi/titles/get-title-by-id-response.json";

    mockDefaultConfiguration(getWiremockUrl());
    stubFor(
        get(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/titles.*"), true))
        .willReturn(new ResponseDefinitionBuilder()
            .withBody(readFile(stubResponseFile))));

    String actualResponse = getWithOk(EHOLDINGS_TITLES_PATH + "/" + STUB_TITLE_ID).asString();

    JSONAssert.assertEquals(
      readFile("responses/kb-ebsco/titles/expected-title-by-id.json"), actualResponse, false);
  }

  @Test
  public void shouldReturnTitleTagsWhenValidId() throws IOException, URISyntaxException {
    try {
      String stubResponseFile = "responses/rmapi/titles/get-title-by-id-response.json";
      TagsTestUtil.insertTag(vertx, STUB_TITLE_ID, RecordType.TITLE, STUB_TAG_VALUE);
      mockDefaultConfiguration(getWiremockUrl());
      stubFor(
        get(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/titles.*"), true))
          .willReturn(new ResponseDefinitionBuilder()
            .withBody(readFile(stubResponseFile))));

      Title actualResponse = getWithOk(EHOLDINGS_TITLES_PATH + "/" + STUB_TITLE_ID).as(Title.class);

      assertTrue(actualResponse.getData().getAttributes().getTags().getTagList().contains(STUB_TAG_VALUE));
    }
    finally {
      TagsTestUtil.clearTags(vertx);
      TestUtil.clearDataFromTable(vertx, TITLES_TABLE_NAME);
    }
  }

  @Test
  public void shouldReturn404WhenRMAPINotFoundOnTitleGet() throws IOException, URISyntaxException {
    String stubResponseFile = "responses/rmapi/titles/get-title-by-id-not-found-response.json";

    mockDefaultConfiguration(getWiremockUrl());
    stubFor(
        get(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/titles.*"), true))
        .willReturn(new ResponseDefinitionBuilder()
            .withBody(readFile(stubResponseFile))
            .withStatus(404)));

    JsonapiError error = getWithStatus(EHOLDINGS_TITLES_PATH + "/" + STUB_TITLE_ID, SC_NOT_FOUND)
      .as(JsonapiError.class);

    assertThat(error.getErrors().get(0).getTitle(), equalTo("Title not found"));
  }

  @Test
  public void shouldReturn400WhenValidationErrorOnTitleGet() throws IOException, URISyntaxException {
    mockDefaultConfiguration(getWiremockUrl());

    JsonapiError error = getWithStatus(EHOLDINGS_TITLES_PATH + "/12345aaa", SC_BAD_REQUEST)
      .as(JsonapiError.class);

    assertThat(error.getErrors().get(0).getTitle(), equalTo("Title id is invalid - 12345aaa"));
  }

  @Test
  public void shouldReturn500WhenRMApiReturns500ErrorOnTitleGet() throws IOException, URISyntaxException {
    mockDefaultConfiguration(getWiremockUrl());
    stubFor(
      get(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/titles.*"), true))
        .willReturn(new ResponseDefinitionBuilder()
          .withStatus(500)));

    getWithStatus(EHOLDINGS_TITLES_PATH + "/" + STUB_TITLE_ID, SC_INTERNAL_SERVER_ERROR);
  }

  @Test
  public void shouldReturnTitleWithResourcesWhenIncludeResources() throws IOException, URISyntaxException {
    String rmapiResponseFile = "responses/rmapi/titles/get-title-by-id-response.json";
    String rmapiUrl = "/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/titles.*";

    mockDefaultConfiguration(getWiremockUrl());
    mockGet(new RegexPattern(rmapiUrl), rmapiResponseFile);

    String actual = getWithStatus(EHOLDINGS_TITLES_PATH + "/" + STUB_TITLE_ID + "?include=resources", SC_OK).asString();
    String expected = readFile("responses/kb-ebsco/titles/get-title-by-id-include-resources-response.json");

    JSONAssert.assertEquals(expected, actual, false);
  }

  @Test
  public void shouldReturnTitleWithoutResourcesWhenInvalidInclude() throws IOException, URISyntaxException {
    String rmapiResponseFile = "responses/rmapi/titles/get-title-by-id-response.json";
    String rmapiUrl = "/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/titles.*";

    mockDefaultConfiguration(getWiremockUrl());
    mockGet(new RegexPattern(rmapiUrl), rmapiResponseFile);

    String actual = getWithStatus(EHOLDINGS_TITLES_PATH + "/" + STUB_TITLE_ID + "?include=badValue", SC_OK).asString();
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
      TestUtil.clearDataFromTable(vertx, TITLES_TABLE_NAME);
    }
  }

  @Test
  public void shouldAddTitleDataOnPost() throws URISyntaxException, IOException {
    try {
      postTitle(Collections.singletonList(STUB_TAG_VALUE));
      List<TitlesTestUtil.DbTitle> titles = TitlesTestUtil.getTitles(vertx);
      assertEquals(1, titles.size());
      assertEquals(STUB_TITLE_ID, titles.get(0).getId());
      assertEquals(STUB_TITLE_NAME, titles.get(0).getName());
    } finally {
      TagsTestUtil.clearTags(vertx);
      TestUtil.clearDataFromTable(vertx, TITLES_TABLE_NAME);
    }
  }

  @Test
  public void shouldReturn400WhenInvalidPostRequest() throws URISyntaxException, IOException {

    String errorResponse = "responses/rmapi/packages/post-package-400-error-response.json";
    String titlePostStubRequestFile = "requests/kb-ebsco/title/post-title-request.json";
    EqualToJsonPattern postBodyPattern = new EqualToJsonPattern("{\n  \"titleName\" : \"Test Title\",\n  \"edition\" : \"Test edition\",\n  \"publisherName\" : \"Test publisher\",\n  \"pubType\" : \"thesisdissertation\",\n  \"description\" : \"Lorem ipsum dolor sit amet, consectetuer adipiscing elit.\",\n  \"isPeerReviewed\" : true,\n  \"identifiersList\" : [ {\n    \"id\" : \"1111-2222-3333\",\n    \"subtype\" : 2,\n    \"type\" : 0\n  } ],\n  \"contributorsList\" : [ {\n    \"type\" : \"author\",\n    \"contributor\" : \"smith, john\"\n  }, {\n    \"type\" : \"illustrator\",\n    \"contributor\" : \"smith, ralph\"\n  } ],\n  \"peerReviewed\" : true\n}", true, true);

    mockDefaultConfiguration(getWiremockUrl());

    stubFor(
      post(new UrlPathPattern(new EqualToPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors/"
        + STUB_VENDOR_ID + "/packages/" + STUB_PACKAGE_ID + "/titles"), false))
        .withRequestBody(postBodyPattern)
        .willReturn(new ResponseDefinitionBuilder()
          .withBody(readFile(errorResponse))
          .withStatus(SC_BAD_REQUEST)));

    postWithStatus(EHOLDINGS_TITLES_PATH, readFile(titlePostStubRequestFile), SC_BAD_REQUEST);

  }

  @Test
  public void shouldReturnUpdatedValuesForCustomTitleOnSuccessfulPut() throws IOException, URISyntaxException {
    String expectedTitleFile = "responses/kb-ebsco/titles/expected-updated-title.json";
    String actualResponse = putTitle(null);

    JSONAssert.assertEquals(readFile(expectedTitleFile), actualResponse, false);

    verify(1, putRequestedFor(new UrlPathPattern(new RegexPattern(CUSTOM_RESOURCE_ENDPOINT), true))
      .withRequestBody(equalToJson(readFile("requests/rmapi/resources/put-custom-resource-is-selected-multiple-attributes.json"))));
  }

  @Test
  public void shouldUpdateTitleTagsOnSuccessfulPut() throws IOException, URISyntaxException {
    try {

      List<String> newTags = Arrays.asList(STUB_TAG_VALUE, STUB_TAG_VALUE2);

      putTitle(newTags);

      List<String> tags = TagsTestUtil.getTags(vertx);
      assertThat(tags, containsInAnyOrder(newTags.toArray()));
    } finally {
      TagsTestUtil.clearTags(vertx);
      TestUtil.clearDataFromTable(vertx, TITLES_TABLE_NAME);
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

      mockDefaultConfiguration(getWiremockUrl());

      stubFor(
        get(new UrlPathPattern(new RegexPattern(CUSTOM_TITLE_ENDPOINT), false))
          .willReturn(new ResponseDefinitionBuilder().withBody(readFile(resourceResponse))));

      putWithOk(EHOLDINGS_TITLES_PATH + "/" + STUB_CUSTOM_TITLE_ID, mapper.writeValueAsString(request));

      List<String> tags = TagsTestUtil.getTags(vertx);
      assertThat(tags, containsInAnyOrder(newTags.toArray()));
      WireMock.verify(0, putRequestedFor(anyUrl()));
    } finally {
      TagsTestUtil.clearTags(vertx);
      TestUtil.clearDataFromTable(vertx, TITLES_TABLE_NAME);
    }
  }

  @Test
  public void shouldAddTitleDataOnPut() throws IOException, URISyntaxException {
    try {

      putTitle(Arrays.asList(STUB_TAG_VALUE, STUB_TAG_VALUE2));

      List<TitlesTestUtil.DbTitle> titles = TitlesTestUtil.getTitles(vertx);
      assertEquals(1, titles.size());
      assertEquals(STUB_CUSTOM_TITLE_ID, titles.get(0).getId());
      assertEquals("sd-test-java-again", titles.get(0).getName());
    } finally {
      TagsTestUtil.clearTags(vertx);
      TestUtil.clearDataFromTable(vertx, TITLES_TABLE_NAME);
    }
  }

  @Test
  public void shouldDeleteTitleDataOnPutWithEmptyTagList() throws IOException, URISyntaxException {

    putTitle(Collections.singletonList(STUB_TAG_VALUE));
    putTitle(Collections.emptyList());

    List<TitlesTestUtil.DbTitle> packages = TitlesTestUtil.getTitles(vertx);
    assertThat(packages, is(empty()));
  }

  @Test
  public void shouldUpdateTitleDataOnSecondPut() throws IOException, URISyntaxException {
    try {
      String newName = "new name";
      String updatedResponse = "responses/rmapi/resources/get-custom-resource-updated-title-name-response.json";

      putTitle(readFile(updatedResponse), Collections.singletonList(STUB_TAG_VALUE));

      ObjectMapper mapper = new ObjectMapper();
      TitlePutRequest request = mapper.readValue(readFile("requests/kb-ebsco/title/put-title.json"), TitlePutRequest.class);
      request.getData().getAttributes().withName(newName);

      stubFor(
        get(new UrlPathPattern(new RegexPattern(CUSTOM_TITLE_ENDPOINT), false))
          .willReturn(new ResponseDefinitionBuilder().withBody(readFile(updatedResponse))));

      putWithOk(EHOLDINGS_TITLES_PATH + "/" + STUB_CUSTOM_TITLE_ID, mapper.writeValueAsString(request));

      List<TitlesTestUtil.DbTitle> titles = TitlesTestUtil.getTitles(vertx);
      assertEquals(1, titles.size());
      assertEquals(STUB_CUSTOM_TITLE_ID, titles.get(0).getId());
      assertEquals(newName, titles.get(0).getName());
    } finally {
      TagsTestUtil.clearTags(vertx);
      TestUtil.clearDataFromTable(vertx, TITLES_TABLE_NAME);
    }
  }


  @Test
  public void shouldReturn422WhenNameIsNotProvided() throws URISyntaxException, IOException {
    mockDefaultConfiguration(getWiremockUrl());
    putWithStatus(EHOLDINGS_TITLES_PATH + "/" + STUB_TITLE_ID,
      readFile("requests/kb-ebsco/title/put-title-null-name.json"), SC_UNPROCESSABLE_ENTITY);
  }

  private String putTitle(List<String> tags) throws IOException, URISyntaxException {
    String updatedResourceResponse = "responses/rmapi/resources/get-custom-resource-updated-response.json";
    return putTitle(readFile(updatedResourceResponse), tags);
  }

  private String putTitle(String updatedResourceResponse, List<String> tags) throws IOException, URISyntaxException {
    mockDefaultConfiguration(getWiremockUrl());

    stubFor(
      get(new UrlPathPattern(new RegexPattern(CUSTOM_TITLE_ENDPOINT), false))
        .willReturn(new ResponseDefinitionBuilder().withBody(updatedResourceResponse)));

    stubFor(
      put(new UrlPathPattern(new RegexPattern(CUSTOM_RESOURCE_ENDPOINT), true))
        .willReturn(new ResponseDefinitionBuilder().withStatus(SC_NO_CONTENT)));


    ObjectMapper mapper = new ObjectMapper();
    TitlePutRequest titleToBeUpdated = mapper.readValue(readFile("requests/kb-ebsco/title/put-title.json"), TitlePutRequest.class);

    if (tags != null) {
      titleToBeUpdated.getData().getAttributes().setTags(new Tags()
        .withTagList(tags));
    }

    return putWithOk(EHOLDINGS_TITLES_PATH + "/" + STUB_CUSTOM_TITLE_ID, mapper.writeValueAsString(titleToBeUpdated)).asString();
  }

  private ExtractableResponse<Response> postTitle(List<String> tags) throws IOException, URISyntaxException {
    String titleCreatedIdStubResponseFile = "responses/rmapi/titles/post-title-response.json";
    String titlePostStubRequestFile = "requests/kb-ebsco/title/post-title-request.json";
    String getTitleByTitleIdStubFile = "responses/rmapi/titles/get-title-by-id-for-post-request.json";

    mockDefaultConfiguration(getWiremockUrl());
    EqualToJsonPattern postBodyPattern = new EqualToJsonPattern("{\n  \"titleName\" : \"Test Title\",\n  \"edition\" : \"Test edition\",\n  \"publisherName\" : \"Test publisher\",\n  \"pubType\" : \"thesisdissertation\",\n  \"description\" : \"Lorem ipsum dolor sit amet, consectetuer adipiscing elit.\",\n  \"isPeerReviewed\" : true,\n  \"identifiersList\" : [ {\n    \"id\" : \"1111-2222-3333\",\n    \"subtype\" : 2,\n    \"type\" : 0\n  } ],\n  \"contributorsList\" : [ {\n    \"type\" : \"author\",\n    \"contributor\" : \"smith, john\"\n  }, {\n    \"type\" : \"illustrator\",\n    \"contributor\" : \"smith, ralph\"\n  } ],\n  \"peerReviewed\" : true\n}", true, true);

    stubFor(
      post(new UrlPathPattern(new EqualToPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors/"
        + STUB_VENDOR_ID + "/packages/" + STUB_PACKAGE_ID + "/titles"), false))
        .withRequestBody(postBodyPattern)
        .willReturn(new ResponseDefinitionBuilder()
          .withBody(readFile(titleCreatedIdStubResponseFile))
          .withStatus(SC_OK)));

    stubFor(
      get(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/titles/" + STUB_TITLE_ID), true))
        .willReturn(new ResponseDefinitionBuilder()
          .withBody(readFile(getTitleByTitleIdStubFile))
          .withStatus(SC_OK)));

    ObjectMapper mapper = new ObjectMapper();
    TitlePostRequest request = mapper.readValue(readFile(titlePostStubRequestFile), TitlePostRequest.class);
    request.getData().getAttributes().setTags(new Tags().withTagList(tags));

    return postWithOk(EHOLDINGS_TITLES_PATH, mapper.writeValueAsString(request));
  }
}
