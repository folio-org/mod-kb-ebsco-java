package org.folio.rest.impl;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CREATED;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.Matchers.notNullValue;

import static org.folio.rest.util.RestConstants.JSON_API_TYPE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.restassured.RestAssured;
import io.restassured.http.Header;
import io.restassured.http.Headers;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.unit.TestContext;
import org.apache.http.protocol.HTTP;
import org.junit.Before;
import org.junit.BeforeClass;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import org.folio.cache.VertxCache;
import org.folio.config.cache.VendorIdCacheKey;
import org.folio.holdingsiq.model.Configuration;
import org.folio.holdingsiq.model.PackageByIdData;
import org.folio.holdingsiq.model.Title;
import org.folio.holdingsiq.model.VendorById;
import org.folio.rmapi.cache.PackageCacheKey;
import org.folio.rmapi.cache.ResourceCacheKey;
import org.folio.rmapi.cache.TitleCacheKey;
import org.folio.rmapi.cache.VendorCacheKey;
import org.folio.spring.SpringContextUtil;
import org.folio.test.util.TestBase;

/**
 * Base test class for tests that use wiremock and vertx http servers,
 * test that inherits this class must use VertxUnitRunner as test runner
 */
public abstract class WireMockTestBase extends TestBase {

  protected static final Header CONTENT_TYPE_HEADER = new Header(HTTP.CONTENT_TYPE, JSON_API_TYPE);
  protected static final String STUB_CUSTOMER_ID = "TEST_CUSTOMER_ID";
  protected static final String STUB_API_KEY = "TEST_API_KEY";

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

  @BeforeClass
  public static void setUpClass(TestContext context) {
    configProperties.put("spring.configuration", "org.folio.spring.config.TestConfig");
    TestBase.setUpClass(context);
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

  protected ExtractableResponse<Response> getWithStatus(String endpoint, int code, Header... headers) {
    return RestAssured.given()
      .spec(this.getRequestSpecification())
      .headers(new Headers(headers))
      .when()
      .get(endpoint)
      .then()
      .log()
      .ifValidationFails()
      .statusCode(code)
      .extract();
  }

  protected ExtractableResponse<Response> putWithOk(String endpoint, String putBody) {
    return putWithStatus(endpoint, putBody, SC_OK, CONTENT_TYPE_HEADER);
  }

  protected ExtractableResponse<Response> postWithOk(String endpoint, String postBody) {
    return postWithStatus(endpoint, postBody, SC_OK, CONTENT_TYPE_HEADER);
  }

  protected ExtractableResponse<Response> postWithCreated(String endpoint, String postBody) {
    return postWithStatus(endpoint, postBody, SC_CREATED, CONTENT_TYPE_HEADER);
  }

  @Override
  protected ExtractableResponse<Response> putWithNoContent(String resourcePath, String putBody, Header... headers) {
    return super.putWithNoContent(resourcePath, putBody, addContentHeader(headers));
  }

  @Override
  protected ExtractableResponse<Response> putWithStatus(String resourcePath, String putBody, int expectedStatus,
                                                        Header... headers) {
    return super.putWithStatus(resourcePath, putBody, expectedStatus, addContentHeader(headers));
  }

  @Override
  protected ExtractableResponse<Response> postWithStatus(String resourcePath, String postBody, int expectedStatus,
                                                         Header... headers) {
    return super.postWithStatus(resourcePath, postBody, expectedStatus, addContentHeader(headers));
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

  private Header[] addContentHeader(Header[] headers) {
    List<Header> headerList = new ArrayList<>(Arrays.asList(headers));
    headerList.add(CONTENT_TYPE_HEADER);
    return headerList.toArray(new Header[0]);
  }
}
