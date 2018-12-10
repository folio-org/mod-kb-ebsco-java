package org.folio.rest.impl;

import static org.folio.util.TestUtil.STUB_TENANT;

import com.github.tomakehurst.wiremock.common.Slf4jNotifier;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.restassured.RestAssured;
import io.restassured.http.Header;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import org.folio.config.cache.RMAPIConfigurationCache;
import org.folio.http.HttpConsts;
import org.folio.rest.RestVerticle;
import org.folio.rest.tools.utils.NetworkUtils;
import org.folio.rest.util.RestConstants;
import org.folio.util.TestUtil;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base test class for tests that use wiremock and vertx http servers,
 * test that inherits this class must use VertxUnitRunner as test runner
 */
public abstract class WireMockTestBase {

  protected Logger log = LoggerFactory.getLogger(this.getClass());

  protected static final Header CONTENT_TYPE_HEADER = new Header(HttpConsts.CONTENT_TYPE_HEADER, HttpConsts.JSON_API_TYPE);
  protected static final String STUB_CUSTOMER_ID = "TEST_CUSTOMER_ID";
  protected static final String CONFIGURATION_STUB_FILE = "responses/configuration/get-configuration.json";
  protected static int port;
  protected static String host;
  protected static final Vertx vertx = Vertx.vertx();

  @Rule
  public TestRule watcher = new TestWatcher() {

    @Override
    protected void starting(Description description) {
      log.info("********** Running test method: {}.{} ********** ", description.getClassName(), description.getMethodName());
    }

  };

  @Rule
  public WireMockRule userMockServer = new WireMockRule(
    WireMockConfiguration.wireMockConfig()
      .dynamicPort()
      .notifier(new Slf4jNotifier(true)));

  @BeforeClass
  public static void setUpClass(final TestContext context) {
    vertx.exceptionHandler(context.exceptionHandler());
    port = NetworkUtils.nextFreePort();
    host = "http://localhost";

    DeploymentOptions restVerticleDeploymentOptions = new DeploymentOptions().setConfig(new JsonObject().put("http.port", port));
    vertx.deployVerticle(RestVerticle.class.getName(), restVerticleDeploymentOptions, context.asyncAssertSuccess());
  }

  @Before
  public void setUp() throws Exception {
    new RMAPIConfigurationCache(vertx, 100L).invalidate(STUB_TENANT);
  }

  /**
   * Creates RestAssured specification that is configured with data from Vertx and Wiremock servers
   */
  protected RequestSpecification getRequestSpecification() {
    return TestUtil.getRequestSpecificationBuilder(host + ":" + port)
      .addHeader(RestConstants.OKAPI_URL_HEADER, getWiremockUrl())
      .setPort(port)
      .build();
  }

  /**
   * Returns url of Wiremock server used in this test
   */
  protected String getWiremockUrl() {
    return host + ":" + userMockServer.port();
  }

  protected ExtractableResponse<Response> getResponseWithStatus(String resourcePath, int expectedStatus) {
    return RestAssured.given()
      .spec(getRequestSpecification())
      .when()
      .get(resourcePath)
      .then()
      .statusCode(expectedStatus).extract();
  }

}
