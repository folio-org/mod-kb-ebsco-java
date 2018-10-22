package org.folio.rest.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.folio.rest.RestVerticle;
import org.folio.rest.jaxrs.model.Configs;
import org.folio.rest.tools.utils.NetworkUtils;
import org.folio.rest.util.RestConstants;
import org.folio.util.TestUtil;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.URISyntaxException;

import static org.hamcrest.Matchers.equalTo;

@RunWith(VertxUnitRunner.class)
public class EholdingsStatusTest {
  private static final Header TENANT_HEADER = new Header(RestConstants.OKAPI_TENANT_HEADER, "fs");
  private static final Header TOKEN_HEADER = new Header(RestConstants.OKAPI_TOKEN_HEADER, "TEST_OKAPI_TOKEN");

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

    DeploymentOptions restVerticleDeploymentOptions = new DeploymentOptions().setConfig(new JsonObject().put("http.port", port));
    vertx.deployVerticle(RestVerticle.class.getName(), restVerticleDeploymentOptions, context.asyncAssertSuccess());

    spec = new RequestSpecBuilder()
      .setBaseUri(host + ":" + port)
      .build();
  }

  @Test
  public void shouldReturnTrueWhenRMAPIRequestCompletesWith200Status() throws IOException, URISyntaxException {
    String wiremockUrl = host + ":" + userMockServer.port();

    ObjectMapper mapper = new ObjectMapper();
    Configs configurations = mapper.readValue(TestUtil.getFile("responses/configuration/get-configuration.json"), Configs.class);
    configurations.getConfigs().get(0).setValue(wiremockUrl);

    WireMock.stubFor(
      WireMock.get(new UrlPathPattern(new RegexPattern("/configurations/entries.*"), true))
        .willReturn(new ResponseDefinitionBuilder()
          .withBody(mapper.writeValueAsString(configurations))));

    WireMock.stubFor(
      WireMock.get(new UrlPathPattern(new RegexPattern("/rm/rmaccounts.*"), true))
        .willReturn(new ResponseDefinitionBuilder()
          .withBody(TestUtil.readFile("responses/rmapi/get-vendors-response.json"))));

    RestAssured.given()
      .spec(spec).port(port)
      .header(new Header(RestConstants.OKAPI_URL_HEADER, wiremockUrl))
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .when()
      .get("eholdings/status")
      .then()
      .statusCode(200)
      .body("data.attributes.isConfigurationValid", equalTo(true));
  }

  @Test
  public void shouldReturnFalseWhenRMAPIRequestCompletesWithErrorStatus() throws IOException, URISyntaxException {
    String wiremockUrl = host + ":" + userMockServer.port();

    ObjectMapper mapper = new ObjectMapper();
    Configs configurations = mapper.readValue(TestUtil.getFile("responses/configuration/get-configuration.json"), Configs.class);
    configurations.getConfigs().get(0).setValue(wiremockUrl);

    WireMock.stubFor(
      WireMock.get(new UrlPathPattern(new RegexPattern("/configurations/entries.*"), true))
        .willReturn(new ResponseDefinitionBuilder()
          .withBody(mapper.writeValueAsString(configurations))));

    WireMock.stubFor(
      WireMock.get(new UrlPathPattern(new RegexPattern("/rm/rmaccounts.*"), true))
        .willReturn(new ResponseDefinitionBuilder().withStatus(401)));

    RestAssured.given()
      .spec(spec).port(port)
      .header(new Header(RestConstants.OKAPI_URL_HEADER, wiremockUrl))
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .when()
      .get("eholdings/status")
      .then()
      .statusCode(200)
      .body("data.attributes.isConfigurationValid", equalTo(false));
  }

  @Test
  public void shouldReturn500OnInvalidOkapiUrl() throws IOException, URISyntaxException {
    RestAssured.given()
      .spec(spec).port(port)
      .header(new Header(RestConstants.OKAPI_URL_HEADER, "wrongUrl^"))
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .when()
      .get("eholdings/status")
      .then()
      .statusCode(500);
  }
}
