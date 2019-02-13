package org.folio.rest.impl;

import java.io.IOException;

import com.github.tomakehurst.wiremock.common.Slf4jNotifier;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.restassured.RestAssured;
import io.restassured.http.Header;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.apache.http.HttpStatus;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import org.folio.config.RMAPIConfiguration;
import org.folio.config.cache.VendorIdCacheKey;
import org.folio.config.cache.VertxCache;
import org.folio.http.HttpConsts;
import org.folio.rest.util.RestConstants;
import org.folio.spring.SpringContextUtil;
import org.folio.util.TestUtil;

/**
 * Base test class for tests that use wiremock and vertx http servers,
 * test that inherits this class must use VertxUnitRunner as test runner
 */
public abstract class WireMockTestBase {

  protected Logger log = LoggerFactory.getLogger(this.getClass());

  protected static final Header CONTENT_TYPE_HEADER = new Header(HttpConsts.CONTENT_TYPE_HEADER, HttpConsts.JSON_API_TYPE);
  protected static final String STUB_CUSTOMER_ID = "TEST_CUSTOMER_ID";
  protected static final String CONFIGURATION_STUB_FILE = "responses/kb-ebsco/configuration/get-configuration.json";
  protected int port = TestSetUpHelper.getPort();
  protected String host = TestSetUpHelper.getHost();
  protected Vertx vertx = TestSetUpHelper.getVertx();

  private static boolean needTeardown;

  @Rule
  public TestRule watcher = new TestWatcher() {

    @Override
    protected void starting(Description description) {
      log.info("********** Running test method: {}.{} ********** ", description.getClassName(), description.getMethodName());
    }

  };
  @Autowired
  @Qualifier("rmApiConfigurationCache")
  private VertxCache<String, RMAPIConfiguration> configurationCache;
  @Autowired
  @Qualifier("vendorIdCache")
  private VertxCache<VendorIdCacheKey, Long> vendorIdCache;

  @Rule
  public WireMockRule userMockServer = new WireMockRule(
    WireMockConfiguration.wireMockConfig()
      .dynamicPort()
      .notifier(new Slf4jNotifier(true)));

  @BeforeClass
  public static void setUpClass() throws IOException {
    if(!TestSetUpHelper.isStarted()){
      TestSetUpHelper.startVertxAndPostgres();
      needTeardown = true;
    }
    else {
      needTeardown = false;
    }
  }

  @AfterClass
  public static void tearDownClass() {
    if(needTeardown){
      TestSetUpHelper.stopVertxAndPostgres();
    }
  }

  @Before
  public void setUp() throws Exception {
    SpringContextUtil.autowireDependenciesFromFirstContext(this, vertx);
    configurationCache.invalidateAll();
    vendorIdCache.invalidateAll();
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

  protected ValidatableResponse getResponse(String resourcePath) {
    return RestAssured.given()
      .spec(getRequestSpecification())
      .when()
      .get(resourcePath)
      .then();
  }

  protected ExtractableResponse<Response> getOkResponse(String resourcePath) {
    return getResponseWithStatus(resourcePath, HttpStatus.SC_OK);
  }

  protected ExtractableResponse<Response> getResponseWithStatus(String resourcePath, int expectedStatus) {
    return RestAssured.given()
      .spec(getRequestSpecification())
      .when()
      .get(resourcePath)
      .then()
      .statusCode(expectedStatus).extract();
  }

  protected <T> T sendPutRequestAndRetrieveResponse(String endpoint, String putBody, Class<T> clazz){
    return RestAssured
      .given()
      .spec(getRequestSpecification())
      .header(CONTENT_TYPE_HEADER)
      .body(putBody)
      .when()
      .put(endpoint)
      .then()
      .statusCode(HttpStatus.SC_OK)
      .extract().as(clazz);
  }

  protected <T> T sendPostRequestAndRetrieveResponse(String endpoint, String postBody, Class<T> clazz){
    return RestAssured
      .given()
      .spec(getRequestSpecification())
      .header(CONTENT_TYPE_HEADER)
      .body(postBody)
      .when()
      .post(endpoint)
      .then()
      .statusCode(HttpStatus.SC_OK)
      .extract().as(clazz);
  }
}
