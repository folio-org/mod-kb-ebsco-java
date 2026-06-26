package org.folio.rest.impl;

import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static org.folio.HttpStatus.SC_BAD_REQUEST;
import static org.folio.HttpStatus.SC_FORBIDDEN;
import static org.folio.HttpStatus.SC_NOT_FOUND;
import static org.folio.HttpStatus.SC_NO_CONTENT;
import static org.folio.HttpStatus.SC_OK;
import static org.folio.HttpStatus.SC_UNAUTHORIZED;
import static org.folio.repository.assigneduser.AssignedUsersConstants.ASSIGNED_USERS_TABLE_NAME;
import static org.folio.repository.kbcredentials.KbCredentialsTableConstants.KB_CREDENTIALS_TABLE_NAME;
import static org.folio.util.AssertTestUtil.assertErrorContainsTitle;
import static org.folio.util.AssertTestUtil.assertJsonEqual;
import static org.folio.util.AssignedUsersTestUtil.saveAssignedUser;
import static org.folio.util.KbCredentialsTestUtil.CREDENTIALS_NAME;
import static org.folio.util.KbCredentialsTestUtil.saveKbCredentials;
import static org.folio.util.TestUtil.clearDataFromTable;
import static org.folio.util.TestUtil.readFile;

import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import com.github.tomakehurst.wiremock.matching.RegexPattern;
import org.folio.rest.jaxrs.model.JsonapiError;
import org.folio.util.IntegrationTestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class EholdingsRootProxyImplIntegrationTest extends IntegrationTestBase {

  private static final String EHOLDINGS_ROOT_PROXY_URL = "eholdings/root-proxy";
  private static final String EHOLDINGS_ROOT_PROXY_BY_CREDENTIALS_ID_URL = "/eholdings/kb-credentials/%s/root-proxy";

  // RM API responses
  private static final String RMAPI_ROOT_PROXY_CUSTOM_LABELS_RESPONSE =
    "responses/rmapi/proxiescustomlabels/get-success-response.json";
  private static final String RMAPI_ROOT_PROXY_UPDATED_RESPONSE =
    "responses/rmapi/proxiescustomlabels/get-updated-response.json";
  private static final String RMAPI_ROOT_PROXY_400_ERROR_RESPONSE =
    "responses/rmapi/proxiescustomlabels/put-400-error-response.json";

  // KB-EBSCO expected responses
  private static final String KB_EBSCO_GET_ROOT_PROXY_RESPONSE =
    "responses/kb-ebsco/root-proxy/get-root-proxy-response.json";
  private static final String KB_EBSCO_PUT_ROOT_PROXY_UPDATED_RESPONSE =
    "responses/kb-ebsco/root-proxy/put-root-proxy-response-updated.json";

  // Request payloads
  private static final String KB_EBSCO_PUT_ROOT_PROXY_REQUEST =
    "requests/kb-ebsco/put-root-proxy.json";
  private static final String RMAPI_PUT_ROOT_PROXY_REQUEST =
    "requests/rmapi/proxiescustomlabels/put-root-proxy.json";

  @AfterEach
  void tearDown() {
    clearDataFromTable(vertx, ASSIGNED_USERS_TABLE_NAME);
    clearDataFromTable(vertx, KB_CREDENTIALS_TABLE_NAME);
  }

  @Test
  void shouldReturnRootProxyWhenUserAssignedToKbCredentials() {
    saveKbCredentials(STUB_CREDENTIALS_ID, getWiremockUrl(), vertx);
    saveAssignedUser(JOHN_ID, STUB_CREDENTIALS_ID, vertx);

    mockGet(new RegexPattern(rootProxyCustomLabelsRmApi()), readFile(RMAPI_ROOT_PROXY_CUSTOM_LABELS_RESPONSE));

    var actual = getWithStatus(EHOLDINGS_ROOT_PROXY_URL, SC_OK, JOHN_USER_ID_HEADER).asString();

    var expected = readFile(KB_EBSCO_GET_ROOT_PROXY_RESPONSE);
    assertJsonEqual(expected, actual, true);
  }

  @Test
  void shouldReturnRootProxyWhenOneCredentialsExistsAndUserNotAssigned() {
    saveKbCredentials(STUB_CREDENTIALS_ID, getWiremockUrl(), vertx);

    mockGet(new RegexPattern(rootProxyCustomLabelsRmApi()), readFile(RMAPI_ROOT_PROXY_CUSTOM_LABELS_RESPONSE));

    var actual = getWithStatus(EHOLDINGS_ROOT_PROXY_URL, SC_OK, JOHN_USER_ID_HEADER).asString();

    var expected = readFile(KB_EBSCO_GET_ROOT_PROXY_RESPONSE);
    assertJsonEqual(expected, actual, true);
  }

  @Test
  void shouldReturn404WhenUserNotAssignedToKbCredentials() {
    saveKbCredentials(STUB_CREDENTIALS_ID, getWiremockUrl(), vertx);
    saveAssignedUser(JOHN_ID, STUB_CREDENTIALS_ID, vertx);
    saveKbCredentials(getWiremockUrl(), CREDENTIALS_NAME + "1", STUB_API_KEY, "OTHER_CUSTOMER_ID", vertx);

    var error = getWithStatus(EHOLDINGS_ROOT_PROXY_URL, SC_NOT_FOUND, JANE_USER_ID_HEADER).as(JsonapiError.class);

    assertErrorContainsTitle(error, "KB Credentials do not exist or user with userId = " + JANE_ID
                                    + " is not assigned to any available knowledgebase.");
  }

  @Test
  void shouldReturn401WhenRmApiRequestCompletesWith401ErrorStatus() {
    saveKbCredentials(STUB_CREDENTIALS_ID, getWiremockUrl(), vertx);
    saveAssignedUser(JOHN_ID, STUB_CREDENTIALS_ID, vertx);

    mockGet(new RegexPattern(rootProxyCustomLabelsRmApi()), SC_UNAUTHORIZED);
    var error = getWithStatus(EHOLDINGS_ROOT_PROXY_URL, SC_UNAUTHORIZED, JOHN_USER_ID_HEADER).as(JsonapiError.class);
    assertErrorContainsTitle(error, "Unauthorized Access");
  }

  @Test
  void shouldReturn403WhenRmApiRequestCompletesWith403ErrorStatus() {
    saveKbCredentials(STUB_CREDENTIALS_ID, getWiremockUrl(), vertx);
    saveAssignedUser(JOHN_ID, STUB_CREDENTIALS_ID, vertx);

    mockGet(new RegexPattern(rootProxyCustomLabelsRmApi()), SC_FORBIDDEN);
    var error = getWithStatus(EHOLDINGS_ROOT_PROXY_URL, SC_FORBIDDEN, JOHN_USER_ID_HEADER).as(JsonapiError.class);
    assertErrorContainsTitle(error, "Unauthorized");
  }

  @Test
  void shouldReturnRootProxyWhenUserAssignedToCredentials() {
    saveKbCredentials(STUB_CREDENTIALS_ID, getWiremockUrl(), vertx);
    saveAssignedUser(JOHN_ID, STUB_CREDENTIALS_ID, vertx);

    mockGet(new RegexPattern(rootProxyCustomLabelsRmApi()), readFile(RMAPI_ROOT_PROXY_CUSTOM_LABELS_RESPONSE));

    var path = String.format(EHOLDINGS_ROOT_PROXY_BY_CREDENTIALS_ID_URL, STUB_CREDENTIALS_ID);
    var actual = getWithStatus(path, SC_OK, JOHN_USER_ID_HEADER).asString();

    var expected = readFile(KB_EBSCO_GET_ROOT_PROXY_RESPONSE);
    assertJsonEqual(expected, actual, true);
  }

  @Test
  void shouldReturn401WhenRmApiReturns401ErrorStatus() {
    saveKbCredentials(STUB_CREDENTIALS_ID, getWiremockUrl(), vertx);
    saveAssignedUser(JOHN_ID, STUB_CREDENTIALS_ID, vertx);

    mockGet(new RegexPattern(rootProxyCustomLabelsRmApi()), SC_UNAUTHORIZED);
    var path = String.format(EHOLDINGS_ROOT_PROXY_BY_CREDENTIALS_ID_URL, STUB_CREDENTIALS_ID);
    var error = getWithStatus(path, SC_UNAUTHORIZED, JOHN_USER_ID_HEADER).as(JsonapiError.class);
    assertErrorContainsTitle(error, "Unauthorized Access");
  }

  @Test
  void shouldReturn403WhenRmApiReturns403ErrorStatus() {
    saveKbCredentials(STUB_CREDENTIALS_ID, getWiremockUrl(), vertx);
    saveAssignedUser(JOHN_ID, STUB_CREDENTIALS_ID, vertx);

    mockGet(new RegexPattern(rootProxyCustomLabelsRmApi()), SC_FORBIDDEN);
    var path = String.format(EHOLDINGS_ROOT_PROXY_BY_CREDENTIALS_ID_URL, STUB_CREDENTIALS_ID);
    var error = getWithStatus(path, SC_FORBIDDEN, JOHN_USER_ID_HEADER).as(JsonapiError.class);
    assertErrorContainsTitle(error, "Unauthorized");
  }

  @Test
  void shouldReturn404WhenCredentialsNotFound() {
    var path = String.format(EHOLDINGS_ROOT_PROXY_BY_CREDENTIALS_ID_URL, "11111111-1111-1111-a111-111111111111");
    var error = getWithStatus(path, SC_NOT_FOUND, JOHN_USER_ID_HEADER).as(JsonapiError.class);
    assertErrorContainsTitle(error, "KbCredentials not found by id");
  }

  @Test
  void shouldReturnUpdatedProxyOnSuccessfulPut() {
    saveKbCredentials(STUB_CREDENTIALS_ID, getWiremockUrl(), vertx);

    mockGet(new RegexPattern(rootProxyCustomLabelsRmApi()), readFile(RMAPI_ROOT_PROXY_UPDATED_RESPONSE));
    mockPut(new RegexPattern(rootProxyCustomLabelsRmApi()), SC_NO_CONTENT);

    var expected = readFile(KB_EBSCO_PUT_ROOT_PROXY_UPDATED_RESPONSE);
    var path = String.format(EHOLDINGS_ROOT_PROXY_BY_CREDENTIALS_ID_URL, STUB_CREDENTIALS_ID);
    var actual = putWithOk(path, readFile(KB_EBSCO_PUT_ROOT_PROXY_REQUEST)).asString();

    assertJsonEqual(expected, actual, true);

    verifyPut(matching(rootProxyCustomLabelsRmApi()), equalToJson(readFile(RMAPI_PUT_ROOT_PROXY_REQUEST)));
  }

  @Test
  void shouldReturn400WhenInvalidProxyIdAndRmApiErrorOnPut() {
    saveKbCredentials(STUB_CREDENTIALS_ID, getWiremockUrl(), vertx);

    mockGet(new EqualToPattern(rootProxyCustomLabelsRmApi()), readFile(RMAPI_ROOT_PROXY_UPDATED_RESPONSE));
    mockPut(new EqualToPattern(rootProxyCustomLabelsRmApi()), readFile(RMAPI_ROOT_PROXY_400_ERROR_RESPONSE),
      SC_BAD_REQUEST);

    var path = String.format(EHOLDINGS_ROOT_PROXY_BY_CREDENTIALS_ID_URL, STUB_CREDENTIALS_ID);
    var error = putWithStatus(path, readFile(KB_EBSCO_PUT_ROOT_PROXY_REQUEST), SC_BAD_REQUEST).as(JsonapiError.class);

    assertErrorContainsTitle(error, "Invalid Proxy ID");
  }

  @Test
  void shouldReturnNotFoundWhenNoKbCredentialsStored() {
    var path = String.format(EHOLDINGS_ROOT_PROXY_BY_CREDENTIALS_ID_URL, STUB_CREDENTIALS_ID);
    var error = putWithStatus(path, readFile(KB_EBSCO_PUT_ROOT_PROXY_REQUEST), SC_NOT_FOUND).as(JsonapiError.class);
    assertErrorContainsTitle(error, "KbCredentials not found by id");
  }

  @Test
  void shouldReturn401WhenRmApiReturns401() {
    saveKbCredentials(STUB_CREDENTIALS_ID, getWiremockUrl(), vertx);

    mockGet(new RegexPattern(rootProxyCustomLabelsRmApi()), SC_UNAUTHORIZED);
    var path = String.format(EHOLDINGS_ROOT_PROXY_BY_CREDENTIALS_ID_URL, STUB_CREDENTIALS_ID);
    var error = getWithStatus(path, SC_UNAUTHORIZED, JOHN_USER_ID_HEADER).as(JsonapiError.class);
    assertErrorContainsTitle(error, "Unauthorized Access");
  }

  @Test
  void shouldReturn403WhenRmApiReturns403() {
    saveKbCredentials(STUB_CREDENTIALS_ID, getWiremockUrl(), vertx);

    mockGet(new RegexPattern(rootProxyCustomLabelsRmApi()), SC_FORBIDDEN);
    var path = String.format(EHOLDINGS_ROOT_PROXY_BY_CREDENTIALS_ID_URL, STUB_CREDENTIALS_ID);
    var error = getWithStatus(path, SC_FORBIDDEN, JOHN_USER_ID_HEADER).as(JsonapiError.class);
    assertErrorContainsTitle(error, "Unauthorized");
  }
}

