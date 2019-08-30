package org.folio.rest.impl;

import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.apache.http.HttpStatus.SC_UNPROCESSABLE_ENTITY;
import static org.hamcrest.Matchers.equalTo;

import static org.folio.common.ListUtils.mapItems;
import static org.folio.test.util.TestUtil.STUB_TENANT;
import static org.folio.test.util.TestUtil.STUB_TOKEN;
import static org.folio.test.util.TestUtil.getFile;
import static org.folio.test.util.TestUtil.readFile;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.matching.AnythingPattern;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import com.github.tomakehurst.wiremock.matching.RegexPattern;
import com.github.tomakehurst.wiremock.matching.UrlPathPattern;

import io.restassured.RestAssured;
import io.restassured.http.Header;
import io.vertx.ext.unit.junit.VertxUnitRunner;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.folio.rest.jaxrs.model.Config;
import org.folio.rest.jaxrs.model.Configs;
import org.folio.rest.jaxrs.model.Configuration;
import org.folio.rest.util.RestConstants;

@RunWith(VertxUnitRunner.class)
public class EholdingsConfigurationTest extends WireMockTestBase {
  private static final Header TENANT_HEADER = new Header(RestConstants.OKAPI_TENANT_HEADER, "fs");
  private static final Header TOKEN_HEADER = new Header(RestConstants.OKAPI_TOKEN_HEADER, "TEST_OKAPI_TOKEN");

  @Test
  public void shouldReturnConfigurationOnGet() throws IOException, URISyntaxException {
    String stubResponseFilename = "responses/kb-ebsco/configuration/get-configuration.json";
    String stubCustomerId = "TEST_CUSTOMER_ID";
    String stubUrl = "https://api.ebsco.io";
    String expectedMaskedApiKey = "****************************************";

    stubFor(
      get(new UrlPathPattern(new AnythingPattern(), false))
        .withHeader(TENANT_HEADER.getName(), new EqualToPattern(TENANT_HEADER.getValue()))
        .withHeader(TOKEN_HEADER.getName(), new EqualToPattern(TOKEN_HEADER.getValue()))
        .willReturn(new ResponseDefinitionBuilder()
          .withBody(readFile(stubResponseFilename))));

    RestAssured.given()
      .spec(getRequestSpecification())
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
    String wiremockUrl = getWiremockUrl();

    ObjectMapper mapper = new ObjectMapper();
    Configuration configuration = mapper.readValue(getFile("requests/kb-ebsco/put-configuration.json"), Configuration.class);
    configuration.getData().getAttributes().setRmapiBaseUrl(wiremockUrl);

    RestAssured.given()
      .spec(getRequestSpecification())
      .header(CONTENT_TYPE_HEADER)
      .body(mapper.writeValueAsString(configuration))
      .when()
      .put("eholdings/configuration")
      .then()
      .statusCode(200);

    verify(1, postRequestedFor(urlEqualTo("/configurations/entries"))
      .withRequestBody(equalToJson(readFile("requests/kb-ebsco/configuration/post-api-key-configuration.json"))));
    verify(1, postRequestedFor(urlEqualTo("/configurations/entries"))
      .withRequestBody(equalToJson(readFile("requests/kb-ebsco/configuration/post-customer-id-configuration.json"))));

    Config config = mapper.readValue(readFile("requests/kb-ebsco/configuration/post-url-configuration.json"), Config.class);
    config.setValue(wiremockUrl);
    verify(1, postRequestedFor(urlEqualTo("/configurations/entries"))
      .withRequestBody(equalToJson(mapper.writeValueAsString(config))));
  }

