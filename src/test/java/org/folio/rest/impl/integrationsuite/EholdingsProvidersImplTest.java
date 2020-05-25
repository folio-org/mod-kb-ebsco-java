package org.folio.rest.impl.integrationsuite;

import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import static org.folio.repository.RecordType.PACKAGE;
import static org.folio.repository.RecordType.PROVIDER;
import static org.folio.repository.accesstypes.AccessTypeMappingsTableConstants.ACCESS_TYPES_MAPPING_TABLE_NAME;
import static org.folio.repository.accesstypes.AccessTypesTableConstants.ACCESS_TYPES_TABLE_NAME;
import static org.folio.repository.kbcredentials.KbCredentialsTableConstants.KB_CREDENTIALS_TABLE_NAME;
import static org.folio.repository.packages.PackageTableConstants.PACKAGES_TABLE_NAME;
import static org.folio.repository.providers.ProviderTableConstants.PROVIDERS_TABLE_NAME;
import static org.folio.repository.tag.TagTableConstants.TAGS_TABLE_NAME;
import static org.folio.rest.impl.PackagesTestData.FULL_PACKAGE_ID;
import static org.folio.rest.impl.PackagesTestData.FULL_PACKAGE_ID_2;
import static org.folio.rest.impl.PackagesTestData.FULL_PACKAGE_ID_3;
import static org.folio.rest.impl.PackagesTestData.FULL_PACKAGE_ID_4;
import static org.folio.rest.impl.PackagesTestData.FULL_PACKAGE_ID_5;
import static org.folio.rest.impl.PackagesTestData.STUB_PACKAGE_ID;
import static org.folio.rest.impl.PackagesTestData.STUB_PACKAGE_ID_2;
import static org.folio.rest.impl.PackagesTestData.STUB_PACKAGE_ID_3;
import static org.folio.rest.impl.PackagesTestData.STUB_PACKAGE_NAME;
import static org.folio.rest.impl.PackagesTestData.STUB_PACKAGE_NAME_2;
import static org.folio.rest.impl.PackagesTestData.STUB_PACKAGE_NAME_3;
import static org.folio.rest.impl.ProvidersTestData.PROVIDER_TAGS_PATH;
import static org.folio.rest.impl.ProvidersTestData.STUB_VENDOR_ID;
import static org.folio.rest.impl.ProvidersTestData.STUB_VENDOR_ID_2;
import static org.folio.rest.impl.ProvidersTestData.STUB_VENDOR_ID_3;
import static org.folio.rest.impl.ProvidersTestData.STUB_VENDOR_NAME;
import static org.folio.rest.impl.ProvidersTestData.STUB_VENDOR_NAME_2;
import static org.folio.rest.impl.ProvidersTestData.STUB_VENDOR_NAME_3;
import static org.folio.rest.impl.TagsTestData.STUB_TAG_VALUE;
import static org.folio.rest.impl.TagsTestData.STUB_TAG_VALUE_2;
import static org.folio.rest.impl.TagsTestData.STUB_TAG_VALUE_3;
import static org.folio.rest.util.RestConstants.PROVIDERS_TYPE;
import static org.folio.test.util.TestUtil.getFile;
import static org.folio.test.util.TestUtil.mockGet;
import static org.folio.test.util.TestUtil.mockGetWithBody;
import static org.folio.test.util.TestUtil.readFile;
import static org.folio.util.AccessTypesTestUtil.STUB_ACCESS_TYPE_NAME;
import static org.folio.util.AccessTypesTestUtil.STUB_ACCESS_TYPE_NAME_2;
import static org.folio.util.AccessTypesTestUtil.insertAccessTypeMapping;
import static org.folio.util.AccessTypesTestUtil.insertAccessTypes;
import static org.folio.util.AccessTypesTestUtil.testData;
import static org.folio.util.KBTestUtil.clearDataFromTable;
import static org.folio.util.KBTestUtil.getDefaultKbConfiguration;
import static org.folio.util.KBTestUtil.setupDefaultKBConfiguration;
import static org.folio.util.KbCredentialsTestUtil.STUB_TOKEN_HEADER;
import static org.folio.util.PackagesTestUtil.setUpPackage;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.matching.RegexPattern;
import com.github.tomakehurst.wiremock.matching.UrlPathPattern;
import io.restassured.RestAssured;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import io.vertx.core.json.Json;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.JSONAssert;

