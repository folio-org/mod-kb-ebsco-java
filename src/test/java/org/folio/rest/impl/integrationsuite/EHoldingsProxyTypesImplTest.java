package org.folio.rest.impl.integrationsuite;

import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;

import static org.folio.repository.assigneduser.AssignedUsersConstants.ASSIGNED_USERS_TABLE_NAME;
import static org.folio.repository.kbcredentials.KbCredentialsTableConstants.KB_CREDENTIALS_TABLE_NAME;
import static org.folio.test.util.TestUtil.mockGet;
import static org.folio.test.util.TestUtil.readFile;
import static org.folio.util.AssignedUsersTestUtil.insertAssignedUser;
import static org.folio.util.KBTestUtil.clearDataFromTable;
import static org.folio.util.KbCredentialsTestUtil.STUB_CREDENTIALS_NAME;
import static org.folio.util.KbCredentialsTestUtil.STUB_INVALID_TOKEN_HEADER;
import static org.folio.util.KbCredentialsTestUtil.insertKbCredentials;

import java.io.IOException;
import java.net.URISyntaxException;

import com.github.tomakehurst.wiremock.matching.RegexPattern;
import io.restassured.http.Header;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.JSONAssert;

import org.folio.okapi.common.XOkapiHeaders;
import org.folio.rest.impl.WireMockTestBase;
import org.folio.rest.jaxrs.model.JsonapiError;

@RunWith(VertxUnitRunner.class)
public class EHoldingsProxyTypesImplTest extends WireMockTestBase {

  private static final String RMI_PROXIES_URL = "/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/proxies.*";
  private static final String EHOLDINGS_PROXY_TYPES_URL = "eholdings/proxy-types";
  private static final String EHOLDINGS_PROXY_TYPES_BY_CREDENTIALS_ID_URL = "/eholdings/kb-credentials/%s/proxy-types";
  private static final String STUB_CREDENTILS_ID = "12312312-1231-1231-a111-111111111111";
  private static final String JOHN_ID = "47d9ca93-9c82-4d6a-8d7f-7a73963086b9";
  private static final String JANE_ID = "781fce7d-5cf5-490d-ad89-a3d192eb526c";
  private static final String johnToken = "eyJhbGciOiJIUzI1NiJ9." +
    "eyJzdWIiOiJqb2huX2RvZSIsInVzZXJfaWQiOiI0N2Q5Y2E5My05YzgyLTRkNmEtOGQ3Zi03YTczOTYzMDg2Y" +
    "jkiLCJpYXQiOjE1ODU4OTUxNDQsInRlbmFudCI6ImZzIn0.HTx-4aUFIPtEHO-6ZcYML6K3-0VRDGv3KX44JoT3hxg";
  private static final String janeToken = "eyJhbGciOiJIUzI1NiJ9." +
    "eyJzdWIiOiJqYW5lX2RvZSIsInVzZXJfaWQiOiI3ODFmY2U3ZC01Y2Y1LTQ5MGQtYWQ4OS1hM2QxOTJlYjUyN" +
    "mMiLCJpYXQiOjE1ODU4OTUxNDQsInRlbmFudCI6ImZzIn0.kM0PYy49d92g5qhqPgTFz8aknjO7fQlZ5kljCC_M3-c";
  private static final Header JOHN_TOKEN_HEADER = new Header(XOkapiHeaders.TOKEN, johnToken);
  private static final Header JANE_TOKEN_HEADER = new Header(XOkapiHeaders.TOKEN, janeToken);

  @After
  public void tearDown() {
    clearDataFromTable(vertx, ASSIGNED_USERS_TABLE_NAME);
    clearDataFromTable(vertx, KB_CREDENTIALS_TABLE_NAME);
  }