  @Test
  public void shouldDeleteOldConfigurationOnPutWhenConfigurationExists() throws IOException, URISyntaxException {
    ObjectMapper mapper = new ObjectMapper();

    String configsString = readFile("responses/kb-ebsco/configuration/get-configuration.json");
    Configs configs = mapper.readValue(configsString, Configs.class);
    List<String> existingIds = mapItems(configs.getConfigs(), Config::getId);

    mockConfigurationUpdate(configsString);

    Configuration configuration = mapper.readValue(getFile("requests/kb-ebsco/put-configuration.json"), Configuration.class);
    configuration.getData().getAttributes().setRmapiBaseUrl(getWiremockUrl());

    putWithOk("eholdings/configuration", mapper.writeValueAsString(configuration));

    for (String id : existingIds) {
      verify(1, deleteRequestedFor(urlEqualTo("/configurations/entries/" + id)));
    }
  }

  @Test
  public void shouldReturn422OnPutWhenVerificationOfConfigurationFailed() throws IOException, URISyntaxException {
    ObjectMapper mapper = new ObjectMapper();
    stubFor(
      get(new UrlPathPattern(new RegexPattern("/rm/rmaccounts.*"), true))
        .willReturn(new ResponseDefinitionBuilder().withStatus(403)));

    Configuration configuration = mapper.readValue(getFile("requests/kb-ebsco/put-configuration.json"), Configuration.class);
    configuration.getData().getAttributes().setRmapiBaseUrl(getWiremockUrl());

    putWithStatus("eholdings/configuration", mapper.writeValueAsString(configuration), SC_UNPROCESSABLE_ENTITY);
  }

  @Test
  public void shouldReturn500OnServerError() throws IOException, URISyntaxException {
    String wiremockUrl = "wronghost";
    RestAssured.given()
      .spec(getRequestSpecification())
      .header(new Header(RestConstants.OKAPI_URL_HEADER, wiremockUrl))
      .header(CONTENT_TYPE_HEADER)
      .body(readFile("requests/kb-ebsco/put-configuration.json"))
      .when()
      .put("eholdings/configuration")
      .then()
      .statusCode(500);
  }

  private void mockConfigurationUpdate(String configsString) {
    stubFor(
      get(new UrlPathPattern(new RegexPattern("/configurations/entries.*"), true))
        .willReturn(new ResponseDefinitionBuilder()
          .withBody(configsString)));

    stubFor(
      get(new UrlPathPattern(new RegexPattern("/rm/rmaccounts.*"), true))
        .willReturn(new ResponseDefinitionBuilder()
          .withBody("{\"totalResults\": 0, \"vendors\": []}")));

    stubFor(
      delete(new UrlPathPattern(new RegexPattern("/configurations/entries.*"), true))
        .willReturn(new ResponseDefinitionBuilder()
          .withStatus(200)));

    stubFor(
      post(new UrlPathPattern(new RegexPattern("/configurations/entries.*"), true))
        .willReturn(new ResponseDefinitionBuilder()
          .withStatus(200)));
  }

  @Test
  public void shouldReturn500IfConfigurationIsInvalid() {
    stubFor(
      get(new UrlPathPattern(new AnythingPattern(), false))
        .withHeader(TENANT_HEADER.getName(), new EqualToPattern(TENANT_HEADER.getValue()))
        .withHeader(TOKEN_HEADER.getName(), new EqualToPattern(TOKEN_HEADER.getValue()))
        .willReturn(new ResponseDefinitionBuilder()
          .withBody("{}")));

    RestAssured.given()
      .spec(getRequestSpecification())
      .when()
      .get("eholdings/configuration")
      .then()
      .statusCode(500);
  }

  @Test
  public void shouldReturn400OnGetWithoutUrlHeader() {
    RestAssured.given()
      .header(RestConstants.OKAPI_TENANT_HEADER, STUB_TENANT)
      .header(RestConstants.OKAPI_TOKEN_HEADER, STUB_TOKEN)
      .baseUri("http://localhost")
      .port(port)
      .header(new Header(RestConstants.OKAPI_URL_HEADER, null))
      .when()
      .get("eholdings/configuration")
      .then()
      .statusCode(400);
  }
}
