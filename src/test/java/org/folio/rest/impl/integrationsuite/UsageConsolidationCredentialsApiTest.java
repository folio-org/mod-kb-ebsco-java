package org.folio.rest.impl.integrationsuite;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNPROCESSABLE_ENTITY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import static org.folio.repository.uc.UCCredentialsTableConstants.UC_CREDENTIALS_TABLE_NAME;
import static org.folio.util.AssertTestUtil.assertErrorContainsTitle;
import static org.folio.util.KBTestUtil.clearDataFromTable;
import static org.folio.util.UCCredentialsTestUtil.STUB_CLIENT_ID;
import static org.folio.util.UCCredentialsTestUtil.STUB_CLIENT_SECRET;
import static org.folio.util.UCCredentialsTestUtil.UC_CREDENTIALS_ENDPOINT;
import static org.folio.util.UCCredentialsTestUtil.getUCCredentials;
import static org.folio.util.UCCredentialsTestUtil.setUpUCCredentials;

import io.vertx.core.json.Json;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.json.JSONException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

import org.folio.client.uc.UCAuthEbscoClient;
import org.folio.client.uc.model.UCAuthToken;
import org.folio.rest.impl.WireMockTestBase;
import org.folio.rest.jaxrs.model.JsonapiError;
import org.folio.rest.jaxrs.model.UCCredentials;
import org.folio.rest.jaxrs.model.UCCredentialsAttributes;
import org.folio.rest.jaxrs.model.UCCredentialsPresence;
import org.folio.rest.jaxrs.model.UCCredentialsClientId;
import org.folio.rest.jaxrs.model.UCCredentialsClientSecret;

@RunWith(VertxUnitRunner.class)
public class UsageConsolidationCredentialsApiTest extends WireMockTestBase {

  @Autowired
  private UCAuthEbscoClient authEbscoClient;

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    ReflectionTestUtils.setField(authEbscoClient, "baseUrl", getWiremockUrl());
  }

  @After
  public void tearDown() {
    clearDataFromTable(vertx, UC_CREDENTIALS_TABLE_NAME);
  }

  @Test
  public void shouldReturnUCCredentialsPresenceWithTrueOnGetWhenCredentialsExists() {
    setUpUCCredentials(vertx);

    var actual = getWithOk(UC_CREDENTIALS_ENDPOINT).as(UCCredentialsPresence.class);

    assertTrue(actual.getAttributes().getIsPresent());
  }

  @Test
  public void shouldReturnUCCredentialsPresenceWithFalseOnGetWhenCredentialsNotExist() {
    var actual = getWithOk(UC_CREDENTIALS_ENDPOINT).as(UCCredentialsPresence.class);

    assertFalse(actual.getAttributes().getIsPresent());
  }

  @Test
  public void shouldSaveNewValidUCCredentials() {
    mockAuthToken(SC_OK);

    String requestBody = getRequestBody(STUB_CLIENT_ID, STUB_CLIENT_SECRET);
    putWithNoContent(UC_CREDENTIALS_ENDPOINT, requestBody);

    var actualCredentials = getUCCredentials(vertx);

    assertEquals(STUB_CLIENT_ID, actualCredentials.getClientId());
    assertEquals(STUB_CLIENT_SECRET, actualCredentials.getClientSecret());
  }

  @Test
  public void shouldSaveNewInvalidUCCredentials() {
    mockAuthToken(SC_BAD_REQUEST);

    String requestBody = getRequestBody(STUB_CLIENT_ID, STUB_CLIENT_SECRET);
    var error = putWithStatus(UC_CREDENTIALS_ENDPOINT, requestBody, SC_UNPROCESSABLE_ENTITY).as(JsonapiError.class);

    var actualCredentials = getUCCredentials(vertx);

    assertNull(actualCredentials);
    assertErrorContainsTitle(error, "Invalid Usage Consolidation Credentials");
  }

  @Test
  public void shouldUpdateExistingWithNewValidUCCredentials() {
    setUpUCCredentials(vertx);
    mockAuthToken(SC_OK);

    var newClientId = "NEW_ID";
    var newClientSecret = "NEW_TOKEN";
    String requestBody = getRequestBody(newClientId, newClientSecret);
    putWithNoContent(UC_CREDENTIALS_ENDPOINT, requestBody);

    var actualCredentials = getUCCredentials(vertx);

    assertEquals(newClientId, actualCredentials.getClientId());
    assertEquals(newClientSecret, actualCredentials.getClientSecret());
  }

  @Test
  public void shouldNotUpdateExistingWithNewInvalidUCCredentials() {
    setUpUCCredentials(vertx);
    mockAuthToken(SC_BAD_REQUEST);

    var newClientId = "NEW_ID";
    var newClientSecret = "NEW_TOKEN";
    String requestBody = getRequestBody(newClientId, newClientSecret);
    var error = putWithStatus(UC_CREDENTIALS_ENDPOINT, requestBody, SC_UNPROCESSABLE_ENTITY).as(JsonapiError.class);

    assertErrorContainsTitle(error, "Invalid Usage Consolidation Credentials");
    var actualCredentials = getUCCredentials(vertx);

    assertEquals(STUB_CLIENT_ID, actualCredentials.getClientId());
    assertEquals(STUB_CLIENT_SECRET, actualCredentials.getClientSecret());
  }

  @Test
  public void shouldReturn200OnGetUCCredentialsClientId() throws JSONException {
    setUpUCCredentials(vertx);

    String actualResponse = getWithOk(UC_CREDENTIALS_ENDPOINT + "/clientId").asString();

    JSONAssert.assertEquals(getClientIdJSONBody(STUB_CLIENT_ID), actualResponse, false);
  }

  @Test
  public void shouldReturn200OnGetUCCredentialsClientSecret() throws JSONException {
    setUpUCCredentials(vertx);

    String actualResponse = getWithOk(UC_CREDENTIALS_ENDPOINT + "/clientSecret").asString();

    JSONAssert.assertEquals(getClientSecretJSONBody(STUB_CLIENT_SECRET), actualResponse, false);
  }

  private String getRequestBody(String clientId, String clientSecret) {
    var credentials = new UCCredentials()
      .withType(UCCredentials.Type.UC_CREDENTIALS)
      .withAttributes(new UCCredentialsAttributes()
        .withClientId(clientId)
        .withClientSecret(clientSecret)
      );
    return Json.encode(credentials);
  }

  private String getClientIdJSONBody(String clientId) {
    var ucCredentialsClientId = new UCCredentialsClientId()
      .withClientId(clientId);
    return Json.encode(ucCredentialsClientId);
  }

  private String getClientSecretJSONBody(String clientSecret) {
    var ucCredentialsClientSecret = new UCCredentialsClientSecret()
      .withClientSecret(clientSecret);
    return Json.encode(ucCredentialsClientSecret);
  }

  private void mockAuthToken(int status) {
    UCAuthToken stubToken = new UCAuthToken("access_token", "Bearer", 3600L, "openid");
    stubFor(post(urlPathMatching("/oauth-proxy/token"))
      .willReturn(aResponse().withStatus(status).withBody(Json.encode(stubToken)))
    );
  }
}
