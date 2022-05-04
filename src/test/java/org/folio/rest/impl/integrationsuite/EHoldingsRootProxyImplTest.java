package org.folio.rest.impl.integrationsuite;

import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;

import static org.folio.repository.assigneduser.AssignedUsersConstants.ASSIGNED_USERS_TABLE_NAME;
import static org.folio.repository.kbcredentials.KbCredentialsTableConstants.KB_CREDENTIALS_TABLE_NAME;
import static org.folio.rest.impl.RmApiConstants.RMAPI_ROOT_PROXY_CUSTOM_LABELS_URL;
import static org.folio.test.util.TestUtil.mockGet;
import static org.folio.test.util.TestUtil.mockPut;
import static org.folio.test.util.TestUtil.readFile;
import static org.folio.util.AssertTestUtil.assertErrorContainsTitle;
import static org.folio.util.AssignedUsersTestUtil.saveAssignedUser;
import static org.folio.util.KBTestUtil.clearDataFromTable;
import static org.folio.util.KbCredentialsTestUtil.STUB_CREDENTIALS_NAME;
import static org.folio.util.KbCredentialsTestUtil.STUB_INVALID_TOKEN_HEADER;
import static org.folio.util.KbCredentialsTestUtil.saveKbCredentials;

import java.io.IOException;
import java.net.URISyntaxException;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import com.github.tomakehurst.wiremock.matching.RegexPattern;
import com.github.tomakehurst.wiremock.matching.UrlPathPattern;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.json.JSONException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.JSONAssert;

import org.folio.rest.impl.WireMockTestBase;
import org.folio.rest.jaxrs.model.JsonapiError;

@RunWith(VertxUnitRunner.class)
public class EHoldingsRootProxyImplTest extends WireMockTestBase {

  private static final String EHOLDINGS_ROOT_PROXY_URL = "eholdings/root-proxy";
  private static final String EHOLDINGS_ROOT_PROXY_BY_CREDENTIALS_ID_URL = "/eholdings/kb-credentials/%s/root-proxy";

