package org.folio.rest.impl;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNPROCESSABLE_ENTITY;
import static org.folio.repository.uc.UcCredentialsTableConstants.UC_CREDENTIALS_TABLE_NAME;
import static org.folio.util.AssertTestUtil.assertErrorContainsTitle;
import static org.folio.util.TestUtil.clearDataFromTable;
import static org.folio.util.UcCredentialsTestUtil.STUB_CLIENT_ID;
import static org.folio.util.UcCredentialsTestUtil.STUB_CLIENT_SECRET;
import static org.folio.util.UcCredentialsTestUtil.UC_CREDENTIALS_ENDPOINT;
import static org.folio.util.UcCredentialsTestUtil.getUcCredentials;
import static org.folio.util.UcCredentialsTestUtil.setUpUcCredentials;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.tomakehurst.wiremock.matching.AnythingPattern;
import io.vertx.core.json.Json;
import org.folio.client.uc.model.UcAuthToken;
import org.folio.rest.jaxrs.model.JsonapiError;
import org.folio.rest.jaxrs.model.UCCredentials;
import org.folio.rest.jaxrs.model.UCCredentialsAttributes;
import org.folio.rest.jaxrs.model.UCCredentialsClientId;
import org.folio.rest.jaxrs.model.UCCredentialsClientSecret;
import org.folio.rest.jaxrs.model.UCCredentialsPresence;
import org.folio.util.IntegrationTestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

class UsageConsolidationCredentialsApiIntegrationTest extends IntegrationTestBase {

  @AfterEach
  void tearDown() {
    clearDataFromTable(vertx, UC_CREDENTIALS_TABLE_NAME);
  }

  @Test
  void shouldReturnUcCredentialsPresenceWithTrueOnGetWhenCredentialsExists() {
    setUpUcCredentials(vertx);

    var actual = getWithOk(UC_CREDENTIALS_ENDPOINT).as(UCCredentialsPresence.class);

    assertTrue(actual.getAttributes().getIsPresent());
  }

  @Test
  void shouldReturnUcCredentialsPresenceWithFalseOnGetWhenCredentialsNotExist() {
    var actual = getWithOk(UC_CREDENTIALS_ENDPOINT).as(UCCredentialsPresence.class);

    assertFalse(actual.getAttributes().getIsPresent());
  }

  @Test
  void shouldSaveNewValidUcCredentials() {
    mockAuthToken(SC_OK);

    var requestBody = getRequestBody(STUB_CLIENT_ID, STUB_CLIENT_SECRET);
    putWithNoContent(UC_CREDENTIALS_ENDPOINT, requestBody);

    var actualCredentials = getUcCredentials(vertx);

    assertEquals(STUB_CLIENT_ID, actualCredentials.clientId());
    assertEquals(STUB_CLIENT_SECRET, actualCredentials.clientSecret());
  }

  @Test
  void shouldSaveNewInvalidUcCredentials() {
    mockAuthToken(SC_BAD_REQUEST);

    var requestBody = getRequestBody(STUB_CLIENT_ID, STUB_CLIENT_SECRET);
    var error = putWithStatus(UC_CREDENTIALS_ENDPOINT, requestBody, SC_UNPROCESSABLE_ENTITY).as(JsonapiError.class);

    var actualCredentials = getUcCredentials(vertx);

    assertNull(actualCredentials);
    assertErrorContainsTitle(error, "Invalid Usage Consolidation Credentials");
  }

  @Test
  void shouldUpdateExistingWithNewValidUcCredentials() {
    setUpUcCredentials(vertx);
    mockAuthToken(SC_OK);

    var newClientId = "NEW_ID";
    var newClientSecret = "NEW_TOKEN";
    var requestBody = getRequestBody(newClientId, newClientSecret);
    putWithNoContent(UC_CREDENTIALS_ENDPOINT, requestBody);

    var actualCredentials = getUcCredentials(vertx);

    assertEquals(newClientId, actualCredentials.clientId());
    assertEquals(newClientSecret, actualCredentials.clientSecret());
  }

  @Test
  void shouldNotUpdateExistingWithNewInvalidUcCredentials() {
    setUpUcCredentials(vertx);
    mockAuthToken(SC_BAD_REQUEST);

    var newClientId = "NEW_ID";
    var newClientSecret = "NEW_TOKEN";
    var requestBody = getRequestBody(newClientId, newClientSecret);
    var error = putWithStatus(UC_CREDENTIALS_ENDPOINT, requestBody, SC_UNPROCESSABLE_ENTITY).as(JsonapiError.class);
    assertErrorContainsTitle(error, "Invalid Usage Consolidation Credentials");

    var actualCredentials = getUcCredentials(vertx);
    assertEquals(STUB_CLIENT_ID, actualCredentials.clientId());
    assertEquals(STUB_CLIENT_SECRET, actualCredentials.clientSecret());
  }

  @Test
  void shouldReturn200OnGetUcCredentialsClientId() throws Exception {
    setUpUcCredentials(vertx);

    var actualResponse = getWithOk(UC_CREDENTIALS_ENDPOINT + "/clientId").asString();

    JSONAssert.assertEquals(getClientIdJsonBody(), actualResponse, false);
  }

  @Test
  void shouldReturn200OnGetUcCredentialsClientSecret() throws Exception {
    setUpUcCredentials(vertx);

    var actualResponse = getWithOk(UC_CREDENTIALS_ENDPOINT + "/clientSecret").asString();

    JSONAssert.assertEquals(getClientSecretJsonBody(), actualResponse, false);
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

  private String getClientIdJsonBody() {
    var ucCredentialsClientId = new UCCredentialsClientId()
      .withClientId(STUB_CLIENT_ID);
    return Json.encode(ucCredentialsClientId);
  }

  private String getClientSecretJsonBody() {
    var ucCredentialsClientSecret = new UCCredentialsClientSecret()
      .withClientSecret(STUB_CLIENT_SECRET);
    return Json.encode(ucCredentialsClientSecret);
  }

  private void mockAuthToken(int status) {
    var stubToken = new UcAuthToken("access_token", "Bearer", 3600L, "openid");
    mockPost(equalTo("/oauth-proxy/token"), new AnythingPattern(), Json.encode(stubToken), status);
  }
}
