package org.folio.rest.impl.integrationsuite;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.apache.http.HttpStatus.SC_UNPROCESSABLE_ENTITY;
import static org.junit.Assert.assertEquals;

import static org.folio.repository.kbcredentials.KbCredentialsTableConstants.KB_CREDENTIALS_TABLE_NAME;
import static org.folio.repository.uc.UCCredentialsTableConstants.UC_CREDENTIALS_TABLE_NAME;
import static org.folio.repository.uc.UCSettingsTableConstants.UC_SETTINGS_TABLE_NAME;
import static org.folio.util.AssertTestUtil.assertErrorContainsDetail;
import static org.folio.util.AssertTestUtil.assertErrorContainsTitle;
import static org.folio.util.KBTestUtil.clearDataFromTable;
import static org.folio.util.KbCredentialsTestUtil.STUB_API_URL;
import static org.folio.util.KbCredentialsTestUtil.STUB_CREDENTIALS_NAME;
import static org.folio.util.KbCredentialsTestUtil.saveKbCredentials;
import static org.folio.util.UCCredentialsTestUtil.setUpUCCredentials;
import static org.folio.util.UCSettingsTestUtil.METRIC_TYPE_PARAM_TRUE;
import static org.folio.util.UCSettingsTestUtil.UC_SETTINGS_ENDPOINT;
import static org.folio.util.UCSettingsTestUtil.UC_SETTINGS_KEY_ENDPOINT;
import static org.folio.util.UCSettingsTestUtil.UC_SETTINGS_USER_ENDPOINT;
import static org.folio.util.UCSettingsTestUtil.getUCSettings;
import static org.folio.util.UCSettingsTestUtil.saveUCSettings;
import static org.folio.util.UCSettingsTestUtil.stubSettings;

import java.util.UUID;

import io.vertx.core.json.Json;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

import org.folio.client.uc.UCApigeeEbscoClient;
import org.folio.client.uc.UCAuthEbscoClient;
import org.folio.client.uc.model.UCAuthToken;
import org.folio.rest.impl.WireMockTestBase;
import org.folio.rest.jaxrs.model.JsonapiError;
import org.folio.rest.jaxrs.model.Month;
import org.folio.rest.jaxrs.model.PlatformType;
import org.folio.rest.jaxrs.model.UCSettings;
import org.folio.rest.jaxrs.model.UCSettingsDataAttributes;
import org.folio.rest.jaxrs.model.UCSettingsKey;
import org.folio.rest.jaxrs.model.UCSettingsPatchRequest;
import org.folio.rest.jaxrs.model.UCSettingsPatchRequestData;
import org.folio.rest.jaxrs.model.UCSettingsPatchRequestDataAttributes;
import org.folio.rest.jaxrs.model.UCSettingsPostDataAttributes;
import org.folio.rest.jaxrs.model.UCSettingsPostRequest;

@RunWith(VertxUnitRunner.class)
public class EholdingsUsageConsolidationImplTest extends WireMockTestBase {

  private String credentialsId;

