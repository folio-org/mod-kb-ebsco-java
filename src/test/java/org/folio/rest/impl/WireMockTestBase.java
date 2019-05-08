package org.folio.rest.impl;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.Matchers.notNullValue;

import static org.folio.rest.util.RestConstants.JSON_API_TYPE;

import java.io.IOException;

import com.github.tomakehurst.wiremock.common.Slf4jNotifier;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.restassured.RestAssured;
import io.restassured.http.Header;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.apache.http.protocol.HTTP;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import org.folio.cache.VertxCache;
import org.folio.config.cache.VendorIdCacheKey;
import org.folio.holdingsiq.model.Configuration;
import org.folio.holdingsiq.model.PackageByIdData;
import org.folio.holdingsiq.model.Title;
import org.folio.holdingsiq.model.VendorById;
import org.folio.rest.util.RestConstants;
import org.folio.rmapi.cache.PackageCacheKey;
import org.folio.rmapi.cache.ResourceCacheKey;
import org.folio.rmapi.cache.TitleCacheKey;
import org.folio.rmapi.cache.VendorCacheKey;
import org.folio.spring.SpringContextUtil;
import org.folio.util.TestUtil;

/**
 * Base test class for tests that use wiremock and vertx http servers,
 * test that inherits this class must use VertxUnitRunner as test runner
 */
public abstract class WireMockTestBase {

  protected Logger log = LoggerFactory.getLogger(this.getClass());

  protected static final Header CONTENT_TYPE_HEADER = new Header(HTTP.CONTENT_TYPE, JSON_API_TYPE);
  protected static final String STUB_CUSTOMER_ID = "TEST_CUSTOMER_ID";
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
  private VertxCache<String, Configuration> configurationCache;
  @Autowired
  @Qualifier("vendorIdCache")
  private VertxCache<VendorIdCacheKey, Long> vendorIdCache;
  @Autowired
  private VertxCache<PackageCacheKey, PackageByIdData> packageCache;
  @Autowired
  private VertxCache<VendorCacheKey, VendorById> vendorCache;
  @Autowired
  private VertxCache<ResourceCacheKey, Title> resourceCache;
  @Autowired
  private VertxCache<TitleCacheKey, Title> titleCache;

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
    packageCache.invalidateAll();
    vendorCache.invalidateAll();
    resourceCache.invalidateAll();
    titleCache.invalidateAll();
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

  protected ExtractableResponse<Response> getWithOk(String resourcePath) {
    return getWithStatus(resourcePath, SC_OK);
  }

  protected ExtractableResponse<Response> getWithStatus(String resourcePath, int expectedStatus) {
    return RestAssured.given()
      .spec(getRequestSpecification())
      .when()
      .get(resourcePath)
      .then()
      .log().ifValidationFails()
      .statusCode(expectedStatus).extract();
  }

  protected <T> ExtractableResponse<Response> putWithOk(String endpoint, String putBody){
    return putWithStatus(endpoint, putBody, SC_OK);
  }

  protected <T> ExtractableResponse<Response> putWithStatus(String endpoint, String putBody, int expectedStatus){
    return RestAssured
      .given()
      .spec(getRequestSpecification())
      .header(CONTENT_TYPE_HEADER)
      .body(putBody)
      .when()
      .put(endpoint)
      .then()
      .log().ifValidationFails()
      .statusCode(expectedStatus)
      .extract();
  }

  protected <T> ExtractableResponse<Response> postWithOk(String endpoint, String postBody){
    return postWithStatus(endpoint, postBody, SC_OK);
  }

  protected <T> ExtractableResponse<Response> postWithStatus(String endpoint, String postBody, int expectedStatus){
    return RestAssured
      .given()
      .spec(getRequestSpecification())
      .header(CONTENT_TYPE_HEADER)
      .body(postBody)
      .when()
      .post(endpoint)
      .then()
      .log().ifValidationFails()
      .statusCode(expectedStatus)
      .extract();
  }

  protected <T> ExtractableResponse<Response> deleteWithOk(String endpoint){
    return deleteWithStatus(endpoint, SC_NO_CONTENT);
  }

  protected <T> ExtractableResponse<Response> deleteWithStatus(String endpoint, int expectedStatus){
    return RestAssured
      .given()
      .spec(getRequestSpecification())
      .when()
      .delete(endpoint)
      .then()
      .log().ifValidationFails()
      .statusCode(expectedStatus)
      .extract();
  }


  protected void checkResponseNotEmptyWhenStatusIs400(String path) {
    RestAssured.given()
      .spec(getRequestSpecification())
      .when()
      .get(path)
      .then()
      .statusCode(SC_BAD_REQUEST)
      .body("errors.first.title", notNullValue());
  }
}
