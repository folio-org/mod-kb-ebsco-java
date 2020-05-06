package org.folio.rest.impl.integrationsuite;

import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.apache.http.HttpStatus.*;
import static org.folio.test.util.TestUtil.*;
import static org.hamcrest.MatcherAssert.assertThat;

import static org.folio.repository.assigneduser.AssignedUsersConstants.ASSIGNED_USERS_TABLE_NAME;
import static org.folio.repository.kbcredentials.KbCredentialsTableConstants.KB_CREDENTIALS_TABLE_NAME;
import static org.folio.rest.impl.ProxiesTestData.JANE_ID;
import static org.folio.rest.impl.ProxiesTestData.JANE_TOKEN_HEADER;
import static org.folio.rest.impl.ProxiesTestData.JOHN_ID;
import static org.folio.rest.impl.ProxiesTestData.JOHN_TOKEN_HEADER;
import static org.folio.rest.impl.ProxiesTestData.STUB_CREDENTILS_ID;
import static org.folio.rest.impl.RmApiConstants.RMAPI_ROOT_PROXY_CUSTOM_LABELS_URL;
import static org.folio.util.AssignedUsersTestUtil.insertAssignedUser;
import static org.folio.util.KBTestUtil.clearDataFromTable;
import static org.folio.util.KBTestUtil.mockDefaultConfiguration;
import static org.folio.util.KbCredentialsTestUtil.STUB_CREDENTIALS_NAME;
import static org.folio.util.KbCredentialsTestUtil.STUB_INVALID_TOKEN_HEADER;
import static org.folio.util.KbCredentialsTestUtil.insertKbCredentials;
import static org.hamcrest.Matchers.*;

import java.io.IOException;
import java.net.URISyntaxException;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.common.Json;
import com.github.tomakehurst.wiremock.matching.*;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.apache.http.HttpStatus;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.JSONAssert;

import org.folio.rest.impl.WireMockTestBase;
import org.folio.rest.jaxrs.model.JsonapiError;
import org.folio.rest.jaxrs.model.RootProxy;
import org.folio.rest.jaxrs.model.RootProxyData;
import org.folio.rest.jaxrs.model.RootProxyDataAttributes;
import org.folio.rest.util.RestConstants;

@RunWith(VertxUnitRunner.class)
public class EHoldingsRootProxyImplTest extends WireMockTestBase {

  private static final String EHOLDINGS_ROOT_PROXY_URL = "eholdings/root-proxy";
  private static final String EHOLDINGS_ROOT_PROXY_BY_CREDENTIALS_ID_URL = "/eholdings/kb-credentials/%s/root-proxy";

  private static final String RMAPI_ROOT_PROXY_CUSTOM_LABELS_RESPONSE = "responses/rmapi/proxiescustomlabels/get-success-response.json";
  private static final String KB_EBSCO_GET_ROOT_PROXY_RESPONSE = "responses/kb-ebsco/root-proxy/get-root-proxy-response.json";

  @After
  public void tearDown() {
    clearDataFromTable(vertx, ASSIGNED_USERS_TABLE_NAME);
    clearDataFromTable(vertx, KB_CREDENTIALS_TABLE_NAME);
  }

  @Test
  public void shouldReturnRootProxyWhenUserAssignedToKbCredentials() throws IOException, URISyntaxException {
    insertKbCredentials(STUB_CREDENTILS_ID, getWiremockUrl(), STUB_CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);
    insertAssignedUser(JOHN_ID, STUB_CREDENTILS_ID, "john_doe", "John", null, "Doe", "patron", vertx);

    mockGet(new RegexPattern(RMAPI_ROOT_PROXY_CUSTOM_LABELS_URL), RMAPI_ROOT_PROXY_CUSTOM_LABELS_RESPONSE);

    String actual = getWithStatus(EHOLDINGS_ROOT_PROXY_URL, SC_OK, JOHN_TOKEN_HEADER).asString();

    String expected = readFile(KB_EBSCO_GET_ROOT_PROXY_RESPONSE);
    JSONAssert.assertEquals(expected, actual, true);
  }

