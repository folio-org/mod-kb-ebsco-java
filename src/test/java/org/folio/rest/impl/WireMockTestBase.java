package org.folio.rest.impl;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CREATED;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.folio.repository.kbcredentials.KbCredentialsTableConstants.KB_CREDENTIALS_TABLE_NAME;
import static org.folio.rest.util.RestConstants.JSON_API_TYPE;
import static org.folio.util.KbTestUtil.clearDataFromTable;
import static org.hamcrest.Matchers.notNullValue;

import io.restassured.RestAssured;
import io.restassured.http.Header;
import io.restassured.http.Headers;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import io.vertx.ext.unit.TestContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.http.HttpStatus;
import org.apache.http.protocol.HTTP;
import org.folio.cache.VertxCache;
import org.folio.config.cache.VendorIdCacheKey;
import org.folio.holdingsiq.model.Configuration;
import org.folio.holdingsiq.model.PackageByIdData;
import org.folio.holdingsiq.model.Title;
import org.folio.holdingsiq.model.VendorById;
import org.folio.okapi.common.XOkapiHeaders;
import org.folio.rmapi.cache.PackageCacheKey;
import org.folio.rmapi.cache.ResourceCacheKey;
import org.folio.rmapi.cache.TitleCacheKey;
import org.folio.rmapi.cache.VendorCacheKey;
import org.folio.spring.SpringContextUtil;
import org.folio.test.util.TestBase;
import org.junit.Before;
import org.junit.BeforeClass;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * Base test class for tests that use wiremock and vertx http servers,
 * test that inherits this class must use VertxUnitRunner as test runner.
 */
public abstract class WireMockTestBase extends TestBase {

  public static final String JOHN_ID = "47d9ca93-9c82-4d6a-8d7f-7a73963086b9";
  public static final String JOHN_GROUP_ID = "b4b5e97a-0a99-4db9-97df-4fdf406ec74d";
  public static final String JOHN_USERNAME = "john_doe";
  public static final Header JOHN_USER_ID_HEADER = new Header(XOkapiHeaders.USER_ID, JOHN_ID);
  public static final String JANE_ID = "781fce7d-5cf5-490d-ad89-a3d192eb526c";
  public static final String JANE_GROUP_ID = "4bb563d9-3f9d-4e1e-8d1d-04e75666d68f";
  public static final String JANE_USERNAME = "jane_doe";
  public static final Header JANE_USER_ID_HEADER = new Header(XOkapiHeaders.USER_ID, JANE_ID);

  protected static final Header CONTENT_TYPE_HEADER = new Header(HTTP.CONTENT_TYPE, JSON_API_TYPE);
  protected static final String STUB_CUSTOMER_ID = "TEST_CUSTOMER_ID";
  protected static final String STUB_API_KEY = "TEST_API_KEY";
  protected static final String STUB_CREDENTIALS_ID = "12312312-1231-1231-a111-111111111111";

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
  @Autowired
  private VertxCache<String, String> ucTokenCache;

  @BeforeClass
  public static void setUpClass(TestContext context) {
    configProperties.put("spring.configuration", "org.folio.spring.config.TestConfig");
    TestBase.setUpClass(context);

    // An ad-hoc to clear any records after DB setup but before test execution
    // this should be removed once a proper separation between migration scripts and clean DB is in place
    clearDataFromTable(vertx, KB_CREDENTIALS_TABLE_NAME);
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
    ucTokenCache.invalidateAll();
  }

  protected ExtractableResponse<Response> getWithOk(String endpoint, Header... headers) {
    return getWithStatus(endpoint, HttpStatus.SC_OK, headers);
  }

  protected ExtractableResponse<Response> getWithStatus(String endpoint, int code, Header... headers) {
    return given()
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

  protected ExtractableResponse<Response> putWithOk(String endpoint, String putBody, Header... headers) {
    return super.putWithStatus(endpoint, putBody, SC_OK, addContentHeader(headers));
  }

  protected ExtractableResponse<Response> postWithOk(String endpoint, String postBody) {
    return postWithStatus(endpoint, postBody, SC_OK, CONTENT_TYPE_HEADER);
  }

  protected ExtractableResponse<Response> postWithOk(String endpoint, String postBody, Header... headers) {
    return postWithStatus(endpoint, postBody, SC_OK, addContentHeader(headers));
  }

  protected ExtractableResponse<Response> postWithCreated(String endpoint, String postBody) {
    return postWithStatus(endpoint, postBody, SC_CREATED, CONTENT_TYPE_HEADER);
  }

  protected ExtractableResponse<Response> postWithCreated(String endpoint, String postBody, Header... headers) {
    return postWithStatus(endpoint, postBody, SC_CREATED, addContentHeader(headers));
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

  protected ExtractableResponse<Response> patchWithNoContent(String resourcePath, String patchBody, Header... headers) {
    return patchWithStatus(resourcePath, patchBody, SC_NO_CONTENT, headers);
  }

  protected ExtractableResponse<Response> patchWithStatus(String resourcePath, String patchBody, int expectedStatus,
                                                          Header... headers) {
    return given()
      .spec(getRequestSpecification())
      .header(JSON_CONTENT_TYPE_HEADER)
      .headers(new Headers(addContentHeader(headers)))
      .body(patchBody)
      .when()
      .patch(resourcePath)
      .then()
      .log().ifValidationFails()
      .statusCode(expectedStatus)
      .extract();
  }

  protected ExtractableResponse<Response> deleteWithNoContent(String resourcePath, Header... headers) {
    return deleteWithStatus(resourcePath, HttpStatus.SC_NO_CONTENT, headers);
  }

  protected ExtractableResponse<Response> deleteWithStatus(String resourcePath, int expectedStatus, Header... headers) {
    return given()
      .spec(getRequestSpecification())
      .headers(new Headers(headers))
      .when()
      .delete(resourcePath)
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

  private Header[] addContentHeader(Header[] headers) {
    List<Header> headerList = new ArrayList<>(Arrays.asList(headers));
    headerList.add(CONTENT_TYPE_HEADER);
    return headerList.toArray(new Header[0]);
  }
}
