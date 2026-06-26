package org.folio.util;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.badRequest;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.folio.HttpStatus.SC_OK;
import static org.folio.service.locale.LocaleSettingsServiceImpl.LOCALE_ENDPOINT_PATH;
import static org.folio.service.users.UsersLookUpService.CQL_QUERY_PARAM;
import static org.folio.service.users.UsersLookUpService.GROUPS_ENDPOINT;
import static org.folio.service.users.UsersLookUpService.USERS_BY_ID_ENDPOINT;
import static org.folio.service.users.UsersLookUpService.USERS_ENDPOINT;
import static org.folio.util.TestUtil.readFile;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.common.Slf4jNotifier;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.matching.ContentPattern;
import com.github.tomakehurst.wiremock.matching.RegexPattern;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import com.github.tomakehurst.wiremock.matching.UrlPathPattern;
import com.github.tomakehurst.wiremock.matching.UrlPattern;
import io.restassured.http.Header;
import io.vertx.core.json.Json;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.folio.client.uc.model.UcAuthToken;
import org.folio.holdingsiq.model.Configuration;
import org.folio.okapi.common.XOkapiHeaders;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Base test class for tests that use wiremock and vertx http servers,
 * test that inherits this class must use VertxUnitRunner as test runner.
 */
@ExtendWith(TestStartLoggingExtension.class)
public abstract class WireMockTestBase extends RmApiConstants {

  // User IDs
  public static final String USER_1 = "88888888-8888-4888-8888-888888888888";
  public static final String USER_NOT_FOUND = "22222222-2222-4222-2222-222222222222";
  public static final String USER_FORBIDDEN = "33333333-3333-4333-3333-333333333333";
  public static final String JOHN_ID = "47d9ca93-9c82-4d6a-8d7f-7a73963086b9";
  public static final String JOHN_GROUP_ID = "b4b5e97a-0a99-4db9-97df-4fdf406ec74d";
  public static final String JOHN_USERNAME = "john_doe";
  public static final String JANE_ID = "781fce7d-5cf5-490d-ad89-a3d192eb526c";
  public static final String JANE_GROUP_ID = "4bb563d9-3f9d-4e1e-8d1d-04e75666d68f";

  // User ID headers
  public static final Header JOHN_USER_ID_HEADER = new Header(XOkapiHeaders.USER_ID, JOHN_ID);
  public static final Header JANE_USER_ID_HEADER = new Header(XOkapiHeaders.USER_ID, JANE_ID);
  public static final Header USER_NOT_FOUND_USER_ID_HEADER = new Header(XOkapiHeaders.USER_ID, USER_NOT_FOUND);

  @RegisterExtension
  public static WireMockExtension wm = WireMockExtension.newInstance()
    .options(WireMockConfiguration.wireMockConfig()
      .dynamicPort()
      .notifier(new Slf4jNotifier(true))).build();

  // Stub response files
  private static final String USERDATA_INFO_STUB_FILE = "responses/userlookup/mock_user_response_200.json";
  private static final String USERDATA_JOHN_STUB_FILE = "responses/userlookup/mock_user_response_2_200.json";
  private static final String USERDATA_COLLECTION_INFO_STUB_FILE =
    "responses/userlookup/mock_user_collection_response_200.json";
  private static final String GROUP_INFO_STUB_FILE = "responses/userlookup/mock_group_collection_response_200.json";

  public static void mockGet(StringValuePattern urlPattern, String responseBody) {
    wm.stubFor(get(urlPathPattern(urlPattern))
      .willReturn(aResponse()
        .withStatus(SC_OK)
        .withBody(responseBody)));
  }

  public static void mockGet(StringValuePattern urlPattern,
                             Map<String, StringValuePattern> paramsMatching,
                             String responseBody) {
    var mappingBuilder = get(urlPathPattern(urlPattern));
    paramsMatching.forEach(mappingBuilder::withQueryParam);
    wm.stubFor(mappingBuilder
      .willReturn(aResponse()
        .withStatus(SC_OK)
        .withBody(responseBody)));
  }