  @Test
  public void shouldReturnRootProxyWhenOneCredentialsExistsAndUserNotAssigned() throws IOException, URISyntaxException {
    insertKbCredentials(STUB_CREDENTILS_ID, getWiremockUrl(), STUB_CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);

    mockGet(new RegexPattern(RMAPI_ROOT_PROXY_CUSTOM_LABELS_URL), RMAPI_ROOT_PROXY_CUSTOM_LABELS_RESPONSE);

    String actual = getWithStatus(EHOLDINGS_ROOT_PROXY_URL, SC_OK, JOHN_TOKEN_HEADER).asString();

    String expected = readFile(KB_EBSCO_GET_ROOT_PROXY_RESPONSE);
    JSONAssert.assertEquals(expected, actual, true);
  }

  @Test
  public void shouldReturn404WhenUserNotAssignedToKbCredentials() {
    insertKbCredentials(STUB_CREDENTILS_ID, getWiremockUrl(), STUB_CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);
    insertAssignedUser(JOHN_ID, STUB_CREDENTILS_ID, "john_doe", "John", null, "Doe", "patron", vertx);
    insertKbCredentials(getWiremockUrl(), STUB_CREDENTIALS_NAME + "1", STUB_API_KEY, STUB_CUSTOMER_ID, vertx);

    JsonapiError error = getWithStatus(EHOLDINGS_ROOT_PROXY_URL, SC_NOT_FOUND, JANE_TOKEN_HEADER).as(JsonapiError.class);

    Assert.assertThat(error.getErrors().get(0).getTitle(), containsString("User credentials not found: userId = " + JANE_ID));
  }

  @Test
  public void shouldReturn401WhenNoTokenHeader(){
    JsonapiError error = getWithStatus(EHOLDINGS_ROOT_PROXY_URL, SC_UNAUTHORIZED, STUB_INVALID_TOKEN_HEADER).as(JsonapiError.class);
    Assert.assertThat(error.getErrors().get(0).getTitle(), containsString("Unauthorized"));
  }

  @Test
  public void shouldReturn401WhenRMAPIRequestCompletesWith401ErrorStatus() {
    insertKbCredentials(STUB_CREDENTILS_ID, getWiremockUrl(), STUB_CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);
    insertAssignedUser(JOHN_ID, STUB_CREDENTILS_ID, "john_doe", "John", null, "Doe", "patron", vertx);

    mockGet(new RegexPattern(RMAPI_ROOT_PROXY_CUSTOM_LABELS_URL), SC_UNAUTHORIZED);
    final JsonapiError error = getWithStatus(EHOLDINGS_ROOT_PROXY_URL, SC_UNAUTHORIZED, JOHN_TOKEN_HEADER).as(JsonapiError.class);
    Assert.assertThat(error.getErrors().get(0).getTitle(), containsString("Unauthorized Access"));
  }

  @Test
  public void shouldReturn403WhenRMAPIRequestCompletesWith403ErrorStatus() {
    insertKbCredentials(STUB_CREDENTILS_ID, getWiremockUrl(), STUB_CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);
    insertAssignedUser(JOHN_ID, STUB_CREDENTILS_ID, "john_doe", "John", null, "Doe", "patron", vertx);

    mockGet(new RegexPattern(RMAPI_ROOT_PROXY_CUSTOM_LABELS_URL), SC_FORBIDDEN);
    final JsonapiError error = getWithStatus(EHOLDINGS_ROOT_PROXY_URL, SC_FORBIDDEN, JOHN_TOKEN_HEADER).as(JsonapiError.class);
    Assert.assertThat(error.getErrors().get(0).getTitle(), containsString("Unauthorized"));
  }