  @Test
  public void shouldReturnProxyTypesWhenUserAssignedToKbCredentials() throws IOException, URISyntaxException {
    insertKbCredentials(STUB_CREDENTILS_ID, getWiremockUrl(), STUB_CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);
    insertAssignedUser(JOHN_ID, STUB_CREDENTILS_ID, "john_doe", "John", null, "Doe", "patron", vertx);

    mockGet(new RegexPattern(RMI_PROXIES_URL), "responses/rmapi/proxytypes/get-proxy-types-response.json");

    String actual = getWithStatus(EHOLDINGS_PROXY_TYPES_URL, SC_OK, JOHN_TOKEN_HEADER).asString();

    String expected = readFile("responses/kb-ebsco/proxytypes/get-proxy-types-response.json");
    JSONAssert.assertEquals(expected, actual, true);
  }

  @Test
  public void shouldReturnProxyTypesWhenOneCredentialsExistsAndUserNotAssigned() throws IOException, URISyntaxException {
    insertKbCredentials(STUB_CREDENTILS_ID, getWiremockUrl(), STUB_CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);

    mockGet(new RegexPattern(RMI_PROXIES_URL), "responses/rmapi/proxytypes/get-proxy-types-response.json");

    String actual = getWithStatus(EHOLDINGS_PROXY_TYPES_URL, SC_OK, JOHN_TOKEN_HEADER).asString();

    String expected = readFile("responses/kb-ebsco/proxytypes/get-proxy-types-response.json");
    JSONAssert.assertEquals(expected, actual, true);
  }

  @Test
  public void shouldReturnEmptyProxyTypesFromEmptyRMApiResponse() throws IOException, URISyntaxException {
    String credentialsId = insertKbCredentials(getWiremockUrl(), STUB_CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);
    insertAssignedUser(JOHN_ID, credentialsId, "john_doe", "John", null, "Doe", "patron", vertx);
    mockGet(new RegexPattern(RMI_PROXIES_URL), "responses/rmapi/proxytypes/get-proxy-types-empty-response.json");

    String actual = getWithStatus(EHOLDINGS_PROXY_TYPES_URL, SC_OK, JOHN_TOKEN_HEADER).asString();

    String expected = readFile("responses/kb-ebsco/proxytypes/get-proxy-types-empty-response.json");
    JSONAssert.assertEquals(expected, actual, true);
  }

  @Test
  public void shouldReturn404WhenUserNotAssignedToKbCredentials() {
    String credentialsId = insertKbCredentials(getWiremockUrl(), STUB_CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);
    insertAssignedUser(JOHN_ID, credentialsId, "john_doe", "John", null, "Doe", "patron", vertx);
    insertKbCredentials(getWiremockUrl(), STUB_CREDENTIALS_NAME + "1", STUB_API_KEY, STUB_CUSTOMER_ID, vertx);

    JsonapiError error = getWithStatus(EHOLDINGS_PROXY_TYPES_URL, SC_NOT_FOUND, JANE_TOKEN_HEADER).as(JsonapiError.class);

    assertThat(error.getErrors().get(0).getTitle(), containsString("User credentials not found: userId = " + JANE_ID));
  }

  @Test
  public void shouldReturn401WhenNoTokenHeader(){
    JsonapiError error = getWithStatus(EHOLDINGS_PROXY_TYPES_URL, SC_UNAUTHORIZED, STUB_INVALID_TOKEN_HEADER).as(JsonapiError.class);
    assertThat(error.getErrors().get(0).getTitle(), containsString("Unauthorized"));
  }

  @Test
  public void shouldReturn401WhenRMAPIRequestCompletesWith401ErrorStatus() {
    String credentialsId = insertKbCredentials(getWiremockUrl(), STUB_CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);
    insertAssignedUser(JOHN_ID, credentialsId, "john_doe", "John", null, "Doe", "patron", vertx);

    mockGet(new RegexPattern(RMI_PROXIES_URL), SC_UNAUTHORIZED);
    final JsonapiError error = getWithStatus(EHOLDINGS_PROXY_TYPES_URL, SC_UNAUTHORIZED, JOHN_TOKEN_HEADER).as(JsonapiError.class);
    assertThat(error.getErrors().get(0).getTitle(), containsString("Unauthorized Access"));
  }

