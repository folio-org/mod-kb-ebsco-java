package org.folio.rest.impl;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.matching.AnythingPattern;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import com.github.tomakehurst.wiremock.matching.UrlPathPattern;
import com.google.common.io.Files;
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
import org.folio.rest.tools.utils.NetworkUtils;
import org.folio.rest.util.HeaderConstants;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.equalTo;

@RunWith(VertxUnitRunner.class)
public class EholdingsConfigurationTest {
  private static final Header TENANT_HEADER = new Header(HeaderConstants.OKAPI_TENANT_HEADER, "fs");
  private static final Header TOKEN_HEADER = new Header(HeaderConstants.OKAPI_TOKEN_HEADER, "TEST_OKAPI_TOKEN");

  private static RequestSpecification spec;
  private static int port;
  private static String host;

  @org.junit.Rule
  public WireMockRule userMockServer = new WireMockRule(
    WireMockConfiguration.wireMockConfig()
      .dynamicPort()
      .notifier(new ConsoleNotifier(true)));

  @BeforeClass
  public static void setUpClass(final TestContext context) throws Exception {
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
  public void shouldReturnConfigurationOnGet() throws IOException, URISyntaxException {
    String stubResponseFilename = "responses/get-configuration.json";
    String stubCustomerId = "TEST_CUSTOMER_ID";
    String stubUrl = "https://api.ebsco.io";
    String expectedMaskedApiKey = "****************************************";

    WireMock.stubFor(
      WireMock.get(new UrlPathPattern(new AnythingPattern(), false))
        .withHeader(TENANT_HEADER.getName(), new EqualToPattern(TENANT_HEADER.getValue()))
        .withHeader(TOKEN_HEADER.getName(), new EqualToPattern(TOKEN_HEADER.getValue()))
        .willReturn(new ResponseDefinitionBuilder()
          .withBody(readFile(stubResponseFilename))));

    RestAssured.given()
      .spec(spec).port(port)
      .header(new Header(HeaderConstants.OKAPI_URL_HEADER, host + ":" + userMockServer.port()))
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .when()
      .get("eholdings/configuration")
      .then()
      .statusCode(200)
      .body("data.attributes.customerId", equalTo(stubCustomerId))
      .body("data.attributes.apiKey", equalTo(expectedMaskedApiKey))
      .body("data.attributes.rmapiBaseUrl", equalTo(stubUrl));
  }

  @Test
  public void shouldReturn500IfConfigurationIsInvalid() {
    WireMock.stubFor(
      WireMock.get(new UrlPathPattern(new AnythingPattern(), false))
        .withHeader(TENANT_HEADER.getName(), new EqualToPattern(TENANT_HEADER.getValue()))
        .withHeader(TOKEN_HEADER.getName(), new EqualToPattern(TOKEN_HEADER.getValue()))
        .willReturn(new ResponseDefinitionBuilder()
          .withBody("{}")));

    RestAssured.given()
      .spec(spec).port(port)
      .header(new Header(HeaderConstants.OKAPI_URL_HEADER, host + ":" + userMockServer.port()))
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .when()
      .get("eholdings/configuration")
      .then()
      .statusCode(500);
  }

  @Test
  public void shouldReturn400OnGetWithoutUrlHeader() throws IOException, URISyntaxException {
    RestAssured.given()
      .spec(spec).port(port)
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .when()
      .get("eholdings/configuration")
      .then()
      .statusCode(400);
  }

  @Test
  public void shouldReturn400OnGetWithoutTenant() throws IOException, URISyntaxException {
    RestAssured.given()
      .spec(spec).port(port)
      .header(new Header(HeaderConstants.OKAPI_URL_HEADER, host + ":" + userMockServer.port()))
      .header(TOKEN_HEADER)
      .when()
      .get("eholdings/configuration")
      .then()
      .statusCode(400);
  }

  @Test
  public void shouldReturn400OnGetWithoutToken() throws IOException, URISyntaxException {
    RestAssured.given()
      .spec(spec).port(port)
      .header(new Header(HeaderConstants.OKAPI_URL_HEADER, host + ":" + userMockServer.port()))
      .header(TENANT_HEADER)
      .when()
      .get("eholdings/configuration")
      .then()
      .statusCode(400);
  }

  private String readFile(String filename) throws IOException, URISyntaxException {
    return Files.toString(new File(this.getClass().getClassLoader()
      .getResource(filename).toURI()), StandardCharsets.UTF_8);
  }
}