  private static final String RMAPI_ROOT_PROXY_CUSTOM_LABELS_RESPONSE =
    "responses/rmapi/proxiescustomlabels/get-success-response.json";
  private static final String KB_EBSCO_GET_ROOT_PROXY_RESPONSE =
    "responses/kb-ebsco/root-proxy/get-root-proxy-response.json";

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
  }

  @After
  public void tearDown() {
    clearDataFromTable(vertx, ASSIGNED_USERS_TABLE_NAME);
    clearDataFromTable(vertx, KB_CREDENTIALS_TABLE_NAME);
  }

  @Test
  public void shouldReturnRootProxyWhenUserAssignedToKbCredentials() throws IOException, URISyntaxException, JSONException {
    saveKbCredentials(STUB_CREDENTIALS_ID, getWiremockUrl(), STUB_CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);
    saveAssignedUser(JOHN_ID, STUB_CREDENTIALS_ID, vertx);

    mockGet(new RegexPattern(RMAPI_ROOT_PROXY_CUSTOM_LABELS_URL), RMAPI_ROOT_PROXY_CUSTOM_LABELS_RESPONSE);

    String actual = getWithStatus(EHOLDINGS_ROOT_PROXY_URL, SC_OK, JOHN_TOKEN_HEADER).asString();

    String expected = readFile(KB_EBSCO_GET_ROOT_PROXY_RESPONSE);
    JSONAssert.assertEquals(expected, actual, true);
  }

  @Test
  public void shouldReturnRootProxyWhenOneCredentialsExistsAndUserNotAssigned() throws IOException, URISyntaxException, JSONException {
    saveKbCredentials(STUB_CREDENTIALS_ID, getWiremockUrl(), STUB_CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);

    mockGet(new RegexPattern(RMAPI_ROOT_PROXY_CUSTOM_LABELS_URL), RMAPI_ROOT_PROXY_CUSTOM_LABELS_RESPONSE);

    String actual = getWithStatus(EHOLDINGS_ROOT_PROXY_URL, SC_OK, JOHN_TOKEN_HEADER).asString();

    String expected = readFile(KB_EBSCO_GET_ROOT_PROXY_RESPONSE);
    JSONAssert.assertEquals(expected, actual, true);
  }

  @Test
  public void shouldReturn404WhenUserNotAssignedToKbCredentials() {
    saveKbCredentials(STUB_CREDENTIALS_ID, getWiremockUrl(), STUB_CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);
    saveAssignedUser(JOHN_ID, STUB_CREDENTIALS_ID, vertx);
    saveKbCredentials(getWiremockUrl(), STUB_CREDENTIALS_NAME + "1", STUB_API_KEY, "OTHER_CUSTOMER_ID", vertx);

    JsonapiError error = getWithStatus(EHOLDINGS_ROOT_PROXY_URL, SC_NOT_FOUND, JANE_TOKEN_HEADER).as(JsonapiError.class);

    assertErrorContainsTitle(error, "KB Credentials do not exist or user with userId = " + JANE_ID + " is not assigned to any available knowledgebase.");
  }

  @Test
  public void shouldReturn401WhenNoTokenHeader() {
    JsonapiError error = getWithStatus(EHOLDINGS_ROOT_PROXY_URL, SC_UNAUTHORIZED, STUB_INVALID_TOKEN_HEADER)
      .as(JsonapiError.class);
    assertErrorContainsTitle(error, "Invalid token");
  }

  @Test
  public void shouldReturn401WhenRMAPIRequestCompletesWith401ErrorStatus() {
    saveKbCredentials(STUB_CREDENTIALS_ID, getWiremockUrl(), STUB_CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);
    saveAssignedUser(JOHN_ID, STUB_CREDENTIALS_ID, vertx);

    mockGet(new RegexPattern(RMAPI_ROOT_PROXY_CUSTOM_LABELS_URL), SC_UNAUTHORIZED);
    final JsonapiError error = getWithStatus(EHOLDINGS_ROOT_PROXY_URL, SC_UNAUTHORIZED, JOHN_TOKEN_HEADER).as(JsonapiError.class);
    assertErrorContainsTitle(error, "Unauthorized Access");
  }

  @Test
  public void shouldReturn403WhenRMAPIRequestCompletesWith403ErrorStatus() {
    saveKbCredentials(STUB_CREDENTIALS_ID, getWiremockUrl(), STUB_CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);
    saveAssignedUser(JOHN_ID, STUB_CREDENTIALS_ID, vertx);

    mockGet(new RegexPattern(RMAPI_ROOT_PROXY_CUSTOM_LABELS_URL), SC_FORBIDDEN);
    final JsonapiError error = getWithStatus(EHOLDINGS_ROOT_PROXY_URL, SC_FORBIDDEN, JOHN_TOKEN_HEADER).as(JsonapiError.class);
    assertErrorContainsTitle(error, "Unauthorized");
  }

  @Test
  public void shouldReturnRootProxyWhenUserAssignedToCredentials() throws IOException, URISyntaxException, JSONException {
    saveKbCredentials(STUB_CREDENTIALS_ID, getWiremockUrl(), STUB_CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);
    saveAssignedUser(JOHN_ID, STUB_CREDENTIALS_ID, vertx);

    mockGet(new RegexPattern(RMAPI_ROOT_PROXY_CUSTOM_LABELS_URL), RMAPI_ROOT_PROXY_CUSTOM_LABELS_RESPONSE);

    final String path = String.format(EHOLDINGS_ROOT_PROXY_BY_CREDENTIALS_ID_URL, STUB_CREDENTIALS_ID);
    String actual = getWithStatus(path, SC_OK, JOHN_TOKEN_HEADER).asString();

    String expected = readFile(KB_EBSCO_GET_ROOT_PROXY_RESPONSE);
    JSONAssert.assertEquals(expected, actual, true);
  }

  @Test
  public void shouldReturn401WhenRMAPIReturns401ErrorStatus() {
    saveKbCredentials(STUB_CREDENTIALS_ID, getWiremockUrl(), STUB_CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);
    saveAssignedUser(JOHN_ID, STUB_CREDENTIALS_ID, vertx);

    mockGet(new RegexPattern(RMAPI_ROOT_PROXY_CUSTOM_LABELS_URL), SC_UNAUTHORIZED);
    final String path = String.format(EHOLDINGS_ROOT_PROXY_BY_CREDENTIALS_ID_URL, STUB_CREDENTIALS_ID);
    final JsonapiError error = getWithStatus(path, SC_UNAUTHORIZED, JOHN_TOKEN_HEADER).as(JsonapiError.class);
    assertErrorContainsTitle(error, "Unauthorized Access");
  }

  @Test
  public void shouldReturn403WhenRMAPIReturns403ErrorStatus() {
    saveKbCredentials(STUB_CREDENTIALS_ID, getWiremockUrl(), STUB_CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);
    saveAssignedUser(JOHN_ID, STUB_CREDENTIALS_ID, vertx);

    mockGet(new RegexPattern(RMAPI_ROOT_PROXY_CUSTOM_LABELS_URL), SC_FORBIDDEN);
    final String path = String.format(EHOLDINGS_ROOT_PROXY_BY_CREDENTIALS_ID_URL, STUB_CREDENTIALS_ID);
    final JsonapiError error = getWithStatus(path, SC_FORBIDDEN, JOHN_TOKEN_HEADER).as(JsonapiError.class);
    assertErrorContainsTitle(error, "Unauthorized");
  }

  @Test
  public void shouldReturn404WhenCredentialsNotFOund() {
    final String path = String.format(EHOLDINGS_ROOT_PROXY_BY_CREDENTIALS_ID_URL, "11111111-1111-1111-a111-111111111111");
    final JsonapiError error = getWithStatus(path, SC_NOT_FOUND, JOHN_TOKEN_HEADER).as(JsonapiError.class);
    assertErrorContainsTitle(error, "KbCredentials not found by id");
  }

  @Test
  public void shouldReturnUpdatedProxyOnSuccessfulPut() throws IOException, URISyntaxException, JSONException {
    saveKbCredentials(STUB_CREDENTIALS_ID, getWiremockUrl(), STUB_CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);
    String stubResponseFile = "responses/rmapi/proxiescustomlabels/get-updated-response.json";

    mockGet(new RegexPattern(RMAPI_ROOT_PROXY_CUSTOM_LABELS_URL), stubResponseFile);
    mockPut(new RegexPattern(RMAPI_ROOT_PROXY_CUSTOM_LABELS_URL), SC_NO_CONTENT);

    String expected = readFile("responses/kb-ebsco/root-proxy/put-root-proxy-response-updated.json");

    final String path = String.format(EHOLDINGS_ROOT_PROXY_BY_CREDENTIALS_ID_URL, STUB_CREDENTIALS_ID);

    String actual = putWithOk(path, readFile("requests/kb-ebsco/put-root-proxy.json")).asString();

    JSONAssert.assertEquals(expected, actual, true);

    verify(1, putRequestedFor(new UrlPathPattern(new RegexPattern(RMAPI_ROOT_PROXY_CUSTOM_LABELS_URL), true))
      .withRequestBody(equalToJson(readFile("requests/rmapi/proxiescustomlabels/put-root-proxy.json"))));
  }

  @Test
  public void shouldReturn400WhenInvalidProxyIDAndRMAPIErrorOnPut() throws IOException, URISyntaxException {
    String stubGetResponseFile = "responses/rmapi/proxiescustomlabels/get-updated-response.json";
    String stubPutResponseFile = "responses/rmapi/proxiescustomlabels/put-400-error-response.json";

    saveKbCredentials(STUB_CREDENTIALS_ID, getWiremockUrl(), STUB_CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);

    mockGet(new EqualToPattern(RMAPI_ROOT_PROXY_CUSTOM_LABELS_URL), stubGetResponseFile);
    stubFor(
      put(new UrlPathPattern(new EqualToPattern(RMAPI_ROOT_PROXY_CUSTOM_LABELS_URL), false))
        .willReturn(new ResponseDefinitionBuilder().withBody(readFile(stubPutResponseFile)).withStatus(SC_BAD_REQUEST)));

    final String path = String.format(EHOLDINGS_ROOT_PROXY_BY_CREDENTIALS_ID_URL, STUB_CREDENTIALS_ID);
    JsonapiError error =
      putWithStatus(path, readFile("requests/kb-ebsco/put-root-proxy.json"), SC_BAD_REQUEST).as(JsonapiError.class);

    assertErrorContainsTitle(error, "Invalid Proxy ID");
  }

  @Test
  public void shouldReturnNotFoundWhenNoKbCredentialsStored() throws IOException, URISyntaxException {
    final String path = String.format(EHOLDINGS_ROOT_PROXY_BY_CREDENTIALS_ID_URL, STUB_CREDENTIALS_ID);
    JsonapiError error =
      putWithStatus(path, readFile("requests/kb-ebsco/put-root-proxy.json"), SC_NOT_FOUND).as(JsonapiError.class);
    assertErrorContainsTitle(error, "KbCredentials not found by id");
  }

  @Test
  public void shouldReturn401WhenRMAPIReturns401() {
    saveKbCredentials(STUB_CREDENTIALS_ID, getWiremockUrl(), STUB_CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);

    mockGet(new RegexPattern(RMAPI_ROOT_PROXY_CUSTOM_LABELS_URL), SC_UNAUTHORIZED);
    final String path = String.format(EHOLDINGS_ROOT_PROXY_BY_CREDENTIALS_ID_URL, STUB_CREDENTIALS_ID);
    final JsonapiError error = getWithStatus(path, SC_UNAUTHORIZED, JOHN_TOKEN_HEADER).as(JsonapiError.class);
    assertErrorContainsTitle(error, "Unauthorized Access");
  }

  @Test
  public void shouldReturn403WhenRMAPIReturns403() {
    saveKbCredentials(STUB_CREDENTIALS_ID, getWiremockUrl(), STUB_CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);

    mockGet(new RegexPattern(RMAPI_ROOT_PROXY_CUSTOM_LABELS_URL), SC_FORBIDDEN);
    final String path = String.format(EHOLDINGS_ROOT_PROXY_BY_CREDENTIALS_ID_URL, STUB_CREDENTIALS_ID);
    final JsonapiError error = getWithStatus(path, SC_FORBIDDEN, JOHN_TOKEN_HEADER).as(JsonapiError.class);
    assertErrorContainsTitle(error, "Unauthorized");
  }

}

