package org.folio.rest.impl;

import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;

import static org.folio.rest.util.RestConstants.PROVIDERS_TYPE;
import static org.folio.util.TestUtil.getFile;
import static org.folio.util.TestUtil.mockConfiguration;
import static org.folio.util.TestUtil.mockGet;
import static org.folio.util.TestUtil.readFile;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.matching.RegexPattern;
import com.github.tomakehurst.wiremock.matching.UrlPathPattern;

import io.restassured.RestAssured;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import io.vertx.ext.unit.junit.VertxUnitRunner;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.http.HttpStatus;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import org.folio.holdingsiq.model.Vendor;
import org.folio.holdingsiq.model.Vendors;
import org.folio.rest.jaxrs.model.JsonapiError;
import org.folio.rest.jaxrs.model.Provider;
import org.folio.rest.jaxrs.model.ProviderPutRequest;
import org.folio.rest.jaxrs.model.Tags;
import org.folio.rest.jaxrs.model.Token;
import org.folio.rest.util.template.RMAPIServicesFactory;
import org.folio.rmapi.ProvidersServiceImpl;
import org.folio.tag.RecordType;


@RunWith(VertxUnitRunner.class)
public class EholdingsProvidersImplTest extends WireMockTestBase {

  private static final String STUB_VENDOR_NAME = "vendor name";
  private static final String STUB_VENDOR_NAME_2 = "vendor name 2";

  private static final String STUB_VENDOR_TOKEN = "vendor token";
  private static final String STUB_VENDOR_TOKEN_2 = "vendor token 2";

  private static final String STUB_VENDOR_ID = "19";
  private static final Vendor STUB_VENDOR = Vendor.builder()
    .vendorId(Long.parseLong(STUB_VENDOR_ID))
    .vendorName(STUB_VENDOR_NAME)
    .vendorToken(STUB_VENDOR_TOKEN)
    .build();
  private static final String STUB_VENDOR_ID_2 = "18";
  private static final Vendor STUB_VENDOR_2 = Vendor.builder()
    .vendorId(Long.parseLong(STUB_VENDOR_ID_2))
    .vendorName(STUB_VENDOR_NAME_2)
    .vendorToken(STUB_VENDOR_TOKEN_2)
    .build();
  private static final String STUB_VENDOR_ID_3 = "17";
  private static final String STUB_TAG_VALUE = "tag one";
  private static final String STUB_TAG_VALUE_2 = "tag 2";
  private static final String STUB_TAG_VALUE_3 = "tag 3";
  @Autowired
  @Qualifier("spyRmApiServicesFactory")
  private RMAPIServicesFactory servicesFactory;

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

