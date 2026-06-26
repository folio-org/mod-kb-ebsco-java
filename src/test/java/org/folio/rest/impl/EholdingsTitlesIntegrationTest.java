package org.folio.rest.impl;

import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNPROCESSABLE_ENTITY;
import static org.folio.repository.RecordType.RESOURCE;
import static org.folio.repository.accesstypes.AccessTypeMappingsTableConstants.ACCESS_TYPES_MAPPING_TABLE_NAME;
import static org.folio.repository.accesstypes.AccessTypesTableConstants.ACCESS_TYPES_TABLE_NAME;
import static org.folio.repository.holdings.HoldingsTableConstants.HOLDINGS_TABLE;
import static org.folio.repository.kbcredentials.KbCredentialsTableConstants.KB_CREDENTIALS_TABLE_NAME;
import static org.folio.repository.resources.ResourceTableConstants.RESOURCES_TABLE_NAME;
import static org.folio.repository.tag.TagTableConstants.TAGS_TABLE_NAME;
import static org.folio.repository.titles.TitlesTableConstants.TITLES_TABLE_NAME;
import static org.folio.util.AccessTypesTestUtil.STUB_ACCESS_TYPE_NAME;
import static org.folio.util.AccessTypesTestUtil.STUB_ACCESS_TYPE_NAME_2;
import static org.folio.util.AccessTypesTestUtil.insertAccessTypeMapping;
import static org.folio.util.AccessTypesTestUtil.insertAccessTypes;
import static org.folio.util.AccessTypesTestUtil.testData;
import static org.folio.util.AssertTestUtil.assertEqualsLong;
import static org.folio.util.AssertTestUtil.assertErrorContainsTitle;
import static org.folio.util.AssertTestUtil.assertJsonEqual;
import static org.folio.util.KbCredentialsTestUtil.setupDefaultKbConfiguration;
import static org.folio.util.ResourcesTestUtil.buildResource;
import static org.folio.util.ResourcesTestUtil.saveResource;
import static org.folio.util.TagsTestUtil.saveTag;
import static org.folio.util.TestUtil.clearDataFromTable;
import static org.folio.util.TestUtil.readFile;
import static org.folio.util.TestUtil.readJsonFile;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.tomakehurst.wiremock.client.WireMock;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import io.vertx.core.json.Json;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.folio.repository.RecordType;
import org.folio.rest.jaxrs.model.Errors;
import org.folio.rest.jaxrs.model.JsonapiError;
import org.folio.rest.jaxrs.model.Tags;
import org.folio.rest.jaxrs.model.Title;
import org.folio.rest.jaxrs.model.TitleCollection;
import org.folio.rest.jaxrs.model.TitlePostRequest;
import org.folio.rest.jaxrs.model.TitlePutRequest;
import org.folio.util.IntegrationTestBase;
import org.folio.util.TagsTestUtil;
import org.folio.util.TitlesTestUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EholdingsTitlesIntegrationTest extends IntegrationTestBase {

  private static final String EHOLDINGS_TITLES_PATH = "eholdings/titles";

  // RMAPI stub response files
  private static final String RMAPI_TITLES_SEARCH_RESPONSE =
    "responses/rmapi/titles/searchTitles.json";
  private static final String RMAPI_TITLE_BY_ID_RESPONSE =
    "responses/rmapi/titles/get-title-by-id-response.json";
  private static final String RMAPI_TITLE_BY_ID_2_RESPONSE =
    "responses/rmapi/titles/get-title-by-id-2-response.json";
  private static final String RMAPI_TITLE_NOT_FOUND_RESPONSE =
    "responses/rmapi/titles/get-title-by-id-not-found-response.json";
  private static final String RMAPI_TITLE_WITH_RESOURCES_RESPONSE =
    "responses/rmapi/titles/get-title-by-id-response-with-resources.json";
  private static final String RMAPI_POST_TITLE_RESPONSE =
    "responses/rmapi/titles/post-title-response.json";
  private static final String RMAPI_TITLE_FOR_POST_RESPONSE =
    "responses/rmapi/titles/get-title-by-id-for-post-request.json";
  private static final String RMAPI_PACKAGE_400_RESPONSE =
    "responses/rmapi/packages/post-package-400-error-response.json";
  private static final String RMAPI_RESOURCE_UPDATED_RESPONSE =
    "responses/rmapi/resources/get-custom-resource-updated-response.json";
  private static final String RMAPI_RESOURCE_UPDATED_TITLE_NAME_RESPONSE =
    "responses/rmapi/resources/get-custom-resource-updated-title-name-response.json";
  private static final String RMAPI_MANAGED_RESOURCE_UPDATED_RESPONSE =
    "responses/rmapi/resources/get-managed-resource-updated-response.json";

  // KB-EBSCO expected response files
  private static final String EXPECTED_TITLES_RESPONSE =
    "responses/kb-ebsco/titles/expected-titles.json";
  private static final String EXPECTED_TITLES_WITH_RESOURCES_RESPONSE =
    "responses/kb-ebsco/titles/expected-titles-with-resources.json";
  private static final String EXPECTED_TAGGED_TITLES_RESPONSE =
    "responses/kb-ebsco/titles/expected-tagged-titles.json";
  private static final String EXPECTED_TAGGED_TITLES_WITH_RESOURCES_RESPONSE =
    "responses/kb-ebsco/titles/expected-tagged-titles-with-resources.json";
  private static final String EXPECTED_TITLE_BY_ID_RESPONSE =
    "responses/kb-ebsco/titles/expected-title-by-id.json";
  private static final String EXPECTED_TITLE_WITH_RESOURCES_RESPONSE =
    "responses/kb-ebsco/titles/get-title-by-id-include-resources-response.json";
  private static final String EXPECTED_TITLE_WITH_RESOURCES_AND_TAGS_RESPONSE =
    "responses/kb-ebsco/titles/get-title-by-id-include-resources-with-tags-response.json";
  private static final String EXPECTED_TITLE_INVALID_INCLUDE_RESPONSE =
    "responses/kb-ebsco/titles/get-title-by-id-invalid-include-response.json";
  private static final String EXPECTED_CREATED_TITLE_RESPONSE =
    "responses/kb-ebsco/titles/get-created-title-response.json";
  private static final String EXPECTED_UPDATED_TITLE_RESPONSE =
    "responses/kb-ebsco/titles/expected-updated-title.json";

  // Request files
  private static final String POST_TITLE_REQUEST =
    "requests/kb-ebsco/title/post-title-request.json";
  private static final String PUT_TITLE_REQUEST =
    "requests/kb-ebsco/title/put-title.json";
  private static final String PUT_TITLE_NULL_NAME_REQUEST =
    "requests/kb-ebsco/title/put-title-null-name.json";
  private static final String RMAPI_PUT_RESOURCE_REQUEST =
    "requests/rmapi/resources/put-custom-resource-is-selected-multiple-attributes.json";

  private String credentialsId;

  @BeforeEach
  void setUp() {
    credentialsId = setupDefaultKbConfiguration(getWiremockUrl(), vertx);
  }

  @AfterEach
  void tearDown() {
    clearDataFromTable(vertx, ACCESS_TYPES_MAPPING_TABLE_NAME);
    clearDataFromTable(vertx, ACCESS_TYPES_TABLE_NAME);
    clearDataFromTable(vertx, HOLDINGS_TABLE);
    clearDataFromTable(vertx, TAGS_TABLE_NAME);
    clearDataFromTable(vertx, TITLES_TABLE_NAME);
    clearDataFromTable(vertx, RESOURCES_TABLE_NAME);
    clearDataFromTable(vertx, KB_CREDENTIALS_TABLE_NAME);
  }

  @Test
  void shouldReturnTitlesOnGet() {
    mockSearchTitles(RMAPI_TITLES_SEARCH_RESPONSE);

    var actualResponse = getWithOk(EHOLDINGS_TITLES_PATH + "?page=1&filter[name]=Mind&sort=name").asString();
    assertJsonEqual(readFile(EXPECTED_TITLES_RESPONSE), actualResponse);
  }

  @Test
  void shouldReturnTitlesOnGetWithResources() {
    mockSearchTitles(RMAPI_TITLES_SEARCH_RESPONSE);

    var resourcePath = withInclude(EHOLDINGS_TITLES_PATH + "?page=1&filter[name]=Mind&sort=name", "resources");
    var actualResponse = getWithOk(resourcePath).asString();

    assertJsonEqual(readFile(EXPECTED_TITLES_WITH_RESOURCES_RESPONSE), actualResponse);
  }

  @Test
  void shouldReturnTitlesOnSearchByTags() {
    mockGetTitleById(STUB_MANAGED_TITLE_ID, RMAPI_TITLE_BY_ID_RESPONSE);
    mockGetTitleById(STUB_MANAGED_TITLE_ID_2, RMAPI_TITLE_BY_ID_2_RESPONSE);

    saveResource(buildResource(STUB_MANAGED_RESOURCE_ID, credentialsId, STUB_TITLE_NAME), vertx);
    saveResource(buildResource(STUB_MANAGED_RESOURCE_ID_2, credentialsId, STUB_CUSTOM_TITLE_NAME), vertx);
    saveTag(vertx, STUB_MANAGED_RESOURCE_ID, RecordType.RESOURCE, STUB_TAG_VALUE);
    saveTag(vertx, STUB_MANAGED_RESOURCE_ID_2, RecordType.RESOURCE, STUB_TAG_VALUE_2);

    var actualResponse = getWithOk(withTagFilters(EHOLDINGS_TITLES_PATH, STUB_TAG_VALUE, STUB_TAG_VALUE_2)).asString();
    assertJsonEqual(readFile(EXPECTED_TAGGED_TITLES_RESPONSE), actualResponse);
  }

  @Test
  void shouldReturnTitlesOnSearchByTagsWithResources() {
    mockGetTitleById(STUB_MANAGED_TITLE_ID, RMAPI_TITLE_BY_ID_RESPONSE);
    mockGetTitleById(STUB_MANAGED_TITLE_ID_2, RMAPI_TITLE_BY_ID_2_RESPONSE);

    saveResource(buildResource(STUB_MANAGED_RESOURCE_ID, credentialsId, STUB_TITLE_NAME), vertx);
    saveResource(buildResource(STUB_MANAGED_RESOURCE_ID_2, credentialsId, STUB_CUSTOM_TITLE_NAME), vertx);
    saveResource(buildResource(STUB_MANAGED_RESOURCE_ID_3, credentialsId, STUB_CUSTOM_TITLE_NAME), vertx);
    saveTag(vertx, STUB_MANAGED_RESOURCE_ID, RecordType.RESOURCE, STUB_TAG_VALUE);
    saveTag(vertx, STUB_MANAGED_RESOURCE_ID_2, RecordType.RESOURCE, STUB_TAG_VALUE_2);
    saveTag(vertx, STUB_MANAGED_RESOURCE_ID_3, RecordType.RESOURCE, STUB_TAG_VALUE_2);

    var resourcePath =
      withInclude(withTagFilters(EHOLDINGS_TITLES_PATH, STUB_TAG_VALUE, STUB_TAG_VALUE_2), "resources");
    var actualResponse = getWithOk(resourcePath).asString();
    assertJsonEqual(readFile(EXPECTED_TAGGED_TITLES_WITH_RESOURCES_RESPONSE), actualResponse);
  }

  @Test
  void shouldReturnSecondTitleOnSearchByTagsWithPagination() {
    mockGetTitleById(STUB_MANAGED_TITLE_ID, RMAPI_TITLE_BY_ID_RESPONSE);
    mockGetTitleById(STUB_MANAGED_TITLE_ID_2, RMAPI_TITLE_BY_ID_2_RESPONSE);

    saveResource(buildResource(STUB_MANAGED_RESOURCE_ID, credentialsId, STUB_TITLE_NAME), vertx);
    saveResource(buildResource(STUB_MANAGED_RESOURCE_ID_2, credentialsId, STUB_CUSTOM_TITLE_NAME), vertx);
    saveTag(vertx, STUB_MANAGED_RESOURCE_ID, RecordType.RESOURCE, STUB_TAG_VALUE);
    saveTag(vertx, STUB_MANAGED_RESOURCE_ID_2, RecordType.RESOURCE, STUB_TAG_VALUE_2);

    var resourcePath = withTagFilters(EHOLDINGS_TITLES_PATH + "?page=2&count=1", STUB_TAG_VALUE, STUB_TAG_VALUE_2);
    var response = getWithOk(resourcePath).as(TitleCollection.class);
    assertEquals(STUB_MANAGED_TITLE_ID_2, Integer.parseInt(response.getData().getFirst().getId()));
  }

  @Test
  void shouldReturnTitlesOnSearchByAccessTypes() {
    var accessTypes = insertAccessTypes(testData(credentialsId), vertx);
    insertAccessTypeMapping(STUB_MANAGED_RESOURCE_ID, RESOURCE, accessTypes.getFirst().getId(), vertx);
    insertAccessTypeMapping(STUB_MANAGED_RESOURCE_ID_2, RESOURCE, accessTypes.getFirst().getId(), vertx);
    mockGetTitles();

    var resourcePath = withAccessTypeFilters(EHOLDINGS_TITLES_PATH, STUB_ACCESS_TYPE_NAME);
    var titleCollection = getWithOk(resourcePath).as(TitleCollection.class);
    var titles = titleCollection.getData();

    assertEquals(2, titles.size());
    assertEquals(2, (int) titleCollection.getMeta().getTotalResults());
    assertThat(titles, everyItem(hasProperty("id",
      anyOf(equalTo(String.valueOf(STUB_MANAGED_TITLE_ID)), equalTo(String.valueOf(STUB_MANAGED_TITLE_ID_2))))));
  }

  @Test
  void shouldReturnTitlesWithResourcesOnSearchByAccessTypes() {
    var accessTypes = insertAccessTypes(testData(credentialsId), vertx);
    insertAccessTypeMapping(STUB_MANAGED_RESOURCE_ID, RESOURCE, accessTypes.getFirst().getId(), vertx);
    insertAccessTypeMapping(STUB_MANAGED_RESOURCE_ID_2, RESOURCE, accessTypes.getFirst().getId(), vertx);
    mockGetTitles();

    var resourcePath = withInclude(withAccessTypeFilters(EHOLDINGS_TITLES_PATH, STUB_ACCESS_TYPE_NAME), "resources");
    var titleCollection = getWithOk(resourcePath).as(TitleCollection.class);
    var titles = titleCollection.getData();

    assertEquals(2, titles.size());
    assertEquals(2, (int) titleCollection.getMeta().getTotalResults());
    assertThat(titles, everyItem(hasProperty("id",
      anyOf(equalTo(String.valueOf(STUB_MANAGED_TITLE_ID)), equalTo(String.valueOf(STUB_MANAGED_TITLE_ID_2))))));
    assertThat(titles, everyItem(hasProperty("included", not(empty()))));
  }

  @Test
  void shouldReturnTitleOnSearchByAccessTypesWithPagination() {
    var accessTypes = insertAccessTypes(testData(credentialsId), vertx);
    insertAccessTypeMapping(STUB_MANAGED_RESOURCE_ID, RESOURCE, accessTypes.get(0).getId(), vertx);
    insertAccessTypeMapping(STUB_MANAGED_RESOURCE_ID_2, RESOURCE, accessTypes.get(1).getId(), vertx);
    mockGetTitles();

    var resourcePath = withAccessTypeFilters(EHOLDINGS_TITLES_PATH + "?page=2&count=1",
      STUB_ACCESS_TYPE_NAME, STUB_ACCESS_TYPE_NAME_2);
    var titleCollection = getWithOk(resourcePath).as(TitleCollection.class);
    var titles = titleCollection.getData();

    assertEquals(1, titles.size());
    assertEquals(2, (int) titleCollection.getMeta().getTotalResults());
    assertThat(titles, everyItem(hasProperty("id", equalTo(String.valueOf(STUB_MANAGED_TITLE_ID)))));
  }

  @Test
  void shouldReturnEmptyTitlesOnSearchByAccessTypesThatIsNotExist() {
    var accessTypes = insertAccessTypes(testData(credentialsId), vertx);
    insertAccessTypeMapping(STUB_MANAGED_RESOURCE_ID, RESOURCE, accessTypes.get(0).getId(), vertx);
    insertAccessTypeMapping(STUB_MANAGED_RESOURCE_ID_2, RESOURCE, accessTypes.get(1).getId(), vertx);
    mockGetTitles();

    var resourcePath = withAccessTypeFilters(EHOLDINGS_TITLES_PATH, "Not Exist");
    var titleCollection = getWithOk(resourcePath).as(TitleCollection.class);
    var titles = titleCollection.getData();

    assertEquals(0, titles.size());
    assertEquals(0, (int) titleCollection.getMeta().getTotalResults());
  }

  @Test
  void shouldReturnTitlesOnSearchByPackageIds() {
    mockSearchTitles(RMAPI_TITLES_SEARCH_RESPONSE);

    var queryParam = "?filter[packageIds]=1&filter[packageIds]=2&filter[packageIds]=3&filter[name]=Mind";
    var actualResponse = getWithOk(EHOLDINGS_TITLES_PATH + queryParam).asString();
    assertJsonEqual(readFile(EXPECTED_TITLES_RESPONSE), actualResponse);
  }

  @Test
  void shouldReturn400ValidationErrorForPackageIds() {
    var queryParam = "?filter[packageIds]=1&filter[packageIds]=abc&filter[name]=Mind";
    var error = getWithStatus(EHOLDINGS_TITLES_PATH + queryParam, SC_BAD_REQUEST).as(JsonapiError.class);

    assertErrorContainsTitle(error, "Invalid Query Parameter for filter[packageIds]");
  }

  @Test
  void shouldReturn400IfCountOutOfRange() {
    var error = getWithStatus(EHOLDINGS_TITLES_PATH + "?count=1000&page=1&filter[name]=Mind&sort=name",
      SC_BAD_REQUEST).as(JsonapiError.class);

    assertErrorContainsTitle(error, "parameter value {1000} is not valid");
  }

  @Test
  void shouldReturn500WhenRmApiReturns500Error() {
    mockGet(matching(titlesRmApi() + ".*"), SC_INTERNAL_SERVER_ERROR);

    var resourcePath = EHOLDINGS_TITLES_PATH + "?filter[name]=news";
    var error = getWithStatus(resourcePath, SC_INTERNAL_SERVER_ERROR).as(JsonapiError.class);

    assertErrorContainsTitle(error, "Invalid RMAPI response");
  }

  @Test
  void shouldReturnTitleWhenValidId() {
    mockSearchTitles(RMAPI_TITLE_BY_ID_RESPONSE);

    var actualResponse = getWithOk(EHOLDINGS_TITLES_PATH + "/" + STUB_TITLE_ID).asString();
    assertJsonEqual(readFile(EXPECTED_TITLE_BY_ID_RESPONSE), actualResponse);
  }

  @Test
  void shouldReturnTitleTagsWhenValidId() {
    saveTag(vertx, STUB_MANAGED_TITLE_ID, RecordType.TITLE, STUB_TAG_VALUE);
    mockSearchTitles(RMAPI_TITLE_BY_ID_RESPONSE);

    var actualResponse = getWithOk(EHOLDINGS_TITLES_PATH + "/" + STUB_TITLE_ID).as(Title.class);

    assertTrue(actualResponse.getData().getAttributes().getTags().getTagList().contains(STUB_TAG_VALUE));
  }

  @Test
  void shouldReturn404WhenRmApiNotFoundOnTitleGet() {
    mockGet(matching(titlesRmApi() + ".*"),
      readFile(RMAPI_TITLE_NOT_FOUND_RESPONSE), SC_NOT_FOUND);

    var error = getWithStatus(EHOLDINGS_TITLES_PATH + "/" + STUB_TITLE_ID, SC_NOT_FOUND)
      .as(JsonapiError.class);

    assertErrorContainsTitle(error, "Title not found");
  }

  @Test
  void shouldReturn400WhenValidationErrorOnTitleGet() {
    var error = getWithStatus(EHOLDINGS_TITLES_PATH + "/12345aaa", SC_BAD_REQUEST)
      .as(JsonapiError.class);

    assertErrorContainsTitle(error, "Title id is invalid - 12345aaa");
  }

  @Test
  void shouldReturn500WhenRmApiReturns500ErrorOnTitleGet() {
    mockGet(matching(titlesRmApi() + ".*"), SC_INTERNAL_SERVER_ERROR);

    var error = getWithStatus(EHOLDINGS_TITLES_PATH + "/" + STUB_TITLE_ID,
      SC_INTERNAL_SERVER_ERROR).as(JsonapiError.class);

    assertErrorContainsTitle(error, "Invalid RMAPI response");
  }

  @Test
  void shouldReturnTitleWithSortedResourcesWhenIncludeResources() {
    mockSearchTitles(RMAPI_TITLE_WITH_RESOURCES_RESPONSE);

    var actual = getWithStatus(withInclude(EHOLDINGS_TITLES_PATH + "/" + STUB_TITLE_ID, "resources"), SC_OK).asString();

    assertJsonEqual(readFile(EXPECTED_TITLE_WITH_RESOURCES_RESPONSE), actual);
  }

  @Test
  void shouldReturnTitleWithResourcesWhenIncludeResourcesWithTags() {
    saveTag(vertx, STUB_MANAGED_RESOURCE_ID, RecordType.RESOURCE, STUB_TAG_VALUE);
    mockSearchTitles(RMAPI_TITLE_BY_ID_RESPONSE);

    var actual = getWithStatus(withInclude(EHOLDINGS_TITLES_PATH + "/" + STUB_TITLE_ID, "resources"), SC_OK).asString();

    assertJsonEqual(readFile(EXPECTED_TITLE_WITH_RESOURCES_AND_TAGS_RESPONSE), actual);
  }

  @Test
  void shouldReturnTitleWithoutResourcesWhenInvalidInclude() {
    mockSearchTitles(RMAPI_TITLE_BY_ID_RESPONSE);

    var actual = getWithStatus(withInclude(EHOLDINGS_TITLES_PATH + "/" + STUB_TITLE_ID, "badValue"), SC_OK).asString();

    assertJsonEqual(readFile(EXPECTED_TITLE_INVALID_INCLUDE_RESPONSE), actual);
  }

  @Test
  void shouldReturnTitleWhenValidPostRequest() {
    var actual = postTitle(Collections.emptyList()).asString();
    assertJsonEqual(readFile(EXPECTED_CREATED_TITLE_RESPONSE), actual);
  }

  @Test
  void shouldUpdateTagsWhenValidPostRequest() {
    var tagList = Arrays.asList(STUB_TAG_VALUE, STUB_TAG_VALUE_2);
    var actual = postTitle(tagList).as(Title.class);
    var tagsFromDb = TagsTestUtil.getTags(vertx);

    assertThat(actual.getData().getAttributes().getTags().getTagList(), containsInAnyOrder(tagList.toArray()));
    assertThat(tagsFromDb, containsInAnyOrder(tagList.toArray()));
  }

  @Test
  void shouldAddTitleDataOnPost() {
    postTitle(Collections.singletonList(STUB_TAG_VALUE));

    var titles = TitlesTestUtil.getTitles(vertx);
    assertEquals(1, titles.size());
    assertEquals(STUB_TITLE_ID, titles.getFirst().getId());
    assertEquals("Test Title", titles.getFirst().getName());
  }

  @Test
  void shouldReturn400WhenInvalidPostRequest() {
    mockPost(
      WireMock.equalTo(packageTitlesRmApi(STUB_VENDOR_ID, STUB_PACKAGE_ID)),
      readFile(RMAPI_PACKAGE_400_RESPONSE),
      SC_BAD_REQUEST);

    var error = postWithStatus(EHOLDINGS_TITLES_PATH,
      readFile(POST_TITLE_REQUEST), SC_BAD_REQUEST)
      .as(JsonapiError.class);

    assertErrorContainsTitle(error, "Package with the provided name already exists");
  }

  @Test
  void shouldReturnUpdatedValuesForCustomTitleOnSuccessfulPut() {
    var actualResponse = putTitle(null);
    assertJsonEqual(readFile(EXPECTED_UPDATED_TITLE_RESPONSE), actualResponse);

    verifyPut(WireMock.equalTo(resourcesRmApi(CUSTOM_VENDOR_ID, CUSTOM_PACKAGE_ID, CUSTOM_TITLE_ID)),
      equalToJson(readFile(RMAPI_PUT_RESOURCE_REQUEST)));
  }

  @Test
  void shouldUpdateTitleTagsOnSuccessfulPut() {
    var newTags = Arrays.asList(STUB_TAG_VALUE, STUB_TAG_VALUE_2);
    putTitle(newTags);

    assertThat(TagsTestUtil.getTags(vertx), containsInAnyOrder(newTags.toArray()));
  }

  @Test
  void shouldUpdateOnlyTagsOnPutForNonCustomTitle() {
    var request = readJsonFile(PUT_TITLE_REQUEST, TitlePutRequest.class);
    var newTags = Arrays.asList(STUB_TAG_VALUE, STUB_TAG_VALUE_2);
    request.getData().getAttributes().setTags(new Tags().withTagList(newTags));
    mockGet(WireMock.equalTo(titlesRmApi(CUSTOM_TITLE_ID)),
      readFile(RMAPI_MANAGED_RESOURCE_UPDATED_RESPONSE));

    putWithOk(EHOLDINGS_TITLES_PATH + "/" + CUSTOM_TITLE_ID, Json.encode(request));

    assertThat(TagsTestUtil.getTags(vertx), containsInAnyOrder(newTags.toArray()));
    verifyPut(anyUrl(), 0);
  }

  @Test
  void shouldAddTitleDataOnPut() {
    putTitle(Arrays.asList(STUB_TAG_VALUE, STUB_TAG_VALUE_2));

    var titles = TitlesTestUtil.getTitles(vertx);
    assertEquals(1, titles.size());
    assertEqualsLong(titles.getFirst().getId());
    assertEquals("sd-test-java-again", titles.getFirst().getName());
  }

  @Test
  void shouldDeleteTitleDataOnPutWithEmptyTagList() {
    putTitle(Collections.singletonList(STUB_TAG_VALUE));
    putTitle(Collections.emptyList());

    assertTrue(TitlesTestUtil.getTitles(vertx).isEmpty());
  }

  @Test
  void shouldUpdateTitleDataOnSecondPut() {
    var newName = "new name";

    putTitle(readFile(RMAPI_RESOURCE_UPDATED_TITLE_NAME_RESPONSE), Collections.singletonList(STUB_TAG_VALUE));

    var request = readJsonFile(PUT_TITLE_REQUEST, TitlePutRequest.class);
    request.getData().getAttributes().withName(newName);
    mockGet(WireMock.equalTo(titlesRmApi(CUSTOM_TITLE_ID)), readFile(RMAPI_RESOURCE_UPDATED_TITLE_NAME_RESPONSE));

    putWithOk(EHOLDINGS_TITLES_PATH + "/" + CUSTOM_TITLE_ID, Json.encode(request));

    var titles = TitlesTestUtil.getTitles(vertx);
    assertEquals(1, titles.size());
    assertEqualsLong(titles.getFirst().getId());
    assertEquals(newName, titles.getFirst().getName());
  }

  @Test
  void shouldReturn422WhenNameIsNotProvided() {
    var error = putWithStatus(EHOLDINGS_TITLES_PATH + "/" + STUB_TITLE_ID,
      readFile(PUT_TITLE_NULL_NAME_REQUEST), SC_UNPROCESSABLE_ENTITY)
      .as(Errors.class);

    assertTrue(error.getErrors().getFirst().getMessage().contains("must not be null"));
  }

  private String putTitle(List<String> tags) {
    return putTitle(readFile(RMAPI_RESOURCE_UPDATED_RESPONSE), tags);
  }

  private String putTitle(String updatedResourceResponse, List<String> tags) {
    mockGet(WireMock.equalTo(titlesRmApi(CUSTOM_TITLE_ID)), updatedResourceResponse);
    mockPut(matching(resourcesRmApi(CUSTOM_VENDOR_ID, CUSTOM_PACKAGE_ID, CUSTOM_TITLE_ID)), SC_NO_CONTENT);

    var titleToBeUpdated = readJsonFile(PUT_TITLE_REQUEST, TitlePutRequest.class);
    if (tags != null) {
      titleToBeUpdated.getData().getAttributes().setTags(new Tags().withTagList(tags));
    }

    return putWithOk(EHOLDINGS_TITLES_PATH + "/" + CUSTOM_TITLE_ID, Json.encode(titleToBeUpdated)).asString();
  }

  private ExtractableResponse<Response> postTitle(List<String> tags) {
    mockPost(WireMock.equalTo(packageTitlesRmApi(STUB_VENDOR_ID, STUB_PACKAGE_ID)),
      readFile(RMAPI_POST_TITLE_RESPONSE), SC_OK);
    mockGet(matching(titlesRmApi(STUB_TITLE_ID)), readFile(RMAPI_TITLE_FOR_POST_RESPONSE));

    var request = readJsonFile(POST_TITLE_REQUEST, TitlePostRequest.class);
    request.getData().getAttributes().setTags(new Tags().withTagList(tags));

    return postWithOk(EHOLDINGS_TITLES_PATH, Json.encode(request));
  }

  private void mockGetTitles() {
    mockGetTitleById(STUB_MANAGED_TITLE_ID, RMAPI_TITLE_BY_ID_RESPONSE);
    mockGetTitleById(STUB_MANAGED_TITLE_ID_2, RMAPI_TITLE_BY_ID_2_RESPONSE);
  }

  private void mockGetTitleById(int titleId, String responseFile) {
    mockGet(WireMock.equalTo(titlesRmApi(titleId)), readFile(responseFile));
  }

  private void mockSearchTitles(String responseFile) {
    mockGet(matching(titlesRmApi() + ".*"), readFile(responseFile));
  }
}
