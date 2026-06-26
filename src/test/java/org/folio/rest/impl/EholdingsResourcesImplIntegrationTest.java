package org.folio.rest.impl;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static java.lang.String.format;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_UNPROCESSABLE_ENTITY;
import static org.folio.repository.RecordType.RESOURCE;
import static org.folio.repository.accesstypes.AccessTypeMappingsTableConstants.ACCESS_TYPES_MAPPING_TABLE_NAME;
import static org.folio.repository.accesstypes.AccessTypesTableConstants.ACCESS_TYPES_TABLE_NAME;
import static org.folio.repository.kbcredentials.KbCredentialsTableConstants.KB_CREDENTIALS_TABLE_NAME;
import static org.folio.repository.resources.ResourceTableConstants.RESOURCES_TABLE_NAME;
import static org.folio.repository.tag.TagTableConstants.TAGS_TABLE_NAME;
import static org.folio.util.AccessTypesTestUtil.getAccessTypeMappings;
import static org.folio.util.AccessTypesTestUtil.insertAccessTypeMapping;
import static org.folio.util.AccessTypesTestUtil.insertAccessTypes;
import static org.folio.util.AccessTypesTestUtil.testData;
import static org.folio.util.AssertTestUtil.assertEqualsResourceId;
import static org.folio.util.AssertTestUtil.assertErrorContainsTitle;
import static org.folio.util.AssertTestUtil.assertJsonEqual;
import static org.folio.util.KbCredentialsTestUtil.setupDefaultKbConfiguration;
import static org.folio.util.TagsTestUtil.saveTag;
import static org.folio.util.TestUtil.clearDataFromTable;
import static org.folio.util.TestUtil.readFile;
import static org.folio.util.TestUtil.readJsonFile;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.tomakehurst.wiremock.matching.EqualToJsonPattern;
import io.vertx.core.json.Json;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.folio.repository.RecordType;
import org.folio.rest.jaxrs.model.Errors;
import org.folio.rest.jaxrs.model.JsonapiError;
import org.folio.rest.jaxrs.model.Resource;
import org.folio.rest.jaxrs.model.ResourceBulkFetchCollection;
import org.folio.rest.jaxrs.model.ResourcePutRequest;
import org.folio.rest.jaxrs.model.ResourceTags;
import org.folio.rest.jaxrs.model.ResourceTagsPutRequest;
import org.folio.rest.jaxrs.model.Tags;
import org.folio.util.IntegrationTestBase;
import org.folio.util.ResourcesTestUtil;
import org.folio.util.TagsTestUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EholdingsResourcesImplIntegrationTest extends IntegrationTestBase {

  // RM API response files
  private static final String RMAPI_GET_RESOURCE_BY_ID_SUCCESS =
    "responses/rmapi/resources/get-resource-by-id-success-response.json";
  private static final String RMAPI_GET_RESOURCE_NOT_FOUND =
    "responses/rmapi/resources/get-resource-by-id-not-found-response.json";
  private static final String RMAPI_GET_RESOURCE_EMPTY_CUSTOMER_LIST =
    "responses/rmapi/resources/get-resource-by-id-response-empty-customer-list.json";
  private static final String RMAPI_GET_MANAGED_RESOURCE_UPDATED =
    "responses/rmapi/resources/get-managed-resource-updated-response.json";
  private static final String RMAPI_GET_MANAGED_RESOURCE_UPDATED_NOT_SELECTED =
    "responses/rmapi/resources/get-managed-resource-updated-response-is-selected-false.json";
  private static final String RMAPI_GET_CUSTOM_RESOURCE_UPDATED =
    "responses/rmapi/resources/get-custom-resource-updated-response.json";
  private static final String RMAPI_GET_RESOURCES_BY_PACKAGE_ID =
    "responses/rmapi/resources/get-resources-by-package-id-response.json";
  private static final String RMAPI_GET_VENDOR_FOR_RESOURCE =
    "responses/rmapi/vendors/get-vendor-by-id-for-resource.json";
  private static final String RMAPI_GET_PACKAGE_FOR_RESOURCE =
    "responses/rmapi/packages/get-package-by-id-for-resource.json";
  private static final String RMAPI_GET_CUSTOM_PACKAGE =
    "responses/rmapi/packages/get-custom-package-by-id-response.json";

  // KB-EBSCO expected response files
  private static final String EXPECTED_RESOURCE_BY_ID =
    "responses/kb-ebsco/resources/expected-resource-by-id.json";
  private static final String EXPECTED_RESOURCE_WITH_TITLE =
    "responses/kb-ebsco/resources/expected-resource-by-id-with-title.json";
  private static final String EXPECTED_RESOURCE_WITH_PROVIDER =
    "responses/kb-ebsco/resources/expected-resource-by-id-with-provider.json";
  private static final String EXPECTED_RESOURCE_WITH_PACKAGE =
    "responses/kb-ebsco/resources/expected-resource-by-id-with-package.json";
  private static final String EXPECTED_RESOURCE_WITH_ALL_OBJECTS =
    "responses/kb-ebsco/resources/expected-resource-by-id-with-all-objects.json";
  private static final String EXPECTED_MANAGED_RESOURCE =
    "responses/kb-ebsco/resources/expected-managed-resource.json";
  private static final String EXPECTED_CUSTOM_RESOURCE =
    "responses/kb-ebsco/resources/expected-custom-resource.json";
  private static final String EXPECTED_RESOURCE_WITH_ACCESS_TYPE =
    "responses/kb-ebsco/resources/expected-resource-by-id-with-access-type.json";
  private static final String EXPECTED_RESOURCE_AFTER_POST =
    "responses/kb-ebsco/resources/expected-resource-after-post.json";
  private static final String EXPECTED_RESOURCES_BULK_RESPONSE =
    "responses/kb-ebsco/resources/expected-resources-bulk-response.json";
  private static final String EXPECTED_RESOURCES_BULK_WITH_FAILED_IDS =
    "responses/kb-ebsco/resources/expected-resources-bulk-response-with-failed-ids.json";
  private static final String EXPECTED_RESPONSE_TOO_LONG_ID =
    "responses/kb-ebsco/resources/expected-response-on-too-long-id.json";

  // Request payload files
  private static final String REQUEST_PUT_MANAGED_RESOURCE =
    "requests/kb-ebsco/resource/put-managed-resource.json";
  private static final String REQUEST_PUT_MANAGED_RESOURCE_NOT_SELECTED =
    "requests/kb-ebsco/resource/put-managed-resource-is-not-selected.json";
  private static final String REQUEST_PUT_MANAGED_RESOURCE_MISSING_ACCESS_TYPE =
    "requests/kb-ebsco/resource/put-managed-resource-with-missing-access-type.json";
  private static final String REQUEST_PUT_MANAGED_RESOURCE_INVALID_ACCESS_TYPE =
    "requests/kb-ebsco/resource/put-managed-resource-with-invalid-access-type.json";
  private static final String REQUEST_PUT_RESOURCE_WITH_ACCESS_TYPE =
    "requests/kb-ebsco/resource/put-resource-with-access-type.json";
  private static final String REQUEST_PUT_CUSTOM_RESOURCE =
    "requests/kb-ebsco/resource/put-custom-resource.json";
  private static final String REQUEST_PUT_CUSTOM_RESOURCE_WITH_PROXIED_URL =
    "requests/kb-ebsco/resource/put-custom-resource-with-proxied-url.json";
  private static final String REQUEST_PUT_CUSTOM_RESOURCE_INVALID_URL =
    "requests/kb-ebsco/resource/put-custom-resource-invalid-url.json";
  private static final String REQUEST_PUT_RESOURCE_TAGS =
    "requests/kb-ebsco/resource/put-resource-tags.json";
  private static final String REQUEST_POST_RESOURCES =
    "requests/kb-ebsco/resource/post-resources-request.json";
  private static final String REQUEST_POST_RESOURCES_BULK =
    "requests/kb-ebsco/resource/post-resources-bulk.json";
  private static final String REQUEST_POST_RESOURCES_BULK_INVALID_FORMAT =
    "requests/kb-ebsco/resource/post-resources-bulk-with-invalid-id-format.json";
  private static final String REQUEST_POST_RESOURCES_BULK_INVALID_IDS =
    "requests/kb-ebsco/resource/post-resources-bulk-with-invalid-ids.json";
  private static final String REQUEST_POST_RESOURCE_TOO_LONG_ID =
    "requests/kb-ebsco/resource/post-resource-with-too-long-id.json";

  // RM API request files
  private static final String RMAPI_PUT_MANAGED_RESOURCE_IS_SELECTED =
    "requests/rmapi/resources/put-managed-resource-is-selected-multiple-attributes.json";
  private static final String RMAPI_PUT_MANAGED_RESOURCE_NOT_SELECTED =
    "requests/rmapi/resources/put-managed-resource-is-not-selected.json";
  private static final String RMAPI_PUT_CUSTOM_RESOURCE_IS_SELECTED =
    "requests/rmapi/resources/put-custom-resource-is-selected-multiple-attributes.json";
  private static final String RMAPI_SELECT_RESOURCE_REQUEST =
    "requests/rmapi/resources/select-resource-request.json";

  // API endpoint paths
  private static final String RESOURCES_PATH = "eholdings/resources";
  private static final String RESOURCE_BY_ID_PATH = RESOURCES_PATH + "/%s";
  private static final String STUB_MANAGED_RESOURCE_PATH = RESOURCE_BY_ID_PATH.formatted(STUB_MANAGED_RESOURCE_ID);
  private static final String RESOURCE_TAGS_PATH = RESOURCE_BY_ID_PATH.formatted(STUB_CUSTOM_RESOURCE_ID) + "/tags";
  private static final String RESOURCES_BULK_FETCH = RESOURCES_PATH + "/bulk/fetch";

  private String credentialsId;

  @BeforeEach
  void setUp() {
    credentialsId = setupDefaultKbConfiguration(getWiremockUrl(), vertx);
  }

  @AfterEach
  void tearDown() {
    clearDataFromTable(vertx, ACCESS_TYPES_MAPPING_TABLE_NAME);
    clearDataFromTable(vertx, ACCESS_TYPES_TABLE_NAME);
    clearDataFromTable(vertx, TAGS_TABLE_NAME);
    clearDataFromTable(vertx, RESOURCES_TABLE_NAME);
    clearDataFromTable(vertx, KB_CREDENTIALS_TABLE_NAME);
  }

  @Test
  void shouldReturnResourceWhenValidId() {
    mockResource(RMAPI_GET_RESOURCE_BY_ID_SUCCESS);

    var actualResponse = getWithOk(STUB_MANAGED_RESOURCE_PATH).asString();

    assertJsonEqual(readFile(EXPECTED_RESOURCE_BY_ID), actualResponse, true);
  }

  @Test
  void shouldReturnResourceWithTags() {
    saveTag(vertx, STUB_MANAGED_RESOURCE_ID, RecordType.RESOURCE, STUB_TAG_VALUE);
    mockResource(RMAPI_GET_RESOURCE_BY_ID_SUCCESS);

    var resource = getWithOk(STUB_MANAGED_RESOURCE_PATH).as(Resource.class);

    assertTrue(resource.getData().getAttributes().getTags().getTagList().contains(STUB_TAG_VALUE));
  }

  @Test
  void shouldReturnResourceWithTitleWhenTitleFlagSetToTrue() {
    mockResource(RMAPI_GET_RESOURCE_BY_ID_SUCCESS);

    var actualResponse =
      getWithOk(withInclude(RESOURCE_BY_ID_PATH.formatted(STUB_MANAGED_RESOURCE_ID), "title")).asString();

    assertJsonEqual(readFile(EXPECTED_RESOURCE_WITH_TITLE), actualResponse, true);
  }

  @Test
  void shouldReturnResourceWithProviderWhenProviderFlagSetToTrue() {
    mockResource(RMAPI_GET_RESOURCE_BY_ID_SUCCESS);
    mockVendor(RMAPI_GET_VENDOR_FOR_RESOURCE);

    var actualResponse =
      getWithOk(withInclude(RESOURCE_BY_ID_PATH.formatted(STUB_MANAGED_RESOURCE_ID), "provider")).asString();

    assertJsonEqual(readFile(EXPECTED_RESOURCE_WITH_PROVIDER), actualResponse, true);
  }

  @Test
  void shouldReturnResourceWithPackageWhenPackageFlagSetToTrue() {
    mockResource(RMAPI_GET_RESOURCE_BY_ID_SUCCESS);
    mockPackage(RMAPI_GET_PACKAGE_FOR_RESOURCE);

    var actualResponse =
      getWithOk(withInclude(RESOURCE_BY_ID_PATH.formatted(STUB_MANAGED_RESOURCE_ID), "package")).asString();

    assertJsonEqual(readFile(EXPECTED_RESOURCE_WITH_PACKAGE), actualResponse, true);
  }

  @Test
  void shouldReturnResourceWithAllIncludedObjectsWhenIncludeContainsAllObjects() {
    mockResource(RMAPI_GET_RESOURCE_BY_ID_SUCCESS);
    mockVendor(RMAPI_GET_VENDOR_FOR_RESOURCE);
    mockPackage(RMAPI_GET_PACKAGE_FOR_RESOURCE);

    var actualResponse = getWithOk(
      withInclude(RESOURCE_BY_ID_PATH.formatted(STUB_MANAGED_RESOURCE_ID), "package", "title", "provider")).asString();

    assertJsonEqual(readFile(EXPECTED_RESOURCE_WITH_ALL_OBJECTS), actualResponse, true);
  }

  @Test
  void shouldReturn404WhenRmApiNotFoundOnResourceGet() {
    mockGet(matching(packageTitlesRmApi(STUB_VENDOR_ID, STUB_PACKAGE_ID) + ".*"),
      readFile(RMAPI_GET_RESOURCE_NOT_FOUND), SC_NOT_FOUND);

    var error = getWithStatus(STUB_MANAGED_RESOURCE_PATH, SC_NOT_FOUND).as(JsonapiError.class);

    assertErrorContainsTitle(error, "Title is no longer in this package.");
  }

  @Test
  void shouldReturn404WhenCustomerListIsEmpty() {
    mockResource(RMAPI_GET_RESOURCE_EMPTY_CUSTOMER_LIST);

    var error = getWithStatus(STUB_MANAGED_RESOURCE_PATH, SC_NOT_FOUND).as(JsonapiError.class);

    assertErrorContainsTitle(error, "Title is no longer in this package");
  }

  @Test
  void shouldReturn400WhenValidationErrorOnResourceGet() {
    var error = getWithStatus(RESOURCE_BY_ID_PATH.formatted("583-abc-762169"), SC_BAD_REQUEST)
      .as(JsonapiError.class);

    assertErrorContainsTitle(error, "Resource id is invalid - 583-abc-762169");
  }

  @Test
  void shouldReturn500WhenRmApiReturns500ErrorOnResourceGet() {
    mockGet(matching(packageTitlesRmApi(STUB_VENDOR_ID, STUB_PACKAGE_ID) + ".*"), SC_INTERNAL_SERVER_ERROR);

    var error = getWithStatus(STUB_MANAGED_RESOURCE_PATH, SC_INTERNAL_SERVER_ERROR)
      .as(JsonapiError.class);

    assertNotNull(error.getErrors().getFirst().getTitle());
  }

  @Test
  void shouldReturnUpdatedValuesManagedResourceOnSuccessfulPut() {
    var managedResourceEndpoint = resourcesRmApi(STUB_VENDOR_ID, STUB_PACKAGE_ID, STUB_MANAGED_TITLE_ID);

    var actualResponse = mockUpdateResourceScenario(RMAPI_GET_MANAGED_RESOURCE_UPDATED,
      managedResourceEndpoint, STUB_MANAGED_RESOURCE_ID, readFile(REQUEST_PUT_MANAGED_RESOURCE));

    assertJsonEqual(readFile(EXPECTED_MANAGED_RESOURCE), actualResponse, false);

    verifyPut(matching(managedResourceEndpoint), equalToJson(readFile(RMAPI_PUT_MANAGED_RESOURCE_IS_SELECTED)));
  }

  @Test
  void shouldCreateNewAccessTypeMappingOnSuccessfulPut() {
    var accessTypes = insertAccessTypes(testData(credentialsId), vertx);
    var accessTypeId = accessTypes.getFirst().getId();
    var managedResourceEndpoint = resourcesRmApi(STUB_VENDOR_ID, STUB_PACKAGE_ID, STUB_MANAGED_TITLE_ID);

    var requestBody = format(readFile(REQUEST_PUT_RESOURCE_WITH_ACCESS_TYPE), accessTypeId);
    var actualResponse = mockUpdateResourceScenario(RMAPI_GET_MANAGED_RESOURCE_UPDATED,
      managedResourceEndpoint, STUB_MANAGED_RESOURCE_ID, requestBody);

    var expectedJson = format(readFile(EXPECTED_RESOURCE_WITH_ACCESS_TYPE), accessTypeId, accessTypeId);
    assertJsonEqual(expectedJson, actualResponse, false);

    verifyPut(matching(managedResourceEndpoint), equalToJson(readFile(RMAPI_PUT_MANAGED_RESOURCE_IS_SELECTED)));

    var accessTypeMappingsInDb = getAccessTypeMappings(vertx);
    assertEquals(1, accessTypeMappingsInDb.size());
    assertEquals(accessTypeId, accessTypeMappingsInDb.getFirst().getAccessTypeId().toString());
    assertEquals(RESOURCE, accessTypeMappingsInDb.getFirst().getRecordType());
  }

  @Test
  void shouldDeleteAccessTypeMappingOnSuccessfulPut() {
    var accessTypes = insertAccessTypes(testData(credentialsId), vertx);
    var accessTypeId = accessTypes.getFirst().getId();
    insertAccessTypeMapping(STUB_MANAGED_RESOURCE_ID, RESOURCE, accessTypeId, vertx);

    var managedResourceEndpoint = resourcesRmApi(STUB_VENDOR_ID, STUB_PACKAGE_ID, STUB_MANAGED_TITLE_ID);

    var actualResponse = mockUpdateResourceScenario(RMAPI_GET_MANAGED_RESOURCE_UPDATED,
      managedResourceEndpoint, STUB_MANAGED_RESOURCE_ID, readFile(REQUEST_PUT_MANAGED_RESOURCE));

    assertJsonEqual(readFile(EXPECTED_MANAGED_RESOURCE), actualResponse, false);

    verifyPut(matching(managedResourceEndpoint), equalToJson(readFile(RMAPI_PUT_MANAGED_RESOURCE_IS_SELECTED)));

    var accessTypeMappingsInDb = getAccessTypeMappings(vertx);
    assertEquals(0, accessTypeMappingsInDb.size());
  }

  @Test
  void shouldReturn400OnPutPackageWithNotExistedAccessType() {
    var managedResourceEndpoint = resourcesRmApi(STUB_VENDOR_ID, STUB_PACKAGE_ID, STUB_MANAGED_TITLE_ID);

    var error = putWithStatus(STUB_MANAGED_RESOURCE_PATH, readFile(REQUEST_PUT_MANAGED_RESOURCE_MISSING_ACCESS_TYPE),
      SC_BAD_REQUEST, CONTENT_TYPE_HEADER).as(JsonapiError.class);

    verifyPut(matching(managedResourceEndpoint), 0);

    assertErrorContainsTitle(error, "Access type not found: id = 99999999-9999-1999-a999-999999999999");
  }

  @Test
  void shouldReturn422OnPutPackageWithInvalidAccessTypeId() {
    var managedResourceEndpoint = resourcesRmApi(STUB_VENDOR_ID, STUB_PACKAGE_ID, STUB_MANAGED_TITLE_ID);

    var error = putWithStatus(STUB_MANAGED_RESOURCE_PATH, readFile(REQUEST_PUT_MANAGED_RESOURCE_INVALID_ACCESS_TYPE),
      SC_UNPROCESSABLE_ENTITY, CONTENT_TYPE_HEADER).as(Errors.class);

    verifyPut(matching(managedResourceEndpoint), 0);

    assertEquals(1, error.getErrors().size());
    assertEquals(
      "must match \"^[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[1-5][a-fA-F0-9]{3}-[89abAB][a-fA-F0-9]{3}-[a-fA-F0-9]{12}$\"",
      error.getErrors().getFirst().getMessage());
  }

  @Test
  void shouldDeselectManagedResourceOnPutWithSelectedFalse() {
    var managedResourceEndpoint = resourcesRmApi(STUB_VENDOR_ID, STUB_PACKAGE_ID, STUB_MANAGED_TITLE_ID);

    var request = readJsonFile(REQUEST_PUT_MANAGED_RESOURCE_NOT_SELECTED, ResourcePutRequest.class);
    request.getData().getAttributes().setIsSelected(false);
    var actualResponse = mockUpdateResourceScenario(RMAPI_GET_MANAGED_RESOURCE_UPDATED_NOT_SELECTED,
      managedResourceEndpoint, STUB_MANAGED_RESOURCE_ID, Json.encode(request));

    var expectedResource = readJsonFile(EXPECTED_MANAGED_RESOURCE, Resource.class);
    expectedResource.getData().getAttributes().setIsSelected(false);
    assertJsonEqual(Json.encode(expectedResource), actualResponse, false);

    verifyPut(matching(managedResourceEndpoint),
      new EqualToJsonPattern(readFile(RMAPI_PUT_MANAGED_RESOURCE_NOT_SELECTED), true, true));
  }

  @Test
  void shouldReturnUpdatedValuesCustomResourceOnSuccessfulPut() {
    var customResourceEndpoint = resourcesRmApi(CUSTOM_VENDOR_ID, CUSTOM_PACKAGE_ID, CUSTOM_TITLE_ID);

    var actualResponse = mockUpdateResourceScenario(RMAPI_GET_CUSTOM_RESOURCE_UPDATED,
      customResourceEndpoint, STUB_CUSTOM_RESOURCE_ID, readFile(REQUEST_PUT_CUSTOM_RESOURCE));

    assertJsonEqual(readFile(EXPECTED_CUSTOM_RESOURCE), actualResponse, false);

    verifyPut(matching(customResourceEndpoint), equalToJson(readFile(RMAPI_PUT_CUSTOM_RESOURCE_IS_SELECTED)));
  }

  @Test
  void shouldAcceptValuesCustomResourceWithUnrecognizedFieldInProxy() {
    var customResourceEndpoint = resourcesRmApi(CUSTOM_VENDOR_ID, CUSTOM_PACKAGE_ID, CUSTOM_TITLE_ID);

    var actualResponse = mockUpdateResourceScenario(RMAPI_GET_CUSTOM_RESOURCE_UPDATED,
      customResourceEndpoint, STUB_CUSTOM_RESOURCE_ID, readFile(REQUEST_PUT_CUSTOM_RESOURCE_WITH_PROXIED_URL));

    assertJsonEqual(readFile(EXPECTED_CUSTOM_RESOURCE), actualResponse, false);

    verifyPut(matching(customResourceEndpoint), equalToJson(readFile(RMAPI_PUT_CUSTOM_RESOURCE_IS_SELECTED)));
  }

  @Test
  void shouldUpdateTagsOnSuccessfulTagsPut() {
    sendPutTags(Collections.singletonList(STUB_TAG_VALUE));

    var resources = ResourcesTestUtil.getResources(vertx);
    assertEquals(1, resources.size());
    assertEqualsResourceId(resources.getFirst().getId());
    assertEquals(STUB_VENDOR_NAME, resources.getFirst().getName());
  }

  @Test
  void shouldUpdateTagsOnSuccessfulTagsPutWithAlreadyExistingTags() {
    saveTag(vertx, STUB_CUSTOM_RESOURCE_ID, RecordType.RESOURCE, STUB_TAG_VALUE);
    var newTags = Arrays.asList(STUB_TAG_VALUE, STUB_TAG_VALUE_2);
    sendPutTags(newTags);

    var tagsAfterRequest = TagsTestUtil.getTagsForRecordType(vertx, RecordType.RESOURCE);
    assertThat(tagsAfterRequest, containsInAnyOrder(newTags.toArray()));
  }

  @Test
  void shouldReturn422OnPutTagsWhenRequestBodyIsInvalid() {
    var tags = readJsonFile(REQUEST_PUT_RESOURCE_TAGS, ResourceTagsPutRequest.class);
    tags.getData().getAttributes().setName("");

    var error = putWithStatus(RESOURCE_TAGS_PATH, Json.encode(tags), SC_UNPROCESSABLE_ENTITY).as(JsonapiError.class);

    assertErrorContainsTitle(error, "Invalid name");
  }

  @Test
  void shouldReturn422WhenInvalidUrlIsProvidedForCustomResource() {
    var customResourceEndpoint = resourcesRmApi(CUSTOM_VENDOR_ID, CUSTOM_PACKAGE_ID, CUSTOM_TITLE_ID);

    mockGet(matching(customResourceEndpoint), readFile(RMAPI_GET_CUSTOM_RESOURCE_UPDATED));

    var error = putWithStatus(RESOURCE_BY_ID_PATH.formatted(STUB_CUSTOM_RESOURCE_ID),
      readFile(REQUEST_PUT_CUSTOM_RESOURCE_INVALID_URL), SC_UNPROCESSABLE_ENTITY)
      .as(JsonapiError.class);

    assertErrorContainsTitle(error, "Invalid url");
  }

  @Test
  void shouldPostResourceToRmApi() {
    mockPackageResources(RMAPI_GET_RESOURCES_BY_PACKAGE_ID);
    mockPackage(RMAPI_GET_CUSTOM_PACKAGE);
    mockTitle(RMAPI_GET_RESOURCE_BY_ID_SUCCESS);
    mockResource(RMAPI_GET_RESOURCE_BY_ID_SUCCESS);

    var managedResourceEndpoint = resourcesRmApi(STUB_VENDOR_ID, STUB_PACKAGE_ID, STUB_MANAGED_TITLE_ID);
    var putRequestBodyPattern = new EqualToJsonPattern(readFile(RMAPI_SELECT_RESOURCE_REQUEST), true, true);
    mockPut(matching(managedResourceEndpoint), putRequestBodyPattern, SC_NO_CONTENT);

    var actualResponse = postWithOk(RESOURCES_PATH, readFile(REQUEST_POST_RESOURCES))
      .asString();

    assertJsonEqual(readFile(EXPECTED_RESOURCE_AFTER_POST), actualResponse, true);

    verifyPut(equalTo(managedResourceEndpoint), putRequestBodyPattern);
  }

  @Test
  void shouldReturn404IfTitleOrPackageIsNotFound() {
    mockGet(matching(titlesRmApi() + ".*"), SC_NOT_FOUND);
    mockGet(matching(packageRmApi(STUB_PACKAGE_ID)), SC_NOT_FOUND);

    var error = postWithStatus(RESOURCES_PATH, readFile(REQUEST_POST_RESOURCES), SC_NOT_FOUND).as(JsonapiError.class);

    assertErrorContainsTitle(error, "not found");
  }

  @Test
  void shouldReturn422IfPackageIsNotCustom() {
    mockPackageResources(RMAPI_GET_RESOURCES_BY_PACKAGE_ID);
    mockPackage(RMAPI_GET_PACKAGE_FOR_RESOURCE);
    mockTitle(RMAPI_GET_RESOURCE_BY_ID_SUCCESS);

    var error =
      postWithStatus(RESOURCES_PATH, readFile(REQUEST_POST_RESOURCES), SC_UNPROCESSABLE_ENTITY).as(JsonapiError.class);

    assertTrue(error.getErrors().getFirst().getTitle().contains("Invalid PackageId"));
  }

  @Test
  void shouldSendDeleteRequestForResourceAssociatedWithCustomPackage() {
    var putBodyPattern = new EqualToJsonPattern("{\"isSelected\":false}", true, true);
    var customResourceEndpoint = resourcesRmApi(CUSTOM_VENDOR_ID, CUSTOM_PACKAGE_ID, CUSTOM_TITLE_ID);
    deleteResource(putBodyPattern);

    verifyPut(equalTo(customResourceEndpoint), putBodyPattern);
  }

  @Test
  void shouldDeleteTagsOnDeleteRequest() {
    saveTag(vertx, STUB_CUSTOM_RESOURCE_ID, RecordType.RESOURCE, STUB_TAG_VALUE);
    deleteResource(new EqualToJsonPattern("{\"isSelected\":false}", true, true));

    var actualTags = TagsTestUtil.getTags(vertx);
    assertTrue(actualTags.isEmpty());
  }

  @Test
  void shouldDeleteAccessTypeOnDeleteRequest() {
    var accessTypeId = insertAccessTypes(testData(credentialsId), vertx).getFirst().getId();
    insertAccessTypeMapping(STUB_CUSTOM_RESOURCE_ID, RecordType.RESOURCE, accessTypeId, vertx);
    deleteResource(new EqualToJsonPattern("{\"isSelected\":false}", true, true));

    var actualMappings = getAccessTypeMappings(vertx);
    assertTrue(actualMappings.isEmpty());
  }

  @Test
  void shouldReturn400WhenResourceIdIsInvalid() {
    var error = deleteWithStatus(RESOURCE_BY_ID_PATH.formatted("abc-def"), SC_BAD_REQUEST)
      .as(JsonapiError.class);

    assertErrorContainsTitle(error, "Resource id is invalid");
  }

  @Test
  void shouldReturn400WhenTryingToDeleteResourceAssociatedWithManagedPackage() {
    var managedResourceEndpoint = resourcesRmApi(STUB_VENDOR_ID, STUB_PACKAGE_ID, STUB_MANAGED_TITLE_ID);

    mockGet(equalTo(managedResourceEndpoint), readFile(RMAPI_GET_MANAGED_RESOURCE_UPDATED));

    var error = deleteWithStatus(STUB_MANAGED_RESOURCE_PATH, SC_BAD_REQUEST)
      .as(JsonapiError.class);

    assertErrorContainsTitle(error, "Resource cannot be deleted");
  }

  @Test
  void shouldReturnListWithBulkFetchResources() {
    var managedResourceEndpoint = resourcesRmApi(STUB_VENDOR_ID, STUB_PACKAGE_ID, STUB_MANAGED_TITLE_ID);

    mockGet(equalTo(managedResourceEndpoint), readFile(RMAPI_GET_RESOURCE_BY_ID_SUCCESS));

    var postBody = readFile(REQUEST_POST_RESOURCES_BULK);
    var actualResponse = postWithOk(RESOURCES_BULK_FETCH, postBody).asString();

    assertJsonEqual(readFile(EXPECTED_RESOURCES_BULK_RESPONSE), actualResponse, true);
  }

  @Test
  void shouldReturn422OnInvalidIdFormat() {
    var error = postWithStatus(RESOURCES_BULK_FETCH, readFile(REQUEST_POST_RESOURCES_BULK_INVALID_FORMAT),
      SC_UNPROCESSABLE_ENTITY).as(Errors.class);

    assertEquals("elements in list must match pattern", error.getErrors().getFirst().getMessage());
  }

  @Test
  void shouldReturnResponseWhenIdIsTooLong() {
    var actualResponse = postWithOk(RESOURCES_BULK_FETCH, readFile(REQUEST_POST_RESOURCE_TOO_LONG_ID)).asString();

    assertJsonEqual(readFile(EXPECTED_RESPONSE_TOO_LONG_ID), actualResponse, false);
  }

  @Test
  void shouldReturnResourcesAndFailedIds() {
    var managedResourceEndpoint = resourcesRmApi(STUB_VENDOR_ID, STUB_PACKAGE_ID, STUB_MANAGED_TITLE_ID);
    var customResourceEndpoint = resourcesRmApi(CUSTOM_VENDOR_ID, CUSTOM_PACKAGE_ID, CUSTOM_TITLE_ID);
    var thirdResourceEndpoint = resourcesRmApi(186, 3150130, 19087948);

    mockGet(equalTo(managedResourceEndpoint), readFile(RMAPI_GET_RESOURCE_BY_ID_SUCCESS));
    mockGet(equalTo(customResourceEndpoint), readFile(RMAPI_GET_CUSTOM_RESOURCE_UPDATED));
    mockGet(equalTo(thirdResourceEndpoint), readFile(RMAPI_GET_RESOURCE_NOT_FOUND), SC_NOT_FOUND);

    var actualResponse = postWithOk(RESOURCES_BULK_FETCH, readFile(REQUEST_POST_RESOURCES_BULK_INVALID_IDS)).asString();

    assertJsonEqual(readFile(EXPECTED_RESOURCES_BULK_WITH_FAILED_IDS), actualResponse, false);
  }

  @Test
  void shouldReturnErrorWhenRmApiFails() {
    mockGet(matching(packageTitlesRmApi(STUB_VENDOR_ID, STUB_PACKAGE_ID) + ".*"), SC_INTERNAL_SERVER_ERROR);

    var bulkFetchCollection = postWithOk(RESOURCES_BULK_FETCH, readFile(REQUEST_POST_RESOURCES_BULK))
      .as(ResourceBulkFetchCollection.class);

    assertEquals(0, bulkFetchCollection.getIncluded().size());
    assertEquals(1, bulkFetchCollection.getMeta().getFailed().getResources().size());
    assertEquals(STUB_MANAGED_RESOURCE_ID, bulkFetchCollection.getMeta().getFailed().getResources().getFirst());
  }

  private void mockPackageResources(String responseFile) {
    mockGet(matching(packageTitlesRmApi(STUB_VENDOR_ID, STUB_PACKAGE_ID) + ".*"), readFile(responseFile));
  }

  private void mockPackage(String responseFile) {
    mockGet(matching(packageRmApi(STUB_PACKAGE_ID)), readFile(responseFile));
  }

  private void mockResource(String responseFile) {
    mockGet(matching(resourcesRmApi(STUB_VENDOR_ID, STUB_PACKAGE_ID, STUB_MANAGED_TITLE_ID)),
      readFile(responseFile));
  }

  private void mockTitle(String responseFile) {
    mockGet(matching(titlesRmApi() + ".*"), readFile(responseFile));
  }

  private void mockVendor(String responseFile) {
    mockGet(matching(vendorsRmApi(STUB_VENDOR_ID)), readFile(responseFile));
  }

  private String mockUpdateResourceScenario(String updatedResourceResponseFile, String resourceEndpoint,
                                            String resourceId, String requestBody) {
    mockGet(matching(resourceEndpoint), readFile(updatedResourceResponseFile));
    mockPut(matching(resourceEndpoint), SC_NO_CONTENT);
    return putWithOk(RESOURCE_BY_ID_PATH.formatted(resourceId), requestBody).asString();
  }

  private void sendPutTags(List<String> newTags) {
    var tags = readJsonFile(REQUEST_PUT_RESOURCE_TAGS, ResourceTagsPutRequest.class);

    if (newTags != null) {
      tags.getData().getAttributes().setTags(new Tags().withTagList(newTags));
    }

    putWithOk(RESOURCE_TAGS_PATH, Json.encode(tags)).as(ResourceTags.class);
  }

  private void deleteResource(EqualToJsonPattern putBodyPattern) {
    var customResourceEndpoint = resourcesRmApi(CUSTOM_VENDOR_ID, CUSTOM_PACKAGE_ID, CUSTOM_TITLE_ID);

    mockGet(equalTo(customResourceEndpoint), readFile(RMAPI_GET_CUSTOM_RESOURCE_UPDATED));
    mockPut(equalTo(customResourceEndpoint), putBodyPattern, SC_NO_CONTENT);

    deleteWithNoContent(RESOURCE_BY_ID_PATH.formatted(STUB_CUSTOM_RESOURCE_ID));
  }
}