import org.folio.holdingsiq.model.VendorById;
import org.folio.rest.impl.WireMockTestBase;
import org.folio.rest.jaxrs.model.AccessType;
import org.folio.rest.jaxrs.model.JsonapiError;
import org.folio.rest.jaxrs.model.KbCredentials;
import org.folio.rest.jaxrs.model.PackageCollection;
import org.folio.rest.jaxrs.model.PackageCollectionItem;
import org.folio.rest.jaxrs.model.PackageTags;
import org.folio.rest.jaxrs.model.PackageTagsPutRequest;
import org.folio.rest.jaxrs.model.Provider;
import org.folio.rest.jaxrs.model.ProviderCollection;
import org.folio.rest.jaxrs.model.ProviderPutRequest;
import org.folio.rest.jaxrs.model.ProviderTagsPutRequest;
import org.folio.rest.jaxrs.model.Providers;
import org.folio.rest.jaxrs.model.Tags;
import org.folio.rest.jaxrs.model.Token;
import org.folio.util.PackagesTestUtil;
import org.folio.util.ProvidersTestUtil;
import org.folio.util.TagsTestUtil;


@RunWith(VertxUnitRunner.class)
public class EholdingsProvidersImplTest extends WireMockTestBase {

  private static final String PROVIDER_RM_API_PATH = "/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors.*";
  private static final String PROVIDER_PACKAGES_RM_API_PATH =
    "/rm/rmaccounts.*" + STUB_CUSTOMER_ID + "/vendors/" + STUB_VENDOR_ID + "/packages.*";
  private static final UrlPathPattern PROVIDER_URL_PATTERN =
    new UrlPathPattern(new RegexPattern(PROVIDER_RM_API_PATH), true);

  private static final String PROVIDER_PATH = "eholdings/providers";
  private static final String PROVIDER_BY_ID = PROVIDER_PATH + "/" + STUB_VENDOR_ID;
  private static final String PROVIDER_PACKAGES = PROVIDER_BY_ID + "/packages";

  private static final String PUT_PROVIDER = "requests/kb-ebsco/provider/put-provider.json";
  private static final String PUT_PROVIDER_TAGS = "requests/kb-ebsco/provider/put-provider-tags.json";
  private static final String STUB_PACKAGE_RESPONSE = "responses/rmapi/packages/get-packages-by-provider-id.json";

  private KbCredentials configuration;

  @Override @Before
  public void setUp() throws Exception {
    super.setUp();
    setupDefaultKBConfiguration(getWiremockUrl(), vertx);
    configuration = getDefaultKbConfiguration(vertx);
  }

  @After
  public void tearDown() {
    clearDataFromTable(vertx, KB_CREDENTIALS_TABLE_NAME);
  }

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

    stubFor(
      get(PROVIDER_URL_PATTERN)
        .willReturn(new ResponseDefinitionBuilder()
          .withBody(readFile(stubResponseFile))));

