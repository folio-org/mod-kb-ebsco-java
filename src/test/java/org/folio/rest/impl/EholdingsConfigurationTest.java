package org.folio.rest.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.matching.AnythingPattern;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
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
import org.folio.rest.jaxrs.model.Config;
import org.folio.rest.jaxrs.model.Configs;
import org.folio.rest.jaxrs.model.Configuration;
import org.folio.rest.tools.utils.NetworkUtils;
import org.folio.rest.util.RestConstants;
import org.folio.util.TestUtil;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.hamcrest.Matchers.equalTo;

@RunWith(VertxUnitRunner.class)
public class EholdingsConfigurationTest {
  private static final Header TENANT_HEADER = new Header(RestConstants.OKAPI_TENANT_HEADER, "fs");
  private static final Header TOKEN_HEADER = new Header(RestConstants.OKAPI_TOKEN_HEADER, "TEST_OKAPI_TOKEN");
  private static final Header CONTENT_TYPE_HEADER = new Header("Content-Type", "application/vnd.api+json");

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

  @Before
  public void setUp() throws Exception {
    RMAPIConfigurationCache.getInstance().invalidate();
  }

  @Test
  public void shouldReturnConfigurationOnGet() throws IOException, URISyntaxException {
    String stubResponseFilename = "responses/configuration/get-configuration.json";
    String stubCustomerId = "TEST_CUSTOMER_ID";
    String stubUrl = "https://api.ebsco.io";
    String expectedMaskedApiKey = "****************************************";
    String wiremockUrl = host + ":" + userMockServer.port();

    WireMock.stubFor(
      WireMock.get(new UrlPathPattern(new AnythingPattern(), false))
        .withHeader(TENANT_HEADER.getName(), new EqualToPattern(TENANT_HEADER.getValue()))
        .withHeader(TOKEN_HEADER.getName(), new EqualToPattern(TOKEN_HEADER.getValue()))
        .willReturn(new ResponseDefinitionBuilder()
          .withBody(TestUtil.readFile(stubResponseFilename))));

    RestAssured.given()
      .spec(spec).port(port)
      .header(new Header(RestConstants.OKAPI_URL_HEADER, wiremockUrl))
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
  public void shouldSendPostConfigurationRequestsOnPutWhenConfigurationSetForTheFirstTime() throws IOException, URISyntaxException {
    mockConfigurationUpdate("{\"configs\": []}");
    String wiremockUrl = host + ":" + userMockServer.port();

    ObjectMapper mapper = new ObjectMapper();
    Configuration configuration = mapper.readValue(TestUtil.getFile("requests/kb-ebsko/put-configuration.json"), Configuration.class);
    configuration.getData().getAttributes().setRmapiBaseUrl(wiremockUrl);

    RestAssured.given()
      .spec(spec).port(port)
      .header(new Header(RestConstants.OKAPI_URL_HEADER, wiremockUrl))
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(CONTENT_TYPE_HEADER)
      .body(mapper.writeValueAsString(configuration))
      .when()
      .put("eholdings/configuration")
      .then()
      .statusCode(200);

    WireMock.verify(1, postRequestedFor(urlEqualTo("/configurations/entries"))
      .withRequestBody(equalToJson(TestUtil.readFile("requests/configuration/post-api-key-configuration.json"))));
    WireMock.verify(1, postRequestedFor(urlEqualTo("/configurations/entries"))
      .withRequestBody(equalToJson(TestUtil.readFile("requests/configuration/post-customer-id-configuration.json"))));

    Config config = mapper.readValue(TestUtil.readFile("requests/configuration/post-url-configuration.json"), Config.class);
    config.setValue(wiremockUrl);
    WireMock.verify(1, postRequestedFor(urlEqualTo("/configurations/entries"))
      .withRequestBody(equalToJson(mapper.writeValueAsString(config))));
  }

  @Test
  public void shouldDeleteOldConfigurationOnPutWhenConfigurationExists() throws IOException, URISyntaxException {
    ObjectMapper mapper = new ObjectMapper();
    String wiremockUrl = host + ":" + userMockServer.port();

    String configsString = TestUtil.readFile("responses/configuration/get-configuration.json");
    Configs configs = mapper.readValue(configsString, Configs.class);
    List<String> existingIds = configs.getConfigs().stream()
      .map(Config::getId)
      .collect(Collectors.toList());

    mockConfigurationUpdate(configsString);

    Configuration configuration = mapper.readValue(TestUtil.getFile("requests/kb-ebsko/put-configuration.json"), Configuration.class);
    configuration.getData().getAttributes().setRmapiBaseUrl(wiremockUrl);

    RestAssured.given()
      .spec(spec).port(port)
      .header(new Header(RestConstants.OKAPI_URL_HEADER, wiremockUrl))
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(CONTENT_TYPE_HEADER)
      .body(mapper.writeValueAsString(configuration))
      .when()
      .put("eholdings/configuration")
      .then()
      .statusCode(200);

    for (String id : existingIds) {
      WireMock.verify(1, deleteRequestedFor(urlEqualTo("/configurations/entries/" + id)));
    }
  }

  @Test
  public void shouldReturn422OnPutWhenVerificationOfConfigurationFailed() throws IOException, URISyntaxException {
    ObjectMapper mapper = new ObjectMapper();
    String wiremockUrl = host + ":" + userMockServer.port();
    WireMock.stubFor(
      WireMock.get(new UrlPathPattern(new RegexPattern("/rm/rmaccounts.*"), true))
        .willReturn(new ResponseDefinitionBuilder().withStatus(403)));

    Configuration configuration = mapper.readValue(TestUtil.getFile("requests/kb-ebsko/put-configuration.json"), Configuration.class);
    configuration.getData().getAttributes().setRmapiBaseUrl(wiremockUrl);

    RestAssured.given()
      .spec(spec).port(port)
      .header(new Header(RestConstants.OKAPI_URL_HEADER, wiremockUrl))
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(CONTENT_TYPE_HEADER)
      .body(mapper.writeValueAsString(configuration))
      .when()
      .put("eholdings/configuration")
      .then()
      .statusCode(422);
  }

  @Test
  public void shouldReturn500OnServerError() throws IOException, URISyntaxException {
    ObjectMapper mapper = new ObjectMapper();
    String wiremockUrl = "wronghost";
    RestAssured.given()
      .spec(spec).port(port)
      .header(new Header(RestConstants.OKAPI_URL_HEADER, wiremockUrl))
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(CONTENT_TYPE_HEADER)
      .body(TestUtil.readFile("requests/kb-ebsko/put-configuration.json"))
      .when()
      .put("eholdings/configuration")
      .then()
      .statusCode(500);
  }

  private void mockConfigurationUpdate(String configsString) {
    WireMock.stubFor(
      WireMock.get(new UrlPathPattern(new RegexPattern("/configurations/entries.*"), true))
        .willReturn(new ResponseDefinitionBuilder()
          .withBody(configsString)));

    WireMock.stubFor(
      WireMock.get(new UrlPathPattern(new RegexPattern("/rm/rmaccounts.*"), true))
        .willReturn(new ResponseDefinitionBuilder()
          .withBody("{\"totalResults\": 0, \"vendors\": []}")));

    WireMock.stubFor(
      WireMock.delete(new UrlPathPattern(new RegexPattern("/configurations/entries.*"), true))
        .willReturn(new ResponseDefinitionBuilder()
          .withStatus(200)));

    WireMock.stubFor(
      WireMock.post(new UrlPathPattern(new RegexPattern("/configurations/entries.*"), true))
        .willReturn(new ResponseDefinitionBuilder()
          .withStatus(200)));
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
      .header(new Header(RestConstants.OKAPI_URL_HEADER, host + ":" + userMockServer.port()))
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .when()
      .get("eholdings/configuration")
      .then()
      .statusCode(500);
  }

  @Test
  public void shouldReturn400OnGetWithoutUrlHeader() {
    RestAssured.given()
      .spec(spec).port(port)
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .when()
      .get("eholdings/configuration")
      .then()
      .statusCode(400);
  }
}
