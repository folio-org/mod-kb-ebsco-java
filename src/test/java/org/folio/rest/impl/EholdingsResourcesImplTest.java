package org.folio.rest.impl;

import static org.folio.rest.util.RestConstants.PACKAGES_TYPE;
import static org.folio.rest.util.RestConstants.PROVIDERS_TYPE;
import static org.folio.rest.util.RestConstants.TITLES_TYPE;
import static org.folio.util.TestUtil.readFile;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.folio.util.TestUtil.mockConfiguration;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;

import org.folio.rest.jaxrs.model.HasOneRelationship;
import org.folio.rest.jaxrs.model.JsonapiError;
import org.folio.rest.jaxrs.model.RelationshipData;
import org.folio.rest.jaxrs.model.Resource;
import org.folio.util.TestUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.JSONAssert;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.RegexPattern;
import com.github.tomakehurst.wiremock.matching.UrlPathPattern;

import io.restassured.RestAssured;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class EholdingsResourcesImplTest extends WireMockTestBase {

  private static final String STUB_RESOURCE_ID = "583-4345-762169";
  private static final String VENDOR_ID = "583";
  private static final String PACKAGE_ID = "4345";
  private static final String TITLE_ID = "762169";

  @Test
  public void shouldReturnResourceWhenValidId() throws IOException, URISyntaxException {
    String stubResponseFile = "responses/rmapi/resources/get-resource-by-id-success-response.json";

    String wiremockUrl = getWiremockUrl();
    mockConfiguration(CONFIGURATION_STUB_FILE, wiremockUrl);
    mockResource(stubResponseFile);

    String resourceByIdEndpoint = "eholdings/resources/" + STUB_RESOURCE_ID;

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
    String resourceByIdEndpoint = "eholdings/resources/" + STUB_RESOURCE_ID + "?include=title";

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
          .withId(TITLE_ID)));
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

    String resourceByIdEndpoint = "eholdings/resources/" + STUB_RESOURCE_ID + "?include=provider";

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
          .withId(VENDOR_ID)));
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

    String resourceByIdEndpoint = "eholdings/resources/" + STUB_RESOURCE_ID + "?include=package";

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
          .withId(VENDOR_ID + "-" + PACKAGE_ID)));
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

    String resourceByIdEndpoint = "eholdings/resources/" + STUB_RESOURCE_ID + "?include=package,title,provider";

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
          .withId(VENDOR_ID + "-" + PACKAGE_ID)))
      .withProvider(new HasOneRelationship()
        .withData(new RelationshipData()
          .withType(PROVIDERS_TYPE)
          .withId(String.valueOf(VENDOR_ID))))
      .withTitle(new HasOneRelationship()
        .withData(new RelationshipData()
          .withType(TITLES_TYPE)
          .withId(TITLE_ID)));
    JSONAssert.assertEquals(
      mapper.writeValueAsString(resource), actualResponse, false);
  }

  @Test
  public void shouldReturn404WhenRMAPINotFoundOnResourceGet() throws IOException, URISyntaxException {
    String stubResponseFile = "responses/rmapi/resources/get-resource-by-id-not-found-response.json";

    mockConfiguration(CONFIGURATION_STUB_FILE, getWiremockUrl());
    stubFor(
        get(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors/" + VENDOR_ID + "/packages/" + PACKAGE_ID + "/titles.*"), true))
        .willReturn(new ResponseDefinitionBuilder()
            .withBody(readFile(stubResponseFile))
            .withStatus(404)));

    String resourceByIdEndpoint = "eholdings/resources/" + STUB_RESOURCE_ID;

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
      get(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors/" + VENDOR_ID + "/packages/" + PACKAGE_ID + "/titles.*"), true))
        .willReturn(new ResponseDefinitionBuilder()
          .withStatus(500)));

    String resourceByIdEndpoint = "eholdings/resources/" + STUB_RESOURCE_ID;

    RestAssured.given()
      .spec(getRequestSpecification())
      .when()
      .when()
      .get(resourceByIdEndpoint)
      .then()
      .statusCode(500);
  }

  private void mockPackage(String responseFile) throws IOException, URISyntaxException {
    WireMock.stubFor(
      WireMock.get(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors/" + VENDOR_ID + "/packages/" + PACKAGE_ID), true))
        .willReturn(new ResponseDefinitionBuilder()
          .withBody(TestUtil.readFile(responseFile))));
  }

  private void mockResource(String responseFile) throws IOException, URISyntaxException {
    WireMock.stubFor(
      WireMock.get(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors/" + VENDOR_ID + "/packages/" + PACKAGE_ID + "/titles/" + TITLE_ID), true))
        .willReturn(new ResponseDefinitionBuilder()
          .withBody(readFile(responseFile))));
  }

  private void mockVendor(String responseFile) throws IOException, URISyntaxException {
    WireMock.stubFor(
      WireMock.get(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors/" + VENDOR_ID), true))
        .willReturn(new ResponseDefinitionBuilder()
          .withBody(TestUtil.readFile(responseFile))));
  }
}