    mockConfiguration(CONFIGURATION_STUB_FILE, getWiremockUrl());
    stubFor(
      get(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors.*"), true))
        .willReturn(new ResponseDefinitionBuilder()
          .withBody(readFile(stubResponseFile))));

    RestAssured.given()
      .spec(getRequestSpecification())
      .when()
      .get("eholdings/providers?q=e&page=1&sort=name")
      .then()
      .statusCode(200)
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
  public void shouldReturnProvidersWithTagsOnSearchByTagsOnly() throws IOException, URISyntaxException {
    try {
      TagsTestUtil.insertTag(vertx, STUB_VENDOR_ID, RecordType.PROVIDER, STUB_TAG_VALUE);
      TagsTestUtil.insertTag(vertx, STUB_VENDOR_ID_2, RecordType.PROVIDER, STUB_TAG_VALUE);
      TagsTestUtil.insertTag(vertx, STUB_VENDOR_ID_2, RecordType.PROVIDER, STUB_TAG_VALUE_2);
      TagsTestUtil.insertTag(vertx, STUB_VENDOR_ID_3, RecordType.PROVIDER, STUB_TAG_VALUE_3);

      mockConfiguration(CONFIGURATION_STUB_FILE, getWiremockUrl());

      String expectedProviderFile = "responses/kb-ebsco/providers/expected-providers-by-tags.json";
      Vendors stubVendors = Vendors.builder()
        .vendorList(Arrays.asList(STUB_VENDOR, STUB_VENDOR_2))
        .totalResults(2)
        .build();
      ProvidersServiceImpl mockProviderService = mock(ProvidersServiceImpl.class);

      ArgumentCaptor<List<Long>> providerIdsCaptor = ArgumentCaptor.forClass(List.class);
      doReturn(mockProviderService).when(servicesFactory).createProvidersServiceImpl(any(), any());
      doReturn(CompletableFuture.completedFuture(stubVendors))
        .when(mockProviderService).retrieveProviders(providerIdsCaptor.capture());

      String provider = RestAssured.given(getRequestSpecification())
        .when()
        .get("eholdings/providers?filter[tags]=" + STUB_TAG_VALUE + "," + STUB_TAG_VALUE_2)
        .then()
        .statusCode(HttpStatus.SC_OK).extract().asString();

      JSONAssert.assertEquals(readFile(expectedProviderFile), provider, false);
      assertThat(providerIdsCaptor.getValue(),
        containsInAnyOrder(Long.parseLong(STUB_VENDOR_ID), Long.parseLong(STUB_VENDOR_ID_2)));
    } finally {
      TagsTestUtil.clearTags(vertx);
      reset(servicesFactory);
    }
  }

  @Test
  public void shouldReturnProvidersOnGetWithPackages() throws IOException, URISyntaxException {
    String stubResponseFile = "responses/rmapi/vendors/get-vendor-by-id-response.json";
    String stubPackagesResponseFile = "responses/rmapi/packages/get-packages-by-provider-id.json";
    String expectedProviderFile = "responses/kb-ebsco/providers/expected-provider-with-packages.json";

    mockConfiguration(CONFIGURATION_STUB_FILE, getWiremockUrl());
    stubFor(
      get(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors.*"), true))
        .willReturn(new ResponseDefinitionBuilder()
          .withBody(readFile(stubResponseFile))));

    stubFor(
      get(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors/" + STUB_VENDOR_ID + "/packages.*"), true))
        .willReturn(new ResponseDefinitionBuilder()
          .withBody(readFile(stubPackagesResponseFile))));

    RequestSpecification requestSpecification = getRequestSpecification();
    String providerByIdEndpoint = "eholdings/providers/" + STUB_VENDOR_ID + "?include=packages";
    String actualProvider = RestAssured.given(requestSpecification)
      .when()
      .get(providerByIdEndpoint)
      .then()
      .statusCode(HttpStatus.SC_OK).extract().asString();

    JSONAssert.assertEquals(readFile(expectedProviderFile), actualProvider, false);
  }

  @Test
  public void shouldReturnErrorIfParameterInvalid(){

    RestAssured.given()
      .spec(getRequestSpecification())
      .when()
      .get("eholdings/providers?q=e&count=1000")
      .then()
      .statusCode(400)
      .body("errors.first.title", notNullValue());
  }

  @Test
  public void shouldReturn500IfRMApiReturnsError() throws IOException, URISyntaxException {
    String wiremockUrl = getWiremockUrl();
    mockConfiguration(CONFIGURATION_STUB_FILE, wiremockUrl);
    stubFor(
      get(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors.*"), true))
        .willReturn(new ResponseDefinitionBuilder()
          .withStatus(500)));

    RestAssured.given()
      .spec(getRequestSpecification())
      .when()
      .get("eholdings/providers?q=e&count=1")
      .then()
      .statusCode(500)
      .body("errors.first.title", notNullValue());
  }

  @Test
  public void shouldReturnErrorIfSortParameterInvalid() {

    RestAssured.given()
      .spec(getRequestSpecification())
      .when()
      .get("eholdings/providers?q=e&count=10&sort=abc")
      .then()
      .statusCode(400)
      .body("errors.first.title", notNullValue());
  }

  @Test
  public void shouldReturnProviderWhenValidId() throws IOException, URISyntaxException {
    String stubResponseFile = "responses/rmapi/vendors/get-vendor-by-id-response.json";
    String expectedProviderFile = "responses/kb-ebsco/providers/expected-provider.json";

    mockConfiguration(CONFIGURATION_STUB_FILE, getWiremockUrl());
    stubFor(
      get(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors.*"), true))
        .willReturn(new ResponseDefinitionBuilder()
          .withBody(readFile(stubResponseFile))));

    String providerByIdEndpoint = "eholdings/providers/" + STUB_VENDOR_ID;
    RequestSpecification requestSpecification = getRequestSpecification();

    String provider = RestAssured.given(requestSpecification)
      .when()
      .get(providerByIdEndpoint)
      .then()
      .statusCode(HttpStatus.SC_OK).extract().asString();

    JSONAssert.assertEquals(readFile(expectedProviderFile), provider, false);
  }

  @Test
  public void shouldReturnProviderWithTagWhenValidId() throws IOException, URISyntaxException {
    try {
      TagsTestUtil.insertTag(vertx, STUB_VENDOR_ID, RecordType.PROVIDER, STUB_TAG_VALUE);

      String stubResponseFile = "responses/rmapi/vendors/get-vendor-by-id-response.json";

      mockConfiguration(CONFIGURATION_STUB_FILE, getWiremockUrl());
      stubFor(
        get(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors.*"), true))
          .willReturn(new ResponseDefinitionBuilder()
            .withBody(readFile(stubResponseFile))));

      String providerByIdEndpoint = "eholdings/providers/" + STUB_VENDOR_ID;
      RequestSpecification requestSpecification = getRequestSpecification();

      Provider provider = RestAssured.given(requestSpecification)
        .when()
        .get(providerByIdEndpoint)
        .then()
        .statusCode(HttpStatus.SC_OK).extract().as(Provider.class);

      assertTrue(provider.getData().getAttributes().getTags().getTagList().contains(STUB_TAG_VALUE));
    }
    finally {
      TagsTestUtil.clearTags(vertx);
    }
  }

  @Test
  public void shouldReturn404WhenProviderIdNotFound() throws IOException, URISyntaxException {

    mockConfiguration(CONFIGURATION_STUB_FILE, getWiremockUrl());
    stubFor(
      get(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors.*"), true))
        .willReturn(new ResponseDefinitionBuilder()
          .withStatus(HttpStatus.SC_NOT_FOUND)));

    RequestSpecification requestSpecification = getRequestSpecification();
    JsonapiError error = RestAssured.given()
      .spec(requestSpecification)
      .when()
      .get("eholdings/providers/191919")
      .then()
      .statusCode(HttpStatus.SC_NOT_FOUND)
      .extract().as(JsonapiError.class);

    assertThat(error.getErrors().get(0).getTitle(), is("Provider not found"));
  }

  @Test
  public void shouldReturn400WhenInvalidProviderId() throws IOException, URISyntaxException {
    mockConfiguration(CONFIGURATION_STUB_FILE, getWiremockUrl());

    RequestSpecification requestSpecification = getRequestSpecification();
    JsonapiError error = RestAssured.given()
      .spec(requestSpecification)
      .when()
      .get("eholdings/providers/19191919as")
      .then()
      .statusCode(HttpStatus.SC_BAD_REQUEST)
      .extract().as(JsonapiError.class);

    assertThat(error.getErrors().get(0).getTitle(), notNullValue());
  }

  @Test
  public void shouldUpdateAndReturnProviderOnPutWithNoTags() throws IOException, URISyntaxException {
    String stubResponseFile = "responses/rmapi/vendors/get-vendor-updated-response.json";
    String expectedProviderFile = "responses/kb-ebsco/providers/expected-updated-provider.json";
    mockConfiguration(CONFIGURATION_STUB_FILE, getWiremockUrl());

    stubFor(
      get(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors.*"), true))
        .willReturn(new ResponseDefinitionBuilder().withBody(readFile(stubResponseFile))));

    stubFor(
      put(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors.*"), true))
        .willReturn(new ResponseDefinitionBuilder().withStatus(204)));

    ObjectMapper mapper = new ObjectMapper();
    ProviderPutRequest providerToBeUpdated = mapper.readValue(getFile("requests/kb-ebsco/put-provider.json"),
      ProviderPutRequest.class);


    String provider = sendPutRequestAndRetrieveResponse("eholdings/providers/" + STUB_VENDOR_ID,
      mapper.writeValueAsString(providerToBeUpdated)).asString();

    JSONAssert.assertEquals(readFile(expectedProviderFile), provider, false);

    verify(1, putRequestedFor(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors.*"), true))
      .withRequestBody(equalToJson(readFile("requests/rmapi/vendors/put-vendor-token-proxy.json"))));
  }

  @Test
  public void shouldUpdateOnlyProviderTagsWhenNoPackagesAreSelected() throws IOException, URISyntaxException {
    try {
      String stubResponseFile = "responses/rmapi/vendors/get-vendor-without-selected-packages-response.json";
      String expectedProviderFile = "responses/kb-ebsco/providers/expected-provider-with-updated-tags.json";

      mockConfiguration(CONFIGURATION_STUB_FILE, getWiremockUrl());

      stubFor(
        get(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors.*"), true))
          .willReturn(new ResponseDefinitionBuilder().withBody(readFile(stubResponseFile))));

      ObjectMapper mapper = new ObjectMapper();
      ProviderPutRequest providerToBeUpdated = mapper.readValue(getFile("requests/kb-ebsco/put-provider.json"),
        ProviderPutRequest.class);

      providerToBeUpdated.getData().getAttributes().setPackagesSelected(0);
      providerToBeUpdated.getData().getAttributes().setTags(new Tags().withTagList(Arrays.asList(STUB_TAG_VALUE, STUB_TAG_VALUE_2)));

      String provider = sendPutRequestAndRetrieveResponse("eholdings/providers/" + STUB_VENDOR_ID,
        mapper.writeValueAsString(providerToBeUpdated))
        .asString();

      JSONAssert.assertEquals(readFile(expectedProviderFile), provider, false);
      List<String> tags = TagsTestUtil.getTags(vertx);
      assertThat(tags, containsInAnyOrder(STUB_TAG_VALUE, STUB_TAG_VALUE_2));
    } finally {
      TagsTestUtil.clearTags(vertx);
    }
  }

  @Test
  public void shouldUpdateAndReturnProviderOnPutWithTags() throws IOException, URISyntaxException {
    String stubResponseFile = "responses/rmapi/vendors/get-vendor-updated-response.json";
    String expectedProviderFile = "responses/kb-ebsco/providers/expected-updated-provider.json";

    mockConfiguration(CONFIGURATION_STUB_FILE, getWiremockUrl());

    stubFor(
      get(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors.*"), true))
        .willReturn(new ResponseDefinitionBuilder().withBody(readFile(stubResponseFile))));

    stubFor(
      put(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors.*"), true))
        .willReturn(new ResponseDefinitionBuilder().withStatus(204)));

    ObjectMapper mapper = new ObjectMapper();
    ProviderPutRequest providerToBeUpdated = mapper.readValue(getFile("requests/kb-ebsco/put-provider.json"),
      ProviderPutRequest.class);
    providerToBeUpdated.getData().getAttributes().setTags(new Tags()
      .withTagList(Arrays.asList("test tag one", "test tag two")));

    Provider expected = mapper.readValue(readFile(expectedProviderFile), Provider.class);
    expected.getData().getAttributes().setTags(new Tags()
      .withTagList(Arrays.asList("test tag one", "test tag two")));

    Provider provider = sendPutRequestAndRetrieveResponse("eholdings/providers/" + STUB_VENDOR_ID,
      mapper.writeValueAsString(providerToBeUpdated), Provider.class);

    JSONAssert.assertEquals(mapper.writeValueAsString(expected), mapper.writeValueAsString(provider), false);

    verify(1, putRequestedFor(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors.*"), true))
      .withRequestBody(equalToJson(readFile("requests/rmapi/vendors/put-vendor-token-proxy.json"))));

    TagsTestUtil.clearTags(vertx);
  }

  @Test
  public void shouldAddProviderTagsOnPut() throws IOException, URISyntaxException {
    try {
      List<String> newTags = Arrays.asList(STUB_TAG_VALUE, STUB_TAG_VALUE_2);
      sendPutWithTags(Arrays.asList(STUB_TAG_VALUE, STUB_TAG_VALUE_2));
      List<String> tagsAfterRequest = TagsTestUtil.getTagsForRecordType(vertx, RecordType.PROVIDER);
      assertThat(tagsAfterRequest, containsInAnyOrder(newTags.toArray()));
    } finally {
      TagsTestUtil.clearTags(vertx);
    }
  }

  @Test
  public void shouldAddProviderTagsOnPutWhenProviderAlreadyHasTags() throws IOException, URISyntaxException {
    try {
      TagsTestUtil.insertTag(vertx, STUB_VENDOR_ID, RecordType.PROVIDER, STUB_TAG_VALUE);
      List<String> newTags = Arrays.asList(STUB_TAG_VALUE, STUB_TAG_VALUE_2);
      sendPutWithTags(Arrays.asList(STUB_TAG_VALUE, STUB_TAG_VALUE_2));
      List<String> tagsAfterRequest = TagsTestUtil.getTagsForRecordType(vertx, RecordType.PROVIDER);
      assertThat(tagsAfterRequest, containsInAnyOrder(newTags.toArray()));
    } finally {
      TagsTestUtil.clearTags(vertx);
    }
  }

  @Test
  public void shouldDeleteAndAddProviderTagsOnPut() throws IOException, URISyntaxException {
    try {
      TagsTestUtil.insertTag(vertx, STUB_VENDOR_ID, RecordType.PROVIDER, "old tag value");
      List<String> newTags = Arrays.asList(STUB_TAG_VALUE, STUB_TAG_VALUE_2);
      sendPutWithTags(Arrays.asList(STUB_TAG_VALUE, STUB_TAG_VALUE_2));
      List<String> tagsAfterRequest = TagsTestUtil.getTagsForRecordType(vertx, RecordType.PROVIDER);
      assertThat(tagsAfterRequest, containsInAnyOrder(newTags.toArray()));
    } finally {
      TagsTestUtil.clearTags(vertx);
    }
  }

  @Test
  public void shouldDeleteAllProviderTagsOnPutWhenRequestHasEmptyListOfTags() throws IOException, URISyntaxException {
      TagsTestUtil.insertTag(vertx, STUB_VENDOR_ID, RecordType.PROVIDER, "old tag value");
      sendPutWithTags(Collections.emptyList());
      List<String> tagsAfterRequest = TagsTestUtil.getTagsForRecordType(vertx, RecordType.PROVIDER);
      assertThat(tagsAfterRequest, empty());
  }

  @Test
  public void shouldDoNothingOnPutWhenRequestHasNotTags() throws IOException, URISyntaxException {
      sendPutWithTags(null);
      List<String> tagsAfterRequest = TagsTestUtil.getTagsForRecordType(vertx, RecordType.PROVIDER);
      assertThat(tagsAfterRequest, empty());
  }

  @Test
  public void shouldReturn400WhenRMAPIErrorOnPut() throws IOException, URISyntaxException {
    String stubResponseFile = "responses/rmapi/vendors/put-vendor-token-not-allowed-response.json";

    mockConfiguration(CONFIGURATION_STUB_FILE, getWiremockUrl());

    stubFor(
      put(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors.*"), true))
        .willReturn(new ResponseDefinitionBuilder().withBody(readFile(stubResponseFile)).withStatus(400)));

    ObjectMapper mapper = new ObjectMapper();
    ProviderPutRequest providerToBeUpdated = mapper.readValue(getFile("requests/kb-ebsco/put-provider.json"),
      ProviderPutRequest.class);

    String providerByIdEndpoint = "eholdings/providers/" + STUB_VENDOR_ID;

    JsonapiError error = RestAssured
      .given()
      .spec(getRequestSpecification())
      .header(CONTENT_TYPE_HEADER)
      .body(mapper.writeValueAsString(providerToBeUpdated))
      .when()
      .put(providerByIdEndpoint)
      .then()
      .statusCode(HttpStatus.SC_BAD_REQUEST)
      .extract().as(JsonapiError.class);

    assertThat(error.getErrors().get(0).getTitle(), equalTo("Provider does not allow token"));

  }

  @Test
  public void shouldReturn422WhenBodyInputInvalidOnPut() throws IOException, URISyntaxException {
    mockConfiguration(CONFIGURATION_STUB_FILE, getWiremockUrl());

    ObjectMapper mapper = new ObjectMapper();
    ProviderPutRequest providerToBeUpdated = mapper.readValue(getFile("requests/kb-ebsco/put-provider.json"),
      ProviderPutRequest.class);

    Token providerToken = new Token();
    providerToken.setValue(RandomStringUtils.randomAlphanumeric(501));

    providerToBeUpdated.getData().getAttributes().setProviderToken(providerToken);

    String providerByIdEndpoint = "eholdings/providers/" + STUB_VENDOR_ID;

    JsonapiError error = RestAssured
      .given()
      .spec(getRequestSpecification())
      .header(CONTENT_TYPE_HEADER)
      .body(mapper.writeValueAsString(providerToBeUpdated))
      .when()
      .put(providerByIdEndpoint)
      .then()
      .statusCode(HttpStatus.SC_UNPROCESSABLE_ENTITY)
      .extract().as(JsonapiError.class);

    assertThat(error.getErrors().get(0).getTitle(), equalTo("Invalid value"));
    assertThat(error.getErrors().get(0).getDetail(), equalTo("Value is too long (maximum is 500 characters)"));

  }

  @Test
  public void shouldReturnProviderPackagesWhenValidId() throws IOException, URISyntaxException {
    String rmapiProviderPackagesUrl = "/rm/rmaccounts.*" + STUB_CUSTOMER_ID + "/vendors/"
      + STUB_VENDOR_ID + "/packages.*";
    String providerPackagesUrl = "eholdings/providers/" + STUB_VENDOR_ID + "/packages";
    String packageStubResponseFile = "responses/rmapi/packages/get-packages-by-provider-id.json";

    mockConfiguration(CONFIGURATION_STUB_FILE, getWiremockUrl());
    mockGet(new RegexPattern(rmapiProviderPackagesUrl), packageStubResponseFile);

    String actual = getResponseWithStatus(providerPackagesUrl, 200).asString();
    String expected = readFile("responses/kb-ebsco/packages/expected-package-collection-with-one-element.json");

    JSONAssert.assertEquals(expected, actual, false);
  }

  @Test
  public void shouldReturn400IfProviderIdInvalid() {
    checkResponseNotEmptyWhenStatusIs400("eholdings/providers/invalid/packages");
  }

  @Test
  public void shouldReturn400IfCountOutOfRange() {
    checkResponseNotEmptyWhenStatusIs400("eholdings/providers/" + STUB_VENDOR_ID + "/packages?count=120");
  }

  @Test
  public void shouldReturn400IfFilterTypeInvalid() {
    checkResponseNotEmptyWhenStatusIs400("eholdings/providers/" + STUB_VENDOR_ID +
      "/packages?q=Search&filter[selected]=true&filter[type]=unsupported");
  }

  @Test
  public void shouldReturn400IfFilterSelectedInvalid() {
    checkResponseNotEmptyWhenStatusIs400("eholdings/providers/" + STUB_VENDOR_ID +
      "/packages?q=Search&filter[selected]=invalid");
  }

  @Test()
  public void shouldReturn400IfPageOffsetInvalid() {
    final ExtractableResponse<Response> response = getResponseWithStatus("eholdings/providers/" + STUB_VENDOR_ID + "/packages?q=Search&count=5&page=abc",
      HttpStatus.SC_BAD_REQUEST);
    assertThat(response.response().asString(), containsString("For input string: \"abc\""));
  }

  @Test
  public void shouldReturn400IfSortInvalid() {
    checkResponseNotEmptyWhenStatusIs400("eholdings/providers/" +
      STUB_VENDOR_ID + "/packages?q=Search&sort=invalid");
  }

  @Test
  public void shouldReturn400IfQueryParamInvalid() {
    checkResponseNotEmptyWhenStatusIs400("/eholdings/providers/" + STUB_VENDOR_ID + "/packages?q=");
  }

  @Test
  public void shouldReturn404WhenNonProviderIdNotFound() throws IOException, URISyntaxException {
    String rmapiInvalidProviderIdUrl = "/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors/191919/packages";

    mockConfiguration(CONFIGURATION_STUB_FILE, getWiremockUrl());

    mockGet(new RegexPattern(rmapiInvalidProviderIdUrl), HttpStatus.SC_NOT_FOUND);

    JsonapiError error = getResponseWithStatus("/eholdings/providers/191919/packages",
      HttpStatus.SC_NOT_FOUND).as(JsonapiError.class);

    assertThat(error.getErrors().get(0).getTitle(), is("Provider not found"));
  }

  private List<String> sendPutWithTags(List<String> newTags) throws IOException, URISyntaxException {
    String stubResponseFile = "responses/rmapi/vendors/get-vendor-updated-response.json";

    mockConfiguration(CONFIGURATION_STUB_FILE, getWiremockUrl());

    stubFor(
      get(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors.*"), true))
        .willReturn(new ResponseDefinitionBuilder().withBody(readFile(stubResponseFile))));

    stubFor(
      put(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors.*"), true))
        .willReturn(new ResponseDefinitionBuilder().withStatus(204)));

    ObjectMapper mapper = new ObjectMapper();
    ProviderPutRequest providerToBeUpdated = mapper.readValue(getFile("requests/kb-ebsco/put-provider.json"),
      ProviderPutRequest.class);

    if(newTags != null) {
      providerToBeUpdated.getData().getAttributes().setTags(new Tags()
        .withTagList(newTags));
    }
    sendPutRequestAndRetrieveResponse("eholdings/providers/" + STUB_VENDOR_ID, mapper.writeValueAsString(providerToBeUpdated), Provider.class);

    return newTags;
  }
}
