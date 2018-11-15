package org.folio.rest.impl;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.matching.RegexPattern;
import com.github.tomakehurst.wiremock.matching.UrlPathPattern;
import io.restassured.RestAssured;
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

@RunWith(VertxUnitRunner.class)
public class EHoldingsRootProxyImplTest {
  private final String configurationStubFile = "responses/configuration/get-configuration.json";
  private static final String STUB_CUSTOMER_ID = "TEST_CUSTOMER_ID";
  private static final String ROOT_PROXY_ID = "root-proxy";
  private static final String ROOT_PROXY_TYPE = "rootProxies";

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
  }

  @Before
  public void setUp() {
    RMAPIConfigurationCache.getInstance().invalidate();
  }
  
  @Test
  public void shouldReturnRootProxyWhenCustIdAndAPIKeyAreValid() throws IOException, URISyntaxException {
    String stubResponseFile = "responses/rmapi/proxiescustomlabels/get-root-proxy-custom-labels-success-response.json";

    String expectedRootProxyID = "<n>";

    String wiremockUrl = host + ":" + userMockServer.port();
    TestUtil.mockConfiguration(configurationStubFile, wiremockUrl);
    WireMock.stubFor(
      WireMock.get(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/"), true))
        .willReturn(new ResponseDefinitionBuilder()
          .withBody(TestUtil.readFile(stubResponseFile))));

    RequestSpecification requestSpecification = getRequestSpecification();

    RestAssured.given()
      .spec(requestSpecification)
      .when()
      .get("eholdings/root-proxy")
      .then()
      .statusCode(200)
      .body("data.id", equalTo(ROOT_PROXY_ID))
      .body("data.type", equalTo(ROOT_PROXY_TYPE))
      .body("data.attributes.id", equalTo(ROOT_PROXY_ID))
      .body("data.attributes.proxyTypeId", equalTo(expectedRootProxyID));
  }
  
  @Test
  public void shouldReturnUnauthorizedWhenRMAPIRequestCompletesWith401ErrorStatus() throws IOException, URISyntaxException {
    String wiremockUrl = host + ":" + userMockServer.port();
    TestUtil.mockConfiguration(configurationStubFile, wiremockUrl);
    WireMock.stubFor(
      WireMock.get(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/"), true))
        .willReturn(new ResponseDefinitionBuilder().withStatus(401)));
    
    RequestSpecification requestSpecification = getRequestSpecification();

    RestAssured.given()
      .spec(requestSpecification)
      .when()
      .get("eholdings/root-proxy")
      .then()
      .statusCode(403);
  }
  
  @Test
  public void shouldReturnUnauthorizedWhenRMAPIRequestCompletesWith403ErrorStatus() throws IOException, URISyntaxException {
    String wiremockUrl = host + ":" + userMockServer.port();
    TestUtil.mockConfiguration(configurationStubFile, wiremockUrl);
    WireMock.stubFor(
      WireMock.get(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/"), true))
        .willReturn(new ResponseDefinitionBuilder().withStatus(403)));
    
    RequestSpecification requestSpecification = getRequestSpecification();

    RestAssured.given()
      .spec(requestSpecification)
      .when()
      .get("eholdings/root-proxy")
      .then()
      .statusCode(403);
  }
  
  private RequestSpecification getRequestSpecification() {
    return TestUtil.getRequestSpecificationBuilder(host + ":" + port)
      .addHeader(RestConstants.OKAPI_URL_HEADER,   host + ":" + userMockServer.port())
    .build();
  }

}

