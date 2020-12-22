package org.folio.rest.impl.integrationsuite;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import static org.folio.repository.kbcredentials.KbCredentialsTableConstants.KB_CREDENTIALS_TABLE_NAME;
import static org.folio.repository.uc.UCSettingsTableConstants.UC_SETTINGS_TABLE_NAME;
import static org.folio.test.util.TestUtil.readFile;
import static org.folio.util.KBTestUtil.clearDataFromTable;
import static org.folio.util.KbCredentialsTestUtil.STUB_API_URL;
import static org.folio.util.KbCredentialsTestUtil.STUB_CREDENTIALS_NAME;
import static org.folio.util.KbCredentialsTestUtil.saveKbCredentials;
import static org.folio.util.UCCredentialsTestUtil.setUpUCCredentials;
import static org.folio.util.UCSettingsTestUtil.saveUCSettings;
import static org.folio.util.UCSettingsTestUtil.stubSettings;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.OffsetDateTime;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.matching.RegexPattern;
import com.github.tomakehurst.wiremock.matching.UrlPathPattern;
import io.restassured.http.Header;
import io.vertx.core.json.Json;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.apache.http.protocol.HTTP;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

import org.folio.client.uc.UCApigeeEbscoClient;
import org.folio.client.uc.UCAuthEbscoClient;
import org.folio.client.uc.model.UCAuthToken;
import org.folio.holdingsiq.service.impl.ConfigurationClientProvider;
import org.folio.repository.holdings.DbHoldingInfo;
import org.folio.rest.client.ConfigurationsClient;
import org.folio.rest.impl.WireMockTestBase;
import org.folio.rest.jaxrs.model.JsonapiError;
import org.folio.service.locale.LocaleSettingsService;
import org.folio.util.HoldingsTestUtil;

@RunWith(VertxUnitRunner.class)
public class EholdingsExportImplTest extends WireMockTestBase {

  public static final String UC_COSTPERUSE_PACKAGE_REQ = "/uc/costperuse/package/%s";
  public static final String UC_COSTPERUSE_TITLES_REQ = "/uc/costperuse/titles";
  private final String EXPORT_PACKAGE_TITLES = "/eholdings/packages/%d-%d/resources/costperuse/export%s";
  private final int STUB_PROVIDER_ID = 123;
  private final int STUB_PACKAGE_ID = 456;
  private final String STUB_QUERY_PARAMS = "?platform=publisher&fiscalYear=2019";

  protected static final Header CONTENT_TYPE_CSV_HEADER = new Header(HTTP.CONTENT_TYPE, "text/csv");

  private String credentialsId;

  @Autowired
  private UCAuthEbscoClient authEbscoClient;
  @Autowired
  private UCApigeeEbscoClient apigeeEbscoClient;
  @Autowired
  private LocaleSettingsService configurationService;
  private ConfigurationClientProvider configurationClientProvider;
  private ConfigurationsClient mockConfigurationsClient;

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();

