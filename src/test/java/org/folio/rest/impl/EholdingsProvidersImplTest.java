package org.folio.rest.impl;

import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
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

import static org.folio.rest.util.RestConstants.PROVIDERS_TYPE;
import static org.folio.tag.repository.providers.ProviderTableConstants.PROVIDERS_TABLE_NAME;
import static org.folio.util.TestUtil.getFile;
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
import com.github.tomakehurst.wiremock.matching.RegexPattern;
import com.github.tomakehurst.wiremock.matching.UrlPathPattern;
import io.restassured.RestAssured;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.JSONAssert;

import org.folio.rest.jaxrs.model.JsonapiError;
import org.folio.rest.jaxrs.model.Provider;
import org.folio.rest.jaxrs.model.ProviderPutRequest;
import org.folio.rest.jaxrs.model.Tags;
import org.folio.rest.jaxrs.model.Token;
import org.folio.tag.RecordType;
import org.folio.util.ProvidersTestUtil;
import org.folio.util.TagsTestUtil;
import org.folio.util.TestUtil;


@RunWith(VertxUnitRunner.class)
public class EholdingsProvidersImplTest extends WireMockTestBase {
  private static final String STUB_VENDOR_ID = "19";
  private static final String STUB_TAG_VALUE = "tag one";
  private static final String STUB_TAG_VALUE_2 = "tag 2";

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

