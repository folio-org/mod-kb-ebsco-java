package org.folio.rest.impl.integrationsuite;

import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.folio.repository.assigneduser.AssignedUsersConstants.ASSIGNED_USERS_TABLE_NAME;
import static org.folio.repository.kbcredentials.KbCredentialsTableConstants.KB_CREDENTIALS_TABLE_NAME;
import static org.folio.rest.impl.RmApiConstants.RMAPI_PROXIES_URL;
import static org.folio.test.util.TestUtil.mockGet;
import static org.folio.test.util.TestUtil.readFile;
import static org.folio.util.AssertTestUtil.assertErrorContainsTitle;
import static org.folio.util.AssignedUsersTestUtil.saveAssignedUser;
import static org.folio.util.KbCredentialsTestUtil.STUB_CREDENTIALS_NAME;
import static org.folio.util.KbCredentialsTestUtil.saveKbCredentials;
import static org.folio.util.KbTestUtil.clearDataFromTable;

import com.github.tomakehurst.wiremock.matching.RegexPattern;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import java.io.IOException;
import java.net.URISyntaxException;
import org.folio.rest.impl.WireMockTestBase;
import org.folio.rest.jaxrs.model.JsonapiError;
import org.folio.util.KbCredentialsTestUtil;
import org.json.JSONException;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.JSONAssert;

@RunWith(VertxUnitRunner.class)
public class EholdingsProxyTypesImplTest extends WireMockTestBase {

  private static final String EHOLDINGS_PROXY_TYPES_URL = "eholdings/proxy-types";
  private static final String EHOLDINGS_PROXY_TYPES_BY_CREDENTIALS_ID_URL = "/eholdings/kb-credentials/%s/proxy-types";

  @After
  public void tearDown() {
    clearDataFromTable(vertx, ASSIGNED_USERS_TABLE_NAME);
    clearDataFromTable(vertx, KB_CREDENTIALS_TABLE_NAME);
  }

  @Test
  public void shouldReturnProxyTypesWhenUserAssignedToKbCredentials()
    throws IOException, URISyntaxException, JSONException {
    saveKbCredentials(STUB_CREDENTIALS_ID, getWiremockUrl(), vertx);
    saveAssignedUser(JOHN_ID, STUB_CREDENTIALS_ID, vertx);

    mockGet(new RegexPattern(RMAPI_PROXIES_URL), "responses/rmapi/proxytypes/get-proxy-types-response.json");

    String actual = getWithStatus(EHOLDINGS_PROXY_TYPES_URL, SC_OK, JOHN_USER_ID_HEADER).asString();

    String expected = readFile("responses/kb-ebsco/proxytypes/get-proxy-types-response.json");
    JSONAssert.assertEquals(expected, actual, true);
  }

  @Test
  public void shouldReturnProxyTypesWhenOneCredentialsExistsAndUserNotAssigned()
    throws IOException, URISyntaxException, JSONException {
    saveKbCredentials(STUB_CREDENTIALS_ID, getWiremockUrl(), vertx);

    mockGet(new RegexPattern(RMAPI_PROXIES_URL), "responses/rmapi/proxytypes/get-proxy-types-response.json");

    String actual = getWithStatus(EHOLDINGS_PROXY_TYPES_URL, SC_OK, JOHN_USER_ID_HEADER).asString();

    String expected = readFile("responses/kb-ebsco/proxytypes/get-proxy-types-response.json");
    JSONAssert.assertEquals(expected, actual, true);
  }

  @Test
  public void shouldReturnEmptyProxyTypesFromEmptyRmApiResponse()
    throws IOException, URISyntaxException, JSONException {
    saveKbCredentials(STUB_CREDENTIALS_ID, getWiremockUrl(), vertx);
    saveAssignedUser(JOHN_ID, STUB_CREDENTIALS_ID, vertx);
    mockGet(new RegexPattern(RMAPI_PROXIES_URL), "responses/rmapi/proxytypes/get-proxy-types-empty-response.json");

    String actual = getWithStatus(EHOLDINGS_PROXY_TYPES_URL, SC_OK, JOHN_USER_ID_HEADER).asString();

    String expected = readFile("responses/kb-ebsco/proxytypes/get-proxy-types-empty-response.json");
    JSONAssert.assertEquals(expected, actual, true);
  }

