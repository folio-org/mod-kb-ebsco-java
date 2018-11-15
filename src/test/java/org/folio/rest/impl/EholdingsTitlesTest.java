package org.folio.rest.impl;

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

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.isEmptyOrNullString;

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
}
