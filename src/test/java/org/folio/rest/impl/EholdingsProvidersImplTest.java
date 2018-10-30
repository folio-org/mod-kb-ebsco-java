package org.folio.rest.impl;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.matching.RegexPattern;
import com.github.tomakehurst.wiremock.matching.UrlPathPattern;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.Header;
import io.restassured.specification.RequestSpecification;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.folio.config.cache.RMAPIConfigurationCache;
import org.folio.rest.RestVerticle;
import org.folio.rest.tools.utils.NetworkUtils;
import org.folio.rest.util.RestConstants;
import org.folio.util.TestUtil;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.URISyntaxException;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@RunWith(VertxUnitRunner.class)
public class EholdingsProvidersImplTest {
  private static final Header TENANT_HEADER = new Header(RestConstants.OKAPI_TENANT_HEADER, "fs");
  private static final Header TOKEN_HEADER = new Header(RestConstants.OKAPI_TOKEN_HEADER, "TEST_OKAPI_TOKEN");
  private static final String STUB_CUSTOMER_ID = "TEST_CUSTOMER_ID";
  private static RequestSpecification spec;
  private static int port;
  private static String host;

  @org.junit.Rule
  public WireMockRule userMockServer = new WireMockRule(
    WireMockConfiguration.wireMockConfig()
      .dynamicPort()
      .notifier(new ConsoleNotifier(true)));

  @BeforeClass
  public static void setUpClass(final TestContext context) {
    Vertx vertx = Vertx.vertx();
    vertx.exceptionHandler(context.exceptionHandler());
    port = NetworkUtils.nextFreePort();
    host = "http://localhost";

    DeploymentOptions restVerticleDeploymentOptions = new DeploymentOptions()
      .setConfig(new JsonObject().put("http.port", port));
    vertx.deployVerticle(RestVerticle.class.getName(), restVerticleDeploymentOptions, context.asyncAssertSuccess());

    spec = new RequestSpecBuilder()
      .setBaseUri(host + ":" + port)
      .build();
  }

  @Before
  public void setUp() {
    RMAPIConfigurationCache.getInstance().invalidate();
  }

  @Test
  public void shouldReturnProvidersOnGet() throws IOException, URISyntaxException {
    String stubResponseFile = "responses/rmapi/vendors/get-vendors-response.json";
    int expectedTotalResults = 115;
    String id = "131872";
    String name = "Editions de L'Université de Bruxelles";
    int packagesTotal = 1;
    int packagesSelected = 0;
    boolean supportsCustomPackages = false;
    String token = "sampleToken";

    String wiremockUrl = host + ":" + userMockServer.port();
    TestUtil.mockConfiguration("responses/configuration/get-configuration.json", wiremockUrl);
    WireMock.stubFor(
      WireMock.get(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors.*"), true))
        .willReturn(new ResponseDefinitionBuilder()
          .withBody(TestUtil.readFile(stubResponseFile))));

    RestAssured.given()
      .spec(spec).port(port)
      .header(new Header(RestConstants.OKAPI_URL_HEADER, wiremockUrl))
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .when()
      .get("eholdings/providers?q=e&page=1&sort=name")
      .then()
      .statusCode(200)
      .body("meta.totalResults" , equalTo(expectedTotalResults))
      .body("data[0].type", equalTo("providers"))
      .body("data[0].id", equalTo(id))
      .body("data[0].attributes.name", equalTo(name))
      .body("data[0].attributes.packagesTotal", equalTo(packagesTotal))
      .body("data[0].attributes.packagesSelected", equalTo(packagesSelected))
      .body("data[0].attributes.supportsCustomPackages", equalTo(supportsCustomPackages))
      .body("data[0].attributes.providerToken.value", equalTo(token));
  }

  @Test
  public void shouldReturnErrorIfParameterInvalid() {
    RestAssured.given()
      .spec(spec).port(port)
      .header(new Header(RestConstants.OKAPI_URL_HEADER, "http://localhost:8080"))
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .when()
      .get("eholdings/providers?q=e&count=1000")
      .then()
      .statusCode(400)
      .body("errors.first.title" , notNullValue());
  }

  @Test
  public void shouldReturn500IfRMApiReturnsError() throws IOException, URISyntaxException {
    String wiremockUrl = host + ":" + userMockServer.port();
    TestUtil.mockConfiguration("responses/configuration/get-configuration.json", wiremockUrl);
    WireMock.stubFor(
      WireMock.get(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors.*"), true))
        .willReturn(new ResponseDefinitionBuilder()
          .withStatus(500)));

    RestAssured.given()
      .spec(spec).port(port)
      .header(new Header(RestConstants.OKAPI_URL_HEADER, wiremockUrl))
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .when()
      .get("eholdings/providers?q=e&count=1")
      .then()
      .statusCode(500)
      .body("errors.first.title" , notNullValue());
  }
}
