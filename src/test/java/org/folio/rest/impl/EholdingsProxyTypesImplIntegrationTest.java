package org.folio.rest.impl;

import static org.folio.HttpStatus.SC_FORBIDDEN;
import static org.folio.HttpStatus.SC_NOT_FOUND;
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

import com.github.tomakehurst.wiremock.matching.RegexPattern;
import org.folio.rest.jaxrs.model.JsonapiError;
import org.folio.util.IntegrationTestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class EholdingsProxyTypesImplIntegrationTest extends IntegrationTestBase {

  // RM API responses
  private static final String GET_PROXY_TYPES_RESPONSE =
    "responses/rmapi/proxytypes/get-proxy-types-response.json";
  private static final String GET_PROXY_TYPES_EMPTY_RESPONSE =
    "responses/rmapi/proxytypes/get-proxy-types-empty-response.json";

  // KB-EBSCO expected responses
  private static final String EXPECTED_PROXY_TYPES_RESPONSE =
    "responses/kb-ebsco/proxytypes/get-proxy-types-response.json";
  private static final String EXPECTED_PROXY_TYPES_EMPTY_RESPONSE =
    "responses/kb-ebsco/proxytypes/get-proxy-types-empty-response.json";

  private static final String PROXY_TYPES_PATH = "eholdings/proxy-types";
  private static final String PROXY_TYPES_BY_CRED_ID_PATH = "/eholdings/kb-credentials/%s/proxy-types";

  @AfterEach
  void tearDown() {
    clearDataFromTable(vertx, ASSIGNED_USERS_TABLE_NAME);
    clearDataFromTable(vertx, KB_CREDENTIALS_TABLE_NAME);
  }

  @Test
  void shouldReturnProxyTypesWhenUserAssignedToKbCredentials() {
    saveKbCredentials(STUB_CREDENTIALS_ID, getWiremockUrl(), vertx);
    saveAssignedUser(JOHN_ID, STUB_CREDENTIALS_ID, vertx);

    mockGet(new RegexPattern(proxiesRmApi()), readFile(GET_PROXY_TYPES_RESPONSE));

    var actual = getWithStatus(PROXY_TYPES_PATH, SC_OK, JOHN_USER_ID_HEADER).asString();

    assertJsonEqual(readFile(EXPECTED_PROXY_TYPES_RESPONSE), actual, true);
  }

  @Test
  void shouldReturnProxyTypesWhenOneCredentialsExistsAndUserNotAssigned() {
    saveKbCredentials(STUB_CREDENTIALS_ID, getWiremockUrl(), vertx);

    mockGet(new RegexPattern(proxiesRmApi()), readFile(GET_PROXY_TYPES_RESPONSE));

    var actual = getWithStatus(PROXY_TYPES_PATH, SC_OK, JOHN_USER_ID_HEADER).asString();

    assertJsonEqual(readFile(EXPECTED_PROXY_TYPES_RESPONSE), actual, true);
  }

  @Test
  void shouldReturnEmptyProxyTypesFromEmptyRmApiResponse() {
    saveKbCredentials(STUB_CREDENTIALS_ID, getWiremockUrl(), vertx);
    saveAssignedUser(JOHN_ID, STUB_CREDENTIALS_ID, vertx);

    mockGet(new RegexPattern(proxiesRmApi()), readFile(GET_PROXY_TYPES_EMPTY_RESPONSE));

    var actual = getWithStatus(PROXY_TYPES_PATH, SC_OK, JOHN_USER_ID_HEADER).asString();

    assertJsonEqual(readFile(EXPECTED_PROXY_TYPES_EMPTY_RESPONSE), actual, true);
  }

  @Test
  void shouldReturn404WhenUserNotAssignedToKbCredentials() {
    saveKbCredentials(STUB_CREDENTIALS_ID, getWiremockUrl(), vertx);
    saveAssignedUser(JOHN_ID, STUB_CREDENTIALS_ID, vertx);
    saveKbCredentials(getWiremockUrl(), CREDENTIALS_NAME + "1", STUB_API_KEY, "OTHER_CUSTOMER_ID", vertx);

    var error = getWithStatus(PROXY_TYPES_PATH, SC_NOT_FOUND, JANE_USER_ID_HEADER).as(JsonapiError.class);

    assertErrorContainsTitle(error, "KB Credentials do not exist or user with userId = " + JANE_ID
                                    + " is not assigned to any available knowledgebase.");
  }

  @Test
  void shouldReturn401WhenRmApiRequestCompletesWith401ErrorStatus() {
    saveKbCredentials(STUB_CREDENTIALS_ID, getWiremockUrl(), vertx);
    saveAssignedUser(JOHN_ID, STUB_CREDENTIALS_ID, vertx);

    mockGet(new RegexPattern(proxiesRmApi()), SC_UNAUTHORIZED);

    var error = getWithStatus(PROXY_TYPES_PATH, SC_UNAUTHORIZED, JOHN_USER_ID_HEADER).as(JsonapiError.class);
    assertErrorContainsTitle(error, "Unauthorized Access");
  }

  @Test
  void shouldReturn403WhenRmApiRequestCompletesWith403ErrorStatus() {
    saveKbCredentials(STUB_CREDENTIALS_ID, getWiremockUrl(), vertx);
    saveAssignedUser(JOHN_ID, STUB_CREDENTIALS_ID, vertx);

    mockGet(new RegexPattern(proxiesRmApi()), SC_FORBIDDEN);

    var error = getWithStatus(PROXY_TYPES_PATH, SC_FORBIDDEN, JOHN_USER_ID_HEADER).as(JsonapiError.class);
    assertErrorContainsTitle(error, "Unauthorized");
  }

  @Test
  void shouldReturnProxyTypesCollection() {
    saveKbCredentials(STUB_CREDENTIALS_ID, getWiremockUrl(), vertx);

    mockGet(new RegexPattern(proxiesRmApi()), readFile(GET_PROXY_TYPES_RESPONSE));

    var resourcePath = String.format(PROXY_TYPES_BY_CRED_ID_PATH, STUB_CREDENTIALS_ID);
    var actual = getWithOk(resourcePath).asString();

    assertJsonEqual(readFile(EXPECTED_PROXY_TYPES_RESPONSE), actual, true);
  }

  @Test
  void shouldReturn401WhenRmApiReturns401ErrorStatus() {
    saveKbCredentials(STUB_CREDENTIALS_ID, getWiremockUrl(), vertx);
    saveAssignedUser(JOHN_ID, STUB_CREDENTIALS_ID, vertx);

    mockGet(new RegexPattern(proxiesRmApi()), SC_UNAUTHORIZED);

    var path = String.format(PROXY_TYPES_BY_CRED_ID_PATH, STUB_CREDENTIALS_ID);
    var error = getWithStatus(path, SC_UNAUTHORIZED, JOHN_USER_ID_HEADER).as(JsonapiError.class);
    assertErrorContainsTitle(error, "Unauthorized Access");
  }

  @Test
  void shouldReturn403WhenRmApiReturns403ErrorStatus() {
    saveKbCredentials(STUB_CREDENTIALS_ID, getWiremockUrl(), vertx);
    saveAssignedUser(JOHN_ID, STUB_CREDENTIALS_ID, vertx);

    mockGet(new RegexPattern(proxiesRmApi()), SC_FORBIDDEN);

    var path = String.format(PROXY_TYPES_BY_CRED_ID_PATH, STUB_CREDENTIALS_ID);
    var error = getWithStatus(path, SC_FORBIDDEN, JOHN_USER_ID_HEADER).as(JsonapiError.class);
    assertErrorContainsTitle(error, "Unauthorized");
  }

  @Test
  void shouldReturn404WhenCredentialsNotFound() {
    var path = String.format(PROXY_TYPES_BY_CRED_ID_PATH, "11111111-1111-1111-a111-111111111111");
    var error = getWithStatus(path, SC_NOT_FOUND, JOHN_USER_ID_HEADER).as(JsonapiError.class);
    assertErrorContainsTitle(error, "KbCredentials not found by id");
  }
}

