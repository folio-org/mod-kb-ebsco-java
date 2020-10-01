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
import static org.apache.http.HttpStatus.SC_UNPROCESSABLE_ENTITY;
import static org.junit.Assert.assertEquals;

import static org.folio.repository.kbcredentials.KbCredentialsTableConstants.KB_CREDENTIALS_TABLE_NAME;
import static org.folio.repository.uc.UCSettingsTableConstants.UC_SETTINGS_TABLE_NAME;
import static org.folio.util.AssertTestUtil.assertErrorContainsDetail;
import static org.folio.util.AssertTestUtil.assertErrorContainsTitle;
import static org.folio.util.KBTestUtil.clearDataFromTable;
import static org.folio.util.KbCredentialsTestUtil.STUB_API_URL;
import static org.folio.util.KbCredentialsTestUtil.STUB_CREDENTIALS_NAME;
import static org.folio.util.KbCredentialsTestUtil.saveKbCredentials;
import static org.folio.util.UCCredentialsTestUtil.setUpUCCredentials;
import static org.folio.util.UCSettingsTestUtil.UC_SETTINGS_ENDPOINT;
import static org.folio.util.UCSettingsTestUtil.getUCSettings;
import static org.folio.util.UCSettingsTestUtil.saveUCSettings;
import static org.folio.util.UCSettingsTestUtil.stubSettings;

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
import org.folio.client.uc.UCAuthToken;
import org.folio.rest.impl.WireMockTestBase;
import org.folio.rest.jaxrs.model.JsonapiError;
import org.folio.rest.jaxrs.model.Month;
import org.folio.rest.jaxrs.model.UCSettings;
import org.folio.rest.jaxrs.model.UCSettingsPatchRequest;
import org.folio.rest.jaxrs.model.UCSettingsPatchRequestData;
import org.folio.rest.jaxrs.model.UCSettingsPatchRequestDataAttributes;

@RunWith(VertxUnitRunner.class)
public class EholdingsKbCredentialsUCImplTest extends WireMockTestBase {

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
    mockFailedVerification();
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

  private void mockSuccessfulVerification() {
    stubFor(get(urlMatching("/uc/costperuse.*")).willReturn(aResponse().withStatus(SC_OK)));
  }

  private void mockFailedVerification() {
    stubFor(get(urlMatching("/uc/costperuse.*")).willReturn(aResponse().withStatus(SC_BAD_REQUEST)));
  }

  private void mockAuthToken() {
    UCAuthToken stubToken = new UCAuthToken("access_token", "Bearer", 3600L, "openid");
    stubFor(post(urlPathMatching("/oauth-proxy/token"))
      .willReturn(aResponse().withStatus(SC_OK).withBody(Json.encode(stubToken)))
    );
  }
}