  @Test
  public void shouldReturn403WhenRMAPIRequestCompletesWith403ErrorStatus() {
    String credentialsId = insertKbCredentials(getWiremockUrl(), STUB_CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);
    insertAssignedUser(JOHN_ID, credentialsId, "john_doe", "John", null, "Doe", "patron", vertx);

    mockGet(new RegexPattern(RMI_PROXIES_URL), SC_FORBIDDEN);
    final JsonapiError error = getWithStatus(EHOLDINGS_PROXY_TYPES_URL, SC_FORBIDDEN, JOHN_TOKEN_HEADER).as(JsonapiError.class);
    assertThat(error.getErrors().get(0).getTitle(), containsString("Unauthorized"));
  }

  @Test
  public void shouldReturnProxyTypesCollection() throws IOException, URISyntaxException {
    insertKbCredentials(STUB_CREDENTILS_ID, getWiremockUrl(), STUB_CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);

    mockGet(new RegexPattern(RMI_PROXIES_URL), "responses/rmapi/proxytypes/get-proxy-types-response.json");

    String resourcePath = String.format(EHOLDINGS_PROXY_TYPES_BY_CREDENTIALS_ID_URL, STUB_CREDENTILS_ID);
    String actual = getWithOk(resourcePath).asString();

    String expected = readFile("responses/kb-ebsco/proxytypes/get-proxy-types-response.json");
    JSONAssert.assertEquals(expected, actual, true);
  }

  @Test
  public void shouldReturnEmptyCollection() throws IOException, URISyntaxException {
    insertKbCredentials(STUB_CREDENTILS_ID, getWiremockUrl(), STUB_CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);

    mockGet(new RegexPattern(RMI_PROXIES_URL), "responses/rmapi/proxytypes/get-proxy-types-response.json");

    String resourcePath = String.format(EHOLDINGS_PROXY_TYPES_BY_CREDENTIALS_ID_URL, STUB_CREDENTILS_ID);
    String actual = getWithOk(resourcePath).asString();

    String expected = readFile("responses/kb-ebsco/proxytypes/get-proxy-types-response.json");
    JSONAssert.assertEquals(expected, actual, true);
  }

  @Test
  public void shouldReturn401WhenRMAPIReturns401ErrorStatus() {
    String credentialsId = insertKbCredentials(getWiremockUrl(), STUB_CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);
    insertAssignedUser(JOHN_ID, credentialsId, "john_doe", "John", null, "Doe", "patron", vertx);

    mockGet(new RegexPattern(RMI_PROXIES_URL), SC_UNAUTHORIZED);
    final String path = String.format(EHOLDINGS_PROXY_TYPES_BY_CREDENTIALS_ID_URL, credentialsId);
    final JsonapiError error = getWithStatus(path, SC_UNAUTHORIZED, JOHN_TOKEN_HEADER).as(JsonapiError.class);
    assertThat(error.getErrors().get(0).getTitle(), containsString("Unauthorized Access"));
  }

  @Test
  public void shouldReturn403WhenRMAPIReturns403ErrorStatus() {
    String credentialsId = insertKbCredentials(getWiremockUrl(), STUB_CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);
    insertAssignedUser(JOHN_ID, credentialsId, "john_doe", "John", null, "Doe", "patron", vertx);

    mockGet(new RegexPattern(RMI_PROXIES_URL), SC_FORBIDDEN);
    final String path = String.format(EHOLDINGS_PROXY_TYPES_BY_CREDENTIALS_ID_URL, credentialsId);
    final JsonapiError error = getWithStatus(path, SC_FORBIDDEN, JOHN_TOKEN_HEADER).as(JsonapiError.class);
    assertThat(error.getErrors().get(0).getTitle(), containsString("Unauthorized"));
  }

  @Test
  public void shouldReturn404WhenCredentialsNotFOund() {
    final String path = String.format(EHOLDINGS_PROXY_TYPES_BY_CREDENTIALS_ID_URL, "11111111-1111-1111-a111-111111111111");
    final JsonapiError error = getWithStatus(path, SC_NOT_FOUND, JOHN_TOKEN_HEADER).as(JsonapiError.class);
    assertThat(error.getErrors().get(0).getTitle(), containsString("KbCredentials not found by id"));
  }
}

