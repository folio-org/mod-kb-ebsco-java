package org.folio.rest.impl.integrationsuite;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNPROCESSABLE_ENTITY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import static org.folio.repository.RecordType.RESOURCE;
import static org.folio.repository.accesstypes.AccessTypeMappingsTableConstants.ACCESS_TYPES_MAPPING_TABLE_NAME;
import static org.folio.repository.accesstypes.AccessTypesTableConstants.ACCESS_TYPES_TABLE_NAME;
import static org.folio.repository.holdings.HoldingsTableConstants.HOLDINGS_TABLE;
import static org.folio.repository.kbcredentials.KbCredentialsTableConstants.KB_CREDENTIALS_TABLE_NAME;
import static org.folio.repository.resources.ResourceTableConstants.RESOURCES_TABLE_NAME;
import static org.folio.repository.tag.TagTableConstants.TAGS_TABLE_NAME;
import static org.folio.repository.titles.TitlesTableConstants.TITLES_TABLE_NAME;
import static org.folio.rest.impl.PackagesTestData.STUB_PACKAGE_ID;
import static org.folio.rest.impl.ProvidersTestData.STUB_VENDOR_ID;
import static org.folio.rest.impl.ResourcesTestData.STUB_MANAGED_RESOURCE_ID;
import static org.folio.rest.impl.ResourcesTestData.STUB_MANAGED_RESOURCE_ID_2;
import static org.folio.rest.impl.ResourcesTestData.STUB_MANAGED_RESOURCE_ID_3;
import static org.folio.rest.impl.TagsTestData.STUB_TAG_VALUE;
import static org.folio.rest.impl.TagsTestData.STUB_TAG_VALUE_2;
import static org.folio.rest.impl.TitlesTestData.CUSTOM_RESOURCE_ENDPOINT;
import static org.folio.rest.impl.TitlesTestData.CUSTOM_TITLE_ENDPOINT;
import static org.folio.rest.impl.TitlesTestData.STUB_CUSTOM_TITLE_ID;
import static org.folio.rest.impl.TitlesTestData.STUB_CUSTOM_TITLE_NAME;
import static org.folio.rest.impl.TitlesTestData.STUB_MANAGED_TITLE_ID;
import static org.folio.rest.impl.TitlesTestData.STUB_MANAGED_TITLE_ID_2;
import static org.folio.rest.impl.TitlesTestData.STUB_TITLE_ID;
import static org.folio.rest.impl.TitlesTestData.STUB_TITLE_NAME;
import static org.folio.test.util.TestUtil.mockGet;
import static org.folio.test.util.TestUtil.readFile;
import static org.folio.util.AccessTypesTestUtil.STUB_ACCESS_TYPE_NAME;
import static org.folio.util.AccessTypesTestUtil.STUB_ACCESS_TYPE_NAME_2;
import static org.folio.util.AccessTypesTestUtil.insertAccessTypeMapping;
import static org.folio.util.AccessTypesTestUtil.insertAccessTypes;
import static org.folio.util.AccessTypesTestUtil.testData;
import static org.folio.util.AssertTestUtil.assertEqualsLong;
import static org.folio.util.AssertTestUtil.assertErrorContainsTitle;
import static org.folio.util.KBTestUtil.clearDataFromTable;
import static org.folio.util.KBTestUtil.getDefaultKbConfiguration;
import static org.folio.util.KBTestUtil.setupDefaultKBConfiguration;
import static org.folio.util.KbCredentialsTestUtil.STUB_TOKEN_HEADER;
import static org.folio.util.ResourcesTestUtil.buildResource;
import static org.folio.util.ResourcesTestUtil.saveResource;
import static org.folio.util.TagsTestUtil.saveTag;
import static org.folio.util.TitlesTestUtil.mockGetTitles;

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
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.json.JSONException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.JSONAssert;

import org.folio.repository.RecordType;
import org.folio.repository.titles.DbTitle;
import org.folio.rest.impl.WireMockTestBase;
import org.folio.rest.jaxrs.model.AccessType;
import org.folio.rest.jaxrs.model.Errors;
import org.folio.rest.jaxrs.model.JsonapiError;
import org.folio.rest.jaxrs.model.KbCredentials;
import org.folio.rest.jaxrs.model.Tags;
import org.folio.rest.jaxrs.model.Title;
import org.folio.rest.jaxrs.model.TitleCollection;
import org.folio.rest.jaxrs.model.TitleCollectionItem;
import org.folio.rest.jaxrs.model.TitlePostRequest;
import org.folio.rest.jaxrs.model.TitlePutRequest;
import org.folio.util.TagsTestUtil;
import org.folio.util.TitlesTestUtil;

