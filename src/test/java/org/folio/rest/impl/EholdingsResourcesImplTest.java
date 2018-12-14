package org.folio.rest.impl;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.folio.rest.util.RestConstants.PACKAGES_TYPE;
import static org.folio.rest.util.RestConstants.PROVIDERS_TYPE;
import static org.folio.rest.util.RestConstants.TITLES_TYPE;
import static org.folio.util.TestUtil.readFile;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static org.folio.util.TestUtil.getFile;
import static org.folio.util.TestUtil.mockConfiguration;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;

import org.apache.http.HttpStatus;
import org.folio.rest.jaxrs.model.HasOneRelationship;
import org.folio.rest.jaxrs.model.JsonapiError;
import org.folio.rest.jaxrs.model.RelationshipData;
import org.folio.rest.jaxrs.model.Resource;
import org.folio.rmapi.model.PackageData;
import org.folio.util.TestUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.JSONAssert;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.EqualToJsonPattern;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import com.github.tomakehurst.wiremock.matching.RegexPattern;
import com.github.tomakehurst.wiremock.matching.UrlPathPattern;

import io.restassured.RestAssured;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class EholdingsResourcesImplTest extends WireMockTestBase {

  private static final String STUB_MANAGED_RESOURCE_ID = "583-4345-762169";
  private static final String STUB_MANAGED_VENDOR_ID = "583";
  private static final String STUB_MANAGED_PACKAGE_ID = "4345";
  private static final String STUB_MANAGED_TITLE_ID = "762169";
  private static final String STUB_CUSTOM_RESOURCE_ID = "123356-3157070-19412030";
  private static final String STUB_CUSTOM_VENDOR_ID = "123356";
  private static final String STUB_CUSTOM_PACKAGE_ID = "3157070";
  private static final String STUB_CUSTOM_TITLE_ID = "19412030";
  private static final String MANAGED_RESOURCE_ENDPOINT = "/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors/" + STUB_MANAGED_VENDOR_ID + "/packages/" + STUB_MANAGED_PACKAGE_ID + "/titles/" + STUB_MANAGED_TITLE_ID;
  private static final String CUSTOM_RESOURCE_ENDPOINT = "/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors/" + STUB_CUSTOM_VENDOR_ID + "/packages/" + STUB_CUSTOM_PACKAGE_ID + "/titles/" + STUB_CUSTOM_TITLE_ID;

  @Test
  public void shouldReturnResourceWhenValidId() throws IOException, URISyntaxException {
    String stubResponseFile = "responses/rmapi/resources/get-resource-by-id-success-response.json";

    String wiremockUrl = getWiremockUrl();
    mockConfiguration(CONFIGURATION_STUB_FILE, wiremockUrl);
    mockResource(stubResponseFile);

    String resourceByIdEndpoint = "eholdings/resources/" + STUB_MANAGED_RESOURCE_ID;

    String actualResponse = RestAssured.given()
      .spec(getRequestSpecification())
      .when()
      .get(resourceByIdEndpoint)
      .then()
      .statusCode(200).extract().asString();

    JSONAssert.assertEquals(
      readFile("responses/kb-ebsco/resources/expected-resource-by-id.json"), actualResponse, false);
  }

  @Test
  public void shouldReturnResourceWithTitleWhenTitleFlagSetToTrue() throws IOException, URISyntaxException {
    String stubResponseFile = "responses/rmapi/resources/get-resource-by-id-success-response.json";
    String expectedResourceFile = "responses/kb-ebsco/resources/expected-resource-by-id.json";
    String expectedTitleForResourceFile = "responses/kb-ebsco/titles/expected-title-by-id-for-resource.json";

    String wiremockUrl = getWiremockUrl();
    TestUtil.mockConfiguration(CONFIGURATION_STUB_FILE, wiremockUrl);

    mockResource(stubResponseFile);
    String resourceByIdEndpoint = "eholdings/resources/" + STUB_MANAGED_RESOURCE_ID + "?include=title";

    String actualResponse = RestAssured.given()
      .spec(getRequestSpecification())
      .when()
      .get(resourceByIdEndpoint)
      .then()
      .statusCode(200).extract().asString();

    ObjectMapper mapper = new ObjectMapper();
    Resource resource = mapper.readValue(readFile(expectedResourceFile), Resource.class);
    Object includedItem = mapper.readValue(readFile(expectedTitleForResourceFile), Object.class);
    resource.withIncluded(
      Collections.singletonList(includedItem));
    resource.getData()
      .getRelationships()
      .withTitle(new HasOneRelationship()
        .withData(new RelationshipData()
          .withType(TITLES_TYPE)
          .withId(STUB_MANAGED_TITLE_ID)));
    JSONAssert.assertEquals(
      mapper.writeValueAsString(resource), actualResponse, false);
  }

  @Test
  public void shouldReturnResourceWithProviderWhenProviderFlagSetToTrue() throws IOException, URISyntaxException {
    String stubResourceResponseFile = "responses/rmapi/resources/get-resource-by-id-success-response.json";
    String stubVendorResponseFile = "responses/rmapi/vendors/get-vendor-by-id-for-resource.json";
    String expectedResourceFile = "responses/kb-ebsco/resources/expected-resource-by-id.json";
    String expectedProviderForResourceFile = "responses/kb-ebsco/providers/expected-provider-by-id-for-resource.json";

    String wiremockUrl = getWiremockUrl();
    TestUtil.mockConfiguration(CONFIGURATION_STUB_FILE, wiremockUrl);

    mockResource(stubResourceResponseFile);
    mockVendor(stubVendorResponseFile);

    String resourceByIdEndpoint = "eholdings/resources/" + STUB_MANAGED_RESOURCE_ID + "?include=provider";

    String actualResponse = RestAssured.given()
      .spec(getRequestSpecification())
      .when()
      .get(resourceByIdEndpoint)
      .then()
      .statusCode(200).extract().asString();

    ObjectMapper mapper = new ObjectMapper();

    Resource resource = mapper.readValue(readFile(expectedResourceFile), Resource.class);
    Object includedItem = mapper.readValue(readFile(expectedProviderForResourceFile), Object.class);
    resource.withIncluded(
      Collections.singletonList(includedItem));
    resource.getData()
      .getRelationships()
      .withProvider(new HasOneRelationship()
        .withData(new RelationshipData()
          .withType(PROVIDERS_TYPE)
          .withId(STUB_MANAGED_VENDOR_ID)));
    JSONAssert.assertEquals(
      mapper.writeValueAsString(resource), actualResponse, false);
  }

  @Test
  public void shouldReturnResourceWithPackageWhenPackageFlagSetToTrue() throws IOException, URISyntaxException {
    String stubResourceResponseFile = "responses/rmapi/resources/get-resource-by-id-success-response.json";
    String stubPackageResponseFile = "responses/rmapi/packages/get-package-by-id-for-resource.json";
    String expectedResourceFile = "responses/kb-ebsco/resources/expected-resource-by-id.json";
    String expectedPackageForResourceFile = "responses/kb-ebsco/packages/expected-package-by-id-for-resource.json";

    String wiremockUrl = getWiremockUrl();
    TestUtil.mockConfiguration(CONFIGURATION_STUB_FILE, wiremockUrl);

    mockResource(stubResourceResponseFile);
    mockPackage(stubPackageResponseFile);

    String resourceByIdEndpoint = "eholdings/resources/" + STUB_MANAGED_RESOURCE_ID + "?include=package";

    String actualResponse = RestAssured.given()
      .spec(getRequestSpecification())
      .when()
      .get(resourceByIdEndpoint)
      .then()
      .statusCode(200).extract().asString();

    ObjectMapper mapper = new ObjectMapper();

    Resource resource = mapper.readValue(readFile(expectedResourceFile), Resource.class);
    Object includedItem = mapper.readValue(readFile(expectedPackageForResourceFile), Object.class);
    resource.withIncluded(
      Collections.singletonList(includedItem));
    resource.getData()
      .getRelationships()
      .withPackage(new HasOneRelationship()
        .withData(new RelationshipData()
          .withType(PACKAGES_TYPE)
          .withId(STUB_MANAGED_VENDOR_ID + "-" + STUB_MANAGED_PACKAGE_ID)));
    JSONAssert.assertEquals(
      mapper.writeValueAsString(resource), actualResponse, false);
  }

  @Test
  public void shouldReturnResourceWithAllIncludedObjectsWhenIncludeContainsAllObjects() throws IOException, URISyntaxException {
    String stubResourceResponseFile = "responses/rmapi/resources/get-resource-by-id-success-response.json";
    String stubVendorResponseFile = "responses/rmapi/vendors/get-vendor-by-id-for-resource.json";
    String stubPackageResponseFile = "responses/rmapi/packages/get-package-by-id-for-resource.json";
    String expectedResourceFile = "responses/kb-ebsco/resources/expected-resource-by-id.json";
    String expectedProviderForResourceFile = "responses/kb-ebsco/providers/expected-provider-by-id-for-resource.json";
    String expectedPackageForResourceFile = "responses/kb-ebsco/packages/expected-package-by-id-for-resource.json";
    String expectedTitleForResourceFile = "responses/kb-ebsco/titles/expected-title-by-id-for-resource.json";

    String wiremockUrl = getWiremockUrl();
    TestUtil.mockConfiguration(CONFIGURATION_STUB_FILE, wiremockUrl);

    mockResource(stubResourceResponseFile);
    mockVendor(stubVendorResponseFile);
    mockPackage(stubPackageResponseFile);

    String resourceByIdEndpoint = "eholdings/resources/" + STUB_MANAGED_RESOURCE_ID + "?include=package,title,provider";

    String actualResponse = RestAssured.given()
      .spec(getRequestSpecification())
      .when()
      .get(resourceByIdEndpoint)
      .then()
      .statusCode(200).extract().asString();

    ObjectMapper mapper = new ObjectMapper();

    Resource resource = mapper.readValue(readFile(expectedResourceFile), Resource.class);
    Object includedProvider = mapper.readValue(readFile(expectedProviderForResourceFile), Object.class);
    Object includedTitle = mapper.readValue(readFile(expectedTitleForResourceFile), Object.class);
    Object includedPackage = mapper.readValue(readFile(expectedPackageForResourceFile), Object.class);

    resource.withIncluded(
      Arrays.asList(includedProvider, includedTitle, includedPackage));
    resource.getData()
      .getRelationships()
      .withPackage(new HasOneRelationship()
        .withData(new RelationshipData()
          .withType(PACKAGES_TYPE)
          .withId(STUB_MANAGED_VENDOR_ID + "-" + STUB_MANAGED_PACKAGE_ID)))
      .withProvider(new HasOneRelationship()
        .withData(new RelationshipData()
          .withType(PROVIDERS_TYPE)
          .withId(String.valueOf(STUB_MANAGED_VENDOR_ID))))
      .withTitle(new HasOneRelationship()
        .withData(new RelationshipData()
          .withType(TITLES_TYPE)
          .withId(STUB_MANAGED_TITLE_ID)));
    JSONAssert.assertEquals(
      mapper.writeValueAsString(resource), actualResponse, false);
  }

  @Test
  public void shouldReturn404WhenRMAPINotFoundOnResourceGet() throws IOException, URISyntaxException {
    String stubResponseFile = "responses/rmapi/resources/get-resource-by-id-not-found-response.json";

    mockConfiguration(CONFIGURATION_STUB_FILE, getWiremockUrl());
    stubFor(
        get(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors/" + STUB_MANAGED_VENDOR_ID + "/packages/" + STUB_MANAGED_PACKAGE_ID + "/titles.*"), true))
        .willReturn(new ResponseDefinitionBuilder()
            .withBody(readFile(stubResponseFile))
            .withStatus(404)));

    String resourceByIdEndpoint = "eholdings/resources/" + STUB_MANAGED_RESOURCE_ID;

    JsonapiError error = RestAssured.given()
    .spec(getRequestSpecification())
    .when()
    .get(resourceByIdEndpoint)
    .then()
    .statusCode(404)
    .extract().as(JsonapiError.class);

    assertThat(error.getErrors().get(0).getTitle(), equalTo("Resource not found"));
  }

  @Test
  public void shouldReturn400WhenValidationErrorOnResourceGet() throws IOException, URISyntaxException {
    mockConfiguration(CONFIGURATION_STUB_FILE, getWiremockUrl());

    String resourceByIdEndpoint = "eholdings/resources/583-abc-762169";

    JsonapiError error = RestAssured.given()
    .spec(getRequestSpecification())
    .when()
    .get(resourceByIdEndpoint)
    .then()
    .statusCode(400)
    .extract().as(JsonapiError.class);

    assertThat(error.getErrors().get(0).getTitle(), equalTo("Resource id is invalid"));
  }

  @Test
  public void shouldReturn500WhenRMApiReturns500ErrorOnResourceGet() throws IOException, URISyntaxException {
    mockConfiguration(CONFIGURATION_STUB_FILE, getWiremockUrl());
    stubFor(
      get(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors/" + STUB_MANAGED_VENDOR_ID + "/packages/" + STUB_MANAGED_PACKAGE_ID + "/titles.*"), true))
        .willReturn(new ResponseDefinitionBuilder()
          .withStatus(500)));

    String resourceByIdEndpoint = "eholdings/resources/" + STUB_MANAGED_RESOURCE_ID;

    RestAssured.given()
      .spec(getRequestSpecification())
      .when()
      .when()
      .get(resourceByIdEndpoint)
      .then()
      .statusCode(500);
  }
  
  @Test
  public void shouldReturnUpdatedValuesManagedResourceOnSuccessfulPut() throws IOException, URISyntaxException {
    String stubResponseFile = "responses/rmapi/resources/get-managed-resource-updated-response.json";

    mockConfiguration(CONFIGURATION_STUB_FILE, getWiremockUrl());

    stubFor(
      get(new UrlPathPattern(new RegexPattern(MANAGED_RESOURCE_ENDPOINT), false))
        .willReturn(new ResponseDefinitionBuilder().withBody(readFile(stubResponseFile))));

    stubFor(
      put(new UrlPathPattern(new RegexPattern(MANAGED_RESOURCE_ENDPOINT), true))
        .willReturn(new ResponseDefinitionBuilder().withStatus(HttpStatus.SC_NO_CONTENT)));
    
    Resource expected = ResourcesTestData.getExpectedManagedResource();
    
    String updateResourceEndpoint = "eholdings/resources/" + STUB_MANAGED_RESOURCE_ID;

    Resource resource = RestAssured
      .given()
      .spec(getRequestSpecification())
      .header(CONTENT_TYPE_HEADER)
      .body(readFile("requests/kb-ebsco/resource/put-managed-resource.json"))
      .when()
      .put(updateResourceEndpoint)
      .then()
      .statusCode(HttpStatus.SC_OK)
      .extract().as(Resource.class);
    
    compareResource(resource, expected);

    verify(1, putRequestedFor(new UrlPathPattern(new RegexPattern(MANAGED_RESOURCE_ENDPOINT), true))
      .withRequestBody(equalToJson(readFile("requests/rmapi/resources/put-managed-resource-is-selected-multiple-attributes.json"))));
  }
  
  @Test
  public void shouldReturnUpdatedValuesCustomResourceOnSuccessfulPut() throws IOException, URISyntaxException {
    String stubResponseFile = "responses/rmapi/resources/get-custom-resource-updated-response.json";

    mockConfiguration(CONFIGURATION_STUB_FILE, getWiremockUrl());

    stubFor(
      get(new UrlPathPattern(new RegexPattern(CUSTOM_RESOURCE_ENDPOINT), false))
        .willReturn(new ResponseDefinitionBuilder().withBody(readFile(stubResponseFile))));

    stubFor(
      put(new UrlPathPattern(new RegexPattern(CUSTOM_RESOURCE_ENDPOINT), true))
        .willReturn(new ResponseDefinitionBuilder().withStatus(HttpStatus.SC_NO_CONTENT)));
    
    Resource expected = ResourcesTestData.getExpectedCustomResource();
    
    String updateResourceEndpoint = "eholdings/resources/" + STUB_CUSTOM_RESOURCE_ID;

    Resource resource = RestAssured
      .given()
      .spec(getRequestSpecification())
      .header(CONTENT_TYPE_HEADER)
      .body(readFile("requests/kb-ebsco/resource/put-custom-resource.json"))
      .when()
      .put(updateResourceEndpoint)
      .then()
      .statusCode(HttpStatus.SC_OK)
      .extract().as(Resource.class);
    
    compareResource(resource, expected);

    verify(1, putRequestedFor(new UrlPathPattern(new RegexPattern(CUSTOM_RESOURCE_ENDPOINT), true))
      .withRequestBody(equalToJson(readFile("requests/rmapi/resources/put-custom-resource-is-selected-multiple-attributes.json"))));
  }
  
  @Test
  public void shouldReturn422WhenManagedResourceIsNotSelectedAndTryToUpdateOtherFields() throws URISyntaxException, IOException {
    String stubResponseFile = "responses/rmapi/resources/get-managed-resource-updated-response.json";

    mockConfiguration(CONFIGURATION_STUB_FILE, getWiremockUrl());

    stubFor(
      get(new UrlPathPattern(new RegexPattern(MANAGED_RESOURCE_ENDPOINT), false))
        .willReturn(new ResponseDefinitionBuilder().withBody(readFile(stubResponseFile))));
    
    String updateResourceEndpoint = "eholdings/resources/" + STUB_MANAGED_RESOURCE_ID;
    
    RestAssured.given()
      .spec(getRequestSpecification())
      .header(CONTENT_TYPE_HEADER)
      .when()
      .body(readFile("requests/kb-ebsco/resource/put-managed-resource-not-selected-update-fields.json"))
      .put(updateResourceEndpoint)
      .then()
      .statusCode(HttpStatus.SC_UNPROCESSABLE_ENTITY);
  }
  
  @Test
  public void shouldReturn422WhenInvalidUrlIsProvidedForCustomResource() throws URISyntaxException, IOException {
    String stubResponseFile = "responses/rmapi/resources/get-custom-resource-updated-response.json";

    mockConfiguration(CONFIGURATION_STUB_FILE, getWiremockUrl());

    stubFor(
      get(new UrlPathPattern(new RegexPattern(CUSTOM_RESOURCE_ENDPOINT), false))
        .willReturn(new ResponseDefinitionBuilder().withBody(readFile(stubResponseFile))));
    
    String updateResourceEndpoint = "eholdings/resources/" + STUB_CUSTOM_RESOURCE_ID;
    
    RestAssured.given()
      .spec(getRequestSpecification())
      .header(CONTENT_TYPE_HEADER)
      .when()
      .body(readFile("requests/kb-ebsco/resource/put-custom-resource-invalid-url.json"))
      .put(updateResourceEndpoint)
      .then()
      .statusCode(HttpStatus.SC_UNPROCESSABLE_ENTITY);
  }
  
  @Test
  public void shouldReturn422WhenNameIsNotProvidedForCustomResource() throws URISyntaxException, IOException {
    String stubResponseFile = "responses/rmapi/resources/get-custom-resource-updated-response.json";

    mockConfiguration(CONFIGURATION_STUB_FILE, getWiremockUrl());

    stubFor(
      get(new UrlPathPattern(new RegexPattern(CUSTOM_RESOURCE_ENDPOINT), false))
        .willReturn(new ResponseDefinitionBuilder().withBody(readFile(stubResponseFile))));
    
    String updateResourceEndpoint = "eholdings/resources/" + STUB_CUSTOM_RESOURCE_ID;
    
    RestAssured.given()
      .spec(getRequestSpecification())
      .header(CONTENT_TYPE_HEADER)
      .when()
      .body(readFile("requests/kb-ebsco/resource/put-custom-resource-null-name.json"))
      .put(updateResourceEndpoint)
      .then()
      .statusCode(HttpStatus.SC_UNPROCESSABLE_ENTITY);
  }
  
  @Test
  public void shouldReturn422WhenPublicationTypeIsNotProvidedForCustomResource() throws URISyntaxException, IOException {
    String stubResponseFile = "responses/rmapi/resources/get-custom-resource-updated-response.json";

    mockConfiguration(CONFIGURATION_STUB_FILE, getWiremockUrl());

    stubFor(
      get(new UrlPathPattern(new RegexPattern(CUSTOM_RESOURCE_ENDPOINT), false))
        .willReturn(new ResponseDefinitionBuilder().withBody(readFile(stubResponseFile))));
    
    String updateResourceEndpoint = "eholdings/resources/" + STUB_CUSTOM_RESOURCE_ID;
    
    RestAssured.given()
      .spec(getRequestSpecification())
      .header(CONTENT_TYPE_HEADER)
      .when()
      .body(readFile("requests/kb-ebsco/resource/put-custom-resource-null-publication-type.json"))
      .put(updateResourceEndpoint)
      .then()
      .statusCode(HttpStatus.SC_UNPROCESSABLE_ENTITY);
  }

  private void compareResource(Resource actual, Resource expected) {
    assertThat(actual.getData().getType(), equalTo(expected.getData().getType()));
    assertThat(actual.getData().getId(), equalTo(expected.getData().getId()));
    assertThat(actual.getData().getAttributes().getIsSelected(), equalTo(expected.getData().getAttributes().getIsSelected()));
    assertThat(actual.getData().getAttributes().getProxy().getId(), equalTo(expected.getData().getAttributes().getProxy().getId()));
    assertThat(actual.getData().getAttributes().getProxy().getInherited(), equalTo(expected.getData().getAttributes().getProxy().getInherited()));
    assertThat(actual.getData().getAttributes().getCoverageStatement(), equalTo(expected.getData().getAttributes().getCoverageStatement()));
    assertThat(actual.getData().getAttributes().getVisibilityData().getIsHidden(), equalTo(expected.getData().getAttributes().getVisibilityData().getIsHidden()));
    assertThat(actual.getData().getAttributes().getCustomEmbargoPeriod().getEmbargoUnit(), equalTo(expected.getData().getAttributes().getCustomEmbargoPeriod().getEmbargoUnit()));
    assertThat(actual.getData().getAttributes().getCustomEmbargoPeriod().getEmbargoValue(), equalTo(expected.getData().getAttributes().getCustomEmbargoPeriod().getEmbargoValue()));
    assertThat(actual.getData().getAttributes().getDescription(), equalTo(expected.getData().getAttributes().getDescription()));
    assertThat(actual.getData().getAttributes().getEdition(), equalTo(expected.getData().getAttributes().getEdition()));
    assertThat(actual.getData().getAttributes().getIsPeerReviewed(), equalTo(expected.getData().getAttributes().getIsPeerReviewed()));
    assertThat(actual.getData().getAttributes().getIsTitleCustom(), equalTo(expected.getData().getAttributes().getIsTitleCustom()));
    assertThat(actual.getData().getAttributes().getPublisherName(), equalTo(expected.getData().getAttributes().getPublisherName()));
    assertThat(actual.getData().getAttributes().getName(), equalTo(expected.getData().getAttributes().getName()));
    assertThat(actual.getData().getAttributes().getPublicationType(), equalTo(expected.getData().getAttributes().getPublicationType()));
    assertThat(actual.getData().getAttributes().getUrl(), equalTo(expected.getData().getAttributes().getUrl()));
  }

  @Test
  public void shouldPostResourceToRMAPI() throws IOException, URISyntaxException {
    String stubTitleResponseFile = "responses/rmapi/resources/get-resource-by-id-success-response.json";
    String stubPackageResponseFile = "responses/rmapi/packages/get-custom-package-by-id-response.json";
    String stubPackageResourcesFile = "responses/rmapi/resources/get-resources-by-package-id-response.json";
    String postStubRequest = "requests/kb-ebsco/resource/post-resources-request.json";

    String expectedResourceFile = "responses/kb-ebsco/resources/expected-resource-by-id.json";

    EqualToJsonPattern putRequestBodyPattern = new EqualToJsonPattern(readFile("requests/rmapi/resources/select-resource-request.json"),
      true, true);

    String wiremockUrl = getWiremockUrl();
    TestUtil.mockConfiguration(CONFIGURATION_STUB_FILE, wiremockUrl);

    mockPackageResources(stubPackageResourcesFile);
    mockPackage(stubPackageResponseFile);
    mockTitle(stubTitleResponseFile);
    mockResource(stubTitleResponseFile);

    WireMock.stubFor(
      WireMock.put(new UrlPathPattern(new RegexPattern(MANAGED_RESOURCE_ENDPOINT), true))
        .withRequestBody(putRequestBodyPattern)
        .willReturn(new ResponseDefinitionBuilder()
          .withStatus(HttpStatus.SC_NO_CONTENT)));

    String actualResponse = RestAssured.given()
      .spec(getRequestSpecification())
      .header("Content-type","application/vnd.api+json")
      .body(readFile(postStubRequest))
      .when()
      .post("eholdings/resources")
      .then()
      .statusCode(200).extract().asString();

    JSONAssert.assertEquals(
      readFile(expectedResourceFile), actualResponse, false);

    verify(1, putRequestedFor(new UrlPathPattern(new EqualToPattern(MANAGED_RESOURCE_ENDPOINT), false))
      .withRequestBody(putRequestBodyPattern));
  }

  @Test
  public void shouldReturn404IfTitleOrPackageIsNotFound() throws IOException, URISyntaxException {
    String postStubRequest= "requests/kb-ebsco/resource/post-resources-request.json";

    String wiremockUrl = getWiremockUrl();
    TestUtil.mockConfiguration(CONFIGURATION_STUB_FILE, wiremockUrl);

    WireMock.stubFor(
      WireMock.get(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/titles.*"), true))
        .willReturn(new ResponseDefinitionBuilder()
          .withStatus(404)));

    WireMock.stubFor(
      WireMock.get(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors/" + STUB_MANAGED_VENDOR_ID + "/packages/" + STUB_MANAGED_PACKAGE_ID), true))
        .willReturn(new ResponseDefinitionBuilder()
          .withStatus(404)));

    RestAssured.given()
      .spec(getRequestSpecification())
      .header("Content-type","application/vnd.api+json")
      .body(readFile(postStubRequest))
      .when()
      .post("eholdings/resources")
      .then()
      .statusCode(404);
  }

  @Test
  public void shouldReturn422IfPackageIsNotCustom() throws IOException, URISyntaxException {
    String stubTitleResponseFile = "responses/rmapi/resources/get-resource-by-id-success-response.json";
    String stubPackageResponseFile = "responses/rmapi/packages/get-package-by-id-for-resource.json";
    String stubPackageResourcesFile = "responses/rmapi/resources/get-resources-by-package-id-response.json";
    String postStubRequest= "requests/kb-ebsco/resource/post-resources-request.json";

    String wiremockUrl = getWiremockUrl();
    TestUtil.mockConfiguration(CONFIGURATION_STUB_FILE, wiremockUrl);

    mockPackageResources(stubPackageResourcesFile);
    mockPackage(stubPackageResponseFile);
    mockTitle(stubTitleResponseFile);

    RestAssured.given()
      .spec(getRequestSpecification())
      .header("Content-type","application/vnd.api+json")
      .body(readFile(postStubRequest))
      .when()
      .post("eholdings/resources")
      .then()
      .statusCode(422);
  }
  
  @Test
  public void shouldSendDeleteRequestForResourceAssociatedWithCustomPackage() throws IOException, URISyntaxException {
    String stubResponseFile = "responses/rmapi/resources/get-custom-resource-updated-response.json";
    mockConfiguration(CONFIGURATION_STUB_FILE, getWiremockUrl());

    UrlPathPattern resourceUrlPattern = new UrlPathPattern(new EqualToPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors/" + STUB_CUSTOM_VENDOR_ID + "/packages/" + STUB_CUSTOM_PACKAGE_ID + "/titles/" + STUB_CUSTOM_TITLE_ID), false);
    EqualToJsonPattern putBodyPattern = new EqualToJsonPattern("{\"isSelected\":false}", true, true);

    stubFor(
      get(resourceUrlPattern)
        .willReturn(new ResponseDefinitionBuilder()
          .withBody(readFile(stubResponseFile))));

    stubFor(
      put(resourceUrlPattern)
        .withRequestBody(putBodyPattern)
        .willReturn(new ResponseDefinitionBuilder()
          .withStatus(HttpStatus.SC_NO_CONTENT)));

    RestAssured.given()
      .spec(getRequestSpecification())
      .when()
      .delete("eholdings/resources/" + STUB_CUSTOM_RESOURCE_ID)
      .then()
      .statusCode(HttpStatus.SC_NO_CONTENT);

    verify(1, putRequestedFor(resourceUrlPattern)
      .withRequestBody(putBodyPattern));
  }

  @Test
  public void shouldReturn400WhenResourceIdIsInvalid() {
    RestAssured.given()
      .spec(getRequestSpecification())
      .when()
      .delete("eholdings/resources/abc-def")
      .then()
      .statusCode(HttpStatus.SC_BAD_REQUEST);
  }

  @Test
  public void shouldReturn400WhenTryingToDeleteResourceAssociatedWithManagedPackage() throws URISyntaxException, IOException {
    String stubResponseFile = "responses/rmapi/resources/get-managed-resource-updated-response.json";
    mockConfiguration(CONFIGURATION_STUB_FILE, getWiremockUrl());

    UrlPathPattern resourceUrlPattern = new UrlPathPattern(new EqualToPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors/" + STUB_MANAGED_VENDOR_ID + "/packages/" + STUB_MANAGED_PACKAGE_ID + "/titles/" + STUB_MANAGED_TITLE_ID), false);
    stubFor(
        get(resourceUrlPattern)
          .willReturn(new ResponseDefinitionBuilder()
            .withBody(readFile(stubResponseFile))));

    RestAssured.given()
      .spec(getRequestSpecification())
      .when()
      .delete("eholdings/resources/" + STUB_MANAGED_RESOURCE_ID)
      .then()
      .statusCode(HttpStatus.SC_BAD_REQUEST);
  }

  private void mockPackageResources(String stubPackageResourcesFile) throws IOException, URISyntaxException {
    UrlPathPattern packagesResourcesPattern = new UrlPathPattern(
      new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors/" + STUB_MANAGED_VENDOR_ID + "/packages/" + STUB_MANAGED_PACKAGE_ID + "/titles.*"), true);
    WireMock.stubFor(
      WireMock.get(packagesResourcesPattern)
        .willReturn(new ResponseDefinitionBuilder()
          .withBody(TestUtil.readFile(stubPackageResourcesFile))));
  }

  private void mockPackage(String responseFile) throws IOException, URISyntaxException {
    WireMock.stubFor(
      WireMock.get(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors/" + STUB_MANAGED_VENDOR_ID + "/packages/" + STUB_MANAGED_PACKAGE_ID), true))
        .willReturn(new ResponseDefinitionBuilder()
          .withBody(TestUtil.readFile(responseFile))));
  }

  private void mockResource(String responseFile) throws IOException, URISyntaxException {
    WireMock.stubFor(
      WireMock.get(new UrlPathPattern(new RegexPattern(MANAGED_RESOURCE_ENDPOINT), false))
        .willReturn(new ResponseDefinitionBuilder()
          .withBody(readFile(responseFile))));
  }

  private void mockTitle(String responseFile) throws IOException, URISyntaxException {
    WireMock.stubFor(
      WireMock.get(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/titles.*"), true))
        .willReturn(new ResponseDefinitionBuilder()
          .withBody(readFile(responseFile))));
  }

  private void mockVendor(String responseFile) throws IOException, URISyntaxException {
    WireMock.stubFor(
      WireMock.get(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors/" + STUB_MANAGED_VENDOR_ID), true))
        .willReturn(new ResponseDefinitionBuilder()
          .withBody(TestUtil.readFile(responseFile))));
  }
}