  public static void mockGet(StringValuePattern urlPattern, int status) {
    wm.stubFor(get(urlPathPattern(urlPattern))
      .willReturn(new ResponseDefinitionBuilder().withStatus(status)));
  }

  public static void mockGet(StringValuePattern urlPattern, String responseBody, int status) {
    wm.stubFor(get(urlPathPattern(urlPattern))
      .willReturn(aResponse()
        .withStatus(status)
        .withBody(responseBody)));
  }

  public static void mockPost(StringValuePattern urlPattern, String responseBody, int status) {
    wm.stubFor(post(urlPathPattern(urlPattern))
      .willReturn(aResponse()
        .withBody(responseBody)
        .withStatus(status)));
  }

  public static void mockPost(StringValuePattern urlPattern, ContentPattern<?> bodyPattern,
                              String responseBody, int status) {
    wm.stubFor(post(urlPathPattern(urlPattern))
      .withRequestBody(bodyPattern)
      .willReturn(aResponse()
        .withBody(responseBody)
        .withStatus(status)));
  }

  public static void mockPut(StringValuePattern urlPattern, ContentPattern<?> bodyPattern, int status) {
    wm.stubFor(put(urlPathPattern(urlPattern))
      .withRequestBody(bodyPattern)
      .willReturn(new ResponseDefinitionBuilder()
        .withStatus(status)));
  }

  public static void mockPut(StringValuePattern urlPattern, int status) {
    wm.stubFor(put(urlPathPattern(urlPattern))
      .willReturn(new ResponseDefinitionBuilder()
        .withStatus(status)));
  }

  public static void mockPut(StringValuePattern urlPattern, String responseBody, int status) {
    wm.stubFor(put(urlPathPattern(urlPattern))
      .willReturn(aResponse()
        .withBody(responseBody)
        .withStatus(status)));
  }

  public static void verifyPut(StringValuePattern urlPattern, ContentPattern<?> bodyPattern) {
    verifyPut(urlPattern, bodyPattern, 1);
  }

  public static void verifyPut(StringValuePattern urlPattern, ContentPattern<?> bodyPattern, int count) {
    wm.verify(count, putRequestedFor(urlPathPattern(urlPattern)).withRequestBody(bodyPattern));
  }

  public static void verifyPut(StringValuePattern urlPattern, int count) {
    wm.verify(count, putRequestedFor(urlPathPattern(urlPattern)));
  }

  public static void verifyPut(UrlPattern urlPattern, int count) {
    wm.verify(count, putRequestedFor(urlPattern));
  }

  public static void verifyGet(StringValuePattern urlPattern, int count) {
    wm.verify(count, getRequestedFor(urlPathPattern(urlPattern)));
  }

  public static void verifyPost(StringValuePattern urlPattern, int count) {
    wm.verify(count, postRequestedFor(urlPathPattern(urlPattern)));
  }

  public static void mockResponseList(UrlPathPattern urlPattern, ResponseDefinitionBuilder... responses) {
    int scenarioStep = 0;
    String scenarioName = "Scenario -" + UUID.randomUUID();
    for (ResponseDefinitionBuilder response : responses) {
      if (scenarioStep == 0) {
        wm.stubFor(
          get(urlPattern)
            .inScenario(scenarioName)
            .willSetStateTo(String.valueOf(++scenarioStep))
            .willReturn(response));
      } else {
        wm.stubFor(
          get(urlPattern)
            .inScenario(scenarioName)
            .whenScenarioStateIs(String.valueOf(scenarioStep))
            .willSetStateTo(String.valueOf(++scenarioStep))
            .willReturn(response));
      }
    }
  }

  public Configuration getStubConfiguration() {
    return Configuration.builder()
      .url(wm.baseUrl())
      .customerId(STUB_CUSTOMER_ID)
      .apiKey(STUB_API_KEY)
      .build();
  }

  protected String getWiremockUrl() {
    return wm.baseUrl();
  }

  protected void mockSuccessfulLocaleResponse() {
    mockSuccessfulLocaleResponse("responses/configuration/locale-settings.json");
  }

