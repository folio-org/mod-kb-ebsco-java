package org.folio.rest.impl;

import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.folio.repository.kbcredentials.KbCredentialsTableConstants.KB_CREDENTIALS_TABLE_NAME;
import static org.folio.repository.uc.UcSettingsTableConstants.UC_SETTINGS_TABLE_NAME;
import static org.folio.util.HoldingsTestUtil.saveHoldingsFromFiles;
import static org.folio.util.KbCredentialsTestUtil.setupDefaultKbConfiguration;
import static org.folio.util.TestUtil.clearDataFromTable;
import static org.folio.util.TestUtil.readFile;
import static org.folio.util.UcCredentialsTestUtil.setUpUcCredentials;
import static org.folio.util.UcSettingsTestUtil.saveUcSettings;
import static org.folio.util.UcSettingsTestUtil.stubSettings;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.restassured.http.Header;
import org.apache.http.HttpHeaders;
import org.folio.util.IntegrationTestBase;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EholdingsExportImplIntegrationTest extends IntegrationTestBase {

  // UC API endpoints
  private static final String UC_COSTPERUSE_PACKAGE_REQ = "/uc/costperuse/package/%s";
  private static final String UC_COSTPERUSE_TITLES_REQ = "/uc/costperuse/titles";
  private static final String EXPORT_PACKAGE_TITLES = "/eholdings/packages/%d-%d/resources/costperuse/export%s";

  private static final int STUB_PROVIDER_ID = 123;
  private static final int STUB_PACKAGE_ID = 456;
  private static final String STUB_QUERY_PARAMS = "?platform=publisher&fiscalYear=2019";

  // UC API stub responses
  private static final String UC_PACKAGE_COST_EMPTY_RESPONSE =
    "responses/uc/packages/get-package-cost-per-use-with-empty-cost-response.json";
  private static final String UC_PACKAGE_EMPTY_RESPONSE =
    "responses/uc/packages/empty-package-cost-per-use-response.json";
  private static final String UC_TITLES_DIFFERENT_PACKAGES_RESPONSE =
    "responses/export/get-different-title-packages-response.json";
  private static final String UC_TITLES_EMPTY_COST_RESPONSE =
    "responses/export/get-empty-cost-per-use-title-response.json";

  // Expected export responses
  private static final String EXPECTED_EXPORT_THREE_ITEMS_USD =
    "responses/kb-ebsco/export/expected-export-three-items-usd.txt";
  private static final String EXPECTED_EXPORT_THREE_ITEMS_ZERO_VALUES =
    "responses/kb-ebsco/export/expected-export-three-items-zero-values.txt";
  private static final String EXPECTED_EMPTY_EXPORT =
    "responses/kb-ebsco/export/expected-empty-export-response.txt";

  // Holdings files
  private static final String HOLDING_FOR_EXPORT_1 = "responses/kb-ebsco/export/holding-for-export-1.json";
  private static final String HOLDING_FOR_EXPORT_2 = "responses/kb-ebsco/export/holding-for-export-2.json";
  private static final String HOLDING_FOR_EXPORT_3 = "responses/kb-ebsco/export/holding-for-export-3.json";

  private static final Header CONTENT_TYPE_CSV_HEADER = new Header(HttpHeaders.CONTENT_TYPE, "text/csv");

  private String credentialsId;

  @BeforeEach
  void setUp() {
    credentialsId = setupDefaultKbConfiguration(getWiremockUrl(), vertx);
  }

  @AfterEach
  void tearDown() {
    clearDataFromTable(vertx, UC_SETTINGS_TABLE_NAME);
    clearDataFromTable(vertx, KB_CREDENTIALS_TABLE_NAME);
  }

  @Test
  void shouldReturnExportResponse() {
    setUpUcCredentials(vertx);
    saveUcSettings(stubSettings(credentialsId), vertx);
    mockAuthToken();

    saveHoldingsFromFiles(credentialsId, vertx, HOLDING_FOR_EXPORT_1, HOLDING_FOR_EXPORT_2, HOLDING_FOR_EXPORT_3);
    mockFailedLocaleResponse();

    mockGet(matching(String.format(UC_COSTPERUSE_PACKAGE_REQ, STUB_PACKAGE_ID)),
      readFile(UC_PACKAGE_COST_EMPTY_RESPONSE));
    mockPost(matching(UC_COSTPERUSE_TITLES_REQ), readFile(UC_TITLES_DIFFERENT_PACKAGES_RESPONSE), SC_OK);

    var url = String.format(EXPORT_PACKAGE_TITLES, STUB_PROVIDER_ID, STUB_PACKAGE_ID, STUB_QUERY_PARAMS);
    var actual = getWithOk(url, CONTENT_TYPE_CSV_HEADER).body().asString();

    assertNotNull(actual);
    assertEquals(readFile(EXPECTED_EXPORT_THREE_ITEMS_USD), actual);
  }

  @Test
  void shouldReturnExportWithZeroValuesResponseWhenNoCostPerUseInfo() {
    setUpUcCredentials(vertx);
    saveUcSettings(stubSettings(credentialsId), vertx);
    mockAuthToken();

    saveHoldingsFromFiles(credentialsId, vertx, HOLDING_FOR_EXPORT_1, HOLDING_FOR_EXPORT_2, HOLDING_FOR_EXPORT_3);
    mockSuccessfulLocaleResponse();

    mockGet(matching(String.format(UC_COSTPERUSE_PACKAGE_REQ, STUB_PACKAGE_ID)), readFile(UC_PACKAGE_EMPTY_RESPONSE));
    mockPost(matching(UC_COSTPERUSE_TITLES_REQ), readFile(UC_TITLES_EMPTY_COST_RESPONSE), SC_OK);

    var url = String.format(EXPORT_PACKAGE_TITLES, STUB_PROVIDER_ID, STUB_PACKAGE_ID, STUB_QUERY_PARAMS);
    var actual = getWithOk(url, CONTENT_TYPE_CSV_HEADER).body().asString();

    assertNotNull(actual);
    assertEquals(actual, readFile(EXPECTED_EXPORT_THREE_ITEMS_ZERO_VALUES));
  }

  @Test
  void shouldReturnEmptyResponseWhenNoHoldings() {
    setUpUcCredentials(vertx);
    saveUcSettings(stubSettings(credentialsId), vertx);
    mockAuthToken();

    mockSuccessfulLocaleResponse();

    mockGet(matching(String.format(UC_COSTPERUSE_PACKAGE_REQ, STUB_PACKAGE_ID)), readFile(UC_PACKAGE_EMPTY_RESPONSE));

    var url = String.format(EXPORT_PACKAGE_TITLES, STUB_PROVIDER_ID, STUB_PACKAGE_ID, STUB_QUERY_PARAMS);
    var actual = getWithOk(url, CONTENT_TYPE_CSV_HEADER).body().asString();

    var ucPackagePublisher = String.format(UC_COSTPERUSE_PACKAGE_REQ, STUB_PACKAGE_ID)
                             + "?fiscalYear=2019&fiscalMonth=apr&analysisCurrency=USD&aggregatedFullText=true";

    wm.verify(1, getRequestedFor(urlEqualTo(ucPackagePublisher)));
    wm.verify(0, getRequestedFor(urlMatching(UC_COSTPERUSE_TITLES_REQ + "?")));
    assertEquals(actual, readFile(EXPECTED_EMPTY_EXPORT));
  }

  @Test
  void shouldReturnResponseWithPublisherEqualsToAllWhenNotSpecifiedInUrl() {
    setUpUcCredentials(vertx);
    saveUcSettings(stubSettings(credentialsId), vertx);
    mockAuthToken();

    saveHoldingsFromFiles(credentialsId, vertx, HOLDING_FOR_EXPORT_1, HOLDING_FOR_EXPORT_2, HOLDING_FOR_EXPORT_3);
    mockFailedLocaleResponse();

    var requestUrl = String.format(UC_COSTPERUSE_PACKAGE_REQ, STUB_PACKAGE_ID);
    mockGet(matching(requestUrl), readFile(UC_PACKAGE_COST_EMPTY_RESPONSE));
    mockPost(matching(UC_COSTPERUSE_TITLES_REQ), readFile(UC_TITLES_DIFFERENT_PACKAGES_RESPONSE), SC_OK);

    var url = String.format(EXPORT_PACKAGE_TITLES, STUB_PROVIDER_ID, STUB_PACKAGE_ID, "?fiscalYear=2019");
    var actual = getWithOk(url, CONTENT_TYPE_CSV_HEADER).body().asString();

    assertNotNull(actual);
    wm.verify(1, getRequestedFor(
      urlEqualTo(requestUrl + "?fiscalYear=2019&fiscalMonth=apr&analysisCurrency=USD&aggregatedFullText=true")));
    wm.verify(1, postRequestedFor(urlEqualTo(ucCostPerUseTitlesUrl("2019", "apr", "USD", false, false))));
    wm.verify(1, postRequestedFor(urlEqualTo(ucCostPerUseTitlesUrl("2019", "apr", "USD", true, false))));
  }

  @Test
  void shouldReturn401ErrorWhenCanNotRetrieveAuthToken() {
    setUpUcCredentials(vertx);
    saveUcSettings(stubSettings(credentialsId), vertx);

    mockPost(matching("/oauth-proxy/token"), "Unable to proceed request", SC_BAD_REQUEST);
    mockSuccessfulLocaleResponse();

    var url = String.format(EXPORT_PACKAGE_TITLES, STUB_PROVIDER_ID, STUB_PACKAGE_ID, STUB_QUERY_PARAMS);

    var error = getWithStatus(url, SC_UNAUTHORIZED, CONTENT_TYPE_CSV_HEADER).asString();

    assertThat(error, Matchers.containsString("Unrecognized token"));
  }

  @Test
  void shouldReturnExportFileWithDefaultLocaleSettings() {
    setUpUcCredentials(vertx);
    saveUcSettings(stubSettings(credentialsId), vertx);
    mockAuthToken();

    saveHoldingsFromFiles(credentialsId, vertx,
      HOLDING_FOR_EXPORT_1, HOLDING_FOR_EXPORT_2, HOLDING_FOR_EXPORT_3);
    mockFailedLocaleResponse();

    mockGet(matching(String.format(UC_COSTPERUSE_PACKAGE_REQ, STUB_PACKAGE_ID)), readFile(UC_PACKAGE_EMPTY_RESPONSE));
    mockPost(matching(UC_COSTPERUSE_TITLES_REQ), readFile(UC_TITLES_EMPTY_COST_RESPONSE), SC_OK);

    var url = String.format(EXPORT_PACKAGE_TITLES, STUB_PROVIDER_ID, STUB_PACKAGE_ID, STUB_QUERY_PARAMS);
    var actual = getWithOk(url, CONTENT_TYPE_CSV_HEADER).body().asString();
    assertNotNull(actual);

    assertEquals(actual, readFile(EXPECTED_EXPORT_THREE_ITEMS_ZERO_VALUES));
  }
}