  @Test
  public void shouldReturn404WhenUserNotAssignedToKbCredentials() {
    saveKbCredentials(STUB_CREDENTIALS_ID, getWiremockUrl(), vertx);
    saveAssignedUser(JOHN_ID, STUB_CREDENTIALS_ID, vertx);
    KbCredentialsTestUtil
      .saveKbCredentials(getWiremockUrl(), STUB_CREDENTIALS_NAME + "1", STUB_API_KEY, "OTHER_CUSTOMER_ID", vertx);

    JsonapiError error =
      getWithStatus(EHOLDINGS_PROXY_TYPES_URL, SC_NOT_FOUND, JANE_USER_ID_HEADER).as(JsonapiError.class);

    assertErrorContainsTitle(error, "KB Credentials do not exist or user with userId = " + JANE_ID
      + " is not assigned to any available knowledgebase.");
  }

  @Test
  public void shouldReturn401WhenRmApiRequestCompletesWith401ErrorStatus() {
    saveKbCredentials(STUB_CREDENTIALS_ID, getWiremockUrl(), vertx);
    saveAssignedUser(JOHN_ID, STUB_CREDENTIALS_ID, vertx);

    mockGet(new RegexPattern(RMAPI_PROXIES_URL), SC_UNAUTHORIZED);
    final JsonapiError error =
      getWithStatus(EHOLDINGS_PROXY_TYPES_URL, SC_UNAUTHORIZED, JOHN_USER_ID_HEADER).as(JsonapiError.class);
    assertErrorContainsTitle(error, "Unauthorized Access");
  }

  @Test
  public void shouldReturn403WhenRmApiRequestCompletesWith403ErrorStatus() {
    saveKbCredentials(STUB_CREDENTIALS_ID, getWiremockUrl(), vertx);
    saveAssignedUser(JOHN_ID, STUB_CREDENTIALS_ID, vertx);

    mockGet(new RegexPattern(RMAPI_PROXIES_URL), SC_FORBIDDEN);
    final JsonapiError error =
      getWithStatus(EHOLDINGS_PROXY_TYPES_URL, SC_FORBIDDEN, JOHN_USER_ID_HEADER).as(JsonapiError.class);
    assertErrorContainsTitle(error, "Unauthorized");
  }

  @Test
  public void shouldReturnProxyTypesCollection() throws IOException, URISyntaxException, JSONException {
    saveKbCredentials(STUB_CREDENTIALS_ID, getWiremockUrl(), vertx);

    mockGet(new RegexPattern(RMAPI_PROXIES_URL), "responses/rmapi/proxytypes/get-proxy-types-response.json");

    String resourcePath = String.format(EHOLDINGS_PROXY_TYPES_BY_CREDENTIALS_ID_URL, STUB_CREDENTIALS_ID);
    String actual = getWithOk(resourcePath).asString();

    String expected = readFile("responses/kb-ebsco/proxytypes/get-proxy-types-response.json");
    JSONAssert.assertEquals(expected, actual, true);
  }

  @Test
  public void shouldReturn401WhenRmApiReturns401ErrorStatus() {
    saveKbCredentials(STUB_CREDENTIALS_ID, getWiremockUrl(), vertx);
    saveAssignedUser(JOHN_ID, STUB_CREDENTIALS_ID, vertx);

    mockGet(new RegexPattern(RMAPI_PROXIES_URL), SC_UNAUTHORIZED);
    final String path = String.format(EHOLDINGS_PROXY_TYPES_BY_CREDENTIALS_ID_URL, STUB_CREDENTIALS_ID);
    final JsonapiError error = getWithStatus(path, SC_UNAUTHORIZED, JOHN_USER_ID_HEADER).as(JsonapiError.class);
    assertErrorContainsTitle(error, "Unauthorized Access");
  }

  @Test
  public void shouldReturn403WhenRmApiReturns403ErrorStatus() {
    saveKbCredentials(STUB_CREDENTIALS_ID, getWiremockUrl(), vertx);
    saveAssignedUser(JOHN_ID, STUB_CREDENTIALS_ID, vertx);

    mockGet(new RegexPattern(RMAPI_PROXIES_URL), SC_FORBIDDEN);
    final String path = String.format(EHOLDINGS_PROXY_TYPES_BY_CREDENTIALS_ID_URL, STUB_CREDENTIALS_ID);
    final JsonapiError error = getWithStatus(path, SC_FORBIDDEN, JOHN_USER_ID_HEADER).as(JsonapiError.class);
    assertErrorContainsTitle(error, "Unauthorized");
  }

  @Test
  public void shouldReturn404WhenCredentialsNotFound() {
    final String path =
      String.format(EHOLDINGS_PROXY_TYPES_BY_CREDENTIALS_ID_URL, "11111111-1111-1111-a111-111111111111");
    final JsonapiError error = getWithStatus(path, SC_NOT_FOUND, JOHN_USER_ID_HEADER).as(JsonapiError.class);
    assertErrorContainsTitle(error, "KbCredentials not found by id");
  }
}