  @Autowired
  private UCAuthEbscoClient authEbscoClient;
  @Autowired
  private UCApigeeEbscoClient apigeeEbscoClient;

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    ReflectionTestUtils.setField(authEbscoClient, "baseUrl", getWiremockUrl());
    ReflectionTestUtils.setField(apigeeEbscoClient, "baseUrl", getWiremockUrl());
    credentialsId = saveKbCredentials(STUB_API_URL, STUB_CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);
  }

  @After
  public void tearDown() {
    clearDataFromTable(vertx, UC_SETTINGS_TABLE_NAME);
    clearDataFromTable(vertx, KB_CREDENTIALS_TABLE_NAME);
  }

  @Test
  public void shouldReturnUCSettingsOnGetByUserHeaders() {
    UCSettings stubSettings = stubSettings(credentialsId);
    String settingsId = saveUCSettings(stubSettings, vertx);

    UCSettings actual = getWithOk(UC_SETTINGS_USER_ENDPOINT, JOHN_TOKEN_HEADER).as(UCSettings.class);

    UCSettings expected = stubSettings.withId(settingsId);
    expected.getAttributes().withCustomerKey("*".repeat(40));
    assertEquals(expected, actual);
  }

  @Test
  public void shouldReturnUCSettingsKeyOnGet() {
    UCSettings stubSettings = stubSettings(credentialsId);
    String settingsId = saveUCSettings(stubSettings, vertx);

    String resourcePath = String.format(UC_SETTINGS_KEY_ENDPOINT, credentialsId);
    UCSettingsKey actual = getWithOk(resourcePath, JOHN_TOKEN_HEADER).as(UCSettingsKey.class);

    assertEquals(settingsId, actual.getId());
    assertEquals(stubSettings.getAttributes().getCustomerKey(), actual.getAttributes().getCustomerKey());
  }

  @Test
  public void shouldReturnUCSettingsOnGetByUserHeadersWithMetricType() {
    UCSettings stubSettings = stubSettings(credentialsId);
    String settingsId = saveUCSettings(stubSettings, vertx);
    setUpUCCredentials(vertx);
    mockMetricTypeWithExpectedTypeId();
    mockAuthToken();

    UCSettings actual = getWithOk(UC_SETTINGS_USER_ENDPOINT + METRIC_TYPE_PARAM_TRUE, JOHN_TOKEN_HEADER)
      .as(UCSettings.class);

    UCSettings expected = stubSettings.withId(settingsId);
    expected.getAttributes().withCustomerKey("*".repeat(40));
    expected.getAttributes().withMetricType(UCSettingsDataAttributes.MetricType.TOTAL);
    assertEquals(expected, actual);
  }

  @Test
  public void shouldReturnUCSettingsOnGet() {
    UCSettings stubSettings = stubSettings(credentialsId);
    String settingsId = saveUCSettings(stubSettings, vertx);

    String resourcePath = String.format(UC_SETTINGS_ENDPOINT, credentialsId);
    UCSettings actual = getWithOk(resourcePath).as(UCSettings.class);

    UCSettings expected = stubSettings.withId(settingsId);
    expected.getAttributes().withCustomerKey("*".repeat(40));
    assertEquals(expected, actual);
  }

  @Test
  public void shouldReturn404OnGetNotExistedSettings() {
    String resourcePath = String.format(UC_SETTINGS_ENDPOINT, credentialsId);
    JsonapiError actual = getWithStatus(resourcePath, SC_NOT_FOUND).as(JsonapiError.class);

    String expectedErrorMessage = String.format("Usage Consolidation is not "
      + "enabled for KB credentials with id [%s]", credentialsId);
    assertErrorContainsTitle(actual, expectedErrorMessage);
  }

  @Test
  public void shouldUpdateUCSettingsOnPatch() {
    mockAuthToken();
    mockSuccessfulVerification();
    UCSettings stubSettings = stubSettings(credentialsId);
    saveUCSettings(stubSettings, vertx);

    String newCurrencyValue = "UAH";
    UCSettingsPatchRequest patchData = new UCSettingsPatchRequest()
      .withData(new UCSettingsPatchRequestData()
        .withType(UCSettingsPatchRequestData.Type.UC_SETTINGS)
        .withAttributes(new UCSettingsPatchRequestDataAttributes()
          .withCurrency(newCurrencyValue)));

    String resourcePath = String.format(UC_SETTINGS_ENDPOINT, credentialsId);
    patchWithNoContent(resourcePath, Json.encode(patchData), JOHN_TOKEN_HEADER);

    UCSettings actual = getUCSettings(vertx).get(0);
    assertEquals(newCurrencyValue, actual.getAttributes().getCurrency());
  }

  @Test
  public void shouldReturn422OnPatchWithInvalidCurrency() {
    mockAuthToken();
    mockSuccessfulVerification();
    setUpUCCredentials(vertx);
    UCSettings stubSettings = stubSettings(credentialsId);
    saveUCSettings(stubSettings, vertx);

    String newCurrencyValue = "VVV";
    UCSettingsPatchRequest patchData = new UCSettingsPatchRequest()
      .withData(new UCSettingsPatchRequestData()
        .withType(UCSettingsPatchRequestData.Type.UC_SETTINGS)
        .withAttributes(new UCSettingsPatchRequestDataAttributes()
          .withCurrency(newCurrencyValue)));

    String resourcePath = String.format(UC_SETTINGS_ENDPOINT, credentialsId);
    JsonapiError error = patchWithStatus(resourcePath, Json.encode(patchData), SC_UNPROCESSABLE_ENTITY, JOHN_TOKEN_HEADER)
      .as(JsonapiError.class);

    assertErrorContainsTitle(error, "Invalid value");
    assertErrorContainsDetail(error, "Value 'VVV' is invalid for 'currency'");
  }

  @Test
  public void shouldReturn422OnPatchWithInvalidMonth() {
    mockAuthToken();
    mockSuccessfulVerification();

    UCSettingsPatchRequest patchData = new UCSettingsPatchRequest()
      .withData(new UCSettingsPatchRequestData()
        .withType(UCSettingsPatchRequestData.Type.UC_SETTINGS)
        .withAttributes(new UCSettingsPatchRequestDataAttributes()
          .withStartMonth(Month.DEC)));

    String patchBody = Json.encode(patchData);
    String resourcePath = String.format(UC_SETTINGS_ENDPOINT, credentialsId);
    JsonapiError error = patchWithStatus(resourcePath, patchBody, SC_NOT_FOUND, JOHN_TOKEN_HEADER).as(JsonapiError.class);

    assertErrorContainsTitle(error, "Usage Consolidation is not enabled for KB credentials");
  }

  @Test
  public void shouldReturn422OnPatchWithInvalidCustomerKey() {
    mockAuthToken();
    mockFailed400Verification();
    setUpUCCredentials(vertx);
    saveUCSettings(stubSettings(credentialsId), vertx);

    UCSettingsPatchRequest patchData = new UCSettingsPatchRequest()
      .withData(new UCSettingsPatchRequestData()
        .withType(UCSettingsPatchRequestData.Type.UC_SETTINGS)
        .withAttributes(new UCSettingsPatchRequestDataAttributes().withCustomerKey("invalid-customer-key")));

    String patchBody = Json.encode(patchData);
    String resourcePath = String.format(UC_SETTINGS_ENDPOINT, credentialsId);
    JsonapiError error = patchWithStatus(resourcePath, patchBody, SC_UNPROCESSABLE_ENTITY, JOHN_TOKEN_HEADER)
      .as(JsonapiError.class);

    assertErrorContainsTitle(error, "Invalid UC Credentials");
  }

  @Test
  public void shouldReturn201OnPostSettingsWithDefaultValues() {
    mockAuthToken();
    mockSuccessfulVerification();
    setUpUCCredentials(vertx);

    String resourcePath = String.format(UC_SETTINGS_ENDPOINT, credentialsId);
    String postBody = Json.encode(getPostRequest());
    UCSettings ucSettings = postWithCreated(resourcePath, postBody, JOHN_TOKEN_HEADER).as(UCSettings.class);

    assertEquals(credentialsId, ucSettings.getAttributes().getCredentialsId());
    assertEquals(Month.JAN, ucSettings.getAttributes().getStartMonth());
    assertEquals(PlatformType.ALL, ucSettings.getAttributes().getPlatformType());
  }

  @Test
  public void shouldReturn201OnPostSettingsWhenDataIsValid() {
    mockAuthToken();
    mockSuccessfulVerification();
    setUpUCCredentials(vertx);

    String resourcePath = String.format(UC_SETTINGS_ENDPOINT, credentialsId);
    String postBody = Json.encode(getPostRequestNoDefault());
    UCSettings ucSettings = postWithCreated(resourcePath, postBody, JOHN_TOKEN_HEADER).as(UCSettings.class);

    assertEquals(credentialsId, ucSettings.getAttributes().getCredentialsId());
    assertEquals(Month.FEB, ucSettings.getAttributes().getStartMonth());
    assertEquals(PlatformType.NON_PUBLISHER, ucSettings.getAttributes().getPlatformType());
    assertEquals("USD", ucSettings.getAttributes().getCurrency());
  }

  @Test
  public void shouldReturn422OnPostSettingsWhenCurrencyIsInvalid() {
    mockAuthToken();
    mockSuccessfulVerification();
    setUpUCCredentials(vertx);

    String resourcePath = String.format(UC_SETTINGS_ENDPOINT, credentialsId);
    var postRequest = getPostRequestNoDefault();
    postRequest.getData().getAttributes().setCurrency("aaa");
    String postBody = Json.encode(postRequest);
    JsonapiError error =
      postWithStatus(resourcePath, postBody, SC_UNPROCESSABLE_ENTITY, JOHN_TOKEN_HEADER).as(JsonapiError.class);

    assertErrorContainsTitle(error, "Invalid value");
    assertErrorContainsDetail(error, "is invalid for 'currency'");
  }

  @Test
  public void shouldReturn422WhenKbCredentialsNotExist() {
    mockAuthToken();
    mockSuccessfulVerification();
    setUpUCCredentials(vertx);

    String credentialsId = UUID.randomUUID().toString();
    String resourcePath = String.format(UC_SETTINGS_ENDPOINT, credentialsId);
    String postBody = Json.encode(getPostRequest());
    JsonapiError error =
      postWithStatus(resourcePath, postBody, SC_UNPROCESSABLE_ENTITY, JOHN_TOKEN_HEADER).as(JsonapiError.class);

    String expectedErrorMessage = String.format("'%s' is invalid for 'kb_credentials_id'", credentialsId);
    assertErrorContainsTitle(error, "Invalid value");
    assertErrorContainsDetail(error, expectedErrorMessage);
  }

  @Test
  public void shouldReturn422WhenSaveTwoEntitiesWithSameCredentialsId() {
    mockAuthToken();
    mockSuccessfulVerification();
    setUpUCCredentials(vertx);

    String resourcePath = String.format(UC_SETTINGS_ENDPOINT, credentialsId);
    String postBody = Json.encode(getPostRequest());
    postWithCreated(resourcePath, postBody, JOHN_TOKEN_HEADER);

    JsonapiError error =
      postWithStatus(resourcePath, postBody, SC_UNPROCESSABLE_ENTITY, JOHN_TOKEN_HEADER).as(JsonapiError.class);

    String expectedErrorMessage = String.format("'%s' is invalid for", credentialsId);
    assertErrorContainsTitle(error, "Invalid value");
    assertErrorContainsDetail(error, expectedErrorMessage);
  }

  @Test
  public void shouldReturn422WhenUCCredentialNotExist() {
    clearDataFromTable(vertx, UC_CREDENTIALS_TABLE_NAME);

    String resourcePath = String.format(UC_SETTINGS_ENDPOINT, credentialsId);
    String postBody = Json.encode(getPostRequest());
    JsonapiError error =
      postWithStatus(resourcePath, postBody, SC_UNPROCESSABLE_ENTITY, JOHN_TOKEN_HEADER).as(JsonapiError.class);

    String expectedErrorMessage = "Invalid UC API Credentials";
    assertErrorContainsTitle(error, expectedErrorMessage);
  }

  @Test
  public void shouldReturn401WhenNoHeaderProvided() {
    mockAuthToken();
    mockSuccessfulVerification();
    setUpUCCredentials(vertx);

    String resourcePath = String.format(UC_SETTINGS_ENDPOINT, credentialsId);
    String postBody = Json.encode(getPostRequest());
    JsonapiError error = postWithStatus(resourcePath, postBody, SC_UNAUTHORIZED).as(JsonapiError.class);

    assertErrorContainsTitle(error, "Invalid token");
  }

  @Test
  public void shouldReturn401WhenAuthTokenExpired() {
    mockAuthToken();
    mockFailed401Verification();
    setUpUCCredentials(vertx);

    String resourcePath = String.format(UC_SETTINGS_ENDPOINT, credentialsId);
    String postBody = Json.encode(getPostRequest());
    JsonapiError error = postWithStatus(resourcePath, postBody, SC_UNAUTHORIZED).as(JsonapiError.class);

    assertErrorContainsTitle(error, "Invalid token");
  }

  private void mockSuccessfulVerification() {
    stubFor(get(urlMatching("/uc/costperuse.*")).willReturn(aResponse().withStatus(SC_OK)));
  }

  private void mockFailed400Verification() {
    stubFor(get(urlMatching("/uc/costperuse.*")).willReturn(aResponse().withStatus(SC_BAD_REQUEST)));
  }

  private void mockFailed401Verification() {
    stubFor(get(urlMatching("/uc/costperuse.*")).willReturn(aResponse().withStatus(SC_UNAUTHORIZED)));
  }

  private void mockAuthToken() {
    UCAuthToken stubToken = new UCAuthToken("access_token", "Bearer", 3600L, "openid");
    stubFor(post(urlPathMatching("/oauth-proxy/token"))
      .willReturn(aResponse().withStatus(SC_OK).withBody(Json.encode(stubToken)))
    );
  }

  private void mockMetricTypeWithExpectedTypeId() {
    stubFor(get(urlMatching("/uc/usageanalysis/analysismetrictype"))
      .willReturn(aResponse().withStatus(SC_OK)
        .withBody("{\"metricTypeId\":33,\"description\":\"R5 - Total_Item_Requests\"}")));
  }

  private UCSettingsPostRequest getPostRequest() {
    return new UCSettingsPostRequest()
      .withData(new UCSettingsPostDataAttributes()
        .withType(UCSettingsPostDataAttributes.Type.UC_SETTINGS)
        .withAttributes(new UCSettingsDataAttributes()
          .withCurrency("usd")
          .withCustomerKey("zzz")));
  }

  private UCSettingsPostRequest getPostRequestNoDefault() {
    return new UCSettingsPostRequest()
      .withData(new UCSettingsPostDataAttributes()
        .withType(UCSettingsPostDataAttributes.Type.UC_SETTINGS)
        .withAttributes(new UCSettingsDataAttributes()
          .withCurrency("usd")
          .withCustomerKey("zzz")
          .withPlatformType(PlatformType.NON_PUBLISHER)
          .withStartMonth(Month.FEB)
        )
      );
  }
}