@RunWith(VertxUnitRunner.class)
public class EholdingsTitlesTest extends WireMockTestBase {

  public static final String EHOLDINGS_TITLES_PATH = "eholdings/titles";

  private KbCredentials configuration;

  @Override @Before
  public void setUp() throws Exception {
    super.setUp();
    setupDefaultKBConfiguration(getWiremockUrl(), vertx);
    configuration = getDefaultKbConfiguration(vertx);
    setUpTestUsers();
  }

  @After
  public void tearDown() {
    clearDataFromTable(vertx, ACCESS_TYPES_MAPPING_TABLE_NAME);
    clearDataFromTable(vertx, ACCESS_TYPES_TABLE_NAME);
    clearDataFromTable(vertx, HOLDINGS_TABLE);
    clearDataFromTable(vertx, TAGS_TABLE_NAME);
    clearDataFromTable(vertx, TITLES_TABLE_NAME);
    clearDataFromTable(vertx, RESOURCES_TABLE_NAME);
    clearDataFromTable(vertx, KB_CREDENTIALS_TABLE_NAME);
    tearDownTestUsers();
  }

  @Test
  public void shouldReturnTitlesOnGet() throws IOException, URISyntaxException, JSONException {
    String stubResponseFile = "responses/rmapi/titles/searchTitles.json";

    stubFor(
      get(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/titles.*"), true))
        .willReturn(new ResponseDefinitionBuilder()
          .withBody(readFile(stubResponseFile))));

    String resourcePath = EHOLDINGS_TITLES_PATH + "?page=1&filter[name]=Mind&sort=name";
    String actualResponse = getWithOk(resourcePath, STUB_TOKEN_HEADER).asString();
    JSONAssert.assertEquals(readFile("responses/kb-ebsco/titles/expected-titles.json"), actualResponse, true);
  }

  @Test
  public void shouldReturnTitlesOnGetWithResources() throws IOException, URISyntaxException, JSONException {
    String stubResponseFile = "responses/rmapi/titles/searchTitles.json";

    stubFor(
      get(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/titles.*"), true))
        .willReturn(new ResponseDefinitionBuilder()
          .withBody(readFile(stubResponseFile))));

    String resourcePath = EHOLDINGS_TITLES_PATH + "?page=1&filter[name]=Mind&sort=name&include=resources";
    String actualResponse = getWithOk(resourcePath, STUB_TOKEN_HEADER).asString();
    JSONAssert.assertEquals(readFile("responses/kb-ebsco/titles/expected-titles-with-resources.json"), actualResponse, true);
  }

  @Test
  public void shouldReturnTitlesOnSearchByTags() throws IOException, URISyntaxException, JSONException {
    String stubResponseFile1 = "responses/rmapi/titles/get-title-by-id-response.json";
    String stubResponseFile2 = "responses/rmapi/titles/get-title-by-id-2-response.json";

    stubFor(
      get(urlMatching("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/titles/" + STUB_MANAGED_TITLE_ID))
        .willReturn(aResponse().withBody(readFile(stubResponseFile1))));

    stubFor(
      get(urlMatching("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/titles/" + STUB_MANAGED_TITLE_ID_2))
        .willReturn(aResponse().withBody(readFile(stubResponseFile2))));

    saveResource(buildResource(STUB_MANAGED_RESOURCE_ID, configuration.getId(), STUB_TITLE_NAME), vertx);
    saveResource(buildResource(STUB_MANAGED_RESOURCE_ID_2, configuration.getId(), STUB_CUSTOM_TITLE_NAME), vertx);
    saveTag(vertx, STUB_MANAGED_RESOURCE_ID, RecordType.RESOURCE, STUB_TAG_VALUE);
    saveTag(vertx, STUB_MANAGED_RESOURCE_ID_2, RecordType.RESOURCE, STUB_TAG_VALUE_2);

    String resourcePath = EHOLDINGS_TITLES_PATH + "?filter[tags]=" + STUB_TAG_VALUE + "&filter[tags]=" + STUB_TAG_VALUE_2;
    String actualResponse = getWithOk(resourcePath, STUB_TOKEN_HEADER).asString();
    JSONAssert.assertEquals(readFile("responses/kb-ebsco/titles/expected-tagged-titles.json"), actualResponse, true);
  }

  @Test
  public void shouldReturnTitlesOnSearchByTagsWithResources() throws IOException, URISyntaxException, JSONException {
    String stubResponseFile1 = "responses/rmapi/titles/get-title-by-id-response.json";
    String stubResponseFile2 = "responses/rmapi/titles/get-title-by-id-2-response.json";

    stubFor(
      get(urlMatching("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/titles/" + STUB_MANAGED_TITLE_ID))
        .willReturn(aResponse().withBody(readFile(stubResponseFile1))));

    stubFor(
      get(urlMatching("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/titles/" + STUB_MANAGED_TITLE_ID_2))
        .willReturn(aResponse().withBody(readFile(stubResponseFile2))));

    saveResource(buildResource(STUB_MANAGED_RESOURCE_ID, configuration.getId(), STUB_TITLE_NAME), vertx);
    saveResource(buildResource(STUB_MANAGED_RESOURCE_ID_2, configuration.getId(), STUB_CUSTOM_TITLE_NAME), vertx);
    saveResource(buildResource(STUB_MANAGED_RESOURCE_ID_3, configuration.getId(), STUB_CUSTOM_TITLE_NAME), vertx);
    saveTag(vertx, STUB_MANAGED_RESOURCE_ID, RecordType.RESOURCE, STUB_TAG_VALUE);
    saveTag(vertx, STUB_MANAGED_RESOURCE_ID_2, RecordType.RESOURCE, STUB_TAG_VALUE_2);
    saveTag(vertx, STUB_MANAGED_RESOURCE_ID_3, RecordType.RESOURCE, STUB_TAG_VALUE_2);

    String resourcePath = EHOLDINGS_TITLES_PATH + "?filter[tags]=" + STUB_TAG_VALUE+ "&filter[tags]=" + STUB_TAG_VALUE_2+"&include=resources";
    String actualResponse = getWithOk(resourcePath, STUB_TOKEN_HEADER).asString();
    JSONAssert.assertEquals(readFile("responses/kb-ebsco/titles/expected-tagged-titles-with-resources.json"), actualResponse, true);
  }

  @Test
  public void shouldReturnSecondTitleOnSearchByTagsWithPagination() throws IOException, URISyntaxException {
    String stubResponseFile1 = "responses/rmapi/titles/get-title-by-id-response.json";
    String stubResponseFile2 = "responses/rmapi/titles/get-title-by-id-2-response.json";

    stubFor(
      get(urlMatching("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/titles/" + STUB_MANAGED_TITLE_ID))
        .willReturn(aResponse().withBody(readFile(stubResponseFile1))));

    stubFor(
      get(urlMatching("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/titles/" + STUB_MANAGED_TITLE_ID_2))
        .willReturn(aResponse().withBody(readFile(stubResponseFile2))));

    saveResource(buildResource(STUB_MANAGED_RESOURCE_ID, configuration.getId(), STUB_TITLE_NAME), vertx);
    saveResource(buildResource(STUB_MANAGED_RESOURCE_ID_2, configuration.getId(), STUB_CUSTOM_TITLE_NAME), vertx);
    saveTag(vertx, STUB_MANAGED_RESOURCE_ID, RecordType.RESOURCE, STUB_TAG_VALUE);
    saveTag(vertx, STUB_MANAGED_RESOURCE_ID_2, RecordType.RESOURCE, STUB_TAG_VALUE_2);

    String resourcePath = EHOLDINGS_TITLES_PATH + "?page=2&count=1&filter[tags]="
      + STUB_TAG_VALUE + "&filter[tags]=" + STUB_TAG_VALUE_2;
    TitleCollection response = getWithOk(resourcePath, STUB_TOKEN_HEADER).as(TitleCollection.class);
    assertEquals(STUB_MANAGED_TITLE_ID_2, response.getData().get(0).getId());
  }

  @Test
  public void shouldReturnTitlesOnSearchByAccessTypes() throws IOException, URISyntaxException {
    List<AccessType> accessTypes = insertAccessTypes(testData(configuration.getId()), vertx);
    insertAccessTypeMapping(STUB_MANAGED_RESOURCE_ID, RESOURCE, accessTypes.get(0).getId(), vertx);
    insertAccessTypeMapping(STUB_MANAGED_RESOURCE_ID_2, RESOURCE, accessTypes.get(0).getId(), vertx);

    mockGetTitles();

    String resourcePath = EHOLDINGS_TITLES_PATH + "?filter[access-type]=" + STUB_ACCESS_TYPE_NAME;
    TitleCollection titleCollection = getWithOk(resourcePath, STUB_TOKEN_HEADER).as(TitleCollection.class);
    List<TitleCollectionItem> titles = titleCollection.getData();

    assertThat(titles, hasSize(2));
    assertEquals(2, (int) titleCollection.getMeta().getTotalResults());
    assertThat(titles, everyItem(hasProperty("id",
      anyOf(equalTo(STUB_MANAGED_TITLE_ID), equalTo(STUB_MANAGED_TITLE_ID_2))
    )));
  }

  @Test
  public void shouldReturnTitlesWithResourcesOnSearchByAccessTypes() throws IOException, URISyntaxException {
    List<AccessType> accessTypes = insertAccessTypes(testData(configuration.getId()), vertx);
    insertAccessTypeMapping(STUB_MANAGED_RESOURCE_ID, RESOURCE, accessTypes.get(0).getId(), vertx);
    insertAccessTypeMapping(STUB_MANAGED_RESOURCE_ID_2, RESOURCE, accessTypes.get(0).getId(), vertx);

    mockGetTitles();

    String resourcePath = EHOLDINGS_TITLES_PATH + "?filter[access-type]=" + STUB_ACCESS_TYPE_NAME + "&include=resources";
    TitleCollection titleCollection = getWithOk(resourcePath, STUB_TOKEN_HEADER).as(TitleCollection.class);
    List<TitleCollectionItem> titles = titleCollection.getData();

    assertThat(titles, hasSize(2));
    assertEquals(2, (int) titleCollection.getMeta().getTotalResults());
    assertThat(titles, everyItem(hasProperty("id",
      anyOf(equalTo(STUB_MANAGED_TITLE_ID), equalTo(STUB_MANAGED_TITLE_ID_2))
    )));
    assertThat(titles, everyItem(hasProperty("included", not(empty()))));
  }

  @Test
  public void shouldReturnTitleOnSearchByAccessTypesWithPagination() throws IOException, URISyntaxException {
    List<AccessType> accessTypes = insertAccessTypes(testData(configuration.getId()), vertx);
    insertAccessTypeMapping(STUB_MANAGED_RESOURCE_ID, RESOURCE, accessTypes.get(0).getId(), vertx);
    insertAccessTypeMapping(STUB_MANAGED_RESOURCE_ID_2, RESOURCE, accessTypes.get(1).getId(), vertx);

    mockGetTitles();

    String resourcePath = EHOLDINGS_TITLES_PATH + "?page=2&count=1&filter[access-type]=" + STUB_ACCESS_TYPE_NAME
      + "&filter[access-type]=" + STUB_ACCESS_TYPE_NAME_2;
    TitleCollection titleCollection = getWithOk(resourcePath, STUB_TOKEN_HEADER).as(TitleCollection.class);
    List<TitleCollectionItem> titles = titleCollection.getData();

    assertThat(titles, hasSize(1));
    assertEquals(2, (int) titleCollection.getMeta().getTotalResults());
    assertThat(titles, everyItem(hasProperty("id", equalTo(STUB_MANAGED_TITLE_ID))));
  }

  @Test
  public void shouldReturnEmptyTitlesOnSearchByAccessTypesThatIsNotExist() throws IOException, URISyntaxException {
    List<AccessType> accessTypes = insertAccessTypes(testData(configuration.getId()), vertx);
    insertAccessTypeMapping(STUB_MANAGED_RESOURCE_ID, RESOURCE, accessTypes.get(0).getId(), vertx);
    insertAccessTypeMapping(STUB_MANAGED_RESOURCE_ID_2, RESOURCE, accessTypes.get(1).getId(), vertx);

    mockGetTitles();

    String resourcePath = EHOLDINGS_TITLES_PATH + "?filter[access-type]=Not Exist";
    TitleCollection titleCollection = getWithOk(resourcePath, STUB_TOKEN_HEADER).as(TitleCollection.class);
    List<TitleCollectionItem> titles = titleCollection.getData();

    assertThat(titles, hasSize(0));
    assertEquals(0, (int) titleCollection.getMeta().getTotalResults());
  }

  @Test
  public void shouldReturn400IfCountOutOfRange() {
    JsonapiError error =
      getWithStatus(EHOLDINGS_TITLES_PATH + "?count=1000&page=1&filter[name]=Mind&sort=name", SC_BAD_REQUEST,
        STUB_TOKEN_HEADER).as(JsonapiError.class);

    assertErrorContainsTitle(error, "parameter value {1000} is not valid");
  }

  @Test
  public void shouldReturn500WhenRMApiReturns500Error() {
    stubFor(
      get(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/titles.*"), true))
        .willReturn(new ResponseDefinitionBuilder()
          .withStatus(500)));

    JsonapiError error =
      getWithStatus(EHOLDINGS_TITLES_PATH + "?filter[name]=news", SC_INTERNAL_SERVER_ERROR, STUB_TOKEN_HEADER)
        .as(JsonapiError.class);

    assertErrorContainsTitle(error, "Invalid RMAPI response");
  }

  @Test
  public void shouldReturnTitleWhenValidId() throws IOException, URISyntaxException, JSONException {
    mockGetManagedTitleById();
    String actualResponse = getWithOk(EHOLDINGS_TITLES_PATH + "/" + STUB_TITLE_ID, STUB_TOKEN_HEADER).asString();
    JSONAssert.assertEquals(
      readFile("responses/kb-ebsco/titles/expected-title-by-id.json"), actualResponse, false);
  }

  @Test
  public void shouldReturnTitleTagsWhenValidId() throws IOException, URISyntaxException {
    String stubResponseFile = "responses/rmapi/titles/get-title-by-id-response.json";
    saveTag(vertx, STUB_MANAGED_TITLE_ID, RecordType.TITLE, STUB_TAG_VALUE);

    stubFor(
      get(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/titles.*"), true))
        .willReturn(new ResponseDefinitionBuilder()
          .withBody(readFile(stubResponseFile))));

    Title actualResponse = getWithOk(EHOLDINGS_TITLES_PATH + "/" + STUB_TITLE_ID, STUB_TOKEN_HEADER).as(Title.class);

    assertTrue(actualResponse.getData().getAttributes().getTags().getTagList().contains(STUB_TAG_VALUE));
  }

  @Test
  public void shouldReturn404WhenRMAPINotFoundOnTitleGet() throws IOException, URISyntaxException {
    String stubResponseFile = "responses/rmapi/titles/get-title-by-id-not-found-response.json";

    stubFor(
      get(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/titles.*"), true))
        .willReturn(new ResponseDefinitionBuilder()
          .withBody(readFile(stubResponseFile))
          .withStatus(404)));

    JsonapiError error = getWithStatus(EHOLDINGS_TITLES_PATH + "/" + STUB_TITLE_ID, SC_NOT_FOUND, STUB_TOKEN_HEADER)
      .as(JsonapiError.class);

    assertErrorContainsTitle(error, "Title not found");
  }

  @Test
  public void shouldReturn400WhenValidationErrorOnTitleGet() {
    JsonapiError error = getWithStatus(EHOLDINGS_TITLES_PATH + "/12345aaa", SC_BAD_REQUEST, STUB_TOKEN_HEADER)
      .as(JsonapiError.class);

    assertErrorContainsTitle(error, "Title id is invalid - 12345aaa");
  }

  @Test
  public void shouldReturn500WhenRMApiReturns500ErrorOnTitleGet() {
    stubFor(
      get(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/titles.*"), true))
        .willReturn(new ResponseDefinitionBuilder()
          .withStatus(500)));

    JsonapiError error =
      getWithStatus(EHOLDINGS_TITLES_PATH + "/" + STUB_TITLE_ID, SC_INTERNAL_SERVER_ERROR, STUB_TOKEN_HEADER)
        .as(JsonapiError.class);

    assertErrorContainsTitle(error, "Invalid RMAPI response");
  }

  @Test
  public void shouldReturnTitleWithSortedResourcesWhenIncludeResources() throws IOException, URISyntaxException, JSONException {
    String rmapiResponseFile = "responses/rmapi/titles/get-title-by-id-response-with-resources.json";
    String rmapiUrl = "/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/titles.*";

    mockGet(new RegexPattern(rmapiUrl), rmapiResponseFile);

    String actual = getWithStatus(EHOLDINGS_TITLES_PATH + "/" + STUB_TITLE_ID + "?include=resources", SC_OK,
      STUB_TOKEN_HEADER).asString();
    String expected = readFile("responses/kb-ebsco/titles/get-title-by-id-include-resources-response.json");

    JSONAssert.assertEquals(expected, actual, true);
  }

  @Test
  public void shouldReturnTitleWithResourcesWhenIncludeResourcesWithTags() throws IOException, URISyntaxException, JSONException {
    saveTag(vertx, STUB_MANAGED_RESOURCE_ID, RecordType.RESOURCE, STUB_TAG_VALUE);

    String rmapiResponseFile = "responses/rmapi/titles/get-title-by-id-response.json";
    String rmapiUrl = "/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/titles.*";

    mockGet(new RegexPattern(rmapiUrl), rmapiResponseFile);

    String actual = getWithStatus(EHOLDINGS_TITLES_PATH + "/" + STUB_TITLE_ID + "?include=resources", SC_OK,
      STUB_TOKEN_HEADER).asString();
    String expected = readFile("responses/kb-ebsco/titles/get-title-by-id-include-resources-with-tags-response.json");

    JSONAssert.assertEquals(expected, actual, true);
  }

  @Test
  public void shouldReturnTitleWithoutResourcesWhenInvalidInclude() throws IOException, URISyntaxException, JSONException {
    String rmapiResponseFile = "responses/rmapi/titles/get-title-by-id-response.json";
    String rmapiUrl = "/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/titles.*";

    mockGet(new RegexPattern(rmapiUrl), rmapiResponseFile);

    String actual = getWithStatus(EHOLDINGS_TITLES_PATH + "/" + STUB_TITLE_ID + "?include=badValue", SC_OK,
      STUB_TOKEN_HEADER).asString();
    String expected = readFile("responses/kb-ebsco/titles/get-title-by-id-invalid-include-response.json");

    JSONAssert.assertEquals(expected, actual, false);
  }

  @Test
  public void shouldReturnTitleWhenValidPostRequest() throws IOException, URISyntaxException, JSONException {
    String actual = postTitle(Collections.emptyList()).asString();
    String expected = readFile("responses/kb-ebsco/titles/get-created-title-response.json");

    JSONAssert.assertEquals(expected, actual, false);
  }

  @Test
  public void shouldUpdateTagsWhenValidPostRequest() throws IOException, URISyntaxException {
    List<String> tagList = Arrays.asList(STUB_TAG_VALUE, STUB_TAG_VALUE_2);
    Title actual = postTitle(tagList).as(Title.class);
    List<String> tagsFromDB = TagsTestUtil.getTags(vertx);
    assertThat(actual.getData().getAttributes().getTags().getTagList(), containsInAnyOrder(tagList.toArray()));
    assertThat(tagsFromDB, containsInAnyOrder(tagList.toArray()));
  }

  @Test
  public void shouldAddTitleDataOnPost() throws URISyntaxException, IOException {
    postTitle(Collections.singletonList(STUB_TAG_VALUE));
    List<DbTitle> titles = TitlesTestUtil.getTitles(vertx);
    assertEquals(1, titles.size());
    assertEquals(STUB_TITLE_ID, String.valueOf(titles.get(0).getId()));
    assertEquals("Test Title", titles.get(0).getName());
  }

  @Test
  public void shouldReturn400WhenInvalidPostRequest() throws URISyntaxException, IOException {
    String errorResponse = "responses/rmapi/packages/post-package-400-error-response.json";
    String titlePostStubRequestFile = "requests/kb-ebsco/title/post-title-request.json";
    EqualToJsonPattern postBodyPattern = new EqualToJsonPattern(
      "{\n  \"titleName\" : \"Test Title\",\n  \"edition\" : \"Test edition\",\n  \"publisherName\" : \"Test publisher\",\n  \"pubType\" : \"thesisdissertation\",\n  \"description\" : \"Lorem ipsum dolor sit amet, consectetuer adipiscing elit.\",\n  \"isPeerReviewed\" : true,\n  \"identifiersList\" : [ {\n    \"id\" : \"1111-2222-3333\",\n    \"subtype\" : 2,\n    \"type\" : 0\n  } ],\n  \"contributorsList\" : [ {\n    \"type\" : \"author\",\n    \"contributor\" : \"smith, john\"\n  }, {\n    \"type\" : \"illustrator\",\n    \"contributor\" : \"smith, ralph\"\n  } ],\n  \"peerReviewed\" : true\n}",
      true, true);

    stubFor(
      post(new UrlPathPattern(new EqualToPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors/"
        + STUB_VENDOR_ID + "/packages/" + STUB_PACKAGE_ID + "/titles"), false))
        .withRequestBody(postBodyPattern)
        .willReturn(new ResponseDefinitionBuilder()
          .withBody(readFile(errorResponse))
          .withStatus(SC_BAD_REQUEST)));

    JsonapiError error =
      postWithStatus(EHOLDINGS_TITLES_PATH, readFile(titlePostStubRequestFile), SC_BAD_REQUEST, STUB_TOKEN_HEADER)
        .as(JsonapiError.class);

    assertErrorContainsTitle(error, "Package with the provided name already exists");
  }

  @Test
  public void shouldReturnUpdatedValuesForCustomTitleOnSuccessfulPut() throws IOException, URISyntaxException, JSONException {
    String expectedTitleFile = "responses/kb-ebsco/titles/expected-updated-title.json";
    String actualResponse = putTitle(null);

    JSONAssert.assertEquals(readFile(expectedTitleFile), actualResponse, false);

    verify(1, putRequestedFor(new UrlPathPattern(new RegexPattern(CUSTOM_RESOURCE_ENDPOINT), true))
      .withRequestBody(
        equalToJson(readFile("requests/rmapi/resources/put-custom-resource-is-selected-multiple-attributes.json"))));
  }

  @Test
  public void shouldUpdateTitleTagsOnSuccessfulPut() throws IOException, URISyntaxException {
    List<String> newTags = Arrays.asList(STUB_TAG_VALUE, STUB_TAG_VALUE_2);

    putTitle(newTags);

    List<String> tags = TagsTestUtil.getTags(vertx);
    assertThat(tags, containsInAnyOrder(newTags.toArray()));
  }

  @Test
  public void shouldUpdateOnlyTagsOnPutForNonCustomTitle() throws IOException, URISyntaxException {
    String resourceResponse = "responses/rmapi/resources/get-managed-resource-updated-response.json";
    ObjectMapper mapper = new ObjectMapper();
    TitlePutRequest request = mapper.readValue(readFile("requests/kb-ebsco/title/put-title.json"),
      TitlePutRequest.class);
    List<String> newTags = Arrays.asList(STUB_TAG_VALUE, STUB_TAG_VALUE_2);
    request.getData().getAttributes().setTags(new Tags().withTagList(newTags));

    stubFor(get(new UrlPathPattern(new RegexPattern(CUSTOM_TITLE_ENDPOINT), false)).willReturn(
      new ResponseDefinitionBuilder().withBody(readFile(resourceResponse))));

    putWithOk(EHOLDINGS_TITLES_PATH + "/" + STUB_CUSTOM_TITLE_ID, mapper.writeValueAsString(request),
      STUB_TOKEN_HEADER);

    List<String> tags = TagsTestUtil.getTags(vertx);
    assertThat(tags, containsInAnyOrder(newTags.toArray()));
    WireMock.verify(0, putRequestedFor(anyUrl()));
  }

  @Test
  public void shouldAddTitleDataOnPut() throws IOException, URISyntaxException {
    putTitle(Arrays.asList(STUB_TAG_VALUE, STUB_TAG_VALUE_2));

    List<DbTitle> titles = TitlesTestUtil.getTitles(vertx);
    assertEquals(1, titles.size());
    assertEqualsLong(titles.get(0).getId());
    assertEquals("sd-test-java-again", titles.get(0).getName());
  }

  @Test
  public void shouldDeleteTitleDataOnPutWithEmptyTagList() throws IOException, URISyntaxException {
    putTitle(Collections.singletonList(STUB_TAG_VALUE));
    putTitle(Collections.emptyList());

    List<DbTitle> packages = TitlesTestUtil.getTitles(vertx);
    assertThat(packages, is(empty()));
  }

  @Test
  public void shouldUpdateTitleDataOnSecondPut() throws IOException, URISyntaxException {
    String newName = "new name";
    String updatedResponse = "responses/rmapi/resources/get-custom-resource-updated-title-name-response.json";

    putTitle(readFile(updatedResponse), Collections.singletonList(STUB_TAG_VALUE));

    ObjectMapper mapper = new ObjectMapper();
    TitlePutRequest request = mapper.readValue(readFile("requests/kb-ebsco/title/put-title.json"),
      TitlePutRequest.class);
    request.getData().getAttributes().withName(newName);

    stubFor(get(new UrlPathPattern(new RegexPattern(CUSTOM_TITLE_ENDPOINT), false)).willReturn(
      new ResponseDefinitionBuilder().withBody(readFile(updatedResponse))));

    putWithOk(EHOLDINGS_TITLES_PATH + "/" + STUB_CUSTOM_TITLE_ID, mapper.writeValueAsString(request),
      STUB_TOKEN_HEADER);

    List<DbTitle> titles = TitlesTestUtil.getTitles(vertx);
    assertEquals(1, titles.size());
    assertEqualsLong(titles.get(0).getId());
    assertEquals(newName, titles.get(0).getName());
  }

  @Test
  public void shouldReturn422WhenNameIsNotProvided() throws URISyntaxException, IOException {
    Errors error = putWithStatus(EHOLDINGS_TITLES_PATH + "/" + STUB_TITLE_ID,
      readFile("requests/kb-ebsco/title/put-title-null-name.json"), SC_UNPROCESSABLE_ENTITY, STUB_TOKEN_HEADER)
      .as(Errors.class);

    assertThat(error.getErrors().get(0).getMessage(), containsString("must not be null"));
  }

  private String putTitle(List<String> tags) throws IOException, URISyntaxException {
    String updatedResourceResponse = "responses/rmapi/resources/get-custom-resource-updated-response.json";
    return putTitle(readFile(updatedResourceResponse), tags);
  }

  private String putTitle(String updatedResourceResponse, List<String> tags) throws IOException, URISyntaxException {
    stubFor(
      get(new UrlPathPattern(new RegexPattern(CUSTOM_TITLE_ENDPOINT), false))
        .willReturn(new ResponseDefinitionBuilder().withBody(updatedResourceResponse)));

    stubFor(
      put(new UrlPathPattern(new RegexPattern(CUSTOM_RESOURCE_ENDPOINT), true))
        .willReturn(new ResponseDefinitionBuilder().withStatus(SC_NO_CONTENT)));

    ObjectMapper mapper = new ObjectMapper();
    TitlePutRequest titleToBeUpdated =
      mapper.readValue(readFile("requests/kb-ebsco/title/put-title.json"), TitlePutRequest.class);

    if (tags != null) {
      titleToBeUpdated.getData().getAttributes().setTags(new Tags()
        .withTagList(tags));
    }

    return putWithOk(EHOLDINGS_TITLES_PATH + "/" + STUB_CUSTOM_TITLE_ID, mapper.writeValueAsString(titleToBeUpdated),
      STUB_TOKEN_HEADER).asString();
  }

  private ExtractableResponse<Response> postTitle(List<String> tags) throws IOException, URISyntaxException {
    String titleCreatedIdStubResponseFile = "responses/rmapi/titles/post-title-response.json";
    String titlePostStubRequestFile = "requests/kb-ebsco/title/post-title-request.json";
    String getTitleByTitleIdStubFile = "responses/rmapi/titles/get-title-by-id-for-post-request.json";

    EqualToJsonPattern postBodyPattern = new EqualToJsonPattern(
      "{\n  \"titleName\" : \"Test Title\",\n  \"edition\" : \"Test edition\",\n  \"publisherName\" : \"Test publisher\",\n  \"pubType\" : \"thesisdissertation\",\n  \"description\" : \"Lorem ipsum dolor sit amet, consectetuer adipiscing elit.\",\n  \"isPeerReviewed\" : true,\n  \"identifiersList\" : [ {\n    \"id\" : \"1111-2222-3333\",\n    \"subtype\" : 2,\n    \"type\" : 0\n  } ],\n  \"contributorsList\" : [ {\n    \"type\" : \"author\",\n    \"contributor\" : \"smith, john\"\n  }, {\n    \"type\" : \"illustrator\",\n    \"contributor\" : \"smith, ralph\"\n  } ],\n  \"peerReviewed\" : true\n} \"userDefinedField2\": \"test 2\",\n\"userDefinedField3\": \"\",\n\"userDefinedField4\" : null,\n\"userDefinedField5\": \"test 5\"",
      true, true);

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

    return postWithOk(EHOLDINGS_TITLES_PATH, mapper.writeValueAsString(request), STUB_TOKEN_HEADER);
  }

  private void mockGetManagedTitleById() throws IOException, URISyntaxException {
    String stubResponseFile = "responses/rmapi/titles/get-title-by-id-response.json";

    stubFor(
      get(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/titles.*"), true))
        .willReturn(new ResponseDefinitionBuilder()
          .withBody(readFile(stubResponseFile))));
  }

}
