package org.folio.rest.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.io.IOException;
import java.net.URISyntaxException;

import org.folio.rest.jaxrs.model.JsonapiError;
import org.folio.util.TestUtil;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.RegexPattern;
import com.github.tomakehurst.wiremock.matching.UrlPathPattern;

import io.restassured.RestAssured;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class EholdingsResourcesImplTest extends WireMockTestBase {
  
  private static final String STUB_RESOURCE_ID = "583-4345-762169";
  private static final int vendorId = 583;
  private static final int packageId = 4345;

  @Test
  public void shouldReturnResourceWhenValidId() throws IOException, URISyntaxException {
    String stubResponseFile = "responses/rmapi/resources/get-resource-by-id-success-response.json";

    String wiremockUrl = getWiremockUrl();
    TestUtil.mockConfiguration(CONFIGURATION_STUB_FILE, wiremockUrl);
    WireMock.stubFor(
        WireMock.get(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors/" + vendorId + "/packages/" + packageId + "/titles.*"), true))
        .willReturn(new ResponseDefinitionBuilder()
            .withBody(TestUtil.readFile(stubResponseFile))));

    String resourceByIdEndpoint = "eholdings/resources/" + STUB_RESOURCE_ID;

    RestAssured.given()
    .spec(getRequestSpecification())
    .when()
    .get(resourceByIdEndpoint)
    .then()
    .statusCode(200)
    .body("data.type",equalTo("resources"))
    .body("data.id", equalTo("583-4345-762169"))
    .body("data.attributes.isPeerReviewed", equalTo(false))
    .body("data.attributes.isTitleCustom", equalTo(false))
    .body("data.attributes.publisherName", equalTo("Praeger"))
    .body("data.attributes.titleId", equalTo(762169))
    .body("data.attributes.contributors[0].type", equalTo("Author"))
    .body("data.attributes.contributors[0].contributor", equalTo("Beeman, William O."))
    /*
     * List of identifiers returned below from RM API get filtered and sorted to
     * only support types ISSN/ISBN and subtypes Print/Online
     */
    .body("data.attributes.identifiers[0].id", equalTo("978-0-275-98214-0"))
    .body("data.attributes.identifiers[1].id",equalTo("978-0-313-06800-3"))
    .body("data.attributes.identifiers[2].id", equalTo("978-1-282-40979-8"))
    .body("data.attributes.identifiers[0].type", equalTo("ISBN"))
    .body("data.attributes.identifiers[1].type",equalTo("ISBN"))
    .body("data.attributes.identifiers[2].type", equalTo("ISBN"))
    .body("data.attributes.identifiers[0].subtype", equalTo("Print"))
    .body("data.attributes.identifiers[1].subtype",equalTo("Online"))
    .body("data.attributes.identifiers[2].subtype",equalTo("Online"))
    .body("data.attributes.name", equalTo("\"Great Satan\" Vs. the \"Mad Mullahs\": How the United States and Iran Demonize Each Other"))
    .body("data.attributes.publicationType", equalTo("Book"))
    .body("data.attributes.subjects[0].type", equalTo("BISAC"))
    .body("data.attributes.subjects[0].subject", equalTo("SOCIAL SCIENCE / Ethnic Studies / General"))
    .body("data.attributes.customEmbargoPeriod.embargoValue", equalTo(0))
    .body("data.attributes.isPackageCustom", equalTo(false))
    .body("data.attributes.isSelected", equalTo(true))
    .body("data.attributes.isTokenNeeded", equalTo(false))
    .body("data.attributes.locationId", equalTo(2863717))
    .body("data.attributes.managedEmbargoPeriod.embargoValue", equalTo(0))
    .body("data.attributes.packageId", equalTo("583-4345"))
    .body("data.attributes.packageName", equalTo("ABC-CLIO eBook Collection"))
    .body("data.attributes.url", equalTo("https://publisher.abc-clio.com/9780313068003/"))
    .body("data.attributes.providerId", equalTo(583))
    .body("data.attributes.providerName", equalTo("ABC-CLIO"))
    .body("data.attributes.visibilityData.isHidden", equalTo(true))
    .body("data.attributes.visibilityData.reason", equalTo(""))
    .body("data.attributes.managedCoverages[0].beginCoverage", equalTo("2005-01-01"))
    .body("data.attributes.managedCoverages[0].endCoverage", equalTo("2005-12-31"))
    .body("data.attributes.proxy.id", equalTo("<n>"))
    .body("data.attributes.proxy.inherited", equalTo(true))
    .body("data.relationships.provider.meta.included", equalTo(false))
    .body("data.relationships.title.meta.included", equalTo(false))
    .body("data.relationships.package.meta.included", equalTo(false));
  }

  @Test
  public void shouldReturn404WhenRMAPINotFoundOnResourceGet() throws IOException, URISyntaxException {
    String stubResponseFile = "responses/rmapi/resources/get-resource-by-id-not-found-response.json";

    String wiremockUrl = getWiremockUrl();
    TestUtil.mockConfiguration(CONFIGURATION_STUB_FILE, wiremockUrl);
    WireMock.stubFor(
        WireMock.get(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors/" + vendorId + "/packages/" + packageId + "/titles.*"), true))
        .willReturn(new ResponseDefinitionBuilder()
            .withBody(TestUtil.readFile(stubResponseFile))
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
    String wiremockUrl = getWiremockUrl();
    TestUtil.mockConfiguration(CONFIGURATION_STUB_FILE, wiremockUrl);

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
    String wiremockUrl = getWiremockUrl();
    TestUtil.mockConfiguration(CONFIGURATION_STUB_FILE, wiremockUrl);
    WireMock.stubFor(
      WireMock.get(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors/" + vendorId + "/packages/" + packageId + "/titles.*"), true))
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
}
