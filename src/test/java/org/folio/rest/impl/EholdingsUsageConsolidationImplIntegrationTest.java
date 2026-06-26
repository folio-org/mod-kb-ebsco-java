package org.folio.rest.impl;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNPROCESSABLE_ENTITY;
import static org.folio.repository.kbcredentials.KbCredentialsTableConstants.KB_CREDENTIALS_TABLE_NAME;
import static org.folio.repository.uc.UcCredentialsTableConstants.UC_CREDENTIALS_TABLE_NAME;
import static org.folio.repository.uc.UcSettingsTableConstants.UC_SETTINGS_TABLE_NAME;
import static org.folio.util.AssertTestUtil.assertErrorContainsDetail;
import static org.folio.util.AssertTestUtil.assertErrorContainsTitle;
import static org.folio.util.KbCredentialsTestUtil.API_URL;
import static org.folio.util.KbCredentialsTestUtil.CREDENTIALS_NAME;
import static org.folio.util.KbCredentialsTestUtil.saveKbCredentials;
import static org.folio.util.TestUtil.clearDataFromTable;
import static org.folio.util.UcCredentialsTestUtil.setUpUcCredentials;
import static org.folio.util.UcSettingsTestUtil.METRIC_TYPE_PARAM_TRUE;
import static org.folio.util.UcSettingsTestUtil.UC_SETTINGS_ENDPOINT;
import static org.folio.util.UcSettingsTestUtil.UC_SETTINGS_KEY_ENDPOINT;
import static org.folio.util.UcSettingsTestUtil.UC_SETTINGS_USER_ENDPOINT;
import static org.folio.util.UcSettingsTestUtil.getUcSettings;
import static org.folio.util.UcSettingsTestUtil.saveUcSettings;
import static org.folio.util.UcSettingsTestUtil.stubSettings;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.vertx.core.json.Json;
import java.util.UUID;
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
import org.folio.util.IntegrationTestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EholdingsUsageConsolidationImplIntegrationTest extends IntegrationTestBase {

  private String credentialsId;

  @BeforeEach
  void setUp() {
    credentialsId = saveKbCredentials(API_URL, CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);
  }

  @AfterEach
  void tearDown() {
    clearDataFromTable(vertx, UC_CREDENTIALS_TABLE_NAME);
    clearDataFromTable(vertx, UC_SETTINGS_TABLE_NAME);
    clearDataFromTable(vertx, KB_CREDENTIALS_TABLE_NAME);
  }

  @Test
  void shouldReturnUcSettingsOnGetByUserHeaders() {
    UCSettings stubSettings = stubSettings(credentialsId);
    String settingsId = saveUcSettings(stubSettings, vertx);

    UCSettings actual = getWithOk(UC_SETTINGS_USER_ENDPOINT).as(UCSettings.class);

    UCSettings expected = stubSettings.withId(settingsId);
    expected.getAttributes().withCustomerKey("*".repeat(40));
    assertEquals(expected, actual);
  }

  @Test
  void shouldReturnUcSettingsKeyOnGet() {
    UCSettings stubSettings = stubSettings(credentialsId);
    String settingsId = saveUcSettings(stubSettings, vertx);

    String resourcePath = String.format(UC_SETTINGS_KEY_ENDPOINT, credentialsId);
    UCSettingsKey actual = getWithOk(resourcePath).as(UCSettingsKey.class);

    assertEquals(settingsId, actual.getId());
    assertEquals(stubSettings.getAttributes().getCustomerKey(), actual.getAttributes().getCustomerKey());
  }

  @Test
  void shouldReturnUcSettingsOnGetByUserHeadersWithMetricType() {
    final UCSettings stubSettings = stubSettings(credentialsId);
    final String settingsId = saveUcSettings(stubSettings, vertx);
    setUpUcCredentials(vertx);
    mockMetricTypeWithExpectedTypeId();
    mockAuthToken();

    var actual = getWithOk(UC_SETTINGS_USER_ENDPOINT + METRIC_TYPE_PARAM_TRUE)
      .as(UCSettings.class);

    UCSettings expected = stubSettings.withId(settingsId);
    expected.getAttributes().withCustomerKey("*".repeat(40));
    expected.getAttributes().withMetricType(UCSettingsDataAttributes.MetricType.TOTAL);
    assertEquals(expected, actual);
  }

  @Test
  void shouldReturnUcSettingsOnGet() {
    UCSettings stubSettings = stubSettings(credentialsId);
    String settingsId = saveUcSettings(stubSettings, vertx);

    String resourcePath = String.format(UC_SETTINGS_ENDPOINT, credentialsId);
    UCSettings actual = getWithOk(resourcePath).as(UCSettings.class);

    UCSettings expected = stubSettings.withId(settingsId);
    expected.getAttributes().withCustomerKey("*".repeat(40));
    assertEquals(expected, actual);
  }

  @Test
  void shouldReturn404OnGetNotExistedSettings() {
    String resourcePath = String.format(UC_SETTINGS_ENDPOINT, credentialsId);
    JsonapiError actual = getWithStatus(resourcePath, SC_NOT_FOUND).as(JsonapiError.class);

    String expectedErrorMessage = String.format("Usage Consolidation is not "
                                                + "enabled for KB credentials with id [%s]", credentialsId);
    assertErrorContainsTitle(actual, expectedErrorMessage);
  }

  @Test
  void shouldUpdateUcSettingsOnPatch() {
    setUpUcCredentials(vertx);
    mockAuthToken();
    mockSuccessfulVerification();
    UCSettings stubSettings = stubSettings(credentialsId);
    saveUcSettings(stubSettings, vertx);

    String newCurrencyValue = "UAH";
    UCSettingsPatchRequest patchData = new UCSettingsPatchRequest()
      .withData(new UCSettingsPatchRequestData()
        .withType(UCSettingsPatchRequestData.Type.UC_SETTINGS)
        .withAttributes(new UCSettingsPatchRequestDataAttributes()
          .withCurrency(newCurrencyValue)));

    String resourcePath = String.format(UC_SETTINGS_ENDPOINT, credentialsId);
    patchWithNoContent(resourcePath, Json.encode(patchData));

    UCSettings actual = getUcSettings(vertx).getFirst();
    assertEquals(newCurrencyValue, actual.getAttributes().getCurrency());
  }

  @Test
  void shouldReturn422OnPatchWithInvalidCurrency() {
    mockAuthToken();
    mockSuccessfulVerification();
    setUpUcCredentials(vertx);
    UCSettings stubSettings = stubSettings(credentialsId);
    saveUcSettings(stubSettings, vertx);

    String newCurrencyValue = "VVV";
    UCSettingsPatchRequest patchData = new UCSettingsPatchRequest()
      .withData(new UCSettingsPatchRequestData()
        .withType(UCSettingsPatchRequestData.Type.UC_SETTINGS)
        .withAttributes(new UCSettingsPatchRequestDataAttributes()
          .withCurrency(newCurrencyValue)));

    String resourcePath = String.format(UC_SETTINGS_ENDPOINT, credentialsId);
    var error = patchWithStatus(resourcePath, Json.encode(patchData), SC_UNPROCESSABLE_ENTITY)
      .as(JsonapiError.class);

    assertErrorContainsTitle(error, "Invalid value");
    assertErrorContainsDetail(error, "Value 'VVV' is invalid for 'currency'");
  }

  @Test
  void shouldReturn422OnPatchWithInvalidMonth() {
    mockAuthToken();
    mockSuccessfulVerification();

    UCSettingsPatchRequest patchData = new UCSettingsPatchRequest()
      .withData(new UCSettingsPatchRequestData()
        .withType(UCSettingsPatchRequestData.Type.UC_SETTINGS)
        .withAttributes(new UCSettingsPatchRequestDataAttributes()
          .withStartMonth(Month.DEC)));

    String patchBody = Json.encode(patchData);
    String resourcePath = String.format(UC_SETTINGS_ENDPOINT, credentialsId);
    var error = patchWithStatus(resourcePath, patchBody, SC_NOT_FOUND).as(JsonapiError.class);

    assertErrorContainsTitle(error, "Usage Consolidation is not enabled for KB credentials");
  }

  @Test
  void shouldReturn422OnPatchWithInvalidCustomerKey() {
    mockAuthToken();
    mockFailed400Verification();
    setUpUcCredentials(vertx);
    saveUcSettings(stubSettings(credentialsId), vertx);

    UCSettingsPatchRequest patchData = new UCSettingsPatchRequest()
      .withData(new UCSettingsPatchRequestData()
        .withType(UCSettingsPatchRequestData.Type.UC_SETTINGS)
        .withAttributes(new UCSettingsPatchRequestDataAttributes().withCustomerKey("invalid-customer-key")));

    String patchBody = Json.encode(patchData);
    String resourcePath = String.format(UC_SETTINGS_ENDPOINT, credentialsId);
    var error = patchWithStatus(resourcePath, patchBody, SC_UNPROCESSABLE_ENTITY)
      .as(JsonapiError.class);

    assertErrorContainsTitle(error, "Invalid UC Credentials");
  }

  @Test
  void shouldReturn201OnPostSettingsWithDefaultValues() {
    mockAuthToken();
    mockSuccessfulVerification();
    setUpUcCredentials(vertx);

    String resourcePath = String.format(UC_SETTINGS_ENDPOINT, credentialsId);
    String postBody = Json.encode(getPostRequest());
    UCSettings ucSettings = postWithCreated(resourcePath, postBody).as(UCSettings.class);

    assertEquals(credentialsId, ucSettings.getAttributes().getCredentialsId());
    assertEquals(Month.JAN, ucSettings.getAttributes().getStartMonth());
    assertEquals(PlatformType.ALL, ucSettings.getAttributes().getPlatformType());
  }

  @Test
  void shouldReturn201OnPostSettingsWhenDataIsValid() {
    mockAuthToken();
    mockSuccessfulVerification();
    setUpUcCredentials(vertx);

    String resourcePath = String.format(UC_SETTINGS_ENDPOINT, credentialsId);
    String postBody = Json.encode(getPostRequestNoDefault());
    UCSettings ucSettings = postWithCreated(resourcePath, postBody).as(UCSettings.class);

    assertEquals(credentialsId, ucSettings.getAttributes().getCredentialsId());
    assertEquals(Month.FEB, ucSettings.getAttributes().getStartMonth());
    assertEquals(PlatformType.NON_PUBLISHER, ucSettings.getAttributes().getPlatformType());
    assertEquals("USD", ucSettings.getAttributes().getCurrency());
  }

  @Test
  void shouldReturn422OnPostSettingsWhenCurrencyIsInvalid() {
    mockAuthToken();
    mockSuccessfulVerification();
    setUpUcCredentials(vertx);

    String resourcePath = String.format(UC_SETTINGS_ENDPOINT, credentialsId);
    var postRequest = getPostRequestNoDefault();
    postRequest.getData().getAttributes().setCurrency("aaa");
    String postBody = Json.encode(postRequest);
    var error = postWithStatus(resourcePath, postBody, SC_UNPROCESSABLE_ENTITY)
      .as(JsonapiError.class);

    assertErrorContainsTitle(error, "Invalid value");
    assertErrorContainsDetail(error, "is invalid for 'currency'");
  }

  @Test
  void shouldReturn422WhenKbCredentialsNotExist() {
    mockAuthToken();
    mockSuccessfulVerification();
    setUpUcCredentials(vertx);

    String randomCredentialsId = UUID.randomUUID().toString();
    String resourcePath = String.format(UC_SETTINGS_ENDPOINT, randomCredentialsId);
    String postBody = Json.encode(getPostRequest());
    var error = postWithStatus(resourcePath, postBody, SC_UNPROCESSABLE_ENTITY)
      .as(JsonapiError.class);

    String expectedErrorMessage = String.format("'%s' is invalid for 'kb_credentials_id'", randomCredentialsId);
    assertErrorContainsTitle(error, "Invalid value");
    assertErrorContainsDetail(error, expectedErrorMessage);
  }

  @Test
  void shouldReturn422WhenSaveTwoEntitiesWithSameCredentialsId() {
    mockAuthToken();
    mockSuccessfulVerification();
    setUpUcCredentials(vertx);

    String resourcePath = String.format(UC_SETTINGS_ENDPOINT, credentialsId);
    String postBody = Json.encode(getPostRequest());
    postWithCreated(resourcePath, postBody);

    var error = postWithStatus(resourcePath, postBody, SC_UNPROCESSABLE_ENTITY)
      .as(JsonapiError.class);

    String expectedErrorMessage = String.format("'%s' is invalid for", credentialsId);
    assertErrorContainsTitle(error, "Invalid value");
    assertErrorContainsDetail(error, expectedErrorMessage);
  }

  @Test
  void shouldReturn422WhenUcCredentialNotExist() {
    clearDataFromTable(vertx, UC_CREDENTIALS_TABLE_NAME);

    String resourcePath = String.format(UC_SETTINGS_ENDPOINT, credentialsId);
    String postBody = Json.encode(getPostRequest());
    var error = postWithStatus(resourcePath, postBody, SC_UNPROCESSABLE_ENTITY)
      .as(JsonapiError.class);

    String expectedErrorMessage = "Invalid UC API Credentials";
    assertErrorContainsTitle(error, expectedErrorMessage);
  }

  private void mockSuccessfulVerification() {
    mockGet(matching("/uc/costperuse.*"), SC_OK);
  }

  private void mockFailed400Verification() {
    mockGet(matching("/uc/costperuse.*"), SC_BAD_REQUEST);
  }

  private void mockMetricTypeWithExpectedTypeId() {
    mockGet(equalTo("/uc/usageanalysis/analysismetrictype"), """
      {
        "metricTypeId": 33,
        "description": "R5 - Total_Item_Requests"
      }
      """);
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
