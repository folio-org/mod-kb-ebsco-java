package org.folio.rest.impl;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static java.lang.String.format;
import static org.folio.HttpStatus.SC_BAD_REQUEST;
import static org.folio.HttpStatus.SC_FORBIDDEN;
import static org.folio.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.folio.HttpStatus.SC_NOT_FOUND;
import static org.folio.HttpStatus.SC_NO_CONTENT;
import static org.folio.HttpStatus.SC_OK;
import static org.folio.HttpStatus.SC_UNAUTHORIZED;
import static org.folio.HttpStatus.SC_UNPROCESSABLE_CONTENT;
import static org.folio.repository.RecordType.PACKAGE;
import static org.folio.repository.RecordType.RESOURCE;
import static org.folio.repository.accesstypes.AccessTypeMappingsTableConstants.ACCESS_TYPES_MAPPING_TABLE_NAME;
import static org.folio.repository.accesstypes.AccessTypesTableConstants.ACCESS_TYPES_TABLE_NAME;
import static org.folio.repository.kbcredentials.KbCredentialsTableConstants.KB_CREDENTIALS_TABLE_NAME;
import static org.folio.repository.packages.PackageTableConstants.PACKAGES_TABLE_NAME;
import static org.folio.repository.resources.ResourceTableConstants.RESOURCES_TABLE_NAME;
import static org.folio.repository.tag.TagTableConstants.TAGS_TABLE_NAME;
import static org.folio.util.AccessTypesTestUtil.STUB_ACCESS_TYPE_NAME;
import static org.folio.util.AccessTypesTestUtil.STUB_ACCESS_TYPE_NAME_2;
import static org.folio.util.AccessTypesTestUtil.getAccessTypeMappings;
import static org.folio.util.AccessTypesTestUtil.insertAccessType;
import static org.folio.util.AccessTypesTestUtil.insertAccessTypeMapping;
import static org.folio.util.AccessTypesTestUtil.insertAccessTypes;
import static org.folio.util.AccessTypesTestUtil.testData;
import static org.folio.util.AssertTestUtil.assertEqualsPackageId;
import static org.folio.util.AssertTestUtil.assertEqualsUuid;
import static org.folio.util.AssertTestUtil.assertErrorContainsTitle;
import static org.folio.util.AssertTestUtil.assertJsonEqual;
import static org.folio.util.KbCredentialsTestUtil.getDefaultKbConfiguration;
import static org.folio.util.KbCredentialsTestUtil.setupDefaultKbConfiguration;
import static org.folio.util.PackagesTestUtil.buildDbPackage;
import static org.folio.util.PackagesTestUtil.savePackage;
import static org.folio.util.TagsTestUtil.saveTag;
import static org.folio.util.TestUtil.clearDataFromTable;
import static org.folio.util.TestUtil.readFile;
import static org.folio.util.TestUtil.readJsonFile;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.tomakehurst.wiremock.client.WireMock;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import org.folio.holdingsiq.model.CoverageDates;
import org.folio.holdingsiq.model.PackageData;
import org.folio.holdingsiq.model.PackagePut;
import org.folio.rest.jaxrs.model.ContentType;
import org.folio.rest.jaxrs.model.Errors;
import org.folio.rest.jaxrs.model.JsonapiError;
import org.folio.rest.jaxrs.model.KbCredentials;
import org.folio.rest.jaxrs.model.Package;
import org.folio.rest.jaxrs.model.PackageCollection;
import org.folio.rest.jaxrs.model.PackagePostRequest;
import org.folio.rest.jaxrs.model.PackagePutRequest;
import org.folio.rest.jaxrs.model.PackageTags;
import org.folio.rest.jaxrs.model.PackageTagsPutRequest;
import org.folio.rest.jaxrs.model.ResourceCollection;
import org.folio.rest.jaxrs.model.Tags;
import org.folio.util.AccessTypesTestUtil;
import org.folio.util.IntegrationTestBase;
import org.folio.util.PackagesTestUtil;
import org.folio.util.TagsTestUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EholdingsPackagesIntegrationTest extends IntegrationTestBase {

  // RM API responses
  private static final String PACKAGE_STUB_FILE =
    "responses/rmapi/packages/get-package-by-id-response.json";
  private static final String PACKAGE_2_STUB_FILE =
    "responses/rmapi/packages/get-package-by-id-2-response.json";
  private static final String CUSTOM_PACKAGE_STUB_FILE =
    "responses/rmapi/packages/get-custom-package-by-id-response.json";
  private static final String RESOURCES_BY_PACKAGE_ID_STUB_FILE =
    "responses/rmapi/resources/get-resources-by-package-id-response.json";
  private static final String RESOURCES_BY_PACKAGE_ID_EMPTY_STUB_FILE =
    "responses/rmapi/resources/get-resources-by-package-id-response-empty.json";
  private static final String RESOURCES_BY_PACKAGE_ID_EMPTY_CUSTOMER_RESOURCE_LIST_STUB_FILE =
    "responses/rmapi/resources/get-resources-by-package-id-response-empty-customer-list.json";
  private static final String VENDOR_BY_PACKAGE_ID_STUB_FILE =
    "responses/rmapi/vendors/get-vendor-by-id-for-package.json";
  private static final String GET_PACKAGES_RESPONSE =
    "responses/rmapi/packages/get-packages-response.json";
  private static final String GET_PACKAGES_BY_PROVIDER_RESPONSE =
    "responses/rmapi/packages/get-packages-by-provider-id.json";
  private static final String GET_PACKAGE_PROVIDER_RESPONSE =
    "responses/rmapi/packages/get-package-provider-by-id.json";
  private static final String POST_PACKAGE_CREATED_RESPONSE =
    "responses/rmapi/packages/post-package-response.json";
  private static final String POST_PACKAGE_400_RESPONSE =
    "responses/rmapi/packages/post-package-400-error-response.json";
  private static final String GET_PACKAGE_RESOURCES_400_RESPONSE =
    "responses/rmapi/packages/get-package-resources-400-response.json";
  private static final String GET_PACKAGE_NOT_FOUND_RESPONSE =
    "responses/rmapi/packages/get-package-by-id-not-found-response.json";
  private static final String GET_PROXIES_CUSTOMLABELS_RESPONSE =
    "responses/rmapi/proxiescustomlabels/get-success-response.json";
  private static final String GET_TITLE_BY_ID_RESPONSE =
    "responses/rmapi/titles/get-title-by-id-response.json";

  // KB-EBSCO expected responses
  private static final String EXPECTED_PACKAGE_BY_ID_STUB_FILE =
    "responses/kb-ebsco/packages/expected-package-by-id.json";
  private static final String EXPECTED_RESOURCES_STUB_FILE =
    "responses/kb-ebsco/resources/expected-resources-by-package-id.json";
  private static final String EXPECTED_RESOURCES_WITH_TAGS_STUB_FILE =
    "responses/kb-ebsco/resources/expected-resources-by-package-id-with-tags.json";
  private static final String EXPECTED_PACKAGES_COLLECTION_5 =
    "responses/kb-ebsco/packages/expected-package-collection-with-five-elements.json";
  private static final String EXPECTED_PACKAGES_COLLECTION_1 =
    "responses/kb-ebsco/packages/expected-package-collection-with-one-element.json";
  private static final String EXPECTED_PACKAGE_WITH_PROVIDER =
    "responses/kb-ebsco/packages/expected-package-by-id-with-provider.json";
  private static final String EXPECTED_POST_PACKAGES_BULK =
    "responses/kb-ebsco/packages/expected-post-packages-bulk.json";
  private static final String EXPECTED_POST_PACKAGES_BULK_FAILED =
    "responses/kb-ebsco/packages/expected-post-packages-bulk-with-failed-id.json";
  private static final String EXPECTED_POST_PACKAGES_BULK_EMPTY =
    "responses/kb-ebsco/packages/expected-post-packages-bulk-empty.json";

  // Request payloads
  private static final String PUT_PACKAGE_SELECTED_REQUEST =
    "requests/kb-ebsco/package/put-package-selected.json";
  private static final String PUT_PACKAGE_SELECTED_WITH_ACCESS_TYPE_REQUEST =
    "requests/kb-ebsco/package/put-package-selected-with-access-type.json";
  private static final String PUT_PACKAGE_NOT_SELECTED_NON_EMPTY_REQUEST =
    "requests/kb-ebsco/package/put-package-not-selected-non-empty-fields.json";
  private static final String PUT_PACKAGE_CUSTOM_MULTIPLE_ATTRIBUTES_REQUEST =
    "requests/kb-ebsco/package/put-package-custom-multiple-attributes.json";
  private static final String PUT_PACKAGE_CUSTOM_WITH_ACCESS_TYPE_REQUEST =
    "requests/kb-ebsco/package/put-package-custom-with-access-type.json";
  private static final String PUT_PACKAGE_WITH_NOT_EXISTED_ACCESS_TYPE_REQUEST =
    "requests/kb-ebsco/package/put-package-with-not-existed-access-type.json";
  private static final String PUT_PACKAGE_WITH_INVALID_ACCESS_TYPE_REQUEST =
    "requests/kb-ebsco/package/put-package-with-invalid-access-type.json";
  private static final String PUT_PACKAGE_TAGS_REQUEST =
    "requests/kb-ebsco/package/put-package-tags.json";
  private static final String POST_PACKAGE_REQUEST =
    "requests/kb-ebsco/package/post-package-request.json";
  private static final String POST_PACKAGE_WITH_ACCESS_TYPE_REQUEST =
    "requests/kb-ebsco/package/post-package-with-access-type-request.json";
  private static final String POST_PACKAGE_WITH_NOT_EXISTED_ACCESS_TYPE_REQUEST =
    "requests/kb-ebsco/package/post-package-with-not-existed-access-type-request.json";
  private static final String POST_PACKAGE_WITH_INVALID_ACCESS_TYPE_REQUEST =
    "requests/kb-ebsco/package/post-package-with-invalid-access-type-request.json";
  private static final String POST_PACKAGES_BULK_REQUEST =
    "requests/kb-ebsco/package/post-packages-bulk.json";
  private static final String POST_PACKAGES_BULK_WITH_INVALID_ID_REQUEST =
    "requests/kb-ebsco/package/post-packages-bulk-with-invalid-id-format.json";
  private static final String POST_PACKAGES_BULK_WITH_NON_EXISTING_ID_REQUEST =
    "requests/kb-ebsco/package/post-packages-bulk-with-non-existing-id.json";
  private static final String POST_PACKAGES_BULK_EMPTY_REQUEST =
    "requests/kb-ebsco/package/post-packages-bulk-empty.json";

  // RM API request payloads
  private static final String PUT_PACKAGE_SELECTED_RMAPI_REQUEST =
    "requests/rmapi/packages/put-package-is-selected.json";
  private static final String PUT_PACKAGE_CUSTOM_RMAPI_REQUEST =
    "requests/rmapi/packages/put-package-custom.json";
  private static final String POST_PACKAGE_RMAPI_REQUEST =
    "requests/rmapi/packages/post-package.json";

  private KbCredentials configuration;

  @BeforeEach
  void setUp() {
    setupDefaultKbConfiguration(getWiremockUrl(), vertx);
    configuration = getDefaultKbConfiguration(vertx);
  }

  @AfterEach
  void tearDown() {
    clearDataFromTable(vertx, ACCESS_TYPES_MAPPING_TABLE_NAME);
    clearDataFromTable(vertx, ACCESS_TYPES_TABLE_NAME);
    clearDataFromTable(vertx, TAGS_TABLE_NAME);
    clearDataFromTable(vertx, RESOURCES_TABLE_NAME);
    clearDataFromTable(vertx, PACKAGES_TABLE_NAME);
    clearDataFromTable(vertx, KB_CREDENTIALS_TABLE_NAME);
  }

  @Test
  void shouldReturnPackagesOnGet() {
    mockGet(matching(packagesRmApi() + ".*"), readFile(GET_PACKAGES_RESPONSE));
    var expected = readJsonFile(EXPECTED_PACKAGES_COLLECTION_5, PackageCollection.class);

    var actual = getWithOk(packagesPath() + "?q=American&filter[type]=abstractandindex&count=5")
      .as(PackageCollection.class);
    assertEquals(expected, actual);
  }

  @Test
  void shouldReturnPackagesOnSearchByTagsOnly() {
    saveTag(vertx, FULL_PACKAGE_ID, PACKAGE, STUB_TAG_VALUE);
    saveTag(vertx, FULL_PACKAGE_ID_2, PACKAGE, STUB_TAG_VALUE);
    saveTag(vertx, FULL_PACKAGE_ID_2, PACKAGE, STUB_TAG_VALUE_2);
    saveTag(vertx, FULL_PACKAGE_ID_3, PACKAGE, STUB_TAG_VALUE_3);

    setUpPackages(vertx, configuration.getId());

    var packageCollection = getWithOk(withTagFilters(packagesPath(), STUB_TAG_VALUE, STUB_TAG_VALUE_2))
      .as(PackageCollection.class);
    var packages = packageCollection.getData();

    assertEquals(2, (int) packageCollection.getMeta().getTotalResults());
    assertEquals(2, packages.size());
    assertEquals(STUB_PACKAGE_NAME, packages.get(0).getAttributes().getName());
    assertEquals(STUB_PACKAGE_NAME_2, packages.get(1).getAttributes().getName());
  }

  @Test
  void shouldReturnEmptyResponseWhenPackagesReturnedWithErrorOnSearchByTags() {
    savePackage(buildDbPackage(FULL_PACKAGE_ID, configuration.getId(), STUB_PACKAGE_NAME), vertx);
    savePackage(buildDbPackage(FULL_PACKAGE_ID_2, configuration.getId(), STUB_PACKAGE_NAME_2), vertx);
    saveTag(vertx, FULL_PACKAGE_ID, PACKAGE, STUB_TAG_VALUE);
    saveTag(vertx, FULL_PACKAGE_ID_2, PACKAGE, STUB_TAG_VALUE);

    mockGet(matching(packagesRmApi() + ".*"), SC_INTERNAL_SERVER_ERROR);

    var packageCollection = getWithOk(withTagFilters(packagesPath(), STUB_TAG_VALUE)).as(PackageCollection.class);

    assertEquals(2, (int) packageCollection.getMeta().getTotalResults());
  }

  @Test
  void shouldReturnPackagesOnSearchWithPagination() {
    saveTag(vertx, FULL_PACKAGE_ID, PACKAGE, STUB_TAG_VALUE);
    saveTag(vertx, FULL_PACKAGE_ID_2, PACKAGE, STUB_TAG_VALUE);
    saveTag(vertx, FULL_PACKAGE_ID_3, PACKAGE, STUB_TAG_VALUE);

    setUpPackages(vertx, configuration.getId());

    var packageCollection = getWithOk(withTagFilters(packagesPath() + "?page=2&count=1", STUB_TAG_VALUE))
      .as(PackageCollection.class);
    var packages = packageCollection.getData();

    assertEquals(3, (int) packageCollection.getMeta().getTotalResults());
    assertEquals(1, packages.size());
    assertEquals(STUB_PACKAGE_NAME_2, packages.getFirst().getAttributes().getName());
  }

  @Test
  void shouldReturnPackagesOnSearchByAccessTypeWithPagination() {
    var accessTypes = insertAccessTypes(testData(configuration.getId()), vertx);
    insertAccessTypeMapping(FULL_PACKAGE_ID, PACKAGE, accessTypes.get(0).getId(), vertx);
    insertAccessTypeMapping(FULL_PACKAGE_ID_2, PACKAGE, accessTypes.get(1).getId(), vertx);

    setUpPackages(vertx, configuration.getId());

    var resourcePath = withAccessTypeFilters(packagesPath() + "?page=2&count=1",
      STUB_ACCESS_TYPE_NAME, STUB_ACCESS_TYPE_NAME_2);
    var packageCollection = getWithOk(resourcePath).as(PackageCollection.class);
    var packages = packageCollection.getData();

    assertEquals(2, (int) packageCollection.getMeta().getTotalResults());
    assertEquals(1, packages.size());
    assertEquals(STUB_PACKAGE_NAME, packages.getFirst().getAttributes().getName());
  }

  @Test
  void shouldReturnEmptyResponseWhenPackagesReturnedWithErrorOnSearchByAccessType() {
    var accessTypes = insertAccessTypes(testData(configuration.getId()), vertx);
    insertAccessTypeMapping(FULL_PACKAGE_ID, PACKAGE, accessTypes.getFirst().getId(), vertx);
    insertAccessTypeMapping(FULL_PACKAGE_ID_2, PACKAGE, accessTypes.getFirst().getId(), vertx);

    mockGet(matching(".*lists/.*"), SC_INTERNAL_SERVER_ERROR);

    var resourcePath = withAccessTypeFilters(packagesPath(), STUB_ACCESS_TYPE_NAME);
    var packageCollection = getWithOk(resourcePath).as(PackageCollection.class);

    assertEquals(2, (int) packageCollection.getMeta().getTotalResults());
  }

  @Test
  void shouldReturn400OnInvalidFilterCustomParameter() {
    var resourcePath = packagesPath() + "?filter[custom]=test";
    var response = getWithStatus(resourcePath, SC_BAD_REQUEST).asString();

    assertTrue(response.contains("Invalid Query Parameter for filter[custom]: only 'true' is supported"));
  }

  @Test
  void shouldReturn400OnNotSupportedFilterCustomParameter() {
    var resourcePath = packagesPath() + "?filter[custom]=false";
    var response = getWithStatus(resourcePath, SC_BAD_REQUEST).asString();

    assertTrue(response.contains("Invalid Query Parameter for filter[custom]: only 'true' is supported"));
  }

  @Test
  void shouldReturnPackagesOnGetWithPackageId() {
    mockGet(matching(rootProxyCustomLabelsRmApi()), readFile(GET_PROXIES_CUSTOMLABELS_RESPONSE));
    mockGet(matching(providerPackagesRmApi(STUB_VENDOR_ID) + ".*"), readFile(GET_PACKAGES_BY_PROVIDER_RESPONSE));

    var packages = getWithOk(packagesPath() + "?q=a&count=5&page=1&filter[custom]=true").asString();

    assertJsonEqual(readFile(EXPECTED_PACKAGES_COLLECTION_1), packages, true);
  }

  @Test
  void shouldReturnPackagesOnGetById() {
    mockGet(matching(packageRmApi(STUB_PACKAGE_ID)), readFile(CUSTOM_PACKAGE_STUB_FILE));

    var packages = getWithOk(packagePath(FULL_PACKAGE_ID)).asString();

    assertJsonEqual(readFile(EXPECTED_PACKAGE_BY_ID_STUB_FILE), packages);
  }

  @Test
  void shouldReturnPackageWithTagOnGetById() {
    saveTag(vertx, FULL_PACKAGE_ID, PACKAGE, STUB_TAG_VALUE);
    mockGet(matching(packageRmApi(STUB_PACKAGE_ID)), readFile(CUSTOM_PACKAGE_STUB_FILE));

    var packages = getWithOk(packagePath(FULL_PACKAGE_ID)).as(Package.class);

    assertTrue(packages.getData().getAttributes().getTags().getTagList().contains(STUB_TAG_VALUE));
  }

  @Test
  void shouldReturnPackageWithAccessTypeOnGetById() {
    var accessTypes = insertAccessTypes(testData(configuration.getId()), vertx);
    var expectedAccessTypeId = accessTypes.getFirst().getId();
    insertAccessTypeMapping(FULL_PACKAGE_ID, PACKAGE, expectedAccessTypeId, vertx);

    mockGet(matching(packageRmApi(STUB_PACKAGE_ID)), readFile(CUSTOM_PACKAGE_STUB_FILE));
    var packageData = getWithOk(packagePath(FULL_PACKAGE_ID)).as(Package.class);

    assertNotNull(packageData.getIncluded());
    assertEquals(expectedAccessTypeId, packageData.getData().getRelationships().getAccessType().getData().getId());
    assertEquals(expectedAccessTypeId, ((LinkedHashMap<?, ?>) packageData.getIncluded().getFirst()).get("id"));
  }

  @Test
  void shouldAddPackageTagsOnPutTagsWhenPackageAlreadyHasTags() {
    saveTag(vertx, FULL_PACKAGE_ID, PACKAGE, STUB_TAG_VALUE);
    var newTags = List.of(STUB_TAG_VALUE, STUB_TAG_VALUE_2);

    sendPutTags(newTags);

    var tagsAfterRequest = TagsTestUtil.getTagsForRecordType(vertx, PACKAGE);
    assertThat(tagsAfterRequest, containsInAnyOrder(newTags.toArray()));
  }

  @Test
  void shouldAddPackageDataOnPutTags() {
    var tags = Collections.singletonList(STUB_TAG_VALUE);

    sendPutTags(tags);

    var packages = PackagesTestUtil.getPackages(vertx);
    assertEquals(1, packages.size());
    assertEqualsPackageId(packages.getFirst().getId());
    assertEquals(STUB_PACKAGE_NAME, packages.getFirst().getName());
    assertTrue(packages.getFirst().getContentType().equalsIgnoreCase(STUB_PACKAGE_CONTENT_TYPE));
  }

  @Test
  void shouldUpdateTagsOnlyOnPutPackageTagsEndpoint() {
    var tags = Collections.singletonList(STUB_TAG_VALUE);

    sendPutTags(tags);
    var updatedPackage = sendPut(readFile(PACKAGE_STUB_FILE));

    var packageTags = TagsTestUtil.getTagsForRecordType(vertx, PACKAGE);
    assertThat(packageTags, is(tags));
    assertTrue(Objects.isNull(updatedPackage.getData().getAttributes().getTags()));
  }

  @Test
  void shouldDeleteAllPackageTagsOnPutTagsWhenRequestHasEmptyListOfTags() {
    saveTag(vertx, FULL_PACKAGE_ID, PACKAGE, "test one");

    sendPutTags(Collections.emptyList());

    var tagsAfterRequest = TagsTestUtil.getTagsForRecordType(vertx, PACKAGE);
    assertTrue(tagsAfterRequest.isEmpty());
  }

  @Test
  void shouldDoNothingOnPutWhenRequestHasNotTags() {
    sendPutTags(null);
    sendPutTags(null);

    var tagsAfterRequest = TagsTestUtil.getTagsForRecordType(vertx, PACKAGE);
    assertTrue(tagsAfterRequest.isEmpty());
  }

  @Test
  void shouldReturn422OnPutTagsWhenRequestBodyIsInvalid() {
    var tags = readJsonFile(PUT_PACKAGE_TAGS_REQUEST, PackageTagsPutRequest.class);
    tags.getData().getAttributes().setName("");
    var error = putWithStatus(tagsPath(FULL_PACKAGE_ID), Json.encode(tags), SC_UNPROCESSABLE_CONTENT)
      .as(JsonapiError.class);

    assertErrorContainsTitle(error, "Invalid name");
  }

  @Test
  void shouldDeletePackageTagsOnDelete() {
    saveTag(vertx, FULL_PACKAGE_ID, PACKAGE, "test one");
    mockGet(WireMock.equalTo(packageRmApi(STUB_PACKAGE_ID)), readFile(CUSTOM_PACKAGE_STUB_FILE));

    var putBodyPattern = equalToJson("{\"isSelected\":false}", true, true);
    mockPut(WireMock.equalTo(packageRmApi(STUB_PACKAGE_ID)), putBodyPattern, SC_NO_CONTENT);

    deleteWithNoContent(packagePath(FULL_PACKAGE_ID));

    var tagsAfterRequest = TagsTestUtil.getTagsForRecordType(vertx, PACKAGE);
    assertTrue(tagsAfterRequest.isEmpty());
  }

  @Test
  void shouldDeletePackageAccessTypeMappingOnDelete() {
    var accessTypeId = insertAccessTypes(testData(configuration.getId()), vertx).getFirst().getId();
    insertAccessTypeMapping(FULL_PACKAGE_ID, PACKAGE, accessTypeId, vertx);
    mockGet(WireMock.equalTo(packageRmApi(STUB_PACKAGE_ID)), readFile(CUSTOM_PACKAGE_STUB_FILE));

    var putBodyPattern = equalToJson("{\"isSelected\":false}", true, true);
    mockPut(WireMock.equalTo(packageRmApi(STUB_PACKAGE_ID)), putBodyPattern, SC_NO_CONTENT);

    deleteWithNoContent(packagePath(FULL_PACKAGE_ID));

    var mappingsAfterRequest = AccessTypesTestUtil.getAccessTypeMappings(vertx);
    assertTrue(mappingsAfterRequest.isEmpty());
  }

  @Test
  void shouldDeletePackageOnDeleteRequest() {
    sendPost(readFile(POST_PACKAGE_REQUEST));
    sendPutTags(Collections.singletonList(STUB_TAG_VALUE));

    mockGet(WireMock.equalTo(packageRmApi(STUB_PACKAGE_ID)), readFile(CUSTOM_PACKAGE_STUB_FILE));
    mockPut(WireMock.equalTo(packageRmApi(STUB_PACKAGE_ID)), SC_NO_CONTENT);

    deleteWithNoContent(packagePath(FULL_PACKAGE_ID));

    var packages = PackagesTestUtil.getPackages(vertx);
    assertTrue(packages.isEmpty());
  }

  @Test
  void shouldReturn404WhenPackageIsNotFoundOnRmApi() {
    mockGet(matching(packageRmApi(STUB_PACKAGE_ID)), SC_NOT_FOUND);

    var error = getWithStatus(packagePath(FULL_PACKAGE_ID), SC_NOT_FOUND).as(JsonapiError.class);

    assertErrorContainsTitle(error, "not found");
  }

  @Test
  void shouldReturnResourcesWhenIncludedFlagIsSetToResources() {
    mockGet(matching(packageRmApi(STUB_PACKAGE_ID)), readFile(CUSTOM_PACKAGE_STUB_FILE));
    mockResourceById(RESOURCES_BY_PACKAGE_ID_STUB_FILE);

    var actual = getWithOk(withInclude(packagePath(FULL_PACKAGE_ID), "resources")).as(Package.class);

    var expectedPackage = readJsonFile(EXPECTED_PACKAGE_BY_ID_STUB_FILE, Package.class);
    var expectedResources = readJsonFile(EXPECTED_RESOURCES_STUB_FILE, ResourceCollection.class);
    expectedPackage.getIncluded().addAll(expectedResources.getData());

    assertJsonEqual(Json.encode(expectedPackage), Json.encode(actual));
  }

  @Test
  void shouldReturnProviderWhenIncludedFlagIsSetToProvider() {
    mockGet(matching(packageRmApi(STUB_PACKAGE_ID)), readFile(CUSTOM_PACKAGE_STUB_FILE));
    mockGet(matching(vendorsRmApi(STUB_VENDOR_ID)), readFile(VENDOR_BY_PACKAGE_ID_STUB_FILE));

    var actual = getWithOk(withInclude(packagePath(FULL_PACKAGE_ID), "provider")).asString();

    var expected = readFile(EXPECTED_PACKAGE_WITH_PROVIDER);
    assertJsonEqual(expected, actual, true);
  }

  @Test
  void shouldSendDeleteRequestForPackage() {
    mockGet(WireMock.equalTo(packageRmApi(STUB_PACKAGE_ID)), readFile(CUSTOM_PACKAGE_STUB_FILE));
    var putBodyPattern = equalToJson("{\"isSelected\":false}", true, true);
    mockPut(WireMock.equalTo(packageRmApi(STUB_PACKAGE_ID)), putBodyPattern, SC_NO_CONTENT);

    deleteWithNoContent(packagePath(FULL_PACKAGE_ID));

    verifyPut(WireMock.equalTo(packageRmApi(STUB_PACKAGE_ID)), putBodyPattern);
  }

  @Test
  void shouldReturn400WhenPackageIdIsInvalid() {
    var error = deleteWithStatus(packagePath("abc-def"), SC_BAD_REQUEST).asString();

    assertTrue(error.contains("Package or provider id are invalid"));
  }

  @Test
  void shouldReturn400WhenPackageIsNotCustom() {
    var packageData = readJsonFile(CUSTOM_PACKAGE_STUB_FILE, PackageData.class).toBuilder()
      .isCustom(false).build();

    mockGet(WireMock.equalTo(packageRmApi(STUB_PACKAGE_ID)), Json.encode(packageData));

    var error = deleteWithStatus(packagePath(FULL_PACKAGE_ID), SC_BAD_REQUEST).as(JsonapiError.class);

    assertErrorContainsTitle(error, "Package cannot be deleted");
  }

  @Test
  void shouldReturn200WhenSelectingPackage() {
    boolean updatedIsSelected = true;

    var packageData = readJsonFile(PACKAGE_STUB_FILE, PackageData.class).toBuilder()
      .isSelected(updatedIsSelected).build();
    var updatedPackageValue = Json.encode(packageData);
    mockUpdateScenario(readFile(PACKAGE_STUB_FILE), updatedPackageValue);

    var actual = putWithOk(packagePath(FULL_PACKAGE_ID), readFile(PUT_PACKAGE_SELECTED_REQUEST)).as(Package.class);

    assertEquals(updatedIsSelected, actual.getData().getAttributes().getIsSelected());

    verifyPut(WireMock.equalTo(packageRmApi(STUB_PACKAGE_ID)),
      equalToJson(readFile(PUT_PACKAGE_SELECTED_RMAPI_REQUEST), true, true));
  }

  @Test
  void shouldUpdateAllAttributesInSelectedPackage() {
    boolean updatedSelected = true;
    boolean updatedAllowEbscoToAddTitles = true;
    boolean updatedHidden = true;
    String updatedBeginCoverage = "2003-01-01";
    String updatedEndCoverage = "2004-01-01";

    var updatedPackageValue =
      preparePackageData(updatedSelected, updatedHidden, updatedBeginCoverage, updatedEndCoverage,
        updatedAllowEbscoToAddTitles);
    mockUpdateScenario(readFile(PACKAGE_STUB_FILE), updatedPackageValue);

    var actual = putWithOk(packagePath(FULL_PACKAGE_ID), readFile(PUT_PACKAGE_SELECTED_REQUEST)).as(Package.class);

    verifyPut(WireMock.equalTo(packageRmApi(STUB_PACKAGE_ID)),
      equalToJson(readFile(PUT_PACKAGE_SELECTED_RMAPI_REQUEST), true, true));

    assertEquals(updatedSelected, actual.getData().getAttributes().getIsSelected());
    assertEquals(updatedAllowEbscoToAddTitles, actual.getData().getAttributes().getAllowKbToAddTitles());
    assertEquals(updatedBeginCoverage, actual.getData().getAttributes().getCustomCoverage().getBeginCoverage());
    assertEquals(updatedEndCoverage, actual.getData().getAttributes().getCustomCoverage().getEndCoverage());
  }

  @Test
  @SuppressWarnings("checkstyle:methodLength")
  void shouldUpdateAllAttributesInSelectedPackageAndCreateNewAccessTypeMapping() {
    var accessTypes = insertAccessTypes(testData(configuration.getId()), vertx);
    var accessTypeId = accessTypes.getFirst().getId();

    boolean updatedSelected = true;
    boolean updatedAllowEbscoToAddTitles = true;
    boolean updatedHidden = true;
    String updatedBeginCoverage = "2003-01-01";
    String updatedEndCoverage = "2004-01-01";

    var updatedPackageValue =
      preparePackageData(updatedSelected, updatedHidden, updatedBeginCoverage, updatedEndCoverage,
        updatedAllowEbscoToAddTitles);
    mockUpdateScenario(readFile(PACKAGE_STUB_FILE), updatedPackageValue);

    var putBody = format(readFile(PUT_PACKAGE_SELECTED_WITH_ACCESS_TYPE_REQUEST), accessTypeId);
    var actualPackage = putWithOk(packagePath(FULL_PACKAGE_ID), putBody).as(Package.class);

    assertEquals(updatedSelected, actualPackage.getData().getAttributes().getIsSelected());
    assertEquals(updatedAllowEbscoToAddTitles, actualPackage.getData().getAttributes().getAllowKbToAddTitles());
    assertEquals(updatedBeginCoverage, actualPackage.getData().getAttributes().getCustomCoverage().getBeginCoverage());
    assertEquals(updatedEndCoverage, actualPackage.getData().getAttributes().getCustomCoverage().getEndCoverage());

    var accessTypeMappingsInDb = getAccessTypeMappings(vertx);
    assertEquals(1, accessTypeMappingsInDb.size());
    assertEquals(actualPackage.getData().getId(), accessTypeMappingsInDb.getFirst().getRecordId());
    assertEqualsUuid(accessTypeId, accessTypeMappingsInDb.getFirst().getAccessTypeId());
    assertEquals(PACKAGE, accessTypeMappingsInDb.getFirst().getRecordType());
    assertNotNull(actualPackage.getIncluded());
    assertEquals(accessTypeId, actualPackage.getData().getRelationships().getAccessType().getData().getId());
    assertEquals(accessTypeId, ((LinkedHashMap<?, ?>) actualPackage.getIncluded().getFirst()).get("id"));
  }

  @Test
  void shouldDeleteAccessTypeMappingWhenRmApiSend404() {
    var accessTypes = insertAccessTypes(testData(configuration.getId()), vertx);
    var currentAccessTypeId = accessTypes.get(0).getId();
    var newAccessTypeId = accessTypes.get(1).getId();
    insertAccessTypeMapping(FULL_PACKAGE_ID, PACKAGE, currentAccessTypeId, vertx);

    var packageData = readJsonFile(CUSTOM_PACKAGE_STUB_FILE, PackageData.class).toBuilder()
      .contentType("streamingmedia")
      .build();

    var updatedPackageValue = Json.encode(packageData);
    mockUpdateScenario(readFile(CUSTOM_PACKAGE_STUB_FILE), updatedPackageValue);

    var putBody = format(readFile(PUT_PACKAGE_CUSTOM_WITH_ACCESS_TYPE_REQUEST), newAccessTypeId);
    wm.stubFor(get(urlPathEqualTo(packageRmApi(STUB_PACKAGE_ID)))
      .inScenario("Put package")
      .whenScenarioStateIs(STARTED)
      .willReturn(aResponse().withBody(readFile(CUSTOM_PACKAGE_STUB_FILE)))
      .willSetStateTo("Not found"));
    wm.stubFor(get(urlPathEqualTo(packageRmApi(STUB_PACKAGE_ID)))
      .inScenario("Put package")
      .whenScenarioStateIs("Not found")
      .willReturn(aResponse().withStatus(SC_NOT_FOUND)));

    putWithStatus(packagePath(FULL_PACKAGE_ID), putBody, SC_NOT_FOUND);

    var accessTypeMappingsInDb = getAccessTypeMappings(vertx);
    assertEquals(0, accessTypeMappingsInDb.size());
  }

  @Test
  void shouldReturn422OnPutWhenUnselectNonCustomPackageIsHidden() {
    var putBody = readFile(PUT_PACKAGE_NOT_SELECTED_NON_EMPTY_REQUEST);
    mockGet(matching(packageRmApi(STUB_PACKAGE_ID)), readFile(PACKAGE_STUB_FILE));
    var error = putWithStatus(packagePath(FULL_PACKAGE_ID), putBody, SC_UNPROCESSABLE_CONTENT).as(JsonapiError.class);

    verifyPut(WireMock.equalTo(packageRmApi(STUB_PACKAGE_ID)), 0);

    assertErrorContainsTitle(error, "Invalid visibility");
  }

  @Test
  void shouldReturn422OnPutWhenCustomPackageUpdateLikeNotCustom() {
    var putBody = readFile(PUT_PACKAGE_SELECTED_REQUEST);
    mockGet(matching(packageRmApi(STUB_PACKAGE_ID)), readFile(CUSTOM_PACKAGE_STUB_FILE));
    var error = putWithStatus(packagePath(FULL_PACKAGE_ID), putBody, SC_UNPROCESSABLE_CONTENT).as(JsonapiError.class);

    verifyPut(WireMock.equalTo(packageRmApi(STUB_PACKAGE_ID)), 0);

    assertErrorContainsTitle(error, "Package isCustom not matched");
  }

  @Test
  void shouldPassIsFullPackageAttributeToRmApi() {
    var updatedPackage = readJsonFile(PACKAGE_STUB_FILE, PackageData.class).toBuilder()
      .isSelected(true).build();
    mockUpdateScenario(readFile(PACKAGE_STUB_FILE), Json.encode(updatedPackage));
    var request = readJsonFile(PUT_PACKAGE_SELECTED_REQUEST, PackagePutRequest.class);
    request.getData().getAttributes().setIsFullPackage(false);

    putWithOk(packagePath(FULL_PACKAGE_ID), Json.encode(request)).as(Package.class);

    var rmApiPutRequest = readJsonFile(PUT_PACKAGE_SELECTED_RMAPI_REQUEST, PackagePut.class).toBuilder()
      .isFullPackage(false).build();
    verifyPut(WireMock.equalTo(packageRmApi(STUB_PACKAGE_ID)),
      equalToJson(Json.encode(rmApiPutRequest), true, true));
  }

  @Test
  void shouldUpdateAllAttributesInCustomPackage() {
    boolean updatedSelected = true;
    boolean updatedHidden = true;
    String updatedBeginCoverage = "2003-01-01";
    String updatedEndCoverage = "2004-01-01";
    String updatedPackageName = "name of the ages forever and ever";

    var updatedPackageValue = prepareCustomPackageData(updatedSelected, updatedHidden, updatedBeginCoverage,
      updatedEndCoverage, updatedPackageName);
    mockUpdateScenario(readFile(CUSTOM_PACKAGE_STUB_FILE), updatedPackageValue);

    var actualPackage = putWithOk(packagePath(FULL_PACKAGE_ID),
      readFile(PUT_PACKAGE_CUSTOM_MULTIPLE_ATTRIBUTES_REQUEST)).as(Package.class);

    verifyPut(WireMock.equalTo(packageRmApi(STUB_PACKAGE_ID)),
      equalToJson(readFile(PUT_PACKAGE_CUSTOM_RMAPI_REQUEST), true, true));

    assertEquals(updatedSelected, actualPackage.getData().getAttributes().getIsSelected());
    assertEquals(updatedBeginCoverage, actualPackage.getData().getAttributes().getCustomCoverage().getBeginCoverage());
    assertEquals(updatedEndCoverage, actualPackage.getData().getAttributes().getCustomCoverage().getEndCoverage());
    assertEquals(updatedPackageName, actualPackage.getData().getAttributes().getName());
    assertEquals(ContentType.STREAMING_MEDIA, actualPackage.getData().getAttributes().getContentType());
  }

  @Test
  @SuppressWarnings("checkstyle:methodLength")
  void shouldUpdateAllAttributesInCustomPackageAndCreateNewAccessTypeMapping() {
    var accessTypes = insertAccessTypes(testData(configuration.getId()), vertx);
    var accessTypeId = accessTypes.getFirst().getId();

    boolean updatedSelected = true;
    boolean updatedHidden = true;
    String updatedBeginCoverage = "2003-01-01";
    String updatedEndCoverage = "2004-01-01";
    String updatedPackageName = "name of the ages forever and ever";

    var updatedPackageValue = prepareCustomPackageData(updatedSelected, updatedHidden, updatedBeginCoverage,
      updatedEndCoverage, updatedPackageName);
    mockUpdateScenario(readFile(CUSTOM_PACKAGE_STUB_FILE), updatedPackageValue);

    var putBody = format(readFile(PUT_PACKAGE_CUSTOM_WITH_ACCESS_TYPE_REQUEST),
      accessTypeId);
    var actualPackage = putWithOk(packagePath(FULL_PACKAGE_ID), putBody).as(Package.class);

    verifyPut(WireMock.equalTo(packageRmApi(STUB_PACKAGE_ID)),
      equalToJson(readFile(PUT_PACKAGE_CUSTOM_RMAPI_REQUEST), true, true));

    assertEquals(updatedSelected, actualPackage.getData().getAttributes().getIsSelected());
    assertEquals(updatedBeginCoverage, actualPackage.getData().getAttributes().getCustomCoverage().getBeginCoverage());
    assertEquals(updatedEndCoverage, actualPackage.getData().getAttributes().getCustomCoverage().getEndCoverage());
    assertEquals(updatedPackageName, actualPackage.getData().getAttributes().getName());
    assertEquals(ContentType.STREAMING_MEDIA, actualPackage.getData().getAttributes().getContentType());

    var accessTypeMappingsInDb = getAccessTypeMappings(vertx);
    assertEquals(1, accessTypeMappingsInDb.size());
    assertEquals(actualPackage.getData().getId(), accessTypeMappingsInDb.getFirst().getRecordId());
    assertEqualsUuid(accessTypeId, accessTypeMappingsInDb.getFirst().getAccessTypeId());
    assertEquals(PACKAGE, accessTypeMappingsInDb.getFirst().getRecordType());
    assertNotNull(actualPackage.getIncluded());
    assertEquals(accessTypeId, actualPackage.getData().getRelationships().getAccessType().getData().getId());
    assertEquals(accessTypeId, ((LinkedHashMap<?, ?>) actualPackage.getIncluded().getFirst()).get("id"));
  }

  @Test
  @SuppressWarnings("checkstyle:methodLength")
  void shouldUpdateAllAttributesInCustomPackageAndDeleteAccessTypeMapping() {
    var accessTypes = insertAccessTypes(testData(configuration.getId()), vertx);
    var accessTypeId = accessTypes.getFirst().getId();

    insertAccessTypeMapping(FULL_PACKAGE_ID, PACKAGE, accessTypeId, vertx);

    boolean updatedSelected = true;
    boolean updatedHidden = true;
    String updatedBeginCoverage = "2003-01-01";
    String updatedEndCoverage = "2004-01-01";
    String updatedPackageName = "name of the ages forever and ever";

    var updatedPackageValue = prepareCustomPackageData(updatedSelected, updatedHidden, updatedBeginCoverage,
      updatedEndCoverage, updatedPackageName);
    mockUpdateScenario(readFile(CUSTOM_PACKAGE_STUB_FILE), updatedPackageValue);

    var putBody = readFile(PUT_PACKAGE_CUSTOM_MULTIPLE_ATTRIBUTES_REQUEST);
    var actualPackage = putWithOk(packagePath(FULL_PACKAGE_ID), putBody).as(Package.class);

    verifyPut(WireMock.equalTo(packageRmApi(STUB_PACKAGE_ID)),
      equalToJson(readFile(PUT_PACKAGE_CUSTOM_RMAPI_REQUEST), true, true));

    assertEquals(updatedSelected, actualPackage.getData().getAttributes().getIsSelected());
    assertEquals(updatedBeginCoverage, actualPackage.getData().getAttributes().getCustomCoverage().getBeginCoverage());
    assertEquals(updatedEndCoverage, actualPackage.getData().getAttributes().getCustomCoverage().getEndCoverage());
    assertEquals(updatedPackageName, actualPackage.getData().getAttributes().getName());
    assertEquals(ContentType.STREAMING_MEDIA, actualPackage.getData().getAttributes().getContentType());

    var accessTypeMappingsInDb = getAccessTypeMappings(vertx);
    assertEquals(0, accessTypeMappingsInDb.size());

    assertNotNull(actualPackage.getIncluded());
    assertEquals(0, actualPackage.getIncluded().size());
    assertNull(actualPackage.getData().getRelationships().getAccessType());
  }

  @Test
  @SuppressWarnings("checkstyle:methodLength")
  void shouldUpdateAllAttributesInCustomPackageAndUpdateAccessTypeMapping() {
    var accessTypes = insertAccessTypes(testData(configuration.getId()), vertx);
    final var currentAccessTypeId = accessTypes.get(0).getId();
    final var newAccessTypeId = accessTypes.get(1).getId();
    insertAccessTypeMapping(FULL_PACKAGE_ID, PACKAGE, currentAccessTypeId, vertx);

    boolean updatedSelected = true;
    boolean updatedHidden = true;
    String updatedBeginCoverage = "2003-01-01";
    String updatedEndCoverage = "2004-01-01";
    String updatedPackageName = "name of the ages forever and ever";

    var updatedPackageValue = prepareCustomPackageData(updatedSelected, updatedHidden, updatedBeginCoverage,
      updatedEndCoverage, updatedPackageName);
    mockUpdateScenario(readFile(CUSTOM_PACKAGE_STUB_FILE), updatedPackageValue);

    var putBody = format(readFile(PUT_PACKAGE_CUSTOM_WITH_ACCESS_TYPE_REQUEST), newAccessTypeId);
    var actualPackage = putWithOk(packagePath(FULL_PACKAGE_ID), putBody).as(Package.class);

    verifyPut(WireMock.equalTo(packageRmApi(STUB_PACKAGE_ID)),
      equalToJson(readFile(PUT_PACKAGE_CUSTOM_RMAPI_REQUEST), true, true));

    assertEquals(updatedSelected, actualPackage.getData().getAttributes().getIsSelected());
    assertEquals(updatedBeginCoverage, actualPackage.getData().getAttributes().getCustomCoverage().getBeginCoverage());
    assertEquals(updatedEndCoverage, actualPackage.getData().getAttributes().getCustomCoverage().getEndCoverage());
    assertEquals(updatedPackageName, actualPackage.getData().getAttributes().getName());
    assertEquals(ContentType.STREAMING_MEDIA, actualPackage.getData().getAttributes().getContentType());

    var accessTypeMappingsInDb = getAccessTypeMappings(vertx);
    assertEquals(1, accessTypeMappingsInDb.size());
    assertEquals(actualPackage.getData().getId(), accessTypeMappingsInDb.getFirst().getRecordId());
    assertEqualsUuid(newAccessTypeId, accessTypeMappingsInDb.getFirst().getAccessTypeId());
    assertEquals(PACKAGE, accessTypeMappingsInDb.getFirst().getRecordType());
    assertNotNull(actualPackage.getIncluded());
    assertEquals(newAccessTypeId, actualPackage.getData().getRelationships().getAccessType().getData().getId());
    assertEquals(newAccessTypeId, ((LinkedHashMap<?, ?>) actualPackage.getIncluded().getFirst()).get("id"));
  }

  @Test
  void shouldReturn400OnPutPackageWithNotExistedAccessType() {
    var requestBody = readFile(PUT_PACKAGE_WITH_NOT_EXISTED_ACCESS_TYPE_REQUEST);
    mockGet(matching(packageRmApi(STUB_PACKAGE_ID)), readFile(CUSTOM_PACKAGE_STUB_FILE));

    var error = putWithStatus(packagePath(FULL_PACKAGE_ID), requestBody, SC_BAD_REQUEST).as(JsonapiError.class);

    assertErrorContainsTitle(error, "Access type not found: id = 99999999-9999-1999-a999-999999999999");
  }

  @Test
  void shouldReturn422OnPutPackageWithInvalidAccessTypeId() {
    var requestBody = readFile(PUT_PACKAGE_WITH_INVALID_ACCESS_TYPE_REQUEST);

    var error = putWithStatus(packagePath(FULL_PACKAGE_ID), requestBody, SC_UNPROCESSABLE_CONTENT).as(Errors.class);

    assertEquals(1, error.getErrors().size());
    assertEquals(
      "must match \"^[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[1-5][a-fA-F0-9]{3}-[89abAB][a-fA-F0-9]{3}-[a-fA-F0-9]{12}$\"",
      error.getErrors().getFirst().getMessage());
  }

  @Test
  void shouldReturn400WhenRmApiReturns400() {
    var urlPattern = WireMock.equalTo(packageRmApi(STUB_PACKAGE_ID));

    mockGet(urlPattern, readFile(PACKAGE_STUB_FILE));
    mockPut(urlPattern, SC_BAD_REQUEST);

    var error = putWithStatus(packagePath(FULL_PACKAGE_ID), readFile(PUT_PACKAGE_SELECTED_REQUEST), SC_BAD_REQUEST)
      .as(JsonapiError.class);

    assertEquals(1, error.getErrors().size());
  }

  @Test
  void shouldReturn200WhenPackagePostIsValid() {
    var createdPackage = sendPost(readFile(POST_PACKAGE_REQUEST)).as(Package.class);

    assertTrue(Objects.isNull(createdPackage.getData().getAttributes().getTags()));
    wm.verify(1, postRequestedFor(urlPathEqualTo(packageRmApiV1(STUB_VENDOR_ID)))
      .withRequestBody(equalToJson(readFile(POST_PACKAGE_RMAPI_REQUEST), false, true)));
  }

  @Test
  void shouldReturn200OnPostPackageWithExistedAccessType() {
    var accessTypeId = insertAccessType(testData(configuration.getId()).getFirst(), vertx);

    var requestBody = format(readFile(POST_PACKAGE_WITH_ACCESS_TYPE_REQUEST), accessTypeId);
    var createdPackage = sendPost(requestBody).as(Package.class);

    assertTrue(Objects.isNull(createdPackage.getData().getAttributes().getTags()));
    wm.verify(1, postRequestedFor(urlPathEqualTo(packageRmApiV1(STUB_VENDOR_ID)))
      .withRequestBody(equalToJson(readFile(POST_PACKAGE_RMAPI_REQUEST), false, true)));

    var accessTypeMappingsInDb = getAccessTypeMappings(vertx);
    assertEquals(1, accessTypeMappingsInDb.size());
    assertEqualsUuid(accessTypeId, accessTypeMappingsInDb.getFirst().getAccessTypeId());
    assertEquals(PACKAGE, accessTypeMappingsInDb.getFirst().getRecordType());
    assertNotNull(createdPackage.getIncluded());
    assertEquals(accessTypeId, createdPackage.getData().getRelationships().getAccessType().getData().getId());
    assertEquals(accessTypeId, ((LinkedHashMap<?, ?>) createdPackage.getIncluded().getFirst()).get("id"));
  }

  @Test
  void shouldReturn400OnPostPackageWithNotExistedAccessType() {
    var requestBody = readFile(POST_PACKAGE_WITH_NOT_EXISTED_ACCESS_TYPE_REQUEST);
    mockUpdateScenario(readFile(CUSTOM_PACKAGE_STUB_FILE));

    var error = postWithStatus(packagesPath(), requestBody, SC_BAD_REQUEST).as(JsonapiError.class);

    assertErrorContainsTitle(error, "Access type not found: id = 99999999-9999-1999-a999-999999999999");
  }

  @Test
  void shouldReturn422OnPostPackageWithInvalidAccessTypeId() {
    var requestBody = readFile(POST_PACKAGE_WITH_INVALID_ACCESS_TYPE_REQUEST);

    var error = postWithStatus(packagesPath(), requestBody, SC_UNPROCESSABLE_CONTENT).as(Errors.class);

    assertEquals(1, error.getErrors().size());
    assertEquals(
      "must match \"^[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[1-5][a-fA-F0-9]{3}-[89abAB][a-fA-F0-9]{3}-[a-fA-F0-9]{12}$\"",
      error.getErrors().getFirst().getMessage());
  }

  @Test
  void shouldReturn400WhenPackagePostDataIsInvalid() {
    mockGet(WireMock.equalTo(rootProxyCustomLabelsRmApi()), readFile(GET_PACKAGE_PROVIDER_RESPONSE));
    mockPost(WireMock.equalTo(packageRmApiV1(STUB_VENDOR_ID)),
      equalToJson("""
          {
            "contentType" : 8,
            "packageName" : "TEST_NAME",
            "customCoverage" : {
              "beginCoverage" : "2017-12-23",
              "endCoverage" : "2018-03-30"
            }
          }""",
        false, true), readFile(POST_PACKAGE_400_RESPONSE), SC_BAD_REQUEST);

    var response = postWithStatus(packagesPath(), readFile(POST_PACKAGE_REQUEST), SC_BAD_REQUEST);
    assertErrorContainsTitle(response.as(JsonapiError.class), "name already exists");
  }

  @Test
  void shouldReturnDefaultResourcesOnGetWithResources() {
    String query = "?searchfield=titlename&selection=all&resourcetype=all"
                   + "&searchtype=advanced&search=&offset=1&count=25&orderby=titlename";
    shouldReturnResourcesOnGetWithResources(resourcesPath(FULL_PACKAGE_ID), query);
  }

  @Test
  void shouldReturnResourcesWithPagingOnGetWithResources() {
    var resourcesUrl = resourcesPath(FULL_PACKAGE_ID) + "?page=2";
    var query = "?searchfield=titlename&selection=all&resourcetype=all"
                + "&searchtype=advanced&search=&offset=2&count=25&orderby=titlename";
    shouldReturnResourcesOnGetWithResources(resourcesUrl, query);
  }

  @Test
  void shouldReturnEmptyListWhenResourcesAreNotFound() {
    mockResourceById(RESOURCES_BY_PACKAGE_ID_EMPTY_STUB_FILE);

    var actual = getWithOk(resourcesPath(FULL_PACKAGE_ID)).as(ResourceCollection.class);
    assertTrue(actual.getData().isEmpty());
    assertEquals(0, (int) actual.getMeta().getTotalResults());
  }

  @Test
  void shouldReturnResourcesWithTagsOnGetWithResources() {
    saveTag(vertx, "295-2545963-2099944", RESOURCE, STUB_TAG_VALUE);
    saveTag(vertx, "295-2545963-2172685", RESOURCE, STUB_TAG_VALUE_2);
    saveTag(vertx, "295-2545963-2172685", RESOURCE, STUB_TAG_VALUE_3);

    String query = "?searchfield=titlename&selection=all&resourcetype=all&searchtype=advanced"
                   + "&search=&offset=1&count=25&orderby=titlename";

    mockResourceById(RESOURCES_BY_PACKAGE_ID_STUB_FILE);

    var actual = getWithOk(resourcesPath(FULL_PACKAGE_ID)).asString();
    var expected = readFile(EXPECTED_RESOURCES_WITH_TAGS_STUB_FILE);

    assertJsonEqual(expected, actual);

    wm.verify(1, getRequestedFor(urlEqualTo(packageTitlesRmApi(STUB_VENDOR_ID, STUB_PACKAGE_ID) + query)));
  }

  @Test
  void shouldReturnResourcesWithAccessTypesOnGetWithResources() {
    var accessTypes = insertAccessTypes(testData(configuration.getId()), vertx);
    insertAccessTypeMapping(STUB_MANAGED_RESOURCE_ID, RESOURCE, accessTypes.getFirst().getId(), vertx);
    insertAccessTypeMapping(STUB_MANAGED_RESOURCE_ID_2, RESOURCE, accessTypes.getFirst().getId(), vertx);

    mockResourceById(GET_TITLE_BY_ID_RESPONSE);

    var resourcePath = withAccessTypeFilters(resourcesPath(FULL_PACKAGE_ID), STUB_ACCESS_TYPE_NAME);
    var resourceCollection = getWithOk(resourcePath).as(ResourceCollection.class);
    var resources = resourceCollection.getData();

    assertEquals(2, (int) resourceCollection.getMeta().getTotalResults());
    assertEquals(2, resources.size());
    assertThat(resources, everyItem(hasProperty("id",
      anyOf(equalTo(STUB_MANAGED_RESOURCE_ID), equalTo(STUB_MANAGED_RESOURCE_ID_2))
    )));
  }

  @Test
  void shouldReturnResourcesWithAccessTypesOnGetWithResourcesWithPagination() {
    var accessTypes = insertAccessTypes(testData(configuration.getId()), vertx);
    insertAccessTypeMapping(STUB_MANAGED_RESOURCE_ID, RESOURCE, accessTypes.get(0).getId(), vertx);
    insertAccessTypeMapping(STUB_MANAGED_RESOURCE_ID_2, RESOURCE, accessTypes.get(1).getId(), vertx);

    mockResourceById(GET_TITLE_BY_ID_RESPONSE);

    var resourcePath = withAccessTypeFilters(resourcesPath(FULL_PACKAGE_ID) + "?page=2&count=1",
      STUB_ACCESS_TYPE_NAME, STUB_ACCESS_TYPE_NAME_2);
    var resourceCollection = getWithOk(resourcePath).as(ResourceCollection.class);
    var resources = resourceCollection.getData();

    assertEquals(2, (int) resourceCollection.getMeta().getTotalResults());
    assertEquals(1, resources.size());
    assertThat(resources, everyItem(hasProperty("id", equalTo(STUB_MANAGED_RESOURCE_ID))));
  }

  @Test
  void shouldReturnResourcesWithAccessTypesOnGetWithResources1() {
    var accessTypes = insertAccessTypes(testData(configuration.getId()), vertx);
    insertAccessTypeMapping("295-2545963-2099944", RESOURCE, accessTypes.get(0).getId(), vertx);
    insertAccessTypeMapping("295-2545963-2172685", RESOURCE, accessTypes.get(1).getId(), vertx);

    mockResourceById(GET_TITLE_BY_ID_RESPONSE);
    mockResourceById(RESOURCES_BY_PACKAGE_ID_STUB_FILE);

    var resourceCollection = getWithOk(resourcesPath(FULL_PACKAGE_ID)).as(ResourceCollection.class);
    var resources = resourceCollection.getData();

    assertEquals(5, (int) resourceCollection.getMeta().getTotalResults());
    assertEquals(5, resources.size());

    assertEquals("295-2545963-2099944", resources.getFirst().getId());
    assertEquals(1, resources.get(0).getIncluded().size());
    assertEquals(accessTypes.getFirst().getId(),
      ((LinkedHashMap<?, ?>) resources.get(0).getIncluded().getFirst()).get("id"));
    assertEquals("295-2545963-2172685", resources.get(2).getId());
    assertEquals(1, resources.get(2).getIncluded().size());
    assertEquals(accessTypes.get(1).getId(),
      ((LinkedHashMap<?, ?>) resources.get(2).getIncluded().getFirst()).get("id"));
  }

  @Test
  void shouldReturnFilteredResourcesWithNonEmptyCustomerResourceList() {
    mockResourceById(RESOURCES_BY_PACKAGE_ID_EMPTY_CUSTOMER_RESOURCE_LIST_STUB_FILE);

    var resourceCollection = getWithOk(resourcesPath(FULL_PACKAGE_ID)).as(ResourceCollection.class);

    var metaTotalResults = resourceCollection.getMeta();
    assertEquals(3, metaTotalResults.getTotalResults());
  }

  @Test
  void shouldReturn404OnGetWithResourcesWhenPackageNotFound() {
    mockGet(matching(packageTitlesRmApi(STUB_VENDOR_ID, STUB_PACKAGE_ID) + ".*"), SC_NOT_FOUND);

    var error = getWithStatus(resourcesPath(FULL_PACKAGE_ID), SC_NOT_FOUND).as(JsonapiError.class);

    assertErrorContainsTitle(error, "Package not found");
  }

  @Test
  void shouldReturn400OnGetWithResourcesWhenCountOutOfRange() {
    var packageResourcesUrl = resourcesPath(FULL_PACKAGE_ID) + "?count=500";

    var error = getWithStatus(packageResourcesUrl, SC_BAD_REQUEST).as(JsonapiError.class);

    assertErrorContainsTitle(error, "is not valid");
  }

  @Test
  void shouldReturn400OnGetWithResourcesWhenRmApi400() {
    mockGet(matching(packageTitlesRmApi(STUB_VENDOR_ID, STUB_PACKAGE_ID) + ".*"),
      readFile(GET_PACKAGE_RESOURCES_400_RESPONSE), SC_BAD_REQUEST);

    var error = getWithStatus(resourcesPath(FULL_PACKAGE_ID), SC_BAD_REQUEST).as(JsonapiError.class);

    assertErrorContainsTitle(error, "Parameter Count is outside the range 1-100.");
  }

  @Test
  void shouldReturnUnauthorizedOnGetWithResourcesWhenRmApi401() {
    mockGet(matching(packageTitlesRmApi(STUB_VENDOR_ID, STUB_PACKAGE_ID) + ".*"), SC_UNAUTHORIZED);

    var error = getWithStatus(resourcesPath(FULL_PACKAGE_ID), SC_FORBIDDEN).as(JsonapiError.class);

    assertErrorContainsTitle(error, "Unauthorized Access");
  }

  @Test
  void shouldReturnUnauthorizedOnGetWithResourcesWhenRmApi403() {
    mockGet(matching(packageTitlesRmApi(STUB_VENDOR_ID, STUB_PACKAGE_ID) + ".*"), SC_FORBIDDEN);

    var error = getWithStatus(resourcesPath(FULL_PACKAGE_ID), SC_FORBIDDEN).as(JsonapiError.class);

    assertErrorContainsTitle(error, "Unauthorized Access");
  }

  @Test
  void shouldFetchPackagesInBulk() {
    mockGet(matching(packageRmApi(STUB_PACKAGE_ID)), readFile(PACKAGE_STUB_FILE));
    mockGet(matching(packageRmApi(STUB_PACKAGE_ID_2)), readFile(PACKAGE_2_STUB_FILE));

    var postBody = readFile(POST_PACKAGES_BULK_REQUEST);
    var actualResponse = postWithOk(packagesBulkPath(), postBody).asString();

    assertJsonEqual(readFile(EXPECTED_POST_PACKAGES_BULK), actualResponse, true);
  }

  @Test
  void shouldReturn422OnFetchPackagesInBulkWithInvalidIdFormat() {
    var postBody = readFile(POST_PACKAGES_BULK_WITH_INVALID_ID_REQUEST);

    var error = postWithStatus(packagesBulkPath(), postBody, SC_UNPROCESSABLE_CONTENT).as(Errors.class);

    assertEquals("elements in list must match pattern", error.getErrors().getFirst().getMessage());
  }

  @Test
  void shouldReturnPackagesAndFailedIdsOnFetchPackagesInBulk() {
    mockGet(matching(packageRmApi(STUB_PACKAGE_ID)), readFile(PACKAGE_STUB_FILE));
    mockGet(matching(packageRmApi(STUB_PACKAGE_ID_2)), readFile(PACKAGE_2_STUB_FILE));

    mockGet(WireMock.equalTo(packageRmApi(9999999)), readFile(GET_PACKAGE_NOT_FOUND_RESPONSE), SC_NOT_FOUND);

    var postBody = readFile(POST_PACKAGES_BULK_WITH_NON_EXISTING_ID_REQUEST);
    var actualResponse = postWithOk(packagesBulkPath(), postBody).asString();

    assertJsonEqual(readFile(EXPECTED_POST_PACKAGES_BULK_FAILED), actualResponse);
  }

  @Test
  void shouldReturnEmptyPackagesOnFetchPackagesInBulkIfNoPackageIds() {
    var postBody = readFile(POST_PACKAGES_BULK_EMPTY_REQUEST);
    var actualResponse = postWithOk(packagesBulkPath(), postBody).asString();

    assertJsonEqual(readFile(EXPECTED_POST_PACKAGES_BULK_EMPTY), actualResponse);
  }

  private String getPackageResponse(String packageName, int packageId, int providerId) {
    PackageData packageData = readJsonFile(PACKAGE_STUB_FILE, PackageData.class);
    return Json.encode(packageData.toBuilder()
      .packageName(packageName)
      .packageId(packageId)
      .vendorId(providerId)
      .build());
  }

  private void setUpPackages(Vertx vertx, String credentialsId) {
    setUpPackage(vertx, credentialsId, STUB_PACKAGE_ID, STUB_VENDOR_ID, STUB_PACKAGE_NAME);
    setUpPackage(vertx, credentialsId, STUB_PACKAGE_ID_2, STUB_VENDOR_ID_2, STUB_PACKAGE_NAME_2);
    setUpPackage(vertx, credentialsId, STUB_PACKAGE_ID_3, STUB_VENDOR_ID_3, STUB_PACKAGE_NAME_3);
  }

  private void setUpPackage(Vertx vertx, String credentialsId, int packageId, int vendorId, String packageName) {
    var dbPackage = buildDbPackage(vendorId + "-" + packageId, credentialsId, packageName);
    PackagesTestUtil.savePackage(dbPackage, vertx);
    mockPackageWithName(packageId, vendorId, packageName);
  }

  private void mockPackageWithName(int stubPackageId, int stubProviderId, String stubPackageName) {
    mockGet(WireMock.equalTo(packageRmApi(stubPackageId)),
      getPackageResponse(stubPackageName, stubPackageId, stubProviderId));
  }

  private static String packagesPath() {
    return "eholdings/packages";
  }

  private static String packagesBulkPath() {
    return "/eholdings/packages/bulk/fetch";
  }

  private static String packagePath(String packageId) {
    return packagesPath() + "/" + packageId;
  }

  private static String tagsPath(String packageId) {
    return packagePath(packageId) + "/tags";
  }

  private static String resourcesPath(String packageId) {
    return packagesPath() + "/" + packageId + "/resources";
  }

  private String prepareCustomPackageData(boolean updatedSelected, boolean updatedHidden, String updatedBeginCoverage,
                                          String updatedEndCoverage, String updatedPackageName) {
    var packageData = readJsonFile(CUSTOM_PACKAGE_STUB_FILE, PackageData.class);
    var visibilities = packageData.getVisibilityDetails().stream()
      .map(visibilityDetail -> visibilityDetail.toBuilder().hidden(updatedHidden).build())
      .toList();
    packageData = packageData.toBuilder()
      .isSelected(updatedSelected)
      .visibilityDetails(visibilities)
      .customCoverage(CoverageDates.builder()
        .beginCoverage(updatedBeginCoverage)
        .endCoverage(updatedEndCoverage)
        .build())
      .packageName(updatedPackageName)
      .contentType("streamingmedia")
      .build();

    return Json.encode(packageData);
  }

  private String preparePackageData(boolean updatedSelected, boolean updatedHidden, String updatedBeginCoverage,
                                    String updatedEndCoverage, boolean updatedAllowEbscoToAddTitles) {
    var packageData = readJsonFile(PACKAGE_STUB_FILE, PackageData.class);
    var visibilities = packageData.getVisibilityDetails().stream()
      .map(visibilityDetail -> visibilityDetail.toBuilder().hidden(updatedHidden).build())
      .toList();
    packageData = packageData.toBuilder()
      .isSelected(updatedSelected)
      .customCoverage(CoverageDates.builder()
        .beginCoverage(updatedBeginCoverage)
        .endCoverage(updatedEndCoverage)
        .build())
      .allowEbscoToAddTitles(updatedAllowEbscoToAddTitles)
      .visibilityDetails(visibilities)
      .build();

    return Json.encode(packageData);
  }

  private void shouldReturnResourcesOnGetWithResources(String getUrl, String rmApiQuery) {
    mockResourceById(RESOURCES_BY_PACKAGE_ID_STUB_FILE);

    var actual = getWithOk(getUrl).asString();
    var expected = readFile(EXPECTED_RESOURCES_STUB_FILE);

    assertJsonEqual(expected, actual);

    wm.verify(1, getRequestedFor(urlEqualTo(packageTitlesRmApi(STUB_VENDOR_ID, STUB_PACKAGE_ID) + rmApiQuery)));
  }

  private void mockResourceById(String stubFile) {
    mockGet(matching(packageTitlesRmApi(STUB_VENDOR_ID, STUB_PACKAGE_ID) + ".*"), readFile(stubFile));
  }

  private void mockUpdateScenario(String initialPackage, String updatedPackage) {
    mockUpdateScenario(initialPackage);

    mockGet(matching(packageRmApi(STUB_PACKAGE_ID)), updatedPackage);
  }

  private void mockUpdateScenario(String initialPackage) {
    mockGet(matching(packageRmApi(STUB_PACKAGE_ID)), initialPackage);
    mockPut(matching(packageRmApi(STUB_PACKAGE_ID)), SC_NO_CONTENT);
  }

  private Package sendPut(String mockUpdatedPackage) {
    mockUpdateScenario(readFile(PACKAGE_STUB_FILE), mockUpdatedPackage);

    var packageToBeUpdated = readJsonFile(PUT_PACKAGE_SELECTED_REQUEST, PackagePutRequest.class);

    return putWithOk(packagePath(FULL_PACKAGE_ID), Json.encode(packageToBeUpdated)).as(Package.class);
  }

  private void sendPutTags(List<String> newTags) {
    var tags = readJsonFile(PUT_PACKAGE_TAGS_REQUEST, PackageTagsPutRequest.class);

    if (newTags != null) {
      tags.getData().getAttributes().setTags(new Tags()
        .withTagList(newTags));
    }

    putWithOk(tagsPath(FULL_PACKAGE_ID), Json.encode(tags)).as(PackageTags.class);
  }

  private ExtractableResponse<Response> sendPost(String requestBody) {
    mockGet(matching(rootProxyCustomLabelsRmApi()), readFile(GET_PACKAGE_PROVIDER_RESPONSE));
    mockPost(WireMock.equalTo(packageRmApiV1(STUB_VENDOR_ID)), readFile(POST_PACKAGE_CREATED_RESPONSE), SC_OK);
    mockGet(WireMock.equalTo(packageRmApi(STUB_PACKAGE_ID)), readFile(PACKAGE_STUB_FILE));

    var request = Json.decodeValue(requestBody, PackagePostRequest.class);
    return postWithOk(packagesPath(), Json.encode(request));
  }
}
