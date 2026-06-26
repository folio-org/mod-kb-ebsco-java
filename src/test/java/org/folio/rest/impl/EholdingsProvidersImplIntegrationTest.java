package org.folio.rest.impl;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static org.apache.commons.lang3.RandomStringUtils.insecure;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_UNPROCESSABLE_ENTITY;
import static org.folio.repository.RecordType.PACKAGE;
import static org.folio.repository.RecordType.PROVIDER;
import static org.folio.repository.accesstypes.AccessTypeMappingsTableConstants.ACCESS_TYPES_MAPPING_TABLE_NAME;
import static org.folio.repository.accesstypes.AccessTypesTableConstants.ACCESS_TYPES_TABLE_NAME;
import static org.folio.repository.kbcredentials.KbCredentialsTableConstants.KB_CREDENTIALS_TABLE_NAME;
import static org.folio.repository.packages.PackageTableConstants.PACKAGES_TABLE_NAME;
import static org.folio.repository.providers.ProviderTableConstants.PROVIDERS_TABLE_NAME;
import static org.folio.repository.tag.TagTableConstants.TAGS_TABLE_NAME;
import static org.folio.rest.util.RestConstants.PROVIDERS_TYPE;
import static org.folio.util.AccessTypesTestUtil.STUB_ACCESS_TYPE_NAME;
import static org.folio.util.AccessTypesTestUtil.STUB_ACCESS_TYPE_NAME_2;
import static org.folio.util.AccessTypesTestUtil.insertAccessTypeMapping;
import static org.folio.util.AccessTypesTestUtil.insertAccessTypes;
import static org.folio.util.AccessTypesTestUtil.testData;
import static org.folio.util.AssertTestUtil.assertErrorContainsDetail;
import static org.folio.util.AssertTestUtil.assertErrorContainsTitle;
import static org.folio.util.AssertTestUtil.assertJsonEqual;
import static org.folio.util.KbCredentialsTestUtil.getDefaultKbConfiguration;
import static org.folio.util.KbCredentialsTestUtil.setupDefaultKbConfiguration;
import static org.folio.util.ProvidersTestUtil.buildDbProvider;
import static org.folio.util.ProvidersTestUtil.saveProvider;
import static org.folio.util.TagsTestUtil.getTagsForRecordType;
import static org.folio.util.TagsTestUtil.saveTag;
import static org.folio.util.TestUtil.clearDataFromTable;
import static org.folio.util.TestUtil.readFile;
import static org.folio.util.TestUtil.readJsonFile;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.tomakehurst.wiremock.matching.RegexPattern;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import java.util.Arrays;
import java.util.List;
import org.folio.holdingsiq.model.PackageData;
import org.folio.holdingsiq.model.VendorById;
import org.folio.rest.jaxrs.model.AccessType;
import org.folio.rest.jaxrs.model.JsonapiError;
import org.folio.rest.jaxrs.model.KbCredentials;
import org.folio.rest.jaxrs.model.PackageCollection;
import org.folio.rest.jaxrs.model.PackageTags;
import org.folio.rest.jaxrs.model.PackageTagsPutRequest;
import org.folio.rest.jaxrs.model.Provider;
import org.folio.rest.jaxrs.model.ProviderCollection;
import org.folio.rest.jaxrs.model.ProviderPutRequest;
import org.folio.rest.jaxrs.model.ProviderTagsPutRequest;
import org.folio.rest.jaxrs.model.Tags;
import org.folio.rest.jaxrs.model.Token;
import org.folio.util.IntegrationTestBase;
import org.folio.util.PackagesTestUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EholdingsProvidersImplIntegrationTest extends IntegrationTestBase {

  // API endpoint paths
  private static final String PROVIDER_PATH = "eholdings/providers";
  private static final String PROVIDER_BY_ID = PROVIDER_PATH + "/" + STUB_VENDOR_ID;
  private static final String PROVIDER_PACKAGES = PROVIDER_BY_ID + "/packages";

  // RM API response files
  private static final String GET_VENDORS_RESPONSE = "responses/rmapi/vendors/get-vendors-response.json";
  private static final String GET_VENDOR_BY_ID_RESPONSE = "responses/rmapi/vendors/get-vendor-by-id-response.json";
  private static final String GET_VENDOR_UPDATED_RESPONSE = "responses/rmapi/vendors/get-vendor-updated-response.json";
  private static final String PUT_VENDOR_NOT_ALLOWED_RESPONSE =
    "responses/rmapi/vendors/put-vendor-token-not-allowed-response.json";
  private static final String PUT_VENDOR_TOKEN_PROXY_REQUEST = "requests/rmapi/vendors/put-vendor-token-proxy.json";
  private static final String GET_PACKAGES_BY_PROVIDER_EMPTY =
    "responses/rmapi/packages/get-packages-by-provider-id-empty.json";
  private static final String STUB_PACKAGE_RESPONSE = "responses/rmapi/packages/get-packages-by-provider-id.json";

  // KB-EBSCO expected response files
  private static final String EXPECTED_PROVIDER = "responses/kb-ebsco/providers/expected-provider.json";
  private static final String EXPECTED_PROVIDER_WITH_PACKAGES =
    "responses/kb-ebsco/providers/expected-provider-with-packages.json";
  private static final String EXPECTED_UPDATED_PROVIDER =
    "responses/kb-ebsco/providers/expected-updated-provider.json";
  private static final String EXPECTED_PACKAGE_COLLECTION =
    "responses/kb-ebsco/packages/expected-package-collection-with-one-element.json";
  private static final String EXPECTED_PACKAGE_COLLECTION_WITH_TAGS =
    "responses/kb-ebsco/packages/expected-package-collection-with-one-element-with-tags.json";

  // Request payload files
  private static final String PUT_PROVIDER = "requests/kb-ebsco/provider/put-provider.json";
  private static final String PUT_PROVIDER_TAGS = "requests/kb-ebsco/provider/put-provider-tags.json";
  private static final String STUB_PACKAGE_JSON_PATH = "responses/rmapi/packages/get-package-by-id-response.json";

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
    clearDataFromTable(vertx, PROVIDERS_TABLE_NAME);
    clearDataFromTable(vertx, PACKAGES_TABLE_NAME);
    clearDataFromTable(vertx, KB_CREDENTIALS_TABLE_NAME);
  }

  @Test
  void shouldReturnProvidersOnGet() {
    mockGet(equalTo(vendorsRmApi()), readFile(GET_VENDORS_RESPONSE));

    var collection = getWithOk(PROVIDER_PATH + "?q=e&page=1&sort=name").as(ProviderCollection.class);
    var provider = collection.getData().getFirst();

    assertEquals(115, (int) collection.getMeta().getTotalResults());
    assertEquals(PROVIDERS_TYPE, provider.getType());
    assertEquals("131872", provider.getId());

    var attributes = provider.getAttributes();
    assertEquals("Editions de L'Universite de Bruxelles", attributes.getName());
    assertEquals(1, (int) attributes.getPackagesTotal());
    assertEquals(0, (int) attributes.getPackagesSelected());
    assertEquals(false, attributes.getSupportsCustomPackages());
    assertEquals("sampleToken", attributes.getProviderToken().getValue());
  }

  @Test
  void shouldReturnProvidersOnSearchByTagsOnly() {
    saveTag(vertx, STUB_VENDOR_ID, PROVIDER, STUB_TAG_VALUE);
    saveTag(vertx, STUB_VENDOR_ID_2, PROVIDER, STUB_TAG_VALUE);
    saveTag(vertx, STUB_VENDOR_ID_2, PROVIDER, STUB_TAG_VALUE_2);
    saveTag(vertx, STUB_VENDOR_ID_3, PROVIDER, STUB_TAG_VALUE_3);

    setUpTaggedProviders();

    var endpoint = withTagFilters(PROVIDER_PATH, STUB_TAG_VALUE, STUB_TAG_VALUE_2);
    var collection = getWithOk(endpoint).as(ProviderCollection.class);

    var providers = collection.getData();

    assertEquals(2, (int) collection.getMeta().getTotalResults());
    assertEquals(2, providers.size());
    assertEquals(STUB_VENDOR_NAME, providers.get(0).getAttributes().getName());
    assertEquals(STUB_VENDOR_NAME_2, providers.get(1).getAttributes().getName());
  }

  @Test
  void shouldReturnPackagesOnSearchByProviderIdAndTagsOnly() {
    saveTag(vertx, FULL_PACKAGE_ID, PACKAGE, STUB_TAG_VALUE);
    saveTag(vertx, FULL_PACKAGE_ID, PACKAGE, STUB_TAG_VALUE_2);
    saveTag(vertx, FULL_PACKAGE_ID_2, PACKAGE, STUB_TAG_VALUE);
    saveTag(vertx, FULL_PACKAGE_ID_3, PACKAGE, STUB_TAG_VALUE_2);

    setUpPackages(vertx, configuration.getId());

    var endpoint = withTagFilters(PROVIDER_PACKAGES, STUB_TAG_VALUE);
    var packageCollection = getWithOk(endpoint).as(PackageCollection.class);
    var packages = packageCollection.getData();

    assertEquals(1, (int) packageCollection.getMeta().getTotalResults());
    assertEquals(1, packages.size());
    assertThat(packages.getFirst().getAttributes().getTags().getTagList(),
      containsInAnyOrder(STUB_TAG_VALUE, STUB_TAG_VALUE_2));
    assertEquals(STUB_PACKAGE_NAME, packages.getFirst().getAttributes().getName());
  }

  @Test
  void shouldReturnPackagesOnSearchByProviderIdAndTagsWithPagination() {
    saveTag(vertx, FULL_PACKAGE_ID, PACKAGE, STUB_TAG_VALUE);
    saveTag(vertx, FULL_PACKAGE_ID_4, PACKAGE, STUB_TAG_VALUE);
    saveTag(vertx, FULL_PACKAGE_ID_5, PACKAGE, STUB_TAG_VALUE_2);

    var credentialsId = configuration.getId();
    setUpPackage(vertx, credentialsId, STUB_PACKAGE_ID, STUB_VENDOR_ID, STUB_PACKAGE_NAME);
    setUpPackage(vertx, credentialsId, STUB_PACKAGE_ID_2, STUB_VENDOR_ID, STUB_PACKAGE_NAME_2);
    setUpPackage(vertx, credentialsId, STUB_PACKAGE_ID_3, STUB_VENDOR_ID, STUB_PACKAGE_NAME_3);

    var endpoint = withTagFilters(PROVIDER_PACKAGES + "?page=2&count=1", STUB_TAG_VALUE, STUB_TAG_VALUE_2);
    var collection = getWithOk(endpoint).as(PackageCollection.class);
    var packages = collection.getData();

    assertEquals(3, (int) collection.getMeta().getTotalResults());
    assertEquals(1, packages.size());
    assertEquals(STUB_PACKAGE_NAME_2, packages.getFirst().getAttributes().getName());
  }

  @Test
  void shouldReturnPackagesOnSearchByProviderIdAndAccessTypeWithPagination() {
    List<AccessType> accessTypes = insertAccessTypes(testData(configuration.getId()), vertx);
    insertAccessTypeMapping(FULL_PACKAGE_ID, PACKAGE, accessTypes.get(0).getId(), vertx);
    insertAccessTypeMapping(FULL_PACKAGE_ID_4, PACKAGE, accessTypes.get(1).getId(), vertx);

    var credentialsId = configuration.getId();
    setUpPackage(vertx, credentialsId, STUB_PACKAGE_ID, STUB_VENDOR_ID, STUB_PACKAGE_NAME);
    setUpPackage(vertx, credentialsId, STUB_PACKAGE_ID_2, STUB_VENDOR_ID, STUB_PACKAGE_NAME_2);
    setUpPackage(vertx, credentialsId, STUB_PACKAGE_ID_3, STUB_VENDOR_ID, STUB_PACKAGE_NAME_3);

    var resourcePath = withAccessTypeFilters(PROVIDER_PACKAGES + "?page=2&count=1",
      STUB_ACCESS_TYPE_NAME, STUB_ACCESS_TYPE_NAME_2);
    var collection = getWithOk(resourcePath).as(PackageCollection.class);
    var packages = collection.getData();

    assertEquals(2, (int) collection.getMeta().getTotalResults());
    assertEquals(1, packages.size());
    assertEquals(STUB_PACKAGE_NAME, packages.getFirst().getAttributes().getName());
  }

  @Test
  void shouldReturnEmptyResponseWhenPackagesReturnedWithErrorOnSearchByAccessType() {
    var accessTypes = insertAccessTypes(testData(configuration.getId()), vertx);
    insertAccessTypeMapping(FULL_PACKAGE_ID, PACKAGE, accessTypes.getFirst().getId(), vertx);
    insertAccessTypeMapping(FULL_PACKAGE_ID_4, PACKAGE, accessTypes.getFirst().getId(), vertx);

    mockGet(new RegexPattern(".*/lists/.*"), SC_INTERNAL_SERVER_ERROR);

    var endpoint = withAccessTypeFilters(PROVIDER_PACKAGES, STUB_ACCESS_TYPE_NAME);
    var collection = getWithOk(endpoint).as(PackageCollection.class);

    assertEquals(2, collection.getMeta().getTotalResults());
  }

  @Test
  void shouldReturnEmptyResponseWhenProvidersReturnedWithErrorOnSearchByTags() {
    var credentialsId = configuration.getId();
    saveProvider(buildDbProvider(STUB_VENDOR_ID, credentialsId, STUB_VENDOR_NAME), vertx);
    saveProvider(buildDbProvider(STUB_VENDOR_ID_2, credentialsId, STUB_VENDOR_NAME_2), vertx);

    saveTag(vertx, STUB_VENDOR_ID, PROVIDER, STUB_TAG_VALUE);
    saveTag(vertx, STUB_VENDOR_ID_2, PROVIDER, STUB_TAG_VALUE);

    mockGet(matching(vendorsRmApi() + ".*"), SC_INTERNAL_SERVER_ERROR);

    var endpoint = withTagFilters(PROVIDER_PATH, STUB_TAG_VALUE);
    var providerCollection = getWithOk(endpoint).as(ProviderCollection.class);

    assertEquals(2, (int) providerCollection.getMeta().getTotalResults());
  }

  @Test
  void shouldReturnProvidersOnSearchWithTagsAndPagination() {
    saveTag(vertx, STUB_VENDOR_ID, PROVIDER, STUB_TAG_VALUE);
    saveTag(vertx, STUB_VENDOR_ID_2, PROVIDER, STUB_TAG_VALUE);
    saveTag(vertx, STUB_VENDOR_ID_3, PROVIDER, STUB_TAG_VALUE);

    setUpTaggedProviders();

    var endpoint = withTagFilters(PROVIDER_PATH + "?page=2&count=1", STUB_TAG_VALUE);
    var providerCollection = getWithOk(endpoint).as(ProviderCollection.class);
    var providers = providerCollection.getData();

    assertEquals(3, (int) providerCollection.getMeta().getTotalResults());
    assertEquals(1, providers.size());
    assertEquals(STUB_VENDOR_NAME_2, providers.getFirst().getAttributes().getName());
  }

  @Test
  void shouldReturnProvidersOnGetWithPackages() {
    mockGet(equalTo(vendorsRmApi(STUB_VENDOR_ID)), readFile(GET_VENDOR_BY_ID_RESPONSE));
    mockGet(new RegexPattern(providerPackagesRmApi(STUB_VENDOR_ID) + ".*"), readFile(STUB_PACKAGE_RESPONSE));

    var actualProvider = getWithOk(PROVIDER_BY_ID + "?include=packages").asString();

    assertJsonEqual(readFile(EXPECTED_PROVIDER_WITH_PACKAGES), actualProvider);
  }

  @Test
  void shouldReturn500IfRmApiReturnsError() {
    mockGet(equalTo(vendorsRmApi()), SC_INTERNAL_SERVER_ERROR);

    var error = getWithStatus(PROVIDER_PATH + "?q=e&count=1", SC_INTERNAL_SERVER_ERROR).as(JsonapiError.class);

    assertNotNull(error.getErrors().getFirst().getTitle());
  }

  @Test
  void shouldReturnErrorIfSortParameterInvalid() {
    checkResponseNotEmptyWhenStatusIs400(PROVIDER_PATH + "?q=e&count=10&sort=abc");
  }

  @Test
  void shouldReturnProviderWhenValidId() {
    mockGet(equalTo(vendorsRmApi(STUB_VENDOR_ID)), readFile(GET_VENDOR_BY_ID_RESPONSE));

    var provider = getWithOk(PROVIDER_BY_ID).asString();

    assertJsonEqual(readFile(EXPECTED_PROVIDER), provider);
  }

  @Test
  void shouldReturnProviderWithTagWhenValidId() {
    saveTag(vertx, STUB_VENDOR_ID, PROVIDER, STUB_TAG_VALUE);

    mockGet(equalTo(vendorsRmApi(STUB_VENDOR_ID)), readFile(GET_VENDOR_BY_ID_RESPONSE));

    var provider = getWithOk(PROVIDER_BY_ID).as(Provider.class);

    assertTrue(provider.getData().getAttributes().getTags().getTagList().contains(STUB_TAG_VALUE));
  }

  @Test
  void shouldReturn404WhenProviderIdNotFound() {
    mockGet(equalTo(vendorsRmApi()), SC_NOT_FOUND);

    var error = getWithStatus(PROVIDER_PATH + "/191919", SC_NOT_FOUND).as(JsonapiError.class);

    assertErrorContainsTitle(error, "Provider not found");
  }

  @Test
  void shouldReturn400WhenInvalidProviderId() {
    checkResponseNotEmptyWhenStatusIs400(PROVIDER_PATH + "/19191919as");
  }

  @Test
  void shouldUpdateAndReturnProvider() {
    mockGet(equalTo(vendorsRmApi(STUB_VENDOR_ID)), readFile(GET_VENDOR_UPDATED_RESPONSE));
    mockPut(equalTo(vendorsRmApi(STUB_VENDOR_ID)), SC_NO_CONTENT);

    var provider = putWithOk(PROVIDER_BY_ID, readFile(PUT_PROVIDER)).asString();

    assertJsonEqual(readFile(EXPECTED_UPDATED_PROVIDER), provider);

    verifyPut(equalTo(vendorsRmApi(STUB_VENDOR_ID)), equalToJson(readFile(PUT_VENDOR_TOKEN_PROXY_REQUEST)), 1);
  }

  @Test
  void shouldUpdateTagsOnPutTags() {
    var newTags = Arrays.asList(STUB_TAG_VALUE, STUB_TAG_VALUE_2);
    sendPutTags(Arrays.asList(STUB_TAG_VALUE, STUB_TAG_VALUE_2));
    var tagsAfterRequest = getTagsForRecordType(vertx, PROVIDER);
    assertThat(tagsAfterRequest, containsInAnyOrder(newTags.toArray()));
  }

  @Test
  void shouldUpdateTagsOnPutTagsWithAlreadyExistingTags() {
    saveTag(vertx, STUB_VENDOR_ID, PROVIDER, STUB_TAG_VALUE);
    var newTags = Arrays.asList(STUB_TAG_VALUE, STUB_TAG_VALUE_2);
    sendPutTags(Arrays.asList(STUB_TAG_VALUE, STUB_TAG_VALUE_2));
    var tagsAfterRequest = getTagsForRecordType(vertx, PROVIDER);
    assertThat(tagsAfterRequest, containsInAnyOrder(newTags.toArray()));
  }

  @Test
  void shouldReturn422OnPutTagsWhenRequestBodyIsInvalid() {
    var tags = readJsonFile(PUT_PROVIDER_TAGS, PackageTagsPutRequest.class);
    tags.getData().getAttributes().setName("");
    var error = putWithStatus(providerTagsPath(), Json.encode(tags), SC_UNPROCESSABLE_ENTITY).as(JsonapiError.class);

    assertErrorContainsTitle(error, "Invalid name");
    assertErrorContainsDetail(error, "name must not be empty");
  }

  @Test
  void shouldReturn400WhenRmApiErrorOnPut() {
    mockPut(equalTo(vendorsRmApi(STUB_VENDOR_ID)), readFile(PUT_VENDOR_NOT_ALLOWED_RESPONSE), SC_BAD_REQUEST);

    var error = putWithStatus(PROVIDER_BY_ID, readFile(PUT_PROVIDER), SC_BAD_REQUEST).as(JsonapiError.class);

    assertErrorContainsTitle(error, "Provider does not allow token");
  }

  @Test
  void shouldReturn422WhenBodyInputInvalidOnPut() {
    var providerToBeUpdated = readJsonFile(PUT_PROVIDER, ProviderPutRequest.class);

    var providerToken = new Token();
    providerToken.setValue(insecure().nextAlphanumeric(501));
    providerToBeUpdated.getData().getAttributes().setProviderToken(providerToken);

    var putBody = Json.encode(providerToBeUpdated);
    var error = putWithStatus(PROVIDER_BY_ID, putBody, SC_UNPROCESSABLE_ENTITY).as(JsonapiError.class);

    assertErrorContainsTitle(error, "Invalid value");
    assertErrorContainsDetail(error, "Value is too long (maximum is 500 characters)");
  }

  @Test
  void shouldReturnProviderPackagesWhenValidId() {
    mockGet(matching(providerPackagesRmApi(STUB_VENDOR_ID) + ".*"), readFile(STUB_PACKAGE_RESPONSE));

    var actual = getWithOk(PROVIDER_PACKAGES).asString();

    assertJsonEqual(readFile(EXPECTED_PACKAGE_COLLECTION), actual);
  }

  @Test
  void shouldReturnEmptyPackageListWhenNoProviderPackagesAreFound() {
    mockGet(new RegexPattern(providerPackagesRmApi(STUB_VENDOR_ID) + ".*"), readFile(GET_PACKAGES_BY_PROVIDER_EMPTY));

    var packages = getWithOk(PROVIDER_PACKAGES).as(PackageCollection.class);

    assertTrue(packages.getData().isEmpty());
    assertEquals(0, (int) packages.getMeta().getTotalResults());
  }

  @Test
  void shouldReturnProviderPackagesWithTags() {
    setUpPackage(vertx, configuration.getId(), STUB_PACKAGE_ID, STUB_VENDOR_ID, STUB_PACKAGE_NAME);
    saveTag(vertx, FULL_PACKAGE_ID, PACKAGE, STUB_TAG_VALUE);
    saveTag(vertx, FULL_PACKAGE_ID, PACKAGE, STUB_TAG_VALUE_2);

    mockGet(new RegexPattern(providerPackagesRmApi(STUB_VENDOR_ID) + ".*"), readFile(STUB_PACKAGE_RESPONSE));

    var actual = getWithOk(PROVIDER_PACKAGES).asString();

    assertJsonEqual(readFile(EXPECTED_PACKAGE_COLLECTION_WITH_TAGS), actual);
  }

  @Test
  void shouldReturn400IfProviderIdInvalid() {
    checkResponseNotEmptyWhenStatusIs400(PROVIDER_PATH + "/invalid/packages");
  }

  @Test
  void shouldReturn400IfCountOutOfRange() {
    checkResponseNotEmptyWhenStatusIs400(PROVIDER_PACKAGES + "?count=120");
  }

  @Test
  void shouldReturn400IfFilterTypeInvalid() {
    checkResponseNotEmptyWhenStatusIs400(
      PROVIDER_PACKAGES + "?q=Search&filter[selected]=true&filter[type]=unsupported");
  }

  @Test
  void shouldReturn400IfFilterSelectedInvalid() {
    checkResponseNotEmptyWhenStatusIs400(PROVIDER_PACKAGES + "?q=Search&filter[selected]=invalid");
  }

  @Test
  void shouldReturn400IfPageOffsetInvalid() {
    var response = getWithStatus(PROVIDER_PACKAGES + "?q=Search&count=5&page=abc", SC_BAD_REQUEST);
    assertTrue(response.response().asString().contains("For input string: \"abc\""));
  }

  @Test
  void shouldReturn400IfSortInvalid() {
    checkResponseNotEmptyWhenStatusIs400(PROVIDER_PACKAGES + "?q=Search&sort=invalid");
  }

  @Test
  void shouldReturn400IfQueryParamInvalid() {
    checkResponseNotEmptyWhenStatusIs400(PROVIDER_PACKAGES + "?q=");
  }

  @Test
  void shouldReturn404WhenNonProviderIdNotFound() {
    var rmapiInvalidProviderIdUrl = RM_ACCOUNT_V2_API_PATH + "/vendors/191919/lists";
    mockGet(new RegexPattern(rmapiInvalidProviderIdUrl), SC_NOT_FOUND);

    var error = getWithStatus("/eholdings/providers/191919/packages", SC_NOT_FOUND)
      .as(JsonapiError.class);

    assertErrorContainsTitle(error, "Provider not found");
  }

  private String getPackageResponse(String packageName, int packageId, int providerId) {
    PackageData packageData = readJsonFile(STUB_PACKAGE_JSON_PATH, PackageData.class);
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
    var dbPackage = PackagesTestUtil.buildDbPackage(vendorId + "-" + packageId, credentialsId, packageName);
    PackagesTestUtil.savePackage(dbPackage, vertx);
    mockPackageWithName(packageId, vendorId, packageName);
  }

  private void mockPackageWithName(int stubPackageId, int stubProviderId, String stubPackageName) {
    mockGet(equalTo(packageRmApi(stubPackageId)), getPackageResponse(stubPackageName, stubPackageId, stubProviderId));
  }

  private void sendPutTags(List<String> newTags) {
    var tags = readJsonFile(PUT_PROVIDER_TAGS, ProviderTagsPutRequest.class);

    if (newTags != null) {
      tags.getData().getAttributes().setTags(new Tags()
        .withTagList(newTags));
    }

    putWithOk(providerTagsPath(), Json.encode(tags)).as(PackageTags.class);
  }

  private void mockProviderWithName(int providerId, String providerName) {
    var urlPattern = equalTo(vendorsRmApi(providerId));
    mockGet(urlPattern, getProviderResponse(providerName, providerId));
  }

  private String getProviderResponse(String providerName, int providerId) {
    return Json.encode(VendorById.byIdBuilder()
      .vendorName(providerName)
      .vendorId(providerId)
      .build());
  }

  private void setUpTaggedProviders() {
    String credentialsId = configuration.getId();
    saveProvider(buildDbProvider(STUB_VENDOR_ID, credentialsId, STUB_VENDOR_NAME), vertx);
    saveProvider(buildDbProvider(STUB_VENDOR_ID_2, credentialsId, STUB_VENDOR_NAME_2), vertx);
    saveProvider(buildDbProvider(STUB_VENDOR_ID_3, credentialsId, STUB_VENDOR_NAME_3), vertx);

    mockProviderWithName(STUB_VENDOR_ID, STUB_VENDOR_NAME);
    mockProviderWithName(STUB_VENDOR_ID_2, STUB_VENDOR_NAME_2);
    mockProviderWithName(STUB_VENDOR_ID_3, STUB_VENDOR_NAME_3);
  }
}
