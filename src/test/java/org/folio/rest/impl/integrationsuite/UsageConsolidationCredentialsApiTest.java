package org.folio.rest.impl.integrationsuite;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNPROCESSABLE_ENTITY;
import static org.folio.repository.uc.UcCredentialsTableConstants.UC_CREDENTIALS_TABLE_NAME;
import static org.folio.util.AssertTestUtil.assertErrorContainsTitle;
import static org.folio.util.KbTestUtil.clearDataFromTable;
import static org.folio.util.UcCredentialsTestUtil.STUB_CLIENT_ID;
import static org.folio.util.UcCredentialsTestUtil.STUB_CLIENT_SECRET;
import static org.folio.util.UcCredentialsTestUtil.UC_CREDENTIALS_ENDPOINT;
import static org.folio.util.UcCredentialsTestUtil.getUcCredentials;
import static org.folio.util.UcCredentialsTestUtil.setUpUcCredentials;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import io.vertx.core.json.Json;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.folio.client.uc.UcAuthEbscoClient;
import org.folio.client.uc.model.UcAuthToken;
import org.folio.rest.impl.WireMockTestBase;
import org.folio.rest.jaxrs.model.JsonapiError;
import org.folio.rest.jaxrs.model.UCCredentials;
import org.folio.rest.jaxrs.model.UCCredentialsAttributes;
import org.folio.rest.jaxrs.model.UCCredentialsClientId;
import org.folio.rest.jaxrs.model.UCCredentialsClientSecret;
import org.folio.rest.jaxrs.model.UCCredentialsPresence;
import org.json.JSONException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(VertxUnitRunner.class)
public class UsageConsolidationCredentialsApiTest extends WireMockTestBase {

  @Autowired
  private UcAuthEbscoClient authEbscoClient;

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
  public void shouldReturnUcCredentialsPresenceWithTrueOnGetWhenCredentialsExists() {
    setUpUcCredentials(vertx);

    var actual = getWithOk(UC_CREDENTIALS_ENDPOINT).as(UCCredentialsPresence.class);

    assertTrue(actual.getAttributes().getIsPresent());
  }

  @Test
  public void shouldReturnUcCredentialsPresenceWithFalseOnGetWhenCredentialsNotExist() {
    var actual = getWithOk(UC_CREDENTIALS_ENDPOINT).as(UCCredentialsPresence.class);

    assertFalse(actual.getAttributes().getIsPresent());
  }

  @Test
  public void shouldSaveNewValidUcCredentials() {
    mockAuthToken(SC_OK);

    String requestBody = getRequestBody(STUB_CLIENT_ID, STUB_CLIENT_SECRET);
    putWithNoContent(UC_CREDENTIALS_ENDPOINT, requestBody);

    var actualCredentials = getUcCredentials(vertx);

    assertEquals(STUB_CLIENT_ID, actualCredentials.getClientId());
    assertEquals(STUB_CLIENT_SECRET, actualCredentials.getClientSecret());
  }

  @Test
  public void shouldSaveNewInvalidUcCredentials() {
    mockAuthToken(SC_BAD_REQUEST);

    String requestBody = getRequestBody(STUB_CLIENT_ID, STUB_CLIENT_SECRET);
    var error = putWithStatus(UC_CREDENTIALS_ENDPOINT, requestBody, SC_UNPROCESSABLE_ENTITY).as(JsonapiError.class);

    var actualCredentials = getUcCredentials(vertx);

    assertNull(actualCredentials);
    assertErrorContainsTitle(error, "Invalid Usage Consolidation Credentials");
  }

  @Test
  public void shouldUpdateExistingWithNewValidUcCredentials() {
    setUpUcCredentials(vertx);
    mockAuthToken(SC_OK);

    var newClientId = "NEW_ID";
    var newClientSecret = "NEW_TOKEN";
    String requestBody = getRequestBody(newClientId, newClientSecret);
    putWithNoContent(UC_CREDENTIALS_ENDPOINT, requestBody);

    var actualCredentials = getUcCredentials(vertx);

    assertEquals(newClientId, actualCredentials.getClientId());
    assertEquals(newClientSecret, actualCredentials.getClientSecret());
  }

  @Test
  public void shouldNotUpdateExistingWithNewInvalidUcCredentials() {
    setUpUcCredentials(vertx);
    mockAuthToken(SC_BAD_REQUEST);

    var newClientId = "NEW_ID";
    var newClientSecret = "NEW_TOKEN";
    String requestBody = getRequestBody(newClientId, newClientSecret);
    var error = putWithStatus(UC_CREDENTIALS_ENDPOINT, requestBody, SC_UNPROCESSABLE_ENTITY).as(JsonapiError.class);

    assertErrorContainsTitle(error, "Invalid Usage Consolidation Credentials");
    var actualCredentials = getUcCredentials(vertx);

    assertEquals(STUB_CLIENT_ID, actualCredentials.getClientId());
    assertEquals(STUB_CLIENT_SECRET, actualCredentials.getClientSecret());
  }

  @Test
  public void shouldReturn200OnGetUcCredentialsClientId() throws JSONException {
    setUpUcCredentials(vertx);

    String actualResponse = getWithOk(UC_CREDENTIALS_ENDPOINT + "/clientId").asString();

    JSONAssert.assertEquals(getClientIdJsonBody(STUB_CLIENT_ID), actualResponse, false);
  }

  @Test
  public void shouldReturn200OnGetUcCredentialsClientSecret() throws JSONException {
    setUpUcCredentials(vertx);

    String actualResponse = getWithOk(UC_CREDENTIALS_ENDPOINT + "/clientSecret").asString();

    JSONAssert.assertEquals(getClientSecretJsonBody(STUB_CLIENT_SECRET), actualResponse, false);
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

  private String getClientIdJsonBody(String clientId) {
    var ucCredentialsClientId = new UCCredentialsClientId()
      .withClientId(clientId);
    return Json.encode(ucCredentialsClientId);
  }

  private String getClientSecretJsonBody(String clientSecret) {
    var ucCredentialsClientSecret = new UCCredentialsClientSecret()
      .withClientSecret(clientSecret);
    return Json.encode(ucCredentialsClientSecret);
  }

  private void mockAuthToken(int status) {
    UcAuthToken stubToken = new UcAuthToken("access_token", "Bearer", 3600L, "openid");
    stubFor(post(urlPathMatching("/oauth-proxy/token"))
      .willReturn(aResponse().withStatus(status).withBody(Json.encode(stubToken)))
    );
  }
}