  protected void mockSuccessfulLocaleResponse(String configFileName) {
    wm.stubFor(get(urlPathEqualTo(LOCALE_ENDPOINT_PATH))
      .willReturn(ok(readFile(configFileName))));
  }

  protected void mockFailedLocaleResponse() {
    wm.stubFor(get(urlPathEqualTo(LOCALE_ENDPOINT_PATH))
      .willReturn(badRequest()));
  }

  protected void mockLocaleResponseWithInvalidJson() {
    wm.stubFor(get(urlPathEqualTo(LOCALE_ENDPOINT_PATH))
      .willReturn(ok("{ invalid json }")));
  }

  protected void mockLocaleResponseWithEmptyBody() {
    wm.stubFor(get(urlPathEqualTo(LOCALE_ENDPOINT_PATH))
      .willReturn(ok()));
  }

  protected void mockLocaleResponseWithServerError() {
    wm.stubFor(get(urlPathEqualTo(LOCALE_ENDPOINT_PATH))
      .willReturn(serverError()));
  }

  protected void mockLocaleResponseWithNetworkError() {
    wm.stubFor(get(urlPathEqualTo(LOCALE_ENDPOINT_PATH))
      .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));
  }

  protected void mockUsers() {
    mockGet(equalTo(USERS_BY_ID_ENDPOINT.formatted(JOHN_ID)), readFile(USERDATA_JOHN_STUB_FILE));
    mockGet(equalTo(USERS_BY_ID_ENDPOINT.formatted(JANE_ID)), readFile(USERDATA_INFO_STUB_FILE));
    mockGet(equalTo(USERS_BY_ID_ENDPOINT.formatted(USER_1)), readFile(USERDATA_INFO_STUB_FILE));
    mockGet(equalTo(USERS_BY_ID_ENDPOINT.formatted(USER_NOT_FOUND)), SC_NOT_FOUND);
    mockGet(equalTo(USERS_BY_ID_ENDPOINT.formatted(USER_FORBIDDEN)), SC_FORBIDDEN);
    mockGet(equalTo(USERS_ENDPOINT), Map.of(CQL_QUERY_PARAM, matching("id.*")),
      readFile(USERDATA_COLLECTION_INFO_STUB_FILE));

    mockGet(equalTo(GROUPS_ENDPOINT), Map.of(CQL_QUERY_PARAM, matching("id.*")), readFile(GROUP_INFO_STUB_FILE));
  }

  protected String cqlQueryConverter(List<String> ids) {
    return "id=(" + ids.stream()
      .map(StringUtil::cqlEncode).collect(Collectors.joining(" OR ")) + ")";
  }

  protected void mockUserById(String id) {
    mockGet(equalTo(USERS_BY_ID_ENDPOINT.formatted(id)),
      readFile(USERDATA_INFO_STUB_FILE));
  }

  protected void mockUserNotFound(String id) {
    mockGet(equalTo(USERS_BY_ID_ENDPOINT.formatted(id)), SC_NOT_FOUND);
  }

  protected void mockAuthToken() {
    var stubToken = new UcAuthToken("access_token", "Bearer", 3600L, "openid");
    mockPost(matching("/oauth-proxy/token"), Json.encode(stubToken), SC_OK);
  }

  protected void mockVerifyValidCredentialsRequest() {
    mockGet(matching(RM_ACCOUNTS_BASE_URL + ".*"), "{\"totalResults\": 0, \"vendors\": []}");
  }

  protected void mockVerifyFailedCredentialsRequest() {
    mockGet(matching(RM_ACCOUNTS_BASE_URL + ".*"), SC_UNAUTHORIZED);
  }

  @BeforeEach
  void setUp() {
    mockUsers();
  }

  @BeforeAll
  static void beforeAll() {
    System.setProperty("kb.ebsco.uc.auth.url", wm.baseUrl());
  }

  private static UrlPathPattern urlPathPattern(StringValuePattern urlPattern) {
    return new UrlPathPattern(urlPattern, urlPattern instanceof RegexPattern);
  }
}
