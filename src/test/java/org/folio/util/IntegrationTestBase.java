package org.folio.util;

import static io.restassured.RestAssured.given;
import static org.apache.hc.core5.http.ContentType.APPLICATION_JSON;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CREATED;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.folio.repository.kbcredentials.KbCredentialsTableConstants.KB_CREDENTIALS_TABLE_NAME;
import static org.folio.rest.util.RestConstants.JSON_API_TYPE;
import static org.folio.util.KbCredentialsTestUtil.STUB_USER_ID;
import static org.folio.util.TestUtil.STUB_TENANT;
import static org.folio.util.TestUtil.clearDataFromTable;
import static org.hamcrest.Matchers.notNullValue;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.log.LogDetail;
import io.restassured.http.Header;
import io.restassured.http.Headers;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import io.vertx.core.Vertx;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.http.HttpHeaders;
import org.folio.cache.VertxCache;
import org.folio.okapi.common.XOkapiHeaders;
import org.folio.spring.SpringContextUtil;
import org.folio.spring.config.TestConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Base test class for tests that use WireMock and a Vert.x HTTP server backed by an embedded Postgres instance.
 *
 * <p>
 * Each subclass starts and stops its own Vert.x + Postgres via {@link TestSetUpHelper} in {@code @BeforeAll} /
 * {@code @AfterAll}. If a subclass needs its own {@code @BeforeAll} or {@code @AfterAll}, it must use a different
 * method name to avoid hiding {@link #setUpClass()} / {@link #tearDownClass()}, or call them explicitly.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfig.class)
public abstract class IntegrationTestBase extends WireMockTestBase {

  protected static final Header CONTENT_TYPE_HEADER = new Header(HttpHeaders.CONTENT_TYPE, JSON_API_TYPE);
  protected static final Header JSON_CONTENT_TYPE_HEADER = new Header(HttpHeaders.CONTENT_TYPE,
    APPLICATION_JSON.getMimeType());

  protected static String moduleUrl;
  protected static Vertx vertx;

  @Autowired
  private List<VertxCache<?, ?>> caches;

  @BeforeAll
  public static void setUpClass() {
    var configProperties = Map.of("spring.configuration", "org.folio.spring.config.TestConfig");
    TestSetUpHelper.startVertxAndPostgres(configProperties);
    vertx = TestSetUpHelper.getVertx();
    moduleUrl = TestSetUpHelper.getModuleUrl();

    // An ad-hoc to clear any records after DB setup but before test execution
    // this should be removed once a proper separation between migration scripts and clean DB is in place
    clearDataFromTable(vertx, KB_CREDENTIALS_TABLE_NAME);
  }

  @AfterAll
  public static void tearDownClass() {
    TestSetUpHelper.stopVertxAndPostgres();
  }

  protected static String withInclude(String path, String... includes) {
    var separator = path.contains("?") ? "&" : "?";
    return path + separator + "include=" + String.join(",", includes);
  }

  protected static String withTagFilters(String path, String... tags) {
    var separator = path.contains("?") ? "&" : "?";
    return path + Arrays.stream(tags)
      .map(tag -> "filter[tags]=" + tag)
      .collect(Collectors.joining("&", separator, ""));
  }

  protected static String withAccessTypeFilters(String path, String... accessTypes) {
    var separator = path.contains("?") ? "&" : "?";
    return path + Arrays.stream(accessTypes)
      .map(type -> "filter[access-type]=" + type)
      .collect(Collectors.joining("&", separator, ""));
  }

  protected RequestSpecification getRequestSpecification() {
    return new RequestSpecBuilder()
      .addHeader(XOkapiHeaders.TENANT, STUB_TENANT)
      .addHeader(XOkapiHeaders.USER_ID, STUB_USER_ID)
      .addHeader(XOkapiHeaders.URL, getWiremockUrl())
      .setBaseUri(moduleUrl)
      .log(LogDetail.ALL)
      .build();
  }

  protected RequestSpecification givenWithUrl() {
    return new RequestSpecBuilder()
      .addHeader(XOkapiHeaders.URL, getWiremockUrl())
      .setBaseUri(moduleUrl)
      .log(LogDetail.ALL)
      .build();
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

  protected ExtractableResponse<Response> getWithOk(String resourcePath) {
    return getWithStatus(resourcePath, SC_OK);
  }

  protected ExtractableResponse<Response> getWithOk(String endpoint, Header... headers) {
    return getWithStatus(endpoint, SC_OK, headers);
  }

  protected ExtractableResponse<Response> getWithStatus(String resourcePath, int expectedStatus) {
    return given()
      .spec(getRequestSpecification())
      .when()
      .get(resourcePath)
      .then()
      .log()
      .ifValidationFails()
      .statusCode(expectedStatus)
      .extract();
  }

  protected ExtractableResponse<Response> getWithStatus(String endpoint, int expectedStatus, Header... headers) {
    return given()
      .spec(getRequestSpecification())
      .headers(new Headers(headers))
      .when()
      .get(endpoint)
      .then()
      .log()
      .ifValidationFails()
      .statusCode(expectedStatus)
      .extract();
  }

  protected ExtractableResponse<Response> postWithOk(String endpoint, String postBody) {
    return postWithStatus(endpoint, postBody, SC_OK, CONTENT_TYPE_HEADER);
  }

  protected ExtractableResponse<Response> postWithCreated(String endpoint, String postBody) {
    return postWithStatus(endpoint, postBody, SC_CREATED, CONTENT_TYPE_HEADER);
  }

  protected ExtractableResponse<Response> postWithStatus(String resourcePath, String postBody,
                                                         int expectedStatus, Header... headers) {
    return given()
      .spec(getRequestSpecification())
      .header(JSON_CONTENT_TYPE_HEADER)
      .header(CONTENT_TYPE_HEADER)
      .headers(new Headers(headers))
      .body(postBody)
      .when()
      .post(resourcePath)
      .then()
      .log().ifValidationFails()
      .statusCode(expectedStatus)
      .extract();
  }

  protected ExtractableResponse<Response> putWithOk(String endpoint, String putBody) {
    return putWithStatus(endpoint, putBody, SC_OK, CONTENT_TYPE_HEADER);
  }

  protected ExtractableResponse<Response> putWithNoContent(String resourcePath, String putBody, Header... headers) {
    return putWithStatus(resourcePath, putBody, SC_NO_CONTENT, headers);
  }

  protected ExtractableResponse<Response> putWithStatus(String resourcePath, String putBody,
                                                        int expectedStatus, Header... headers) {
    return given()
      .spec(getRequestSpecification())
      .header(JSON_CONTENT_TYPE_HEADER)
      .header(CONTENT_TYPE_HEADER)
      .headers(new Headers(headers))
      .body(putBody)
      .when()
      .put(resourcePath)
      .then()
      .log().ifValidationFails()
      .statusCode(expectedStatus)
      .extract();
  }

  protected ExtractableResponse<Response> patchWithNoContent(String resourcePath, String patchBody) {
    return patchWithStatus(resourcePath, patchBody, SC_NO_CONTENT);
  }

  protected ExtractableResponse<Response> patchWithStatus(String resourcePath, String patchBody, int expectedStatus,
                                                          Header... headers) {
    return given()
      .spec(getRequestSpecification())
      .header(JSON_CONTENT_TYPE_HEADER)
      .header(CONTENT_TYPE_HEADER)
      .headers(new Headers(addContentHeader(headers)))
      .body(patchBody)
      .when()
      .patch(resourcePath)
      .then()
      .log().ifValidationFails()
      .statusCode(expectedStatus)
      .extract();
  }

  protected ExtractableResponse<Response> deleteWithNoContent(String resourcePath) {
    return deleteWithStatus(resourcePath, SC_NO_CONTENT);
  }

  protected ExtractableResponse<Response> deleteWithStatus(String resourcePath, int expectedStatus) {
    return given()
      .spec(getRequestSpecification())
      .when()
      .delete(resourcePath)
      .then()
      .log().ifValidationFails()
      .statusCode(expectedStatus)
      .extract();
  }

  @BeforeEach
  void invalidateCache() {
    SpringContextUtil.autowireDependenciesFromFirstContext(this, vertx);
    caches.forEach(VertxCache::invalidateAll);
  }

  private Header[] addContentHeader(Header[] headers) {
    List<Header> headerList = new ArrayList<>(Arrays.asList(headers));
    headerList.add(CONTENT_TYPE_HEADER);
    return headerList.toArray(new Header[0]);
  }
}