    ReflectionTestUtils.setField(authEbscoClient, "baseUrl", getWiremockUrl());
    ReflectionTestUtils.setField(apigeeEbscoClient, "baseUrl", getWiremockUrl());
    credentialsId = saveKbCredentials(STUB_API_URL, STUB_CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);
    configurationClientProvider = mock(ConfigurationClientProvider.class);
    mockConfigurationsClient = mock(ConfigurationsClient.class);
    when(configurationClientProvider.createClient(anyString(), anyInt(), anyString(), anyString())).thenReturn(mockConfigurationsClient);
  }

  @After
  public void tearDown() {
    clearDataFromTable(vertx, UC_SETTINGS_TABLE_NAME);
    clearDataFromTable(vertx, KB_CREDENTIALS_TABLE_NAME);
  }

  @Test
  public void shouldReturnExportResponse() throws IOException, URISyntaxException {
    setUpUCCredentials(vertx);
    saveUCSettings(stubSettings(credentialsId), vertx);
    mockAuthToken();

    saveHoldingFromFile("responses/kb-ebsco/export/holding-for-export-1.json",
                                       "responses/kb-ebsco/export/holding-for-export-2.json",
                                       "responses/kb-ebsco/export/holding-for-export-3.json");
//    String configFileName = "responses/configuration/locale-settings.json";
    String configFileName = "responses/configuration/locale-settings-empty.json";
    mockSuccessfulConfigurationResponse(configFileName);

    String apigeeGetPackageResponse = "responses/uc/packages/get-package-cost-per-use-with-empty-cost-response.json";
    stubFor(get(urlPathMatching(String.format(UC_COSTPERUSE_PACKAGE_REQ, STUB_PACKAGE_ID)))
      .willReturn(aResponse().withStatus(SC_OK).withBody(readFile(apigeeGetPackageResponse)))
    );

    String apigeeGetTitlePackageResponse =
      "responses/export/get-different-title-packages-response.json";
    stubFor(post(urlPathMatching(UC_COSTPERUSE_TITLES_REQ))
      .willReturn(aResponse().withStatus(SC_OK).withBody(readFile(apigeeGetTitlePackageResponse)))
    );

    var url = String.format(EXPORT_PACKAGE_TITLES, STUB_PROVIDER_ID, STUB_PACKAGE_ID, STUB_QUERY_PARAMS);

    String actual = getWithOk(url, JOHN_TOKEN_HEADER, CONTENT_TYPE_CSV_HEADER).body().asString();

    assertThat(actual, notNullValue());
    assertThat(actual, Matchers.equalTo(readFile("responses/kb-ebsco/export/expected-export-three-items-usd.txt")));
  }


  @Test
  public void shouldReturnExportWithZeroValuesResponseWhenNoCostPerUseInfo() throws IOException, URISyntaxException {
    setUpUCCredentials(vertx);
    saveUCSettings(stubSettings(credentialsId), vertx);
    mockAuthToken();

    saveHoldingFromFile("responses/kb-ebsco/export/holding-for-export-1.json",
      "responses/kb-ebsco/export/holding-for-export-2.json",
      "responses/kb-ebsco/export/holding-for-export-3.json");

    String configFileName = "responses/configuration/locale-settings-empty.json";
    mockSuccessfulConfigurationResponse(configFileName);

    String apigeeGetPackageResponse = "responses/uc/packages/empty-package-cost-per-use-response.json";

    stubFor(get(urlPathMatching(String.format(UC_COSTPERUSE_PACKAGE_REQ, STUB_PACKAGE_ID)))
      .willReturn(aResponse().withStatus(SC_OK).withBody(readFile(apigeeGetPackageResponse)))
    );

    String apigeeGetTitlePackageResponse = "responses/export/get-empty-cost-per-use-title-response.json";
    stubFor(post(urlPathMatching(UC_COSTPERUSE_TITLES_REQ))
      .willReturn(aResponse().withStatus(SC_OK).withBody(readFile(apigeeGetTitlePackageResponse)))
    );

    var url = String.format(EXPORT_PACKAGE_TITLES, STUB_PROVIDER_ID, STUB_PACKAGE_ID, STUB_QUERY_PARAMS);
    String actual = getWithOk(url, JOHN_TOKEN_HEADER, CONTENT_TYPE_CSV_HEADER).body().asString();
    assertThat(actual, notNullValue());

    assertThat(actual, Matchers.equalTo(readFile("responses/kb-ebsco/export/expected-export-three-items-zero-values.txt")));
  }

  @Test
  public void shouldReturnEmptyResponseWhenNoHoldings() throws IOException, URISyntaxException {
    setUpUCCredentials(vertx);
    saveUCSettings(stubSettings(credentialsId), vertx);
    mockAuthToken();

    String configFileName = "responses/configuration/locale-settings-empty.json";
    mockSuccessfulConfigurationResponse(configFileName);

    String apigeeGetPackageResponse = "responses/uc/packages/empty-package-cost-per-use-response.json";

    stubFor(get(urlPathMatching(String.format(UC_COSTPERUSE_PACKAGE_REQ, STUB_PACKAGE_ID)))
      .willReturn(aResponse().withStatus(SC_OK).withBody(readFile(apigeeGetPackageResponse)))
    );

    var url = String.format(EXPORT_PACKAGE_TITLES, STUB_PROVIDER_ID, STUB_PACKAGE_ID, STUB_QUERY_PARAMS);
    String actual = getWithOk(url, JOHN_TOKEN_HEADER, CONTENT_TYPE_CSV_HEADER).body().asString();

    String ucPackagePublisher = "/uc/costperuse/package/456?fiscalYear=2019&fiscalMonth=apr&analysisCurrency=USD&aggregatedFullText=true";
    verify(1, getRequestedFor(urlEqualTo(ucPackagePublisher)));

    String ucTitles = "/uc/costperuse/titles?";
    verify(0, getRequestedFor(urlMatching(ucTitles)));

    assertThat(actual, Matchers.equalTo(readFile("responses/kb-ebsco/export/expected-empty-export-response.txt")));
  }

  @Test
  public void shouldReturnResponseWithPublisherEqualsToAllWhenNotSpecifiedInUrl() throws IOException, URISyntaxException {
    String queryParams = "?fiscalYear=2019";

    setUpUCCredentials(vertx);
    saveUCSettings(stubSettings(credentialsId), vertx);
    mockAuthToken();

    saveHoldingFromFile("responses/kb-ebsco/export/holding-for-export-1.json",
                                       "responses/kb-ebsco/export/holding-for-export-2.json",
                                       "responses/kb-ebsco/export/holding-for-export-3.json");

    String configFileName = "responses/configuration/locale-settings-empty.json";
    mockSuccessfulConfigurationResponse(configFileName);

    String apigeeGetPackageResponse = "responses/uc/packages/get-package-cost-per-use-with-empty-cost-response.json";
    stubFor(get(urlPathMatching(String.format(UC_COSTPERUSE_PACKAGE_REQ, STUB_PACKAGE_ID)))
      .willReturn(aResponse().withStatus(SC_OK).withBody(readFile(apigeeGetPackageResponse)))
    );

    String apigeeGetTitlePackageResponse = "responses/export/get-different-title-packages-response.json";
    stubFor(post(urlPathMatching(UC_COSTPERUSE_TITLES_REQ))
      .willReturn(aResponse().withStatus(SC_OK).withBody(readFile(apigeeGetTitlePackageResponse)))
    );
    var url = String.format(EXPORT_PACKAGE_TITLES, STUB_PROVIDER_ID, STUB_PACKAGE_ID, queryParams);

    String actual = getWithOk(url, JOHN_TOKEN_HEADER, CONTENT_TYPE_CSV_HEADER).body().asString();
    assertThat(actual, notNullValue());

    String ucPackagePublisher = "/uc/costperuse/package/456?fiscalYear=2019&fiscalMonth=apr&analysisCurrency=USD&aggregatedFullText=true";
    verify(1, getRequestedFor(urlEqualTo(ucPackagePublisher)));

    String ucTitlesPublisher = "/uc/costperuse/titles?fiscalYear=2019&fiscalMonth=apr&analysisCurrency=USD&publisherPlatform=false&previousYear=false";
    verify(1, postRequestedFor(urlEqualTo(ucTitlesPublisher)));

    String ucTitlesNonPublisher = "/uc/costperuse/titles?fiscalYear=2019&fiscalMonth=apr&analysisCurrency=USD&publisherPlatform=true&previousYear=false";
    verify(1, postRequestedFor(urlEqualTo(ucTitlesNonPublisher)));
  }

  @Test
  public void shouldReturn401ErrorWhenCanNotRetrieveAuthToken() throws IOException, URISyntaxException {
    setUpUCCredentials(vertx);
    saveUCSettings(stubSettings(credentialsId), vertx);

    String errorMessage = "Unable to proceed request";
    stubFor(post(urlPathMatching("/oauth-proxy/token"))
      .willReturn(aResponse().withStatus(SC_BAD_REQUEST).withBody(errorMessage))
    );
    String configFileName = "responses/configuration/locale-settings-empty.json";
    mockSuccessfulConfigurationResponse(configFileName);

    var url = String.format(EXPORT_PACKAGE_TITLES, STUB_PROVIDER_ID, STUB_PACKAGE_ID, STUB_QUERY_PARAMS);

    getWithStatus(url, SC_UNAUTHORIZED, JOHN_TOKEN_HEADER, CONTENT_TYPE_CSV_HEADER).as(JsonapiError.class);
  }

  @Test
  public void shouldReturnExportFileWithDefaultLocaleSettings() throws IOException, URISyntaxException {

    setUpUCCredentials(vertx);
    saveUCSettings(stubSettings(credentialsId), vertx);
    mockAuthToken();

    saveHoldingFromFile("responses/kb-ebsco/export/holding-for-export-1.json",
      "responses/kb-ebsco/export/holding-for-export-2.json",
      "responses/kb-ebsco/export/holding-for-export-3.json");

    mockFailedConfigurationResponse(SC_BAD_REQUEST);

    String apigeeGetPackageResponse = "responses/uc/packages/empty-package-cost-per-use-response.json";

    stubFor(get(urlPathMatching(String.format(UC_COSTPERUSE_PACKAGE_REQ, STUB_PACKAGE_ID)))
      .willReturn(aResponse().withStatus(SC_OK).withBody(readFile(apigeeGetPackageResponse)))
    );

    String apigeeGetTitlePackageResponse = "responses/export/get-empty-cost-per-use-title-response.json";
    stubFor(post(urlPathMatching(UC_COSTPERUSE_TITLES_REQ))
      .willReturn(aResponse().withStatus(SC_OK).withBody(readFile(apigeeGetTitlePackageResponse)))
    );

    var url = String.format(EXPORT_PACKAGE_TITLES, STUB_PROVIDER_ID, STUB_PACKAGE_ID, STUB_QUERY_PARAMS);
    String actual = getWithOk(url, JOHN_TOKEN_HEADER, CONTENT_TYPE_CSV_HEADER).body().asString();
    assertThat(actual, notNullValue());

    assertThat(actual, Matchers.equalTo(readFile("responses/kb-ebsco/export/expected-export-three-items-zero-values.txt")));
  }

  private void mockAuthToken() {
    UCAuthToken stubToken = new UCAuthToken("access_token", "Bearer", 3600L, "openid");
    stubFor(post(urlPathMatching("/oauth-proxy/token"))
      .willReturn(aResponse().withStatus(SC_OK).withBody(Json.encode(stubToken)))
    );
  }

  private void saveHoldingFromFile(String ... holdingsArray) throws IOException, URISyntaxException {
    for (int i = 0; i <= holdingsArray.length - 1; i++) {
      HoldingsTestUtil.saveHolding(credentialsId,
        Json.decodeValue(readFile(holdingsArray[i]), DbHoldingInfo.class),
        OffsetDateTime.now(), vertx);
    }
  }

  private void mockSuccessfulConfigurationResponse(String configFileName) throws IOException, URISyntaxException {
    stubFor(
      get(new UrlPathPattern(new RegexPattern("/configurations/entries.*"), true))
        .willReturn(new ResponseDefinitionBuilder()
          .withStatus(SC_OK)
          .withBody(readFile(configFileName))));
  }

  private void mockFailedConfigurationResponse(int status) {
    stubFor(
      get(new UrlPathPattern(new RegexPattern("/configurations/entries.*"), true))
        .willReturn(new ResponseDefinitionBuilder()
          .withStatus(status)
          .withBody("")));
  }
}
