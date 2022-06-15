package org.folio.rest.impl.integrationsuite;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
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
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import static org.folio.repository.RecordType.PACKAGE;
import static org.folio.repository.RecordType.RESOURCE;
import static org.folio.repository.accesstypes.AccessTypeMappingsTableConstants.ACCESS_TYPES_MAPPING_TABLE_NAME;
import static org.folio.repository.accesstypes.AccessTypesTableConstants.ACCESS_TYPES_TABLE_NAME;
import static org.folio.repository.kbcredentials.KbCredentialsTableConstants.KB_CREDENTIALS_TABLE_NAME;
import static org.folio.repository.packages.PackageTableConstants.PACKAGES_TABLE_NAME;
import static org.folio.repository.resources.ResourceTableConstants.RESOURCES_TABLE_NAME;
import static org.folio.repository.tag.TagTableConstants.TAGS_TABLE_NAME;
import static org.folio.rest.impl.PackagesTestData.FULL_PACKAGE_ID;
import static org.folio.rest.impl.PackagesTestData.FULL_PACKAGE_ID_2;
import static org.folio.rest.impl.PackagesTestData.FULL_PACKAGE_ID_3;
import static org.folio.rest.impl.PackagesTestData.STUB_PACKAGE_CONTENT_TYPE;
import static org.folio.rest.impl.PackagesTestData.STUB_PACKAGE_ID;
import static org.folio.rest.impl.PackagesTestData.STUB_PACKAGE_ID_2;
import static org.folio.rest.impl.PackagesTestData.STUB_PACKAGE_NAME;
import static org.folio.rest.impl.PackagesTestData.STUB_PACKAGE_NAME_2;
import static org.folio.rest.impl.ProvidersTestData.STUB_VENDOR_ID;
import static org.folio.rest.impl.ResourcesTestData.STUB_MANAGED_RESOURCE_ID;
import static org.folio.rest.impl.ResourcesTestData.STUB_MANAGED_RESOURCE_ID_2;
import static org.folio.rest.impl.TagsTestData.STUB_TAG_VALUE;
import static org.folio.rest.impl.TagsTestData.STUB_TAG_VALUE_2;
import static org.folio.rest.impl.TagsTestData.STUB_TAG_VALUE_3;
import static org.folio.test.util.TestUtil.getFile;
import static org.folio.test.util.TestUtil.mockGet;
import static org.folio.test.util.TestUtil.mockPost;
import static org.folio.test.util.TestUtil.mockPut;
import static org.folio.test.util.TestUtil.readFile;
import static org.folio.util.AccessTypesTestUtil.STUB_ACCESS_TYPE_NAME;
import static org.folio.util.AccessTypesTestUtil.STUB_ACCESS_TYPE_NAME_2;
import static org.folio.util.AccessTypesTestUtil.getAccessTypeMappings;
import static org.folio.util.AccessTypesTestUtil.insertAccessType;
import static org.folio.util.AccessTypesTestUtil.insertAccessTypeMapping;
import static org.folio.util.AccessTypesTestUtil.insertAccessTypes;
import static org.folio.util.AccessTypesTestUtil.testData;
import static org.folio.util.AssertTestUtil.assertEqualsPackageId;
import static org.folio.util.AssertTestUtil.assertEqualsUUID;
import static org.folio.util.AssertTestUtil.assertErrorContainsTitle;
import static org.folio.util.KBTestUtil.clearDataFromTable;
import static org.folio.util.KBTestUtil.getDefaultKbConfiguration;
import static org.folio.util.KBTestUtil.setupDefaultKBConfiguration;
import static org.folio.util.KbCredentialsTestUtil.STUB_TOKEN_HEADER;
import static org.folio.util.PackagesTestUtil.buildDbPackage;
import static org.folio.util.PackagesTestUtil.savePackage;
import static org.folio.util.PackagesTestUtil.setUpPackages;
import static org.folio.util.ResourcesTestUtil.buildResource;
import static org.folio.util.TagsTestUtil.saveTag;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.matching.AnythingPattern;
import com.github.tomakehurst.wiremock.matching.EqualToJsonPattern;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import com.github.tomakehurst.wiremock.matching.RegexPattern;
import com.github.tomakehurst.wiremock.matching.UrlPathPattern;
import io.restassured.RestAssured;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.apache.http.HttpStatus;
import org.json.JSONException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.JSONAssert;

import org.folio.holdingsiq.model.CoverageDates;
import org.folio.holdingsiq.model.PackageByIdData;
import org.folio.holdingsiq.model.PackageData;
import org.folio.holdingsiq.model.PackagePut;
import org.folio.repository.accesstypes.AccessTypeMapping;
import org.folio.repository.packages.DbPackage;
import org.folio.rest.impl.WireMockTestBase;
import org.folio.rest.jaxrs.model.AccessType;
import org.folio.rest.jaxrs.model.ContentType;
import org.folio.rest.jaxrs.model.Errors;
import org.folio.rest.jaxrs.model.JsonapiError;
import org.folio.rest.jaxrs.model.KbCredentials;
import org.folio.rest.jaxrs.model.MetaTotalResults;
import org.folio.rest.jaxrs.model.Package;
import org.folio.rest.jaxrs.model.PackageCollection;
import org.folio.rest.jaxrs.model.PackageCollectionItem;
import org.folio.rest.jaxrs.model.PackagePostRequest;
import org.folio.rest.jaxrs.model.PackagePutRequest;
import org.folio.rest.jaxrs.model.PackageTags;
import org.folio.rest.jaxrs.model.PackageTagsPutRequest;
import org.folio.rest.jaxrs.model.ResourceCollection;
import org.folio.rest.jaxrs.model.ResourceCollectionItem;
import org.folio.rest.jaxrs.model.Tags;
import org.folio.util.AccessTypesTestUtil;
import org.folio.util.PackagesTestUtil;
import org.folio.util.ResourcesTestUtil;
import org.folio.util.TagsTestUtil;

@RunWith(VertxUnitRunner.class)
public class EholdingsPackagesTest extends WireMockTestBase {

  private static final String PACKAGE_STUB_FILE = "responses/rmapi/packages/get-package-by-id-response.json";
  private static final String PACKAGE_2_STUB_FILE = "responses/rmapi/packages/get-package-by-id-2-response.json";
  private static final String CUSTOM_PACKAGE_STUB_FILE = "responses/rmapi/packages/get-custom-package-by-id-response.json";
  private static final String RESOURCES_BY_PACKAGE_ID_STUB_FILE =
    "responses/rmapi/resources/get-resources-by-package-id-response.json";
  private static final String RESOURCES_BY_PACKAGE_ID_EMPTY_STUB_FILE =
    "responses/rmapi/resources/get-resources-by-package-id-response-empty.json";
  private static final String RESOURCES_BY_PACKAGE_ID_EMPTY_CUSTOMER_RESOURCE_LIST_STUB_FILE =
    "responses/rmapi/resources/get-resources-by-package-id-response-empty-customer-list.json";
  private static final String EXPECTED_PACKAGE_BY_ID_STUB_FILE = "responses/kb-ebsco/packages/expected-package-by-id.json";
  private static final String EXPECTED_RESOURCES_STUB_FILE =
    "responses/kb-ebsco/resources/expected-resources-by-package-id.json";
  private static final String EXPECTED_RESOURCES_WITH_TAGS_STUB_FILE =
    "responses/kb-ebsco/resources/expected-resources-by-package-id-with-tags.json";
  private static final String VENDOR_BY_PACKAGE_ID_STUB_FILE = "responses/rmapi/vendors/get-vendor-by-id-for-package.json";

  private static final String PACKAGES_ENDPOINT = "eholdings/packages";
  private static final String PACKAGES_PATH = PACKAGES_ENDPOINT + "/" + FULL_PACKAGE_ID;
  private static final String PACKAGE_TAGS_PATH = PACKAGES_PATH + "/tags";
  private static final String PACKAGE_RESOURCES_PATH = PACKAGES_PATH + "/resources";
  private static final String PACKAGES_BULK_FETCH_PATH = "/eholdings/packages/bulk/fetch";

  private static final String PACKAGES_STUB_URL =
    "/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors/" + STUB_VENDOR_ID + "/packages";
  private static final String PACKAGE_BY_ID_URL = PACKAGES_STUB_URL + "/" + STUB_PACKAGE_ID;
  private static final String PACKAGE_BY_ID_2_URL = PACKAGES_STUB_URL + "/" + STUB_PACKAGE_ID_2;
  private static final UrlPathPattern PACKAGE_URL_PATTERN = new UrlPathPattern(new EqualToPattern(PACKAGE_BY_ID_URL), false);
  private static final String RESOURCES_BY_PACKAGE_ID_URL = PACKAGE_BY_ID_URL + "/titles";

  private static final String PACKAGE_UPDATED_STATE = "Package updated";
  private static final String GET_PACKAGE_SCENARIO = "Get package";
  private static final String STUB_TITLE_NAME = "Activity Theory Perspectives on Technology in Higher Education";