  @Test
  public void shouldReturnRootProxyWhenUserAssignedToCredentials() throws IOException, URISyntaxException {
    insertKbCredentials(STUB_CREDENTILS_ID, getWiremockUrl(), STUB_CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);
    insertAssignedUser(JOHN_ID, STUB_CREDENTILS_ID, "john_doe", "John", null, "Doe", "patron", vertx);

    mockGet(new RegexPattern(RMAPI_ROOT_PROXY_CUSTOM_LABELS_URL), RMAPI_ROOT_PROXY_CUSTOM_LABELS_RESPONSE);

    final String path = String.format(EHOLDINGS_ROOT_PROXY_BY_CREDENTIALS_ID_URL, STUB_CREDENTILS_ID);
    String actual = getWithStatus(path, SC_OK, JOHN_TOKEN_HEADER).asString();

    String expected = readFile(KB_EBSCO_GET_ROOT_PROXY_RESPONSE);
    JSONAssert.assertEquals(expected, actual, true);
  }

  @Test
  public void shouldReturn401WhenRMAPIReturns401ErrorStatus() {
    insertKbCredentials(STUB_CREDENTILS_ID, getWiremockUrl(), STUB_CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);
    insertAssignedUser(JOHN_ID, STUB_CREDENTILS_ID, "john_doe", "John", null, "Doe", "patron", vertx);

    mockGet(new RegexPattern(RMAPI_ROOT_PROXY_CUSTOM_LABELS_URL), SC_UNAUTHORIZED);
    final String path = String.format(EHOLDINGS_ROOT_PROXY_BY_CREDENTIALS_ID_URL, STUB_CREDENTILS_ID);
    final JsonapiError error = getWithStatus(path, SC_UNAUTHORIZED, JOHN_TOKEN_HEADER).as(JsonapiError.class);
    Assert.assertThat(error.getErrors().get(0).getTitle(), containsString("Unauthorized Access"));
  }

  @Test
  public void shouldReturn403WhenRMAPIReturns403ErrorStatus() {
    insertKbCredentials(STUB_CREDENTILS_ID, getWiremockUrl(), STUB_CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);
    insertAssignedUser(JOHN_ID, STUB_CREDENTILS_ID, "john_doe", "John", null, "Doe", "patron", vertx);

    mockGet(new RegexPattern(RMAPI_ROOT_PROXY_CUSTOM_LABELS_URL), SC_FORBIDDEN);
    final String path = String.format(EHOLDINGS_ROOT_PROXY_BY_CREDENTIALS_ID_URL, STUB_CREDENTILS_ID);
    final JsonapiError error = getWithStatus(path, SC_FORBIDDEN, JOHN_TOKEN_HEADER).as(JsonapiError.class);
    Assert.assertThat(error.getErrors().get(0).getTitle(), containsString("Unauthorized"));
  }

  @Test
  public void shouldReturn404WhenCredentialsNotFOund() {
    final String path = String.format(EHOLDINGS_ROOT_PROXY_BY_CREDENTIALS_ID_URL, "11111111-1111-1111-a111-111111111111");
    final JsonapiError error = getWithStatus(path, SC_NOT_FOUND, JOHN_TOKEN_HEADER).as(JsonapiError.class);
    Assert.assertThat(error.getErrors().get(0).getTitle(), containsString("KbCredentials not found by id"));
  }

  @Test
  public void shouldReturnUpdatedProxyOnSuccessfulPut() throws IOException, URISyntaxException {
    insertKbCredentials(STUB_CREDENTILS_ID, getWiremockUrl(), STUB_CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);
    String stubResponseFile = "responses/rmapi/proxiescustomlabels/get-updated-response.json";

    mockGet(new RegexPattern(RMAPI_ROOT_PROXY_CUSTOM_LABELS_URL), stubResponseFile);
    mockPut(new RegexPattern(RMAPI_ROOT_PROXY_CUSTOM_LABELS_URL), SC_NO_CONTENT);

    String expected = readFile("responses/kb-ebsco/root-proxy/put-root-proxy-response-updated.json");

    final String path = String.format(EHOLDINGS_ROOT_PROXY_BY_CREDENTIALS_ID_URL, STUB_CREDENTILS_ID);

    String actual = putWithOk(path, readFile("requests/kb-ebsco/put-root-proxy.json")).asString();

    JSONAssert.assertEquals(expected, actual, true);

    verify(1, putRequestedFor(new UrlPathPattern(new RegexPattern(RMAPI_ROOT_PROXY_CUSTOM_LABELS_URL), true))
      .withRequestBody(equalToJson(readFile("requests/rmapi/proxiescustomlabels/put-root-proxy.json"))));
  }