    RestAssured.given()
      .spec(getRequestSpecification())
      .header(STUB_TOKEN_HEADER)
      .when()
      .get(PROVIDER_PATH + "?q=e&page=1&sort=name")
      .then()
      .statusCode(SC_OK)
      .body("meta.totalResults", equalTo(expectedTotalResults))
      .body("data[0].type", equalTo(PROVIDERS_TYPE))
      .body("data[0].id", equalTo(id))
      .body("data[0].attributes.name", equalTo(name))
      .body("data[0].attributes.packagesTotal", equalTo(packagesTotal))
      .body("data[0].attributes.packagesSelected", equalTo(packagesSelected))
      .body("data[0].attributes.supportsCustomPackages", equalTo(supportsCustomPackages))
      .body("data[0].attributes.providerToken.value", equalTo(token));
  }

  @Test
  public void shouldReturnProvidersOnSearchByTagsOnly() {
    try {
      TagsTestUtil.insertTag(vertx, STUB_VENDOR_ID, PROVIDER, STUB_TAG_VALUE);
      TagsTestUtil.insertTag(vertx, STUB_VENDOR_ID_2, PROVIDER, STUB_TAG_VALUE);
      TagsTestUtil.insertTag(vertx, STUB_VENDOR_ID_2, PROVIDER, STUB_TAG_VALUE_2);
      TagsTestUtil.insertTag(vertx, STUB_VENDOR_ID_3, PROVIDER, STUB_TAG_VALUE_3);

      setUpTaggedProviders();

      ProviderCollection providerCollection =
        getWithOk(PROVIDER_PATH + "?filter[tags]=" + STUB_TAG_VALUE + "," + STUB_TAG_VALUE_2, STUB_TOKEN_HEADER)
          .as(ProviderCollection.class);

      List<Providers> providers = providerCollection.getData();

      assertEquals(2, (int) providerCollection.getMeta().getTotalResults());
      assertEquals(2, providers.size());
      assertEquals(STUB_VENDOR_NAME, providers.get(0).getAttributes().getName());
      assertEquals(STUB_VENDOR_NAME_2, providers.get(1).getAttributes().getName());

    } finally {
      clearDataFromTable(vertx, TAGS_TABLE_NAME);
      clearDataFromTable(vertx, PROVIDERS_TABLE_NAME);
    }
  }

  @Test
  public void shouldReturnPackagesOnSearchByProviderIdAndTagsOnly() throws IOException, URISyntaxException {
    try {
      TagsTestUtil.insertTag(vertx, FULL_PACKAGE_ID, PACKAGE, STUB_TAG_VALUE);
      TagsTestUtil.insertTag(vertx, FULL_PACKAGE_ID, PACKAGE, STUB_TAG_VALUE_2);
      TagsTestUtil.insertTag(vertx, FULL_PACKAGE_ID_2, PACKAGE, STUB_TAG_VALUE);
      TagsTestUtil.insertTag(vertx, FULL_PACKAGE_ID_3, PACKAGE, STUB_TAG_VALUE_2);


      PackagesTestUtil.setUpPackages(vertx, configuration.getId());

      PackageCollection packageCollection =
        getWithOk(PROVIDER_PACKAGES + "?filter[tags]=" + STUB_TAG_VALUE + "," + STUB_TAG_VALUE_2, STUB_TOKEN_HEADER)
          .as(PackageCollection.class);

      List<PackageCollectionItem> packages = packageCollection.getData();

      assertEquals(1, (int) packageCollection.getMeta().getTotalResults());
      assertEquals(1, packages.size());
      assertThat(packages.get(0).getAttributes().getTags().getTagList(),
        containsInAnyOrder(STUB_TAG_VALUE, STUB_TAG_VALUE_2));
      assertEquals(STUB_PACKAGE_NAME, packages.get(0).getAttributes().getName());
    } finally {
      clearDataFromTable(vertx, TAGS_TABLE_NAME);
      clearDataFromTable(vertx, PACKAGES_TABLE_NAME);
    }
  }

  @Test
  public void shouldReturnPackagesOnSearchByProviderIdAndTagsWithPagination() throws IOException, URISyntaxException {
    try {
      TagsTestUtil.insertTag(vertx, FULL_PACKAGE_ID, PACKAGE, STUB_TAG_VALUE);
      TagsTestUtil.insertTag(vertx, FULL_PACKAGE_ID_4, PACKAGE, STUB_TAG_VALUE);
      TagsTestUtil.insertTag(vertx, FULL_PACKAGE_ID_5, PACKAGE, STUB_TAG_VALUE_2);

      String credentialsId = configuration.getId();
      setUpPackage(vertx, credentialsId, STUB_PACKAGE_ID, STUB_VENDOR_ID, STUB_PACKAGE_NAME);
      setUpPackage(vertx, credentialsId, STUB_PACKAGE_ID_2, STUB_VENDOR_ID, STUB_PACKAGE_NAME_2);
      setUpPackage(vertx, credentialsId, STUB_PACKAGE_ID_3, STUB_VENDOR_ID, STUB_PACKAGE_NAME_3);


      PackageCollection packageCollection =
        getWithOk(PROVIDER_PACKAGES + "?page=2&count=1&filter[tags]=" + STUB_TAG_VALUE + "," + STUB_TAG_VALUE_2,
          STUB_TOKEN_HEADER).as(PackageCollection.class);
      List<PackageCollectionItem> packages = packageCollection.getData();

      assertEquals(3, (int) packageCollection.getMeta().getTotalResults());
      assertEquals(1, packages.size());
      assertEquals(STUB_PACKAGE_NAME_2, packages.get(0).getAttributes().getName());
    } finally {
      clearDataFromTable(vertx, TAGS_TABLE_NAME);
      clearDataFromTable(vertx, PACKAGES_TABLE_NAME);
    }
  }

  @Test
  public void shouldReturnPackagesOnSearchByProviderIdAndAccessTypeWithPagination() throws IOException, URISyntaxException {
    try {
      List<AccessType> accessTypes = insertAccessTypes(testData(configuration.getId()), vertx);
      insertAccessTypeMapping(FULL_PACKAGE_ID, PACKAGE, accessTypes.get(0).getId(), vertx);
      insertAccessTypeMapping(FULL_PACKAGE_ID_4, PACKAGE, accessTypes.get(1).getId(), vertx);

      String credentialsId = configuration.getId();
      setUpPackage(vertx, credentialsId, STUB_PACKAGE_ID, STUB_VENDOR_ID, STUB_PACKAGE_NAME);
      setUpPackage(vertx, credentialsId, STUB_PACKAGE_ID_2, STUB_VENDOR_ID, STUB_PACKAGE_NAME_2);
      setUpPackage(vertx, credentialsId, STUB_PACKAGE_ID_3, STUB_VENDOR_ID, STUB_PACKAGE_NAME_3);

      String resourcePath = PROVIDER_PACKAGES + "?page=2&count=1&filter[access-type]="
        + STUB_ACCESS_TYPE_NAME + "&filter[access-type]=" + STUB_ACCESS_TYPE_NAME_2;
      PackageCollection packageCollection = getWithOk(resourcePath, STUB_TOKEN_HEADER).as(PackageCollection.class);

      List<PackageCollectionItem> packages = packageCollection.getData();

      assertEquals(2, (int) packageCollection.getMeta().getTotalResults());
      assertEquals(1, packages.size());
      assertEquals(STUB_PACKAGE_NAME, packages.get(0).getAttributes().getName());
    } finally {
      clearDataFromTable(vertx, ACCESS_TYPES_MAPPING_TABLE_NAME);
      clearDataFromTable(vertx, ACCESS_TYPES_TABLE_NAME);
      clearDataFromTable(vertx, PACKAGES_TABLE_NAME);
    }
  }

  @Test
  public void shouldReturnEmptyResponseWhenPackagesReturnedWithErrorOnSearchByAccessType() {
    try {
      List<AccessType> accessTypes = insertAccessTypes(testData(configuration.getId()), vertx);
      insertAccessTypeMapping(FULL_PACKAGE_ID, PACKAGE, accessTypes.get(0).getId(), vertx);
      insertAccessTypeMapping(FULL_PACKAGE_ID_4, PACKAGE, accessTypes.get(0).getId(), vertx);

      mockGet(new RegexPattern(".*vendors/.*/packages/.*"), SC_INTERNAL_SERVER_ERROR);

      String resourcePath = PROVIDER_PACKAGES + "?filter[access-type]=" + STUB_ACCESS_TYPE_NAME;
      PackageCollection packageCollection = getWithOk(resourcePath, STUB_TOKEN_HEADER).as(PackageCollection.class);
      List<PackageCollectionItem> packages = packageCollection.getData();

      assertEquals(2, (int) packageCollection.getMeta().getTotalResults());
      assertEquals(0, packages.size());
    } finally {
      clearDataFromTable(vertx, ACCESS_TYPES_MAPPING_TABLE_NAME);
      clearDataFromTable(vertx, ACCESS_TYPES_TABLE_NAME);
      clearDataFromTable(vertx, PACKAGES_TABLE_NAME);
    }
  }

  @Test
  public void shouldReturnEmptyResponseWhenProvidersReturnedWithErrorOnSearchByTags() {
    try {
      ProvidersTestUtil.addProvider(vertx, buildDbProvider(STUB_VENDOR_ID, STUB_VENDOR_NAME));
      ProvidersTestUtil.addProvider(vertx, buildDbProvider(STUB_VENDOR_ID_2, STUB_VENDOR_NAME_2));

      TagsTestUtil.insertTag(vertx, STUB_VENDOR_ID, PROVIDER, STUB_TAG_VALUE);
      TagsTestUtil.insertTag(vertx, STUB_VENDOR_ID_2, PROVIDER, STUB_TAG_VALUE);

      mockGet(new RegexPattern(".*vendors/.*"), SC_INTERNAL_SERVER_ERROR);

      ProviderCollection providerCollection = getWithOk(PROVIDER_PATH + "?filter[tags]=" + STUB_TAG_VALUE,
        STUB_TOKEN_HEADER).as(ProviderCollection.class);
      List<Providers> providers = providerCollection.getData();

      assertEquals(2, (int) providerCollection.getMeta().getTotalResults());
      assertEquals(0, providers.size());
    } finally {
      clearDataFromTable(vertx, TAGS_TABLE_NAME);
      clearDataFromTable(vertx, PROVIDERS_TABLE_NAME);
    }
  }

  @Test
  public void shouldReturnProvidersOnSearchWithTagsAndPagination() {
    try {
      TagsTestUtil.insertTag(vertx, STUB_VENDOR_ID, PROVIDER, STUB_TAG_VALUE);
      TagsTestUtil.insertTag(vertx, STUB_VENDOR_ID_2, PROVIDER, STUB_TAG_VALUE);
      TagsTestUtil.insertTag(vertx, STUB_VENDOR_ID_3, PROVIDER, STUB_TAG_VALUE);

      setUpTaggedProviders();

      ProviderCollection providerCollection =
        getWithOk(PROVIDER_PATH + "?page=2&count=1&filter[tags]=" + STUB_TAG_VALUE, STUB_TOKEN_HEADER)
          .as(ProviderCollection.class);
      List<Providers> providers = providerCollection.getData();

      assertEquals(3, (int) providerCollection.getMeta().getTotalResults());
      assertEquals(1, providers.size());
      assertEquals(STUB_VENDOR_NAME_2, providers.get(0).getAttributes().getName());
    } finally {
      clearDataFromTable(vertx, TAGS_TABLE_NAME);
      clearDataFromTable(vertx, PROVIDERS_TABLE_NAME);
    }
  }

  @Test
  public void shouldReturnProvidersOnGetWithPackages() throws IOException, URISyntaxException {
    String stubResponseFile = "responses/rmapi/vendors/get-vendor-by-id-response.json";
    String expectedProviderFile = "responses/kb-ebsco/providers/expected-provider-with-packages.json";

    stubFor(
      get(PROVIDER_URL_PATTERN)
        .willReturn(new ResponseDefinitionBuilder()
          .withBody(readFile(stubResponseFile))));

    stubFor(
      get(new UrlPathPattern(
        new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors/" + STUB_VENDOR_ID + "/packages.*"), true))
        .willReturn(new ResponseDefinitionBuilder()
          .withBody(readFile(STUB_PACKAGE_RESPONSE))));

    String actualProvider = getWithOk(PROVIDER_BY_ID + "?include=packages", STUB_TOKEN_HEADER).asString();

    JSONAssert.assertEquals(readFile(expectedProviderFile), actualProvider, false);
  }

  @Test
  public void shouldReturn500IfRMApiReturnsError() {
    stubFor(
      get(PROVIDER_URL_PATTERN)
        .willReturn(new ResponseDefinitionBuilder()
          .withStatus(SC_INTERNAL_SERVER_ERROR)));

    final JsonapiError error = getWithStatus(PROVIDER_PATH + "?q=e&count=1", SC_INTERNAL_SERVER_ERROR,
      STUB_TOKEN_HEADER).as(JsonapiError.class);

    assertThat(error.getErrors().get(0).getTitle(), notNullValue());
  }

  @Test
  public void shouldReturnErrorIfSortParameterInvalid() {
    checkResponseNotEmptyWhenStatusIs400(PROVIDER_PATH + "?q=e&count=10&sort=abc");
  }

  @Test
  public void shouldReturnProviderWhenValidId() throws IOException, URISyntaxException {
    String stubResponseFile = "responses/rmapi/vendors/get-vendor-by-id-response.json";
    String expectedProviderFile = "responses/kb-ebsco/providers/expected-provider.json";

    stubFor(
      get(PROVIDER_URL_PATTERN)
        .willReturn(new ResponseDefinitionBuilder()
          .withBody(readFile(stubResponseFile))));

    String provider = getWithOk(PROVIDER_BY_ID, STUB_TOKEN_HEADER).asString();

    JSONAssert.assertEquals(readFile(expectedProviderFile), provider, false);
  }

  @Test
  public void shouldReturnProviderWithTagWhenValidId() throws IOException, URISyntaxException {
    try {
      TagsTestUtil.insertTag(vertx, STUB_VENDOR_ID, PROVIDER, STUB_TAG_VALUE);

      String stubResponseFile = "responses/rmapi/vendors/get-vendor-by-id-response.json";

      stubFor(
        get(PROVIDER_URL_PATTERN)
          .willReturn(new ResponseDefinitionBuilder()
            .withBody(readFile(stubResponseFile))));

      Provider provider = getWithOk(PROVIDER_BY_ID, STUB_TOKEN_HEADER).as(Provider.class);

      assertTrue(provider.getData().getAttributes().getTags().getTagList().contains(STUB_TAG_VALUE));
    } finally {
      clearDataFromTable(vertx, TAGS_TABLE_NAME);
    }
  }

  @Test
  public void shouldReturn404WhenProviderIdNotFound() {
    stubFor(
      get(PROVIDER_URL_PATTERN)
        .willReturn(new ResponseDefinitionBuilder()
          .withStatus(SC_NOT_FOUND)));

    JsonapiError error = getWithStatus(PROVIDER_PATH + "/191919", SC_NOT_FOUND, STUB_TOKEN_HEADER).as(JsonapiError.class);

    assertThat(error.getErrors().get(0).getTitle(), is("Provider not found"));
  }

  @Test
  public void shouldReturn400WhenInvalidProviderId() {
    checkResponseNotEmptyWhenStatusIs400(PROVIDER_PATH + "/19191919as");
  }

  @Test
  public void shouldUpdateAndReturnProvider() throws IOException, URISyntaxException {
    String stubResponseFile = "responses/rmapi/vendors/get-vendor-updated-response.json";
    String expectedProviderFile = "responses/kb-ebsco/providers/expected-updated-provider.json";

    stubFor(
      get(PROVIDER_URL_PATTERN)
        .willReturn(new ResponseDefinitionBuilder().withBody(readFile(stubResponseFile))));

    stubFor(
      put(PROVIDER_URL_PATTERN)
        .willReturn(new ResponseDefinitionBuilder().withStatus(SC_NO_CONTENT)));

    String provider = putWithOk(PROVIDER_BY_ID, readFile(PUT_PROVIDER), STUB_TOKEN_HEADER).asString();

    JSONAssert.assertEquals(readFile(expectedProviderFile), provider, false);

    verify(1,
      putRequestedFor(PROVIDER_URL_PATTERN)
        .withRequestBody(equalToJson(readFile("requests/rmapi/vendors/put-vendor-token-proxy.json"))));
  }

  @Test
  public void shouldUpdateTagsOnPutTags() throws IOException, URISyntaxException {
    try {
      List<String> newTags = Arrays.asList(STUB_TAG_VALUE, STUB_TAG_VALUE_2);
      sendPutTags(Arrays.asList(STUB_TAG_VALUE, STUB_TAG_VALUE_2));
      List<String> tagsAfterRequest = TagsTestUtil.getTagsForRecordType(vertx, PROVIDER);
      assertThat(tagsAfterRequest, containsInAnyOrder(newTags.toArray()));
    } finally {
      clearDataFromTable(vertx, TAGS_TABLE_NAME);
      clearDataFromTable(vertx, PROVIDERS_TABLE_NAME);
    }
  }

  @Test
  public void shouldUpdateTagsOnPutTagsWithAlreadyExistingTags() throws IOException, URISyntaxException {
    try {
      TagsTestUtil.insertTag(vertx, STUB_VENDOR_ID, PROVIDER, STUB_TAG_VALUE);
      List<String> newTags = Arrays.asList(STUB_TAG_VALUE, STUB_TAG_VALUE_2);
      sendPutTags(Arrays.asList(STUB_TAG_VALUE, STUB_TAG_VALUE_2));
      List<String> tagsAfterRequest = TagsTestUtil.getTagsForRecordType(vertx, PROVIDER);
      assertThat(tagsAfterRequest, containsInAnyOrder(newTags.toArray()));
    } finally {
      clearDataFromTable(vertx, TAGS_TABLE_NAME);
      clearDataFromTable(vertx, PROVIDERS_TABLE_NAME);
    }
  }

  @Test
  public void shouldReturn422OnPutTagsWhenRequestBodyIsInvalid() throws IOException, URISyntaxException {
    ObjectMapper mapper = new ObjectMapper();
    PackageTagsPutRequest tags = mapper.readValue(getFile(PUT_PROVIDER_TAGS),
      PackageTagsPutRequest.class);
    tags.getData().getAttributes().setName("");
    JsonapiError response = putWithStatus(PROVIDER_TAGS_PATH, mapper.writeValueAsString(tags),
      SC_UNPROCESSABLE_ENTITY, STUB_TOKEN_HEADER).as(JsonapiError.class);

    assertEquals("Invalid name", response.getErrors().get(0).getTitle());
    assertEquals("name must not be empty", response.getErrors().get(0).getDetail());
  }

  @Test
  public void shouldReturn400WhenRMAPIErrorOnPut() throws IOException, URISyntaxException {
    String stubResponseFile = "responses/rmapi/vendors/put-vendor-token-not-allowed-response.json";

    stubFor(
      put(PROVIDER_URL_PATTERN)
        .willReturn(new ResponseDefinitionBuilder().withBody(readFile(stubResponseFile)).withStatus(SC_BAD_REQUEST)));

    JsonapiError error = putWithStatus(PROVIDER_BY_ID, readFile(PUT_PROVIDER), SC_BAD_REQUEST,
      STUB_TOKEN_HEADER).as(JsonapiError.class);

    assertThat(error.getErrors().get(0).getTitle(), equalTo("Provider does not allow token"));

  }

  @Test
  public void shouldReturn422WhenBodyInputInvalidOnPut() throws IOException, URISyntaxException {
    ObjectMapper mapper = new ObjectMapper();
    ProviderPutRequest providerToBeUpdated = mapper.readValue(getFile(PUT_PROVIDER), ProviderPutRequest.class);

    Token providerToken = new Token();
    providerToken.setValue(RandomStringUtils.randomAlphanumeric(501));

    providerToBeUpdated.getData().getAttributes().setProviderToken(providerToken);

    JsonapiError error = putWithStatus(PROVIDER_BY_ID, mapper.writeValueAsString(providerToBeUpdated),
      SC_UNPROCESSABLE_ENTITY, STUB_TOKEN_HEADER).as(JsonapiError.class);

    assertThat(error.getErrors().get(0).getTitle(), equalTo("Invalid value"));
    assertThat(error.getErrors().get(0).getDetail(), equalTo("Value is too long (maximum is 500 characters)"));

  }

  @Test
  public void shouldReturnProviderPackagesWhenValidId() throws IOException, URISyntaxException {
    mockGet(new RegexPattern(PROVIDER_PACKAGES_RM_API_PATH), STUB_PACKAGE_RESPONSE);

    String actual = getWithOk(PROVIDER_PACKAGES, STUB_TOKEN_HEADER).asString();
    String expected = readFile("responses/kb-ebsco/packages/expected-package-collection-with-one-element.json");

    JSONAssert.assertEquals(expected, actual, false);
  }

  @Test
  public void shouldReturnEmptyPackageListWhenNoProviderPackagesAreFound() throws IOException, URISyntaxException {
    String packageStubResponseFile = "responses/rmapi/packages/get-packages-by-provider-id-empty.json";

    mockGet(new RegexPattern(PROVIDER_PACKAGES_RM_API_PATH), packageStubResponseFile);

    PackageCollection packages = getWithOk(PROVIDER_PACKAGES, STUB_TOKEN_HEADER).as(PackageCollection.class);
    assertThat(packages.getData(), empty());
    assertEquals(0, (int) packages.getMeta().getTotalResults());
  }

  @Test
  public void shouldReturnProviderPackagesWithTags() throws IOException, URISyntaxException {
    try {
      setUpPackage(vertx, configuration.getId(), STUB_PACKAGE_ID, STUB_VENDOR_ID, STUB_PACKAGE_NAME);
      TagsTestUtil.insertTag(vertx, FULL_PACKAGE_ID, PACKAGE, STUB_TAG_VALUE);
      TagsTestUtil.insertTag(vertx, FULL_PACKAGE_ID, PACKAGE, STUB_TAG_VALUE_2);


      mockGet(new RegexPattern(PROVIDER_PACKAGES_RM_API_PATH), STUB_PACKAGE_RESPONSE);

      String actual = getWithOk(PROVIDER_PACKAGES, STUB_TOKEN_HEADER).asString();
      String expected = readFile(
        "responses/kb-ebsco/packages/expected-package-collection-with-one-element-with-tags.json");

      JSONAssert.assertEquals(expected, actual, false);
    } finally {
      clearDataFromTable(vertx, TAGS_TABLE_NAME);
      clearDataFromTable(vertx, PACKAGES_TABLE_NAME);
    }
  }

  @Test
  public void shouldReturn400IfProviderIdInvalid() {
    checkResponseNotEmptyWhenStatusIs400(PROVIDER_PATH + "/invalid/packages");
  }

  @Test
  public void shouldReturn400IfCountOutOfRange() {
    checkResponseNotEmptyWhenStatusIs400(PROVIDER_PACKAGES + "?count=120");
  }

  @Test
  public void shouldReturn400IfFilterTypeInvalid() {
    checkResponseNotEmptyWhenStatusIs400(PROVIDER_PACKAGES + "?q=Search&filter[selected]=true&filter[type]=unsupported");
  }

  @Test
  public void shouldReturn400IfFilterSelectedInvalid() {
    checkResponseNotEmptyWhenStatusIs400(PROVIDER_PACKAGES + "?q=Search&filter[selected]=invalid");
  }

  @Test()
  public void shouldReturn400IfPageOffsetInvalid() {
    final ExtractableResponse<Response> response = getWithStatus(
      PROVIDER_PACKAGES + "?q=Search&count=5&page=abc", SC_BAD_REQUEST);
    assertThat(response.response().asString(), containsString("For input string: \"abc\""));
  }

  @Test
  public void shouldReturn400IfSortInvalid() {
    checkResponseNotEmptyWhenStatusIs400(PROVIDER_PACKAGES + "?q=Search&sort=invalid");
  }

  @Test
  public void shouldReturn400IfQueryParamInvalid() {
    checkResponseNotEmptyWhenStatusIs400(PROVIDER_PACKAGES + "?q=");
  }

  @Test
  public void shouldReturn404WhenNonProviderIdNotFound() {
    String rmapiInvalidProviderIdUrl = "/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors/191919/packages";

    mockGet(new RegexPattern(rmapiInvalidProviderIdUrl), SC_NOT_FOUND);

    JsonapiError error = getWithStatus("/eholdings/providers/191919/packages", SC_NOT_FOUND, STUB_TOKEN_HEADER)
      .as(JsonapiError.class);

    assertThat(error.getErrors().get(0).getTitle(), is("Provider not found"));
  }

  private void sendPutTags(List<String> newTags) throws IOException, URISyntaxException {
    ObjectMapper mapper = new ObjectMapper();

    ProviderTagsPutRequest tags = mapper.readValue(getFile(PUT_PROVIDER_TAGS),
      ProviderTagsPutRequest.class);

    if (newTags != null) {
      tags.getData().getAttributes().setTags(new Tags()
        .withTagList(newTags));
    }

    putWithOk(PROVIDER_TAGS_PATH, mapper.writeValueAsString(tags), STUB_TOKEN_HEADER).as(PackageTags.class);
  }

  private void mockProviderWithName(String stubProviderId, String stubProviderName) {
    mockGetWithBody(new RegexPattern(".*vendors/" + stubProviderId),
      getProviderResponse(stubProviderName, stubProviderId));
  }

  private String getProviderResponse(String providerName, String providerId) {
    return Json.encode(VendorById.byIdBuilder()
      .vendorName(providerName)
      .vendorId(Integer.parseInt(providerId))
      .build());
  }

  private ProvidersTestUtil.DbProviders buildDbProvider(String id, String name) {
    return ProvidersTestUtil.DbProviders.builder()
      .id(String.valueOf(id))
      .name(name).build();
  }

  private void setUpTaggedProviders() {
    ProvidersTestUtil.addProvider(vertx, buildDbProvider(STUB_VENDOR_ID, STUB_VENDOR_NAME));
    ProvidersTestUtil.addProvider(vertx, buildDbProvider(STUB_VENDOR_ID_2, STUB_VENDOR_NAME_2));
    ProvidersTestUtil.addProvider(vertx, buildDbProvider(STUB_VENDOR_ID_3, STUB_VENDOR_NAME_3));

    mockProviderWithName(STUB_VENDOR_ID, STUB_VENDOR_NAME);
    mockProviderWithName(STUB_VENDOR_ID_2, STUB_VENDOR_NAME_2);
    mockProviderWithName(STUB_VENDOR_ID_3, STUB_VENDOR_NAME_3);
  }
}
