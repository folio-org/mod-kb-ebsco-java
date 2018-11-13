package org.folio.rest.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.EqualToJsonPattern;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import com.github.tomakehurst.wiremock.matching.UrlPathPattern;
import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.apache.http.HttpStatus;
import org.folio.rmapi.model.PackageData;
import org.folio.util.TestUtil;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.URISyntaxException;

@RunWith(VertxUnitRunner.class)
public class EholdingsPackagesImplTest extends WireMockTestBase {

  private static final String PACKAGE_STUB_FILE = "responses/rmapi/packages/get-package-by-id-response.json";

  @Test
  public void shouldSendDeleteRequestForPackage() throws IOException, URISyntaxException {
    String providerId = "123355";
    String packageId = "2884739";

    String wiremockUrl = getWiremockUrl();
    TestUtil.mockConfiguration(CONFIGURATION_STUB_FILE, wiremockUrl);
    WireMock.stubFor(
      WireMock.get(new UrlPathPattern(new EqualToPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors/" + providerId + "/packages/" + packageId), false))
        .willReturn(new ResponseDefinitionBuilder()
          .withBody(TestUtil.readFile(PACKAGE_STUB_FILE))));

    WireMock.stubFor(
      WireMock.put(new UrlPathPattern(new EqualToPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors/" + providerId + "/packages/" + packageId), false))
        .withRequestBody(new EqualToJsonPattern("{\"isSelected\":false}", true, true))
        .willReturn(new ResponseDefinitionBuilder()
          .withStatus(HttpStatus.SC_NO_CONTENT)));

    RequestSpecification requestSpecification = getRequestSpecification();

    RestAssured.given()
      .spec(requestSpecification)
      .when()
      .delete("eholdings/packages/" + providerId + "-" + packageId)
      .then()
      .statusCode(HttpStatus.SC_NO_CONTENT);
  }

  @Test
  public void shouldReturn400WhenPackageIdIsInvalid() {
    RequestSpecification requestSpecification = getRequestSpecification();
    RestAssured.given()
      .spec(requestSpecification)
      .when()
      .delete("eholdings/packages/abc-def")
      .then()
      .statusCode(HttpStatus.SC_BAD_REQUEST);
  }

  @Test
  public void shouldReturn400WhenPackageIsNotCustom() throws URISyntaxException, IOException {
    String providerId = "123355";
    String packageId = "2884739";

    String wiremockUrl = getWiremockUrl();
    TestUtil.mockConfiguration(CONFIGURATION_STUB_FILE, wiremockUrl);

    ObjectMapper mapper = new ObjectMapper();
    PackageData packageData = mapper.readValue(TestUtil.getFile(PACKAGE_STUB_FILE), PackageData.class);
    packageData.setCustom(false);

    WireMock.stubFor(
      WireMock.get(new UrlPathPattern(new EqualToPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors/" + providerId + "/packages/" + packageId), false))
        .willReturn(new ResponseDefinitionBuilder()
          .withBody(mapper.writeValueAsString(packageData))));

    RequestSpecification requestSpecification = getRequestSpecification();

    RestAssured.given()
      .spec(requestSpecification)
      .when()
      .delete("eholdings/packages/" + providerId + "-" + packageId)
      .then()
      .statusCode(HttpStatus.SC_BAD_REQUEST);
  }
}