  @Test
  public void shouldReturn400WhenInvalidProxyIDAndRMAPIErrorOnPut() throws IOException, URISyntaxException {
    String stubGetResponseFile = "responses/rmapi/proxiescustomlabels/get-updated-response.json";
    String stubPutResponseFile = "responses/rmapi/proxiescustomlabels/put-400-error-response.json";

    insertKbCredentials(STUB_CREDENTILS_ID, getWiremockUrl(), STUB_CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);

    mockGet(new EqualToPattern(RMAPI_ROOT_PROXY_CUSTOM_LABELS_URL), stubGetResponseFile);
    stubFor(
      put(new UrlPathPattern(new EqualToPattern(RMAPI_ROOT_PROXY_CUSTOM_LABELS_URL), false))
        .willReturn(new ResponseDefinitionBuilder().withBody(readFile(stubPutResponseFile)).withStatus(SC_BAD_REQUEST)));

    final String path = String.format(EHOLDINGS_ROOT_PROXY_BY_CREDENTIALS_ID_URL, STUB_CREDENTILS_ID);
    JsonapiError error = putWithStatus(path, readFile("requests/kb-ebsco/put-root-proxy.json"), SC_BAD_REQUEST).as(JsonapiError.class);

    assertThat(error.getErrors().get(0).getTitle(), equalTo("Invalid Proxy ID"));
  }

  @Test
  public void shouldReturnNotFoundWhenNoKbCredentialsStored() throws IOException, URISyntaxException {
    final String path = String.format(EHOLDINGS_ROOT_PROXY_BY_CREDENTIALS_ID_URL, STUB_CREDENTILS_ID);
    JsonapiError error = putWithStatus(path, readFile("requests/kb-ebsco/put-root-proxy.json"), SC_NOT_FOUND).as(JsonapiError.class);
    assertThat(error.getErrors().get(0).getTitle(), containsString("KbCredentials not found by id"));
  }

  @Test
  public void shouldReturn401WhenRMAPIReturns401() {
    insertKbCredentials(STUB_CREDENTILS_ID, getWiremockUrl(), STUB_CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);

    mockGet(new RegexPattern(RMAPI_ROOT_PROXY_CUSTOM_LABELS_URL), SC_UNAUTHORIZED);
    final String path = String.format(EHOLDINGS_ROOT_PROXY_BY_CREDENTIALS_ID_URL, STUB_CREDENTILS_ID);
    final JsonapiError error = getWithStatus(path, SC_UNAUTHORIZED, JOHN_TOKEN_HEADER).as(JsonapiError.class);
    Assert.assertThat(error.getErrors().get(0).getTitle(), containsString("Unauthorized Access"));
  }

  @Test
  public void shouldReturn403WhenRMAPIReturns403() {
    insertKbCredentials(STUB_CREDENTILS_ID, getWiremockUrl(), STUB_CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);

    mockGet(new RegexPattern(RMAPI_ROOT_PROXY_CUSTOM_LABELS_URL), SC_FORBIDDEN);
    final String path = String.format(EHOLDINGS_ROOT_PROXY_BY_CREDENTIALS_ID_URL, STUB_CREDENTILS_ID);
    final JsonapiError error = getWithStatus(path, SC_FORBIDDEN, JOHN_TOKEN_HEADER).as(JsonapiError.class);
    Assert.assertThat(error.getErrors().get(0).getTitle(), containsString("Unauthorized"));
  }

}

