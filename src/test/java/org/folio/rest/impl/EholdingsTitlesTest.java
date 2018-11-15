package org.folio.rest.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.RegexPattern;
import com.github.tomakehurst.wiremock.matching.UrlPathPattern;
import io.restassured.RestAssured;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.folio.util.TestUtil;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

@RunWith(VertxUnitRunner.class)
public class EholdingsTitlesTest extends WireMockTestBase {

  @Test
  public void shouldReturnTitlesOnGet() throws IOException, URISyntaxException {
    String stubResponseFile = "responses/rmapi/titles/searchTitles.json";

    String wiremockUrl = getWiremockUrl();
    TestUtil.mockConfiguration(CONFIGURATION_STUB_FILE, wiremockUrl);
    WireMock.stubFor(
      WireMock.get(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/titles.*"), true))
        .willReturn(new ResponseDefinitionBuilder()
          .withBody(TestUtil.readFile(stubResponseFile))));

    RestAssured.given()
      .spec(getRequestSpecification())
      .when()
      .get("eholdings/titles?page=1&filter[name]=Mind&sort=name")
      .then()
      .statusCode(200)

      .body("meta.totalResults", equalTo(8766))
      .body("data[0].type", equalTo("titles"))
      .body("data[0].id", equalTo("1175655"))
      .body("data[0].attributes.name", equalTo("The $1 Million Reason to Change Your Mind"))
      .body("data[0].attributes.publisherName", isEmptyOrNullString())
      .body("data[0].attributes.isTitleCustom", equalTo(false))
      .body("data[0].attributes.subjects[0].type", equalTo("BISAC"))
      .body("data[0].attributes.subjects[0].subject", equalTo("BUSINESS & ECONOMICS / Small Business"))

      .body("data[0].attributes.identifiers[0].id", equalTo("7209484"))
      .body("data[0].attributes.identifiers[1].id", equalTo("978-1-74216-894-4"))
      .body("data[0].attributes.identifiers[2].id", equalTo("978-0-7303-7792-4"))
      /* List of identifiers returned below from RM API get filtered and sorted to only support types ISSN/ISBN and subtypes Print/Online */
      .body("data[0].attributes.identifiers[0].type", equalTo("ISSN"))
      .body("data[0].attributes.identifiers[1].type", equalTo("ISBN"))
      .body("data[0].attributes.identifiers[2].type", equalTo("ISBN"))

      .body("data[0].attributes.identifiers[0].subtype", equalTo("Print"))
      .body("data[0].attributes.identifiers[1].subtype", equalTo("Print"))
      .body("data[0].attributes.identifiers[2].subtype", equalTo("Online"))

      .body("data[0].attributes.publicationType", equalTo("Book"))

      .body("data[0].relationships.resources.meta.included", equalTo(false));
  }

  @Test
  public void shouldReturn400IfCountOutOfRange() {
    RestAssured.given()
      .spec(getRequestSpecification())
      .when()
      .get("eholdings/titles?count=1000&page=1&filter[name]=Mind&sort=name")
      .then()
      .statusCode(400);
  }

  @Test
  public void shouldReturn500WhenRMApiReturns500Error() throws IOException, URISyntaxException {
    String wiremockUrl = getWiremockUrl();
    TestUtil.mockConfiguration(CONFIGURATION_STUB_FILE, wiremockUrl);
    WireMock.stubFor(
      WireMock.get(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/titles.*"), true))
        .willReturn(new ResponseDefinitionBuilder()
          .withStatus(500)));

    RestAssured.given()
      .spec(getRequestSpecification())
      .when()
      .get("eholdings/titles?filter[name]=news")
      .then()
      .statusCode(500);
  }

  @Test
  public void shouldReturnTitleWhenValidId() throws IOException, URISyntaxException {
    String stubResponseFile = "responses/rmapi/titles/get-title-by-id-response.json";

    String wiremockUrl = host + ":" + userMockServer.port();
    TestUtil.mockConfiguration("responses/configuration/get-configuration.json", wiremockUrl);
    WireMock.stubFor(
        WireMock.get(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/titles.*"), true))
        .willReturn(new ResponseDefinitionBuilder()
            .withBody(TestUtil.readFile(stubResponseFile))));

    String titleByIdEndpoint = "eholdings/titles/" + STUB_TITLE_ID;

    RestAssured.given()
    .spec(spec)
    .port(port)
    .header(new Header(RestConstants.OKAPI_URL_HEADER, wiremockUrl))
    .header(TENANT_HEADER)
    .header(TOKEN_HEADER)
    .when()
    .get(titleByIdEndpoint)
    .then()
    .statusCode(200)
    .body("data.type",equalTo("titles"))
    .body("data.id", equalTo("985846"))
    .body("data.attributes.name", equalTo("F. Scott Fitzgerald's The Great Gatsby (Great Gatsby)"))
    .body("data.attributes.publisherName", equalTo("Chelsea House Publishers"))
    .body("data.attributes.isTitleCustom", equalTo(false))
    .body("data.attributes.subjects[0].type", equalTo("BISAC"))
    .body("data.attributes.subjects[0].subject", equalTo("LITERARY CRITICISM / American / General"))
    .body("data.attributes.identifiers[0].id", equalTo("978-0-7910-3651-8"))
    .body("data.attributes.identifiers[1].id", equalTo("978-0-585-24731-1"))
     /*
         * List of identifiers returned below from RM API get filtered and sorted to
         * only support types ISSN/ISBN and subtypes Print/Online
     */
    .body("data.attributes.identifiers[0].type", equalTo("ISBN"))
    .body("data.attributes.identifiers[1].type",equalTo("ISBN"))
    .body("data.attributes.identifiers[0].subtype", equalTo("Print"))
    .body("data.attributes.identifiers[1].subtype",equalTo("Online"))
    .body("data.attributes.publicationType", equalTo("Book"))
    .body("data.attributes.edition", isEmptyOrNullString())
    .body("data.attributes.description", isEmptyOrNullString())
    .body("data.attributes.isPeerReviewed", equalTo(false))
    .body("data.attributes.contributors[0].type", equalTo("author"))
    .body("data.attributes.contributors[0].contributor", equalTo("Bloom, Harold"));
  }

  @Test
  public void shouldReturn404WhenRMAPINotFoundOnTitleGet() throws IOException, URISyntaxException {
    String stubResponseFile = "responses/rmapi/titles/get-title-by-id-not-found-response.json";

    String wiremockUrl = host + ":" + userMockServer.port();
    TestUtil.mockConfiguration("responses/configuration/get-configuration.json", wiremockUrl);
    WireMock.stubFor(
        WireMock.get(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/titles.*"), true))
        .willReturn(new ResponseDefinitionBuilder()
            .withBody(TestUtil.readFile(stubResponseFile))
            .withStatus(404)));

    String titleByIdEndpoint = "eholdings/titles/" + STUB_TITLE_ID;

    JsonapiError error = RestAssured.given()
    .spec(spec)
    .port(port)
    .header(new Header(RestConstants.OKAPI_URL_HEADER, wiremockUrl))
    .header(TENANT_HEADER)
    .header(TOKEN_HEADER)
    .when()
    .get(titleByIdEndpoint)
    .then()
    .statusCode(404)
    .extract().as(JsonapiError.class);

    assertThat(error.getErrors().get(0).getTitle(), equalTo("Title not found"));
  }

}