  private final ObjectMapper mapper = new ObjectMapper();
  private KbCredentials configuration;

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    setupDefaultKBConfiguration(getWiremockUrl(), vertx);
    configuration = getDefaultKbConfiguration(vertx);
  }

  @After
  public void tearDown() {
    clearDataFromTable(vertx, ACCESS_TYPES_MAPPING_TABLE_NAME);
    clearDataFromTable(vertx, ACCESS_TYPES_TABLE_NAME);
    clearDataFromTable(vertx, TAGS_TABLE_NAME);
    clearDataFromTable(vertx, RESOURCES_TABLE_NAME);
    clearDataFromTable(vertx, PACKAGES_TABLE_NAME);
    clearDataFromTable(vertx, KB_CREDENTIALS_TABLE_NAME);
  }

  @Test
  public void shouldReturnPackagesOnGet() throws IOException, URISyntaxException, JSONException {
    String stubResponseFile = "responses/rmapi/packages/get-packages-response.json";

    mockGet(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/packages.*"), stubResponseFile);

    String packages = getWithOk(PACKAGES_ENDPOINT + "?q=American&filter[type]=abstractandindex&count=5",
      STUB_TOKEN_HEADER).asString();
    JSONAssert.assertEquals(readFile("responses/kb-ebsco/packages/expected-package-collection-with-five-elements.json"),
      packages, false);
  }

  @Test
  public void shouldReturnPackagesOnSearchByTagsOnly() throws IOException, URISyntaxException {
    saveTag(vertx, FULL_PACKAGE_ID, PACKAGE, STUB_TAG_VALUE);
    saveTag(vertx, FULL_PACKAGE_ID_2, PACKAGE, STUB_TAG_VALUE);
    saveTag(vertx, FULL_PACKAGE_ID_2, PACKAGE, STUB_TAG_VALUE_2);
    saveTag(vertx, FULL_PACKAGE_ID_3, PACKAGE, STUB_TAG_VALUE_3);

    setUpPackages(vertx, configuration.getId());

    PackageCollection packageCollection = getWithOk(
      PACKAGES_ENDPOINT + "?filter[tags]=" + STUB_TAG_VALUE + "&filter[tags]=" + STUB_TAG_VALUE_2, STUB_TOKEN_HEADER)
      .as(PackageCollection.class);
    List<PackageCollectionItem> packages = packageCollection.getData();

    assertEquals(2, (int) packageCollection.getMeta().getTotalResults());
    assertEquals(2, packages.size());
    assertEquals(STUB_PACKAGE_NAME, packages.get(0).getAttributes().getName());
    assertEquals(STUB_PACKAGE_NAME_2, packages.get(1).getAttributes().getName());
  }

  @Test
  public void shouldReturnEmptyResponseWhenPackagesReturnedWithErrorOnSearchByTags() {
    savePackage(buildDbPackage(FULL_PACKAGE_ID, configuration.getId(), STUB_PACKAGE_NAME), vertx);
    savePackage(buildDbPackage(FULL_PACKAGE_ID_2, configuration.getId(), STUB_PACKAGE_NAME_2), vertx);
    saveTag(vertx, FULL_PACKAGE_ID, PACKAGE, STUB_TAG_VALUE);
    saveTag(vertx, FULL_PACKAGE_ID_2, PACKAGE, STUB_TAG_VALUE);

    mockGet(new RegexPattern(".*vendors/.*/packages/.*"), HttpStatus.SC_INTERNAL_SERVER_ERROR);

    PackageCollection packageCollection = getWithOk(PACKAGES_ENDPOINT + "?filter[tags]=" + STUB_TAG_VALUE,
      STUB_TOKEN_HEADER).as(PackageCollection.class);
    List<PackageCollectionItem> packages = packageCollection.getData();

    assertEquals(2, (int) packageCollection.getMeta().getTotalResults());
    assertEquals(0, packages.size());
  }

  @Test
  public void shouldReturnPackagesOnSearchWithPagination() throws IOException, URISyntaxException {
    saveTag(vertx, FULL_PACKAGE_ID, PACKAGE, STUB_TAG_VALUE);
    saveTag(vertx, FULL_PACKAGE_ID_2, PACKAGE, STUB_TAG_VALUE);
    saveTag(vertx, FULL_PACKAGE_ID_3, PACKAGE, STUB_TAG_VALUE);

    setUpPackages(vertx, configuration.getId());

    PackageCollection packageCollection = getWithOk(
      PACKAGES_ENDPOINT + "?page=2&count=1&filter[tags]=" + STUB_TAG_VALUE, STUB_TOKEN_HEADER)
      .as(PackageCollection.class);

    List<PackageCollectionItem> packages = packageCollection.getData();

    assertEquals(3, (int) packageCollection.getMeta().getTotalResults());
    assertEquals(1, packages.size());
    assertEquals(STUB_PACKAGE_NAME_2, packages.get(0).getAttributes().getName());
  }

  @Test
  public void shouldReturnPackagesOnSearchByAccessTypeWithPagination() throws IOException, URISyntaxException {
    List<AccessType> accessTypes = insertAccessTypes(testData(configuration.getId()), vertx);
    insertAccessTypeMapping(FULL_PACKAGE_ID, PACKAGE, accessTypes.get(0).getId(), vertx);
    insertAccessTypeMapping(FULL_PACKAGE_ID_2, PACKAGE, accessTypes.get(1).getId(), vertx);

    setUpPackages(vertx, configuration.getId());

    String resourcePath = PACKAGES_ENDPOINT + "?page=2&count=1&filter[access-type]="
      + STUB_ACCESS_TYPE_NAME + "&filter[access-type]=" + STUB_ACCESS_TYPE_NAME_2;
    PackageCollection packageCollection = getWithOk(resourcePath, STUB_TOKEN_HEADER).as(PackageCollection.class);

    List<PackageCollectionItem> packages = packageCollection.getData();

    assertEquals(2, (int) packageCollection.getMeta().getTotalResults());
    assertEquals(1, packages.size());
    assertEquals(STUB_PACKAGE_NAME, packages.get(0).getAttributes().getName());
  }

  @Test
  public void shouldReturnEmptyResponseWhenPackagesReturnedWithErrorOnSearchByAccessType() {
    List<AccessType> accessTypes = insertAccessTypes(testData(configuration.getId()), vertx);
    insertAccessTypeMapping(FULL_PACKAGE_ID, PACKAGE, accessTypes.get(0).getId(), vertx);
    insertAccessTypeMapping(FULL_PACKAGE_ID_2, PACKAGE, accessTypes.get(0).getId(), vertx);

    mockGet(new RegexPattern(".*vendors/.*/packages/.*"), HttpStatus.SC_INTERNAL_SERVER_ERROR);

    String resourcePath = PACKAGES_ENDPOINT + "?filter[access-type]=" + STUB_ACCESS_TYPE_NAME;
    PackageCollection packageCollection = getWithOk(resourcePath, STUB_TOKEN_HEADER).as(PackageCollection.class);
    List<PackageCollectionItem> packages = packageCollection.getData();

    assertEquals(2, (int) packageCollection.getMeta().getTotalResults());
    assertEquals(0, packages.size());
  }

  @Test
  public void shouldReturn400OnInvalidFilterCustomParameter() {
    String invalidParameterForFilterCustom = getWithStatus(
      PACKAGES_ENDPOINT + "?filter[custom]=test", 400, STUB_TOKEN_HEADER).asString();

    assertTrue(invalidParameterForFilterCustom.contains("Invalid Query Parameter for filter[custom]: only 'true' is supported"));
  }

  @Test
  public void shouldReturn400OnNotSupportedFilterCustomParameter() {
    String falseParameterForFilterCustom = getWithStatus(
      PACKAGES_ENDPOINT + "?filter[custom]=false", 400, STUB_TOKEN_HEADER).asString();

    assertTrue(falseParameterForFilterCustom.contains("Invalid Query Parameter for filter[custom]: only 'true' is supported"));
  }

  @Test
  public void shouldReturnPackagesOnGetWithPackageId() throws IOException, URISyntaxException, JSONException {
    String packagesStubResponseFile = "responses/rmapi/packages/get-packages-by-provider-id.json";
    String providerIdByCustIdStubResponseFile = "responses/rmapi/proxiescustomlabels/get-success-response.json";

    mockGet(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/"), providerIdByCustIdStubResponseFile);
    mockGet(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors/" + STUB_VENDOR_ID + "/packages.*"),
      packagesStubResponseFile);

    String packages = getWithOk(PACKAGES_ENDPOINT + "?q=a&count=5&page=1&filter[custom]=true", STUB_TOKEN_HEADER)
      .asString();

    JSONAssert.assertEquals(readFile("responses/kb-ebsco/packages/expected-package-collection-with-one-element.json"),
      packages, false);
  }

  @Test
  public void shouldReturnPackagesOnGetById() throws IOException, URISyntaxException, JSONException {
    mockGet(new RegexPattern(PACKAGE_BY_ID_URL), CUSTOM_PACKAGE_STUB_FILE);

    String packageData = getWithOk(PACKAGES_PATH, STUB_TOKEN_HEADER).asString();

    JSONAssert.assertEquals(readFile(EXPECTED_PACKAGE_BY_ID_STUB_FILE), packageData, false);
  }

  @Test
  public void shouldReturnPackageWithTagOnGetById() throws IOException, URISyntaxException {
    String packageId = FULL_PACKAGE_ID;
    saveTag(vertx, packageId, PACKAGE, STUB_TAG_VALUE);
    mockGet(new RegexPattern(PACKAGE_BY_ID_URL), CUSTOM_PACKAGE_STUB_FILE);

    Package packageData = getWithOk(PACKAGES_ENDPOINT + "/" + packageId, STUB_TOKEN_HEADER).as(Package.class);

    assertTrue(packageData.getData().getAttributes().getTags().getTagList().contains(STUB_TAG_VALUE));
  }

  @Test
  public void shouldReturnPackageWithAccessTypeOnGetById() throws IOException, URISyntaxException {
    List<AccessType> accessTypes = insertAccessTypes(testData(configuration.getId()), vertx);
    String expectedAccessTypeId = accessTypes.get(0).getId();
    insertAccessTypeMapping(FULL_PACKAGE_ID, PACKAGE, expectedAccessTypeId, vertx);

    mockGet(new RegexPattern(PACKAGE_BY_ID_URL), CUSTOM_PACKAGE_STUB_FILE);
    Package packageData = getWithOk(PACKAGES_ENDPOINT + "/" + FULL_PACKAGE_ID, STUB_TOKEN_HEADER).as(Package.class);

    assertNotNull(packageData.getIncluded());
    assertEquals(expectedAccessTypeId, packageData.getData().getRelationships().getAccessType().getData().getId());
    assertEquals(expectedAccessTypeId, ((LinkedHashMap<?, ?>) packageData.getIncluded().get(0)).get("id"));
  }

  @Test
  public void shouldAddPackageTagsOnPutTagsWhenPackageAlreadyHasTags() throws IOException, URISyntaxException {
    saveTag(vertx, FULL_PACKAGE_ID, PACKAGE, STUB_TAG_VALUE);
    List<String> newTags = Arrays.asList(STUB_TAG_VALUE, STUB_TAG_VALUE_2);
    sendPutTags(Arrays.asList(STUB_TAG_VALUE, STUB_TAG_VALUE_2));
    List<String> tagsAfterRequest = TagsTestUtil.getTagsForRecordType(vertx, PACKAGE);
    assertThat(tagsAfterRequest, containsInAnyOrder(newTags.toArray()));
  }

  @Test
  public void shouldAddPackageDataOnPutTags() throws IOException, URISyntaxException {
    List<String> tags = Collections.singletonList(STUB_TAG_VALUE);
    sendPutTags(tags);
    List<DbPackage> packages = PackagesTestUtil.getPackages(vertx);
    assertEquals(1, packages.size());
    assertEqualsPackageId(packages.get(0).getId());
    assertEquals(STUB_PACKAGE_NAME, packages.get(0).getName());
    assertThat(packages.get(0).getContentType(), equalToIgnoringCase(STUB_PACKAGE_CONTENT_TYPE));
  }

  @Test
  public void shouldUpdateTagsOnlyOnPutPackageTagsEndpoint() throws IOException, URISyntaxException {
    List<String> tags = Collections.singletonList(STUB_TAG_VALUE);
    sendPutTags(tags);
    final Package updatedPackage = sendPut(readFile(PACKAGE_STUB_FILE));

    List<String> packageTags = TagsTestUtil.getTagsForRecordType(vertx, PACKAGE);
    assertThat(packageTags, is(tags));
    assertTrue(Objects.isNull(updatedPackage.getData().getAttributes().getTags()));
  }

  @Test
  public void shouldDeleteAllPackageTagsOnPutTagsWhenRequestHasEmptyListOfTags() throws IOException, URISyntaxException {
    saveTag(vertx, FULL_PACKAGE_ID, PACKAGE, "test one");
    sendPutTags(Collections.emptyList());
    List<String> tagsAfterRequest = TagsTestUtil.getTagsForRecordType(vertx, PACKAGE);
    assertThat(tagsAfterRequest, empty());
  }

  @Test
  public void shouldDoNothingOnPutWhenRequestHasNotTags() throws IOException, URISyntaxException {
    sendPutTags(null);
    sendPutTags(null);
    List<String> tagsAfterRequest = TagsTestUtil.getTagsForRecordType(vertx, PACKAGE);
    assertThat(tagsAfterRequest, empty());
  }

  @Test
  public void shouldReturn422OnPutTagsWhenRequestBodyIsInvalid() throws IOException, URISyntaxException {
    PackageTagsPutRequest tags =
      mapper.readValue(getFile("requests/kb-ebsco/package/put-package-tags.json"), PackageTagsPutRequest.class);
    tags.getData().getAttributes().setName("");
    JsonapiError error =
      putWithStatus(PACKAGE_TAGS_PATH, mapper.writeValueAsString(tags), SC_UNPROCESSABLE_ENTITY, STUB_TOKEN_HEADER)
        .as(JsonapiError.class);

    assertErrorContainsTitle(error, "Invalid name");
  }

  @Test
  public void shouldDeletePackageTagsOnDelete() throws IOException, URISyntaxException {
    saveTag(vertx, FULL_PACKAGE_ID, PACKAGE, "test one");

    mockGet(new EqualToPattern(PACKAGE_BY_ID_URL), CUSTOM_PACKAGE_STUB_FILE);

    EqualToJsonPattern putBodyPattern = new EqualToJsonPattern("{\"isSelected\":false}", true, true);
    mockPut(new EqualToPattern(PACKAGE_BY_ID_URL), putBodyPattern, SC_NO_CONTENT);

    deleteWithNoContent(PACKAGES_PATH, STUB_TOKEN_HEADER);

    List<String> tagsAfterRequest = TagsTestUtil.getTagsForRecordType(vertx, PACKAGE);
    assertThat(tagsAfterRequest, empty());
  }

  @Test
  public void shouldDeletePackageAccessTypeMappingOnDelete() throws IOException, URISyntaxException {
    String accessTypeId = insertAccessTypes(testData(configuration.getId()), vertx).get(0).getId();
    insertAccessTypeMapping(FULL_PACKAGE_ID, PACKAGE, accessTypeId, vertx);

    mockGet(new EqualToPattern(PACKAGE_BY_ID_URL), CUSTOM_PACKAGE_STUB_FILE);

    EqualToJsonPattern putBodyPattern = new EqualToJsonPattern("{\"isSelected\":false}", true, true);
    mockPut(new EqualToPattern(PACKAGE_BY_ID_URL), putBodyPattern, SC_NO_CONTENT);

    deleteWithNoContent(PACKAGES_PATH, STUB_TOKEN_HEADER);

    List<AccessTypeMapping> mappingsAfterRequest = AccessTypesTestUtil.getAccessTypeMappings(vertx);
    assertThat(mappingsAfterRequest, empty());
  }

  @Test
  public void shouldDeletePackageOnDeleteRequest() throws IOException, URISyntaxException {
    sendPost(readFile("requests/kb-ebsco/package/post-package-request.json"));
    sendPutTags(Collections.singletonList(STUB_TAG_VALUE));

    mockGet(new EqualToPattern(PACKAGE_BY_ID_URL), CUSTOM_PACKAGE_STUB_FILE);
    mockPut(new EqualToPattern(PACKAGE_BY_ID_URL), new AnythingPattern(), SC_NO_CONTENT);

    deleteWithNoContent(PACKAGES_PATH, STUB_TOKEN_HEADER);

    List<DbPackage> packages = PackagesTestUtil.getPackages(vertx);
    assertThat(packages, is(empty()));
  }

  @Test
  public void shouldReturn404WhenPackageIsNotFoundOnRMAPI() {
    mockGet(new RegexPattern(PACKAGE_BY_ID_URL), SC_NOT_FOUND);

    JsonapiError error = getWithStatus(PACKAGES_PATH, SC_NOT_FOUND, STUB_TOKEN_HEADER).as(JsonapiError.class);

    assertErrorContainsTitle(error, "not found");
  }

  @Test
  public void shouldReturnResourcesWhenIncludedFlagIsSetToResources() throws IOException, URISyntaxException, JSONException {
    mockGet(new RegexPattern(PACKAGE_BY_ID_URL), CUSTOM_PACKAGE_STUB_FILE);
    mockResourceById(RESOURCES_BY_PACKAGE_ID_STUB_FILE);

    Package packageData = getWithOk(PACKAGES_PATH + "?include=resources", STUB_TOKEN_HEADER)
      .as(Package.class);

    Package expectedPackage = mapper.readValue(readFile(EXPECTED_PACKAGE_BY_ID_STUB_FILE), Package.class);
    ResourceCollection expectedResources =
      mapper.readValue(readFile(EXPECTED_RESOURCES_STUB_FILE), ResourceCollection.class);
    expectedPackage.getIncluded().addAll(expectedResources.getData());

    JSONAssert.assertEquals(mapper.writeValueAsString(expectedPackage), mapper.writeValueAsString(packageData), false);
  }

  @Test
  public void shouldReturnProviderWhenIncludedFlagIsSetToProvider() throws IOException, URISyntaxException, JSONException {
    mockGet(new RegexPattern(PACKAGE_BY_ID_URL), CUSTOM_PACKAGE_STUB_FILE);
    mockGet(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors/" + STUB_VENDOR_ID),
      VENDOR_BY_PACKAGE_ID_STUB_FILE);

    String actual = getWithOk(PACKAGES_PATH + "?include=provider", STUB_TOKEN_HEADER).asString();

    String expected = readFile("responses/kb-ebsco/packages/expected-package-by-id-with-provider.json");
    JSONAssert.assertEquals(expected, actual, false);
  }

  @Test
  public void shouldSendDeleteRequestForPackage() throws IOException, URISyntaxException {
    EqualToJsonPattern putBodyPattern = new EqualToJsonPattern("{\"isSelected\":false}", true, true);

    mockGet(new EqualToPattern(PACKAGE_BY_ID_URL), CUSTOM_PACKAGE_STUB_FILE);

    mockPut(new EqualToPattern(PACKAGE_BY_ID_URL), putBodyPattern, SC_NO_CONTENT);

    deleteWithNoContent(PACKAGES_PATH, STUB_TOKEN_HEADER);

    verify(1, putRequestedFor(PACKAGE_URL_PATTERN)
      .withRequestBody(putBodyPattern));
  }

  @Test
  public void shouldReturn400WhenPackageIdIsInvalid() {
    String error = deleteWithStatus(PACKAGES_ENDPOINT + "/abc-def", SC_BAD_REQUEST, STUB_TOKEN_HEADER)
      .asString();

    assertThat(error, containsString("Package or provider id are invalid"));
  }

  @Test
  public void shouldReturn400WhenPackageIsNotCustom() throws URISyntaxException, IOException {
    PackageData packageData = mapper.readValue(getFile(CUSTOM_PACKAGE_STUB_FILE), PackageData.class)
      .toBuilder().isCustom(false).build();

    stubFor(
      get(PACKAGE_URL_PATTERN)
        .willReturn(new ResponseDefinitionBuilder()
          .withBody(mapper.writeValueAsString(packageData))));

    JsonapiError error = deleteWithStatus(PACKAGES_PATH, SC_BAD_REQUEST, STUB_TOKEN_HEADER).as(JsonapiError.class);

    assertErrorContainsTitle(error, "Package cannot be deleted");
  }

  @Test
  public void shouldReturn200WhenSelectingPackage() throws URISyntaxException, IOException {
    boolean updatedIsSelected = true;

    EqualToJsonPattern putBodyPattern =
      new EqualToJsonPattern(readFile("requests/rmapi/packages/put-package-is-selected.json"), true, true);

    PackageByIdData packageData = mapper.readValue(getFile(PACKAGE_STUB_FILE), PackageByIdData.class);
    packageData = packageData.toByIdBuilder().isSelected(updatedIsSelected).build();
    String updatedPackageValue = mapper.writeValueAsString(packageData);
    mockUpdateScenario(readFile(PACKAGE_STUB_FILE), updatedPackageValue);

    Package aPackage = putWithOk(PACKAGES_PATH, readFile("requests/kb-ebsco/package/put-package-selected.json"),
      STUB_TOKEN_HEADER).as(Package.class);

    assertEquals(updatedIsSelected, aPackage.getData().getAttributes().getIsSelected());

    verify(putRequestedFor(PACKAGE_URL_PATTERN)
      .withRequestBody(putBodyPattern));
  }

  @Test
  public void shouldUpdateAllAttributesInSelectedPackage() throws URISyntaxException, IOException {
    boolean updatedSelected = true;
    boolean updatedAllowEbscoToAddTitles = true;
    boolean updatedHidden = true;
    String updatedBeginCoverage = "2003-01-01";
    String updatedEndCoverage = "2004-01-01";

    EqualToJsonPattern putBodyPattern =
      new EqualToJsonPattern(readFile("requests/rmapi/packages/put-package-is-selected.json"), true, true);

    PackageByIdData packageData = mapper.readValue(getFile(PACKAGE_STUB_FILE), PackageByIdData.class);

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
    mockUpdateScenario(readFile(PACKAGE_STUB_FILE), updatedPackageValue);

    Package aPackage = putWithOk(
      PACKAGES_PATH,
      readFile("requests/kb-ebsco/package/put-package-selected.json"), STUB_TOKEN_HEADER).as(Package.class);

    verify(putRequestedFor(PACKAGE_URL_PATTERN)
      .withRequestBody(putBodyPattern));

    assertEquals(updatedSelected, aPackage.getData().getAttributes().getIsSelected());
    assertEquals(updatedAllowEbscoToAddTitles, aPackage.getData().getAttributes().getAllowKbToAddTitles());
    assertEquals(updatedHidden, aPackage.getData().getAttributes().getVisibilityData().getIsHidden());
    assertEquals(updatedBeginCoverage, aPackage.getData().getAttributes().getCustomCoverage().getBeginCoverage());
    assertEquals(updatedEndCoverage, aPackage.getData().getAttributes().getCustomCoverage().getEndCoverage());
  }

  @Test
  public void shouldUpdateAllAttributesInSelectedPackageAndCreateNewAccessTypeMapping()
    throws URISyntaxException, IOException {
    List<AccessType> accessTypes = insertAccessTypes(testData(configuration.getId()), vertx);
    String accessTypeId = accessTypes.get(0).getId();

    boolean updatedSelected = true;
    boolean updatedAllowEbscoToAddTitles = true;
    boolean updatedHidden = true;
    String updatedBeginCoverage = "2003-01-01";
    String updatedEndCoverage = "2004-01-01";

    EqualToJsonPattern putBodyPattern =
      new EqualToJsonPattern(readFile("requests/rmapi/packages/put-package-is-selected.json"), true, true);

    PackageByIdData packageData = mapper.readValue(getFile(PACKAGE_STUB_FILE), PackageByIdData.class);

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
    mockUpdateScenario(readFile(PACKAGE_STUB_FILE), updatedPackageValue);

    String putBody = String.format(readFile("requests/kb-ebsco/package/put-package-selected-with-access-type.json"),
      accessTypeId);
    Package aPackage = putWithOk(PACKAGES_PATH, putBody, STUB_TOKEN_HEADER).as(Package.class);

    verify(putRequestedFor(PACKAGE_URL_PATTERN)
      .withRequestBody(putBodyPattern));

    assertEquals(updatedSelected, aPackage.getData().getAttributes().getIsSelected());
    assertEquals(updatedAllowEbscoToAddTitles, aPackage.getData().getAttributes().getAllowKbToAddTitles());
    assertEquals(updatedHidden, aPackage.getData().getAttributes().getVisibilityData().getIsHidden());
    assertEquals(updatedBeginCoverage, aPackage.getData().getAttributes().getCustomCoverage().getBeginCoverage());
    assertEquals(updatedEndCoverage, aPackage.getData().getAttributes().getCustomCoverage().getEndCoverage());

    List<AccessTypeMapping> accessTypeMappingsInDB = getAccessTypeMappings(vertx);
    assertEquals(1, accessTypeMappingsInDB.size());
    assertEquals(aPackage.getData().getId(), accessTypeMappingsInDB.get(0).getRecordId());
    assertEqualsUUID(accessTypeId, accessTypeMappingsInDB.get(0).getAccessTypeId());
    assertEquals(PACKAGE, accessTypeMappingsInDB.get(0).getRecordType());
    assertNotNull(aPackage.getIncluded());
    assertEquals(accessTypeId, aPackage.getData().getRelationships().getAccessType().getData().getId());
    assertEquals(accessTypeId, ((LinkedHashMap<?, ?>) aPackage.getIncluded().get(0)).get("id"));
  }

  @Test
  public void shouldDeleteAccessTypeMappingWhenRMAPIsend404() throws URISyntaxException, IOException {
    List<AccessType> accessTypes = insertAccessTypes(testData(configuration.getId()), vertx);
    String currentAccessTypeId = accessTypes.get(0).getId();
    String newAccessTypeId = accessTypes.get(1).getId();
    insertAccessTypeMapping(FULL_PACKAGE_ID, PACKAGE, currentAccessTypeId, vertx);

    PackageByIdData packageData = mapper
      .readValue(getFile(CUSTOM_PACKAGE_STUB_FILE), PackageByIdData.class)
      .toByIdBuilder()
      .contentType("streamingmedia")
      .build();

    String updatedPackageValue = mapper.writeValueAsString(packageData);
    mockUpdateScenario(readFile(CUSTOM_PACKAGE_STUB_FILE), updatedPackageValue);

    String getByIdBody = readFile(CUSTOM_PACKAGE_STUB_FILE);
    String putBody = String.format(readFile("requests/kb-ebsco/package/put-package-custom-with-access-type.json"),
      newAccessTypeId);
    stubFor(get(PACKAGE_URL_PATTERN).inScenario("Put package")
      .whenScenarioStateIs(STARTED)
      .willReturn(new ResponseDefinitionBuilder().withBody(getByIdBody))
      .willSetStateTo("Not found"));
    stubFor(get(PACKAGE_URL_PATTERN).inScenario("Put package")
      .whenScenarioStateIs("Not found")
      .willReturn(new ResponseDefinitionBuilder().withStatus(SC_NOT_FOUND)));

    putWithStatus(PACKAGES_PATH, putBody, SC_NOT_FOUND, STUB_TOKEN_HEADER);

    List<AccessTypeMapping> accessTypeMappingsInDB = getAccessTypeMappings(vertx);
    assertEquals(0, accessTypeMappingsInDB.size());
  }

  @Test
  public void shouldReturn422OnPutWhenUnselectNonCustomPackageIsHidden() throws URISyntaxException, IOException {
    String putBody = readFile("requests/kb-ebsco/package/put-package-not-selected-non-empty-fields.json");
    mockGet(new RegexPattern(PACKAGE_BY_ID_URL), PACKAGE_STUB_FILE);
    JsonapiError error = putWithStatus(PACKAGES_PATH, putBody, SC_UNPROCESSABLE_ENTITY, CONTENT_TYPE_HEADER,
      STUB_TOKEN_HEADER).as(JsonapiError.class);

    verify(0, putRequestedFor(PACKAGE_URL_PATTERN));

    assertErrorContainsTitle(error, "Invalid visibilityData.isHidden");
  }

  @Test
  public void shouldReturn422OnPutWhenCustomPackageUpdateLikeNotCustom() throws URISyntaxException, IOException {
    String putBody = readFile("requests/kb-ebsco/package/put-package-selected.json");
    mockGet(new RegexPattern(PACKAGE_BY_ID_URL), CUSTOM_PACKAGE_STUB_FILE);
    JsonapiError error = putWithStatus(PACKAGES_PATH, putBody, SC_UNPROCESSABLE_ENTITY, CONTENT_TYPE_HEADER,
      STUB_TOKEN_HEADER).as(JsonapiError.class);

    verify(0, putRequestedFor(PACKAGE_URL_PATTERN));

    assertErrorContainsTitle(error, "Package isCustom not matched");
  }

  @Test
  public void shouldPassIsFullPackageAttributeToRMAPI() throws URISyntaxException, IOException {
    PackageByIdData updatedPackage = mapper.readValue(getFile(PACKAGE_STUB_FILE), PackageByIdData.class)
      .toByIdBuilder().isSelected(true).build();

    mockUpdateScenario(readFile(PACKAGE_STUB_FILE), mapper.writeValueAsString(updatedPackage));

    PackagePutRequest request =
      mapper.readValue(readFile("requests/kb-ebsco/package/put-package-selected.json"), PackagePutRequest.class);
    request.getData().getAttributes().setIsFullPackage(false);
    putWithOk(PACKAGES_PATH, mapper.writeValueAsString(request), STUB_TOKEN_HEADER).as(Package.class);

    PackagePut rmApiPutRequest =
      mapper.readValue(readFile("requests/rmapi/packages/put-package-is-selected.json"), PackagePut.class)
        .toBuilder().isFullPackage(false).build();

    verify(putRequestedFor(PACKAGE_URL_PATTERN)
      .withRequestBody(new EqualToJsonPattern(mapper.writeValueAsString(rmApiPutRequest), true, true)));
  }

  @Test
  public void shouldUpdateAllAttributesInCustomPackage() throws URISyntaxException, IOException {
    boolean updatedSelected = true;
    boolean updatedHidden = true;
    String updatedBeginCoverage = "2003-01-01";
    String updatedEndCoverage = "2004-01-01";
    String updatedPackageName = "name of the ages forever and ever";

    EqualToJsonPattern putBodyPattern =
      new EqualToJsonPattern(readFile("requests/rmapi/packages/put-package-custom.json"), true, true);

    PackageByIdData packageData = mapper.readValue(getFile(CUSTOM_PACKAGE_STUB_FILE), PackageByIdData.class);

    packageData = packageData.toByIdBuilder()
      .isSelected(updatedSelected)
      .visibilityData(packageData.getVisibilityData().toBuilder().isHidden(updatedHidden).build())
      .customCoverage(CoverageDates.builder()
        .beginCoverage(updatedBeginCoverage)
        .endCoverage(updatedEndCoverage)
        .build())
      .packageName(updatedPackageName)
      .contentType("streamingmedia").build();

    String updatedPackageValue = mapper.writeValueAsString(packageData);
    mockUpdateScenario(readFile(CUSTOM_PACKAGE_STUB_FILE), updatedPackageValue);

    Package aPackage = putWithOk(
      PACKAGES_PATH,
      readFile("requests/kb-ebsco/package/put-package-custom-multiple-attributes.json"),
      STUB_TOKEN_HEADER).as(Package.class);

    verify(putRequestedFor(PACKAGE_URL_PATTERN)
      .withRequestBody(putBodyPattern));

    assertEquals(updatedSelected, aPackage.getData().getAttributes().getIsSelected());
    assertEquals(updatedHidden, aPackage.getData().getAttributes().getVisibilityData().getIsHidden());
    assertEquals(updatedBeginCoverage, aPackage.getData().getAttributes().getCustomCoverage().getBeginCoverage());
    assertEquals(updatedEndCoverage, aPackage.getData().getAttributes().getCustomCoverage().getEndCoverage());
    assertEquals(updatedPackageName, aPackage.getData().getAttributes().getName());
    assertEquals(ContentType.STREAMING_MEDIA, aPackage.getData().getAttributes().getContentType());
  }

  @Test
  public void shouldUpdateAllAttributesInCustomPackageAndCreateNewAccessTypeMapping()
    throws URISyntaxException, IOException {
    List<AccessType> accessTypes = insertAccessTypes(testData(configuration.getId()), vertx);
    String accessTypeId = accessTypes.get(0).getId();

    boolean updatedSelected = true;
    boolean updatedHidden = true;
    String updatedBeginCoverage = "2003-01-01";
    String updatedEndCoverage = "2004-01-01";
    String updatedPackageName = "name of the ages forever and ever";

    EqualToJsonPattern putBodyPattern =
      new EqualToJsonPattern(readFile("requests/rmapi/packages/put-package-custom.json"), true, true);

    PackageByIdData packageData = mapper.readValue(getFile(CUSTOM_PACKAGE_STUB_FILE), PackageByIdData.class);

    packageData = packageData.toByIdBuilder()
      .isSelected(updatedSelected)
      .visibilityData(packageData.getVisibilityData().toBuilder().isHidden(updatedHidden).build())
      .customCoverage(CoverageDates.builder()
        .beginCoverage(updatedBeginCoverage)
        .endCoverage(updatedEndCoverage)
        .build())
      .packageName(updatedPackageName)
      .contentType("streamingmedia").build();

    String updatedPackageValue = mapper.writeValueAsString(packageData);
    mockUpdateScenario(readFile(CUSTOM_PACKAGE_STUB_FILE), updatedPackageValue);

    String putBody = String.format(readFile("requests/kb-ebsco/package/put-package-custom-with-access-type.json"),
      accessTypeId);
    Package aPackage = putWithOk(PACKAGES_PATH, putBody, STUB_TOKEN_HEADER).as(Package.class);

    verify(putRequestedFor(PACKAGE_URL_PATTERN)
      .withRequestBody(putBodyPattern));

    assertEquals(updatedSelected, aPackage.getData().getAttributes().getIsSelected());
    assertEquals(updatedHidden, aPackage.getData().getAttributes().getVisibilityData().getIsHidden());
    assertEquals(updatedBeginCoverage, aPackage.getData().getAttributes().getCustomCoverage().getBeginCoverage());
    assertEquals(updatedEndCoverage, aPackage.getData().getAttributes().getCustomCoverage().getEndCoverage());
    assertEquals(updatedPackageName, aPackage.getData().getAttributes().getName());
    assertEquals(ContentType.STREAMING_MEDIA, aPackage.getData().getAttributes().getContentType());

    List<AccessTypeMapping> accessTypeMappingsInDB = getAccessTypeMappings(vertx);
    assertEquals(1, accessTypeMappingsInDB.size());
    assertEquals(aPackage.getData().getId(), accessTypeMappingsInDB.get(0).getRecordId());
    assertEqualsUUID(accessTypeId, accessTypeMappingsInDB.get(0).getAccessTypeId());
    assertEquals(PACKAGE, accessTypeMappingsInDB.get(0).getRecordType());
    assertNotNull(aPackage.getIncluded());
    assertEquals(accessTypeId, aPackage.getData().getRelationships().getAccessType().getData().getId());
    assertEquals(accessTypeId, ((LinkedHashMap<?, ?>) aPackage.getIncluded().get(0)).get("id"));
  }

  @Test
  public void shouldUpdateAllAttributesInCustomPackageAndDeleteAccessTypeMapping() throws URISyntaxException, IOException {
    List<AccessType> accessTypes = insertAccessTypes(testData(configuration.getId()), vertx);
    String accessTypeId = accessTypes.get(0).getId();

    insertAccessTypeMapping(FULL_PACKAGE_ID, PACKAGE, accessTypeId, vertx);

    boolean updatedSelected = true;
    boolean updatedHidden = true;
    String updatedBeginCoverage = "2003-01-01";
    String updatedEndCoverage = "2004-01-01";
    String updatedPackageName = "name of the ages forever and ever";

    EqualToJsonPattern putBodyPattern =
      new EqualToJsonPattern(readFile("requests/rmapi/packages/put-package-custom.json"), true, true);

    PackageByIdData packageData = mapper.readValue(getFile(CUSTOM_PACKAGE_STUB_FILE), PackageByIdData.class);

    packageData = packageData.toByIdBuilder()
      .isSelected(updatedSelected)
      .visibilityData(packageData.getVisibilityData().toBuilder().isHidden(updatedHidden).build())
      .customCoverage(CoverageDates.builder()
        .beginCoverage(updatedBeginCoverage)
        .endCoverage(updatedEndCoverage)
        .build())
      .packageName(updatedPackageName)
      .contentType("streamingmedia").build();

    String updatedPackageValue = mapper.writeValueAsString(packageData);
    mockUpdateScenario(readFile(CUSTOM_PACKAGE_STUB_FILE), updatedPackageValue);

    String putBody = readFile("requests/kb-ebsco/package/put-package-custom-multiple-attributes.json");
    Package aPackage = putWithOk(PACKAGES_PATH, putBody, STUB_TOKEN_HEADER).as(Package.class);

    verify(putRequestedFor(PACKAGE_URL_PATTERN)
      .withRequestBody(putBodyPattern));

    assertEquals(updatedSelected, aPackage.getData().getAttributes().getIsSelected());
    assertEquals(updatedHidden, aPackage.getData().getAttributes().getVisibilityData().getIsHidden());
    assertEquals(updatedBeginCoverage, aPackage.getData().getAttributes().getCustomCoverage().getBeginCoverage());
    assertEquals(updatedEndCoverage, aPackage.getData().getAttributes().getCustomCoverage().getEndCoverage());
    assertEquals(updatedPackageName, aPackage.getData().getAttributes().getName());
    assertEquals(ContentType.STREAMING_MEDIA, aPackage.getData().getAttributes().getContentType());

    List<AccessTypeMapping> accessTypeMappingsInDB = getAccessTypeMappings(vertx);
    assertEquals(0, accessTypeMappingsInDB.size());

    assertNotNull(aPackage.getIncluded());
    assertEquals(0, aPackage.getIncluded().size());
    assertNull(aPackage.getData().getRelationships().getAccessType());
  }

  @Test
  public void shouldUpdateAllAttributesInCustomPackageAndUpdateAccessTypeMapping() throws URISyntaxException, IOException {
    List<AccessType> accessTypes = insertAccessTypes(testData(configuration.getId()), vertx);
    String currentAccessTypeId = accessTypes.get(0).getId();
    String newAccessTypeId = accessTypes.get(1).getId();
    insertAccessTypeMapping(FULL_PACKAGE_ID, PACKAGE, currentAccessTypeId, vertx);

    boolean updatedSelected = true;
    boolean updatedHidden = true;
    String updatedBeginCoverage = "2003-01-01";
    String updatedEndCoverage = "2004-01-01";
    String updatedPackageName = "name of the ages forever and ever";

    EqualToJsonPattern putBodyPattern =
      new EqualToJsonPattern(readFile("requests/rmapi/packages/put-package-custom.json"), true, true);

    PackageByIdData packageData = mapper.readValue(getFile(CUSTOM_PACKAGE_STUB_FILE), PackageByIdData.class);

    packageData = packageData.toByIdBuilder()
      .isSelected(updatedSelected)
      .visibilityData(packageData.getVisibilityData().toBuilder().isHidden(updatedHidden).build())
      .customCoverage(CoverageDates.builder()
        .beginCoverage(updatedBeginCoverage)
        .endCoverage(updatedEndCoverage)
        .build())
      .packageName(updatedPackageName)
      .contentType("streamingmedia").build();

    String updatedPackageValue = mapper.writeValueAsString(packageData);
    mockUpdateScenario(readFile(CUSTOM_PACKAGE_STUB_FILE), updatedPackageValue);

    String putBody = String.format(readFile("requests/kb-ebsco/package/put-package-custom-with-access-type.json"),
      newAccessTypeId);
    Package aPackage = putWithOk(PACKAGES_PATH, putBody, STUB_TOKEN_HEADER).as(Package.class);

    verify(putRequestedFor(PACKAGE_URL_PATTERN)
      .withRequestBody(putBodyPattern));

    assertEquals(updatedSelected, aPackage.getData().getAttributes().getIsSelected());
    assertEquals(updatedHidden, aPackage.getData().getAttributes().getVisibilityData().getIsHidden());
    assertEquals(updatedBeginCoverage, aPackage.getData().getAttributes().getCustomCoverage().getBeginCoverage());
    assertEquals(updatedEndCoverage, aPackage.getData().getAttributes().getCustomCoverage().getEndCoverage());
    assertEquals(updatedPackageName, aPackage.getData().getAttributes().getName());
    assertEquals(ContentType.STREAMING_MEDIA, aPackage.getData().getAttributes().getContentType());

    List<AccessTypeMapping> accessTypeMappingsInDB = getAccessTypeMappings(vertx);
    assertEquals(1, accessTypeMappingsInDB.size());
    assertEquals(aPackage.getData().getId(), accessTypeMappingsInDB.get(0).getRecordId());
    assertEqualsUUID(newAccessTypeId, accessTypeMappingsInDB.get(0).getAccessTypeId());
    assertEquals(PACKAGE, accessTypeMappingsInDB.get(0).getRecordType());
    assertNotNull(aPackage.getIncluded());
    assertEquals(newAccessTypeId, aPackage.getData().getRelationships().getAccessType().getData().getId());
    assertEquals(newAccessTypeId, ((LinkedHashMap<?, ?>) aPackage.getIncluded().get(0)).get("id"));
  }

  @Test
  public void shouldReturn400OnPutPackageWithNotExistedAccessType() throws URISyntaxException, IOException {
    String requestBody = readFile("requests/kb-ebsco/package/put-package-with-not-existed-access-type.json");
    mockGet(new RegexPattern(PACKAGE_BY_ID_URL), CUSTOM_PACKAGE_STUB_FILE);

    JsonapiError error = putWithStatus(PACKAGES_PATH, requestBody, SC_BAD_REQUEST, CONTENT_TYPE_HEADER, STUB_TOKEN_HEADER)
      .as(JsonapiError.class);

    assertErrorContainsTitle(error, "Access type not found: id = 99999999-9999-1999-a999-999999999999");
  }

  @Test
  public void shouldReturn422OnPutPackageWithInvalidAccessTypeId() throws URISyntaxException, IOException {
    String requestBody = readFile("requests/kb-ebsco/package/put-package-with-invalid-access-type.json");

    Errors error = putWithStatus(PACKAGES_PATH, requestBody, SC_UNPROCESSABLE_ENTITY, CONTENT_TYPE_HEADER, STUB_TOKEN_HEADER)
      .as(Errors.class);

    assertEquals(1, error.getErrors().size());
    assertEquals("must match \"^[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[1-5][a-fA-F0-9]{3}-[89abAB][a-fA-F0-9]{3}-[a-fA-F0-9]{12}$\"",
      error.getErrors().get(0).getMessage());
  }

  @Test
  public void shouldReturn400WhenRMAPIReturns400() throws URISyntaxException, IOException {
    EqualToPattern urlPattern = new EqualToPattern(PACKAGE_BY_ID_URL);

    mockGet(urlPattern, PACKAGE_STUB_FILE);
    mockPut(urlPattern, SC_BAD_REQUEST);

    JsonapiError error =
      putWithStatus(PACKAGES_PATH, readFile("requests/kb-ebsco/package/put-package-selected.json"), SC_BAD_REQUEST,
        STUB_TOKEN_HEADER).as(JsonapiError.class);

    assertEquals(1, error.getErrors().size());
  }

  @Test
  public void shouldReturn200WhenPackagePostIsValid() throws URISyntaxException, IOException {
    String packagePostStubRequestFile = "requests/kb-ebsco/package/post-package-request.json";
    String packagePostRMAPIRequestFile = "requests/rmapi/packages/post-package.json";

    final Package createdPackage = sendPost(readFile(packagePostStubRequestFile)).as(Package.class);

    assertTrue(Objects.isNull(createdPackage.getData().getAttributes().getTags()));
    EqualToJsonPattern postBodyPattern = new EqualToJsonPattern(readFile(packagePostRMAPIRequestFile), false, true);
    verify(1, postRequestedFor(new UrlPathPattern(new EqualToPattern(PACKAGES_STUB_URL), false))
      .withRequestBody(postBodyPattern));
  }

  @Test
  public void shouldReturn200OnPostPackageWithExistedAccessType() throws URISyntaxException, IOException {
    String accessTypeId = insertAccessType(testData(configuration.getId()).get(0), vertx);

    String packagePostRMAPIRequestFile = "requests/rmapi/packages/post-package.json";
    String requestBody = String.format(readFile("requests/kb-ebsco/package/post-package-with-access-type-request.json"),
      accessTypeId);
    Package createdPackage = sendPost(requestBody).as(Package.class);

    assertTrue(Objects.isNull(createdPackage.getData().getAttributes().getTags()));
    EqualToJsonPattern postBodyPattern = new EqualToJsonPattern(readFile(packagePostRMAPIRequestFile), false, true);
    verify(1, postRequestedFor(new UrlPathPattern(new EqualToPattern(PACKAGES_STUB_URL), false))
      .withRequestBody(postBodyPattern));

    List<AccessTypeMapping> accessTypeMappingsInDB = getAccessTypeMappings(vertx);
    assertEquals(1, accessTypeMappingsInDB.size());
    assertEqualsUUID(accessTypeId, accessTypeMappingsInDB.get(0).getAccessTypeId());
    assertEquals(PACKAGE, accessTypeMappingsInDB.get(0).getRecordType());
    assertNotNull(createdPackage.getIncluded());
    assertEquals(accessTypeId, createdPackage.getData().getRelationships().getAccessType().getData().getId());
    assertEquals(accessTypeId, ((LinkedHashMap<?, ?>) createdPackage.getIncluded().get(0)).get("id"));
  }

  @Test
  public void shouldReturn400OnPostPackageWithNotExistedAccessType() throws URISyntaxException, IOException {
    String requestBody = readFile("requests/kb-ebsco/package/post-package-with-not-existed-access-type-request.json");
    mockUpdateScenario(readFile(CUSTOM_PACKAGE_STUB_FILE));

    JsonapiError error = postWithStatus(PACKAGES_ENDPOINT, requestBody, SC_BAD_REQUEST, CONTENT_TYPE_HEADER,
      STUB_TOKEN_HEADER).as(JsonapiError.class);

    assertErrorContainsTitle(error, "Access type not found: id = 99999999-9999-1999-a999-999999999999");
  }

  @Test
  public void shouldReturn422OnPostPackageWithInvalidAccessTypeId() throws URISyntaxException, IOException {
    String requestBody = readFile("requests/kb-ebsco/package/post-package-with-invalid-access-type-request.json");

    Errors error = postWithStatus(PACKAGES_ENDPOINT, requestBody, SC_UNPROCESSABLE_ENTITY, CONTENT_TYPE_HEADER,
      STUB_TOKEN_HEADER).as(Errors.class);

    assertEquals(1, error.getErrors().size());
    assertEquals("must match \"^[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[1-5][a-fA-F0-9]{3}-[89abAB][a-fA-F0-9]{3}-[a-fA-F0-9]{12}$\"",
      error.getErrors().get(0).getMessage());
  }

  @Test
  public void shouldReturn400WhenPackagePostDataIsInvalid() throws URISyntaxException, IOException {
    String providerStubResponseFile = "responses/rmapi/packages/get-package-provider-by-id.json";
    String packagePostStubRequestFile = "requests/kb-ebsco/package/post-package-request.json";
    String response = "responses/rmapi/packages/post-package-400-error-response.json";

    EqualToJsonPattern postBodyPattern = new EqualToJsonPattern(
      "{\n  \"contentType\" : 1,\n  \"packageName\" : \"TEST_NAME\",\n  \"customCoverage\" : {\n    \"beginCoverage\" : \"2017-12-23\",\n    \"endCoverage\" : \"2018-03-30\"\n  }\n}",
      false, true);

    mockGet(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors.*"), providerStubResponseFile);
    mockPost(new EqualToPattern(PACKAGES_STUB_URL),
      postBodyPattern, response, SC_BAD_REQUEST);

    RestAssured.given()
      .spec(getRequestSpecification())
      .header(STUB_TOKEN_HEADER)
      .body(readFile(packagePostStubRequestFile))
      .when()
      .post(PACKAGES_ENDPOINT)
      .then()
      .statusCode(SC_BAD_REQUEST);
  }

  @Test
  public void shouldReturnDefaultResourcesOnGetWithResources() throws IOException, URISyntaxException, JSONException {
    String query =
      "?searchfield=titlename&selection=all&resourcetype=all&searchtype=advanced&search=&offset=1&count=25&orderby=titlename";
    shouldReturnResourcesOnGetWithResources(PACKAGE_RESOURCES_PATH, query);
  }

  @Test
  public void shouldReturnResourcesWithPagingOnGetWithResources() throws IOException, URISyntaxException, JSONException {
    String packageResourcesUrl = PACKAGE_RESOURCES_PATH + "?page=2";
    String query =
      "?searchfield=titlename&selection=all&resourcetype=all&searchtype=advanced&search=&offset=2&count=25&orderby=titlename";
    shouldReturnResourcesOnGetWithResources(packageResourcesUrl, query);
  }

  @Test
  public void shouldReturnEmptyListWhenResourcesAreNotFound() throws IOException, URISyntaxException {
    mockResourceById(RESOURCES_BY_PACKAGE_ID_EMPTY_STUB_FILE);

    ResourceCollection actual = getWithOk(PACKAGE_RESOURCES_PATH, STUB_TOKEN_HEADER).as(ResourceCollection.class);
    assertThat(actual.getData(), empty());
    assertEquals(0, (int) actual.getMeta().getTotalResults());
  }

  @Test
  public void shouldReturnResourcesWithTagsOnGetWithResources() throws IOException, URISyntaxException, JSONException {
    saveTag(vertx, "295-2545963-2099944", RESOURCE, STUB_TAG_VALUE);
    saveTag(vertx, "295-2545963-2172685", RESOURCE, STUB_TAG_VALUE_2);
    saveTag(vertx, "295-2545963-2172685", RESOURCE, STUB_TAG_VALUE_3);

    String query =
      "?searchfield=titlename&selection=all&resourcetype=all&searchtype=advanced&search=&offset=1&count=25&orderby=titlename";

    mockResourceById(RESOURCES_BY_PACKAGE_ID_STUB_FILE);

    String actual = getWithOk(PACKAGE_RESOURCES_PATH, STUB_TOKEN_HEADER).asString();
    String expected = readFile(EXPECTED_RESOURCES_WITH_TAGS_STUB_FILE);

    JSONAssert.assertEquals(expected, actual, false);

    verify(1, getRequestedFor(urlEqualTo(RESOURCES_BY_PACKAGE_ID_URL + query)));
  }

  @Test
  public void shouldReturnResourcesWithAccessTypesOnGetWithResources() throws IOException, URISyntaxException {
    List<AccessType> accessTypes = insertAccessTypes(testData(configuration.getId()), vertx);
    insertAccessTypeMapping(STUB_MANAGED_RESOURCE_ID, RESOURCE, accessTypes.get(0).getId(), vertx);
    insertAccessTypeMapping(STUB_MANAGED_RESOURCE_ID_2, RESOURCE, accessTypes.get(0).getId(), vertx);

    mockResourceById("responses/rmapi/titles/get-title-by-id-response.json");

    String resourcePath = PACKAGES_ENDPOINT + "/" + FULL_PACKAGE_ID + "/resources"
      + "?filter[access-type]=" + STUB_ACCESS_TYPE_NAME;
    ResourceCollection resourceCollection = getWithOk(resourcePath, STUB_TOKEN_HEADER).as(ResourceCollection.class);
    List<ResourceCollectionItem> resources = resourceCollection.getData();

    assertEquals(2, (int) resourceCollection.getMeta().getTotalResults());
    assertEquals(2, resources.size());
    assertThat(resources, everyItem(hasProperty("id",
      anyOf(equalTo(STUB_MANAGED_RESOURCE_ID), equalTo(STUB_MANAGED_RESOURCE_ID_2))
    )));
  }

  @Test
  public void shouldReturnResourcesWithAccessTypesOnGetWithResourcesWithPagination() throws IOException, URISyntaxException {
    List<AccessType> accessTypes = insertAccessTypes(testData(configuration.getId()), vertx);
    insertAccessTypeMapping(STUB_MANAGED_RESOURCE_ID, RESOURCE, accessTypes.get(0).getId(), vertx);
    insertAccessTypeMapping(STUB_MANAGED_RESOURCE_ID_2, RESOURCE, accessTypes.get(1).getId(), vertx);

    mockResourceById("responses/rmapi/titles/get-title-by-id-response.json");

    String resourcePath = PACKAGES_ENDPOINT + "/" + FULL_PACKAGE_ID + "/resources?page=2&count=1&"
      + "filter[access-type]=" + STUB_ACCESS_TYPE_NAME + "&filter[access-type]=" + STUB_ACCESS_TYPE_NAME_2;
    ResourceCollection resourceCollection = getWithOk(resourcePath, STUB_TOKEN_HEADER).as(ResourceCollection.class);
    List<ResourceCollectionItem> resources = resourceCollection.getData();

    assertEquals(2, (int) resourceCollection.getMeta().getTotalResults());
    assertEquals(1, resources.size());
    assertThat(resources, everyItem(hasProperty("id", equalTo(STUB_MANAGED_RESOURCE_ID))));
  }

  @Test
  public void shouldReturnResourcesWithAccessTypesOnGetWithResources1() throws IOException, URISyntaxException {
    List<AccessType> accessTypes = insertAccessTypes(testData(configuration.getId()), vertx);
    insertAccessTypeMapping("295-2545963-2099944", RESOURCE, accessTypes.get(0).getId(), vertx);
    insertAccessTypeMapping("295-2545963-2172685", RESOURCE, accessTypes.get(1).getId(), vertx);

    mockResourceById("responses/rmapi/titles/get-title-by-id-response.json");
    mockResourceById(RESOURCES_BY_PACKAGE_ID_STUB_FILE);

    ResourceCollection resourceCollection = getWithOk(PACKAGE_RESOURCES_PATH, STUB_TOKEN_HEADER).as(ResourceCollection.class);
    List<ResourceCollectionItem> resources = resourceCollection.getData();

    assertEquals(5, (int) resourceCollection.getMeta().getTotalResults());
    assertEquals(5, resources.size());

    assertEquals("295-2545963-2099944", resources.get(0).getId());
    assertEquals(1, resources.get(0).getIncluded().size());
    assertEquals(accessTypes.get(0).getId(), ((LinkedHashMap)resources.get(0).getIncluded().get(0)).get("id"));
    assertEquals("295-2545963-2172685", resources.get(2).getId());
    assertEquals(1, resources.get(2).getIncluded().size());
    assertEquals(accessTypes.get(1).getId(), ((LinkedHashMap)resources.get(2).getIncluded().get(0)).get("id"));
  }

  @Test
  public void shouldReturnFilteredResourcesWithNonEmptyCustomerResourceList() throws IOException, URISyntaxException {
    mockResourceById(RESOURCES_BY_PACKAGE_ID_EMPTY_CUSTOMER_RESOURCE_LIST_STUB_FILE);
    final ResourceCollection resourceCollection = getWithOk(PACKAGE_RESOURCES_PATH, STUB_TOKEN_HEADER)
      .as(ResourceCollection.class);

    final MetaTotalResults metaTotalResults = resourceCollection.getMeta();
    assertThat(metaTotalResults.getTotalResults(), equalTo(3));
  }

  @Test
  public void shouldReturnResourcesWithOnSearchByTags() throws IOException, URISyntaxException, JSONException {
    mockResourceById("responses/rmapi/resources/get-resource-by-id-success-response.json");

    ResourcesTestUtil.saveResource(buildResource(STUB_MANAGED_RESOURCE_ID, configuration.getId(), STUB_TITLE_NAME), vertx);

    saveTag(vertx, STUB_MANAGED_RESOURCE_ID, RESOURCE, STUB_TAG_VALUE);
    saveTag(vertx, STUB_MANAGED_RESOURCE_ID, RESOURCE, STUB_TAG_VALUE_2);

    String packageResourcesUrl = PACKAGE_RESOURCES_PATH + "?filter[tags]=" + STUB_TAG_VALUE;

    String actualResponse = getWithOk(packageResourcesUrl, STUB_TOKEN_HEADER).asString();

    JSONAssert.assertEquals(readFile("responses/kb-ebsco/resources/expected-tagged-resources.json"), actualResponse,
      false);
  }

  @Test
  public void shouldReturn404OnGetWithResourcesWhenPackageNotFound() {
    mockGet(new RegexPattern(RESOURCES_BY_PACKAGE_ID_URL + ".*"), SC_NOT_FOUND);

    JsonapiError error = getWithStatus(PACKAGE_RESOURCES_PATH, SC_NOT_FOUND, STUB_TOKEN_HEADER).as(JsonapiError.class);

    assertErrorContainsTitle(error, "Package not found");
  }

  @Test
  public void shouldReturn400OnGetWithResourcesWhenCountOutOfRange() {
    String packageResourcesUrl = PACKAGE_RESOURCES_PATH + "?count=500";

    JsonapiError error = getWithStatus(packageResourcesUrl, SC_BAD_REQUEST, STUB_TOKEN_HEADER).as(JsonapiError.class);

    assertErrorContainsTitle(error, "is not valid");
  }

  @Test
  public void shouldReturn400OnGetWithResourcesWhenRMAPI400() throws IOException, URISyntaxException {
    String stubResponseFile = "responses/rmapi/packages/get-package-resources-400-response.json";

    stubFor(
      get(
        new UrlPathPattern(new RegexPattern(
          PACKAGE_BY_ID_URL + "/titles.*"),
          true))
        .willReturn(new ResponseDefinitionBuilder()
          .withBody(readFile(stubResponseFile))
          .withStatus(SC_BAD_REQUEST)));

    JsonapiError error = getWithStatus(PACKAGE_RESOURCES_PATH, SC_BAD_REQUEST, STUB_TOKEN_HEADER).as(JsonapiError.class);

    assertErrorContainsTitle(error, "Parameter Count is outside the range 1-100.");
  }

  @Test
  public void shouldReturnUnauthorizedOnGetWithResourcesWhenRMAPI401() {
    mockGet(new RegexPattern(PACKAGE_BY_ID_URL + "/titles.*"), HttpStatus.SC_UNAUTHORIZED);

    JsonapiError error = getWithStatus(PACKAGE_RESOURCES_PATH, SC_FORBIDDEN, STUB_TOKEN_HEADER).as(JsonapiError.class);

    assertErrorContainsTitle(error, "Unauthorized Access");
  }

  @Test
  public void shouldReturnUnauthorizedOnGetWithResourcesWhenRMAPI403() {
    mockGet(new RegexPattern(PACKAGE_BY_ID_URL + "/titles.*"), SC_FORBIDDEN);

    JsonapiError error = getWithStatus(PACKAGE_RESOURCES_PATH, SC_FORBIDDEN, STUB_TOKEN_HEADER).as(JsonapiError.class);

    assertErrorContainsTitle(error, "Unauthorized Access");
  }

  @Test
  public void shouldFetchPackagesInBulk() throws IOException, URISyntaxException, JSONException {
    mockGet(new RegexPattern(PACKAGE_BY_ID_URL), PACKAGE_STUB_FILE);
    mockGet(new RegexPattern(PACKAGE_BY_ID_2_URL), PACKAGE_2_STUB_FILE);

    String postBody = readFile("requests/kb-ebsco/package/post-packages-bulk.json");
    final String actualResponse = postWithOk(PACKAGES_BULK_FETCH_PATH, postBody, STUB_TOKEN_HEADER).asString();

    JSONAssert.assertEquals(readFile("responses/kb-ebsco/packages/expected-post-packages-bulk.json"),
      actualResponse, false);
  }

  @Test
  public void shouldReturn422OnFetchPackagesInBulkWithInvalidIdFormat() throws IOException, URISyntaxException {
    String postBody = readFile("requests/kb-ebsco/package/post-packages-bulk-with-invalid-id-format.json");

    Errors error = postWithStatus(PACKAGES_BULK_FETCH_PATH, postBody, SC_UNPROCESSABLE_ENTITY, STUB_TOKEN_HEADER)
      .as(Errors.class);

    assertThat(error.getErrors().get(0).getMessage(), equalTo("elements in list must match pattern"));
  }

  @Test
  public void shouldReturnPackagesAndFailedIdsOnFetchPackagesInBulk() throws IOException, URISyntaxException, JSONException {
    mockGet(new RegexPattern(PACKAGE_BY_ID_URL), PACKAGE_STUB_FILE);
    mockGet(new RegexPattern(PACKAGE_BY_ID_2_URL), PACKAGE_2_STUB_FILE);

    String notFoundResponse = "responses/rmapi/packages/get-package-by-id-not-found-response.json";
    stubFor(
      get(new UrlPathPattern(new EqualToPattern(PACKAGES_STUB_URL + "/9999999"), false))
        .willReturn(new ResponseDefinitionBuilder()
          .withBody(readFile(notFoundResponse))
          .withStatus(404)));

    String postBody = readFile("requests/kb-ebsco/package/post-packages-bulk-with-non-existing-id.json");
    final String actualResponse = postWithOk(PACKAGES_BULK_FETCH_PATH, postBody, STUB_TOKEN_HEADER).asString();

    JSONAssert.assertEquals(readFile("responses/kb-ebsco/packages/expected-post-packages-bulk-with-failed-id.json"),
      actualResponse, false);
  }

  @Test
  public void shouldReturnEmptyPackagesOnFetchPackagesInBulkIfNoPackageIds() throws IOException, URISyntaxException, JSONException {
    String postBody = readFile("requests/kb-ebsco/package/post-packages-bulk-empty.json");
    final String actualResponse = postWithOk(PACKAGES_BULK_FETCH_PATH, postBody, STUB_TOKEN_HEADER).asString();

    JSONAssert.assertEquals(readFile("responses/kb-ebsco/packages/expected-post-packages-bulk-empty.json"),
      actualResponse, false);
  }

  private void shouldReturnResourcesOnGetWithResources(String getURL, String rmAPIQuery)
    throws IOException, URISyntaxException, JSONException {
    mockResourceById(RESOURCES_BY_PACKAGE_ID_STUB_FILE);

    String actual = getWithOk(getURL, STUB_TOKEN_HEADER).asString();
    String expected = readFile(EXPECTED_RESOURCES_STUB_FILE);

    JSONAssert.assertEquals(expected, actual, false);

    verify(1, getRequestedFor(urlEqualTo(RESOURCES_BY_PACKAGE_ID_URL + rmAPIQuery)));
  }

  private void mockResourceById(String stubFile) throws IOException, URISyntaxException {
    mockGet(new RegexPattern(RESOURCES_BY_PACKAGE_ID_URL + ".*"), stubFile);
  }

  private void mockUpdateScenario(String initialPackage, String updatedPackage) {
    mockUpdateScenario(initialPackage);

    stubFor(
      get(PACKAGE_URL_PATTERN)
        .inScenario(GET_PACKAGE_SCENARIO)
        .whenScenarioStateIs(PACKAGE_UPDATED_STATE)
        .willReturn(new ResponseDefinitionBuilder()
          .withBody(updatedPackage)));
  }

  private void mockUpdateScenario(String initialPackage) {
    stubFor(
      get(PACKAGE_URL_PATTERN)
        .inScenario(GET_PACKAGE_SCENARIO)
        .willReturn(new ResponseDefinitionBuilder()
          .withBody(initialPackage)));

    stubFor(
      put(PACKAGE_URL_PATTERN)
        .inScenario(GET_PACKAGE_SCENARIO)
        .willReturn(new ResponseDefinitionBuilder()
          .withStatus(SC_NO_CONTENT))
        .willSetStateTo(PACKAGE_UPDATED_STATE));
  }

  private Package sendPut(String mockUpdatedPackage) throws IOException, URISyntaxException {
    mockUpdateScenario(readFile(PACKAGE_STUB_FILE), mockUpdatedPackage);

    PackagePutRequest packageToBeUpdated =
      mapper.readValue(getFile("requests/kb-ebsco/package/put-package-selected.json"), PackagePutRequest.class);

    return putWithOk(PACKAGES_PATH, mapper.writeValueAsString(packageToBeUpdated), STUB_TOKEN_HEADER).as(Package.class);
  }

  private void sendPutTags(List<String> newTags) throws IOException, URISyntaxException {
    PackageTagsPutRequest tags =
      mapper.readValue(getFile("requests/kb-ebsco/package/put-package-tags.json"), PackageTagsPutRequest.class);

    if (newTags != null) {
      tags.getData().getAttributes().setTags(new Tags()
        .withTagList(newTags));
    }

    putWithOk(PACKAGE_TAGS_PATH, mapper.writeValueAsString(tags), STUB_TOKEN_HEADER).as(PackageTags.class);
  }

  private ExtractableResponse<Response> sendPost(String requestBody) throws IOException, URISyntaxException {
    String providerStubResponseFile = "responses/rmapi/packages/get-package-provider-by-id.json";
    String packageCreatedIdStubResponseFile = "responses/rmapi/packages/post-package-response.json";

    mockGet(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/"), providerStubResponseFile);
    mockPost(new EqualToPattern(PACKAGES_STUB_URL), new AnythingPattern(), packageCreatedIdStubResponseFile, SC_OK);
    mockGet(new EqualToPattern(PACKAGE_BY_ID_URL), PACKAGE_STUB_FILE);

    PackagePostRequest request = mapper.readValue(requestBody, PackagePostRequest.class);
    return postWithOk(PACKAGES_ENDPOINT, mapper.writeValueAsString(request), STUB_TOKEN_HEADER);
  }
}