    mockDefaultConfiguration(getWiremockUrl());
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
  public void shouldReturnProvidersOnGetWithPackages() throws IOException, URISyntaxException {
    String stubResponseFile = "responses/rmapi/vendors/get-vendor-by-id-response.json";
    String stubPackagesResponseFile = "responses/rmapi/packages/get-packages-by-provider-id.json";
    String expectedProviderFile = "responses/kb-ebsco/providers/expected-provider-with-packages.json";

    mockDefaultConfiguration(getWiremockUrl());
    stubFor(
      get(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors.*"), true))
        .willReturn(new ResponseDefinitionBuilder()
          .withBody(readFile(stubResponseFile))));

    stubFor(
      get(new UrlPathPattern(
        new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors/" + STUB_VENDOR_ID + "/packages.*"), true))
        .willReturn(new ResponseDefinitionBuilder()
          .withBody(readFile(stubPackagesResponseFile))));

    String actualProvider = getWithOk("eholdings/providers/" + STUB_VENDOR_ID + "?include=packages").asString();

    JSONAssert.assertEquals(readFile(expectedProviderFile), actualProvider, false);
  }

  @Test
  public void shouldReturnErrorIfParameterInvalid(){
    checkResponseNotEmptyWhenStatusIs400("eholdings/providers?q=e&count=1000");
  }

  @Test
  public void shouldReturn500IfRMApiReturnsError() throws IOException, URISyntaxException {
    mockDefaultConfiguration(getWiremockUrl());
    stubFor(
      get(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors.*"), true))
        .willReturn(new ResponseDefinitionBuilder()
          .withStatus(500)));

    final JsonapiError error = getWithStatus("eholdings/providers?q=e&count=1", SC_INTERNAL_SERVER_ERROR).as(JsonapiError.class);

    assertThat(error.getErrors().get(0).getTitle(), notNullValue());
  }

  @Test
  public void shouldReturnErrorIfSortParameterInvalid() {
    checkResponseNotEmptyWhenStatusIs400("eholdings/providers?q=e&count=10&sort=abc");
  }

  @Test
  public void shouldReturnProviderWhenValidId() throws IOException, URISyntaxException {
    String stubResponseFile = "responses/rmapi/vendors/get-vendor-by-id-response.json";
    String expectedProviderFile = "responses/kb-ebsco/providers/expected-provider.json";

    mockDefaultConfiguration(getWiremockUrl());
    stubFor(
      get(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors.*"), true))
        .willReturn(new ResponseDefinitionBuilder()
          .withBody(readFile(stubResponseFile))));

    String provider = getWithOk("eholdings/providers/" + STUB_VENDOR_ID).asString();

    JSONAssert.assertEquals(readFile(expectedProviderFile), provider, false);
  }

  @Test
  public void shouldReturnProviderWithTagWhenValidId() throws IOException, URISyntaxException {
    try {
      TagsTestUtil.insertTag(vertx, STUB_VENDOR_ID, RecordType.PROVIDER, STUB_TAG_VALUE);

      String stubResponseFile = "responses/rmapi/vendors/get-vendor-by-id-response.json";

      mockDefaultConfiguration(getWiremockUrl());
      stubFor(
        get(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors.*"), true))
          .willReturn(new ResponseDefinitionBuilder()
            .withBody(readFile(stubResponseFile))));

      Provider provider = getWithOk("eholdings/providers/" + STUB_VENDOR_ID).as(Provider.class);

      assertTrue(provider.getData().getAttributes().getTags().getTagList().contains(STUB_TAG_VALUE));
    } finally {
      TagsTestUtil.clearTags(vertx);
    }
  }

  @Test
  public void shouldReturn404WhenProviderIdNotFound() throws IOException, URISyntaxException {

    mockDefaultConfiguration(getWiremockUrl());
    stubFor(
      get(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors.*"), true))
        .willReturn(new ResponseDefinitionBuilder()
          .withStatus(SC_NOT_FOUND)));

    JsonapiError error = getWithStatus("eholdings/providers/191919", SC_NOT_FOUND).as(JsonapiError.class);

    assertThat(error.getErrors().get(0).getTitle(), is("Provider not found"));
  }

  @Test
  public void shouldReturn400WhenInvalidProviderId() throws IOException, URISyntaxException {
    mockDefaultConfiguration(getWiremockUrl());

    checkResponseNotEmptyWhenStatusIs400("eholdings/providers/19191919as");
  }

  @Test
  public void shouldUpdateAndReturnProviderOnPutWithNoTags() throws IOException, URISyntaxException {
    String stubResponseFile = "responses/rmapi/vendors/get-vendor-updated-response.json";
    String expectedProviderFile = "responses/kb-ebsco/providers/expected-updated-provider.json";
    mockDefaultConfiguration(getWiremockUrl());

    stubFor(
      get(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors.*"), true))
        .willReturn(new ResponseDefinitionBuilder().withBody(readFile(stubResponseFile))));

    stubFor(
      put(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors.*"), true))
        .willReturn(new ResponseDefinitionBuilder().withStatus(204)));

    String provider = putWithOk("eholdings/providers/" + STUB_VENDOR_ID,
      readFile("requests/kb-ebsco/put-provider.json")).asString();

    JSONAssert.assertEquals(readFile(expectedProviderFile), provider, false);

    verify(1,
      putRequestedFor(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors.*"), true))
        .withRequestBody(equalToJson(readFile("requests/rmapi/vendors/put-vendor-token-proxy.json"))));
  }

  @Test
  public void shouldUpdateOnlyProviderTagsWhenNoPackagesAreSelected() throws IOException, URISyntaxException {
    try {
      String stubResponseFile = "responses/rmapi/vendors/get-vendor-without-selected-packages-response.json";
      String expectedProviderFile = "responses/kb-ebsco/providers/expected-provider-with-updated-tags.json";

      mockDefaultConfiguration(getWiremockUrl());

      stubFor(
        get(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors.*"), true))
          .willReturn(new ResponseDefinitionBuilder().withBody(readFile(stubResponseFile))));

      ObjectMapper mapper = new ObjectMapper();
      ProviderPutRequest providerToBeUpdated = mapper.readValue(getFile("requests/kb-ebsco/put-provider.json"),
        ProviderPutRequest.class);

      providerToBeUpdated.getData().getAttributes().setPackagesSelected(0);
      providerToBeUpdated.getData().getAttributes().setTags(
        new Tags().withTagList(Arrays.asList(STUB_TAG_VALUE, STUB_TAG_VALUE_2)));

      String provider = putWithOk("eholdings/providers/" + STUB_VENDOR_ID,
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

    mockDefaultConfiguration(getWiremockUrl());

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

    Provider provider = putWithOk("eholdings/providers/" + STUB_VENDOR_ID,
      mapper.writeValueAsString(providerToBeUpdated)).as(Provider.class);

    JSONAssert.assertEquals(mapper.writeValueAsString(expected), mapper.writeValueAsString(provider), false);

    verify(1,
      putRequestedFor(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors.*"), true))
        .withRequestBody(equalToJson(readFile("requests/rmapi/vendors/put-vendor-token-proxy.json"))));

    TagsTestUtil.clearTags(vertx);
  }

  @Test
  public void shouldAddProviderTagsOnPut() throws IOException, URISyntaxException {
    try {
      List<String> newTags = Arrays.asList(STUB_TAG_VALUE, STUB_TAG_VALUE_2);
      sendPutWithTags(Arrays.asList(STUB_TAG_VALUE, STUB_TAG_VALUE_2));
      List<ProvidersTestUtil.DbProviders> providers = ProvidersTestUtil.getProviders(vertx);
      List<String> tagsAfterRequest = TagsTestUtil.getTagsForRecordType(vertx, RecordType.PROVIDER);

      assertEquals(1, providers.size());
      assertEquals(STUB_VENDOR_ID, providers.get(0).getId());
      assertThat(tagsAfterRequest, containsInAnyOrder(newTags.toArray()));
    } finally {
      TagsTestUtil.clearTags(vertx);
      TestUtil.clearDataFromTable(vertx, PROVIDERS_TABLE_NAME);
    }
  }

  @Test
  public void shouldAddProviderTagsOnPutWhenProviderAlreadyHasTags() throws IOException, URISyntaxException {
    try {
      TagsTestUtil.insertTag(vertx, STUB_VENDOR_ID, RecordType.PROVIDER, STUB_TAG_VALUE);
      List<String> newTags = Arrays.asList(STUB_TAG_VALUE, STUB_TAG_VALUE_2);
      sendPutWithTags(Arrays.asList(STUB_TAG_VALUE, STUB_TAG_VALUE_2));
      List<String> tagsAfterRequest = TagsTestUtil.getTagsForRecordType(vertx, RecordType.PROVIDER);
      List<ProvidersTestUtil.DbProviders> providers = ProvidersTestUtil.getProviders(vertx);

      assertEquals(1, providers.size());
      assertEquals(STUB_VENDOR_ID, providers.get(0).getId());
      assertThat(tagsAfterRequest, containsInAnyOrder(newTags.toArray()));
    } finally {
      TagsTestUtil.clearTags(vertx);
      TestUtil.clearDataFromTable(vertx, PROVIDERS_TABLE_NAME);
    }
  }

  @Test
  public void shouldDeleteAndAddProviderTagsOnPut() throws IOException, URISyntaxException {
    try {
      TagsTestUtil.insertTag(vertx, STUB_VENDOR_ID, RecordType.PROVIDER, "old tag value");
      List<String> newTags = Arrays.asList(STUB_TAG_VALUE, STUB_TAG_VALUE_2);
      sendPutWithTags(Arrays.asList(STUB_TAG_VALUE, STUB_TAG_VALUE_2));
      List<String> tagsAfterRequest = TagsTestUtil.getTagsForRecordType(vertx, RecordType.PROVIDER);
      List<ProvidersTestUtil.DbProviders> providers = ProvidersTestUtil.getProviders(vertx);

      assertEquals(1, providers.size());
      assertEquals(STUB_VENDOR_ID, providers.get(0).getId());
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
    List<ProvidersTestUtil.DbProviders> providers = ProvidersTestUtil.getProviders(vertx);

    assertThat(providers,is(empty()));
    assertThat(tagsAfterRequest, empty());
  }

  @Test
  public void shouldDoNothingOnPutWhenRequestHasNotTags() throws IOException, URISyntaxException {
    sendPutWithTags(null);
    List<String> tagsAfterRequest = TagsTestUtil.getTagsForRecordType(vertx, RecordType.PROVIDER);
    List<ProvidersTestUtil.DbProviders> providers = ProvidersTestUtil.getProviders(vertx);

    assertThat(providers,is(empty()));
    assertThat(tagsAfterRequest, empty());
  }

  @Test
  public void shouldReturn400WhenRMAPIErrorOnPut() throws IOException, URISyntaxException {
    String stubResponseFile = "responses/rmapi/vendors/put-vendor-token-not-allowed-response.json";

    mockDefaultConfiguration(getWiremockUrl());

    stubFor(
      put(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors.*"), true))
        .willReturn(new ResponseDefinitionBuilder().withBody(readFile(stubResponseFile)).withStatus(400)));

    String providerByIdEndpoint = "eholdings/providers/" + STUB_VENDOR_ID;

    JsonapiError error = putWithStatus(providerByIdEndpoint, readFile("requests/kb-ebsco/put-provider.json"),
      SC_BAD_REQUEST).as(JsonapiError.class);

    assertThat(error.getErrors().get(0).getTitle(), equalTo("Provider does not allow token"));

  }

  @Test
  public void shouldReturn422WhenBodyInputInvalidOnPut() throws IOException, URISyntaxException {
    mockDefaultConfiguration(getWiremockUrl());

    ObjectMapper mapper = new ObjectMapper();
    ProviderPutRequest providerToBeUpdated = mapper.readValue(getFile("requests/kb-ebsco/put-provider.json"),
      ProviderPutRequest.class);

    Token providerToken = new Token();
    providerToken.setValue(RandomStringUtils.randomAlphanumeric(501));

    providerToBeUpdated.getData().getAttributes().setProviderToken(providerToken);

    String providerByIdEndpoint = "eholdings/providers/" + STUB_VENDOR_ID;

    JsonapiError error = putWithStatus(providerByIdEndpoint, mapper.writeValueAsString(providerToBeUpdated),
      SC_UNPROCESSABLE_ENTITY).as(JsonapiError.class);

    assertThat(error.getErrors().get(0).getTitle(), equalTo("Invalid value"));
    assertThat(error.getErrors().get(0).getDetail(), equalTo("Value is too long (maximum is 500 characters)"));

  }

  @Test
  public void shouldReturnProviderPackagesWhenValidId() throws IOException, URISyntaxException {
    String rmapiProviderPackagesUrl = "/rm/rmaccounts.*" + STUB_CUSTOMER_ID + "/vendors/"
      + STUB_VENDOR_ID + "/packages.*";
    String providerPackagesUrl = "eholdings/providers/" + STUB_VENDOR_ID + "/packages";
    String packageStubResponseFile = "responses/rmapi/packages/get-packages-by-provider-id.json";

    mockDefaultConfiguration(getWiremockUrl());
    mockGet(new RegexPattern(rmapiProviderPackagesUrl), packageStubResponseFile);

    String actual = getWithOk(providerPackagesUrl).asString();
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
    final ExtractableResponse<Response> response = getWithStatus(
      "eholdings/providers/" + STUB_VENDOR_ID + "/packages?q=Search&count=5&page=abc", SC_BAD_REQUEST);
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

    mockDefaultConfiguration(getWiremockUrl());

    mockGet(new RegexPattern(rmapiInvalidProviderIdUrl), SC_NOT_FOUND);

    JsonapiError error = getWithStatus("/eholdings/providers/191919/packages",
      SC_NOT_FOUND).as(JsonapiError.class);

    assertThat(error.getErrors().get(0).getTitle(), is("Provider not found"));
  }

  private List<String> sendPutWithTags(List<String> newTags) throws IOException, URISyntaxException {
    String stubResponseFile = "responses/rmapi/vendors/get-vendor-updated-response.json";

    mockDefaultConfiguration(getWiremockUrl());

    stubFor(
      get(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors.*"), true))
        .willReturn(new ResponseDefinitionBuilder().withBody(readFile(stubResponseFile))));

    stubFor(
      put(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors.*"), true))
        .willReturn(new ResponseDefinitionBuilder().withStatus(204)));

    ObjectMapper mapper = new ObjectMapper();
    ProviderPutRequest providerToBeUpdated = mapper.readValue(getFile("requests/kb-ebsco/put-provider.json"),
      ProviderPutRequest.class);

    if (newTags != null) {
      providerToBeUpdated.getData().getAttributes().setTags(new Tags()
        .withTagList(newTags));
    }
    putWithOk("eholdings/providers/" + STUB_VENDOR_ID, mapper.writeValueAsString(providerToBeUpdated));

    return newTags;
  }
}
