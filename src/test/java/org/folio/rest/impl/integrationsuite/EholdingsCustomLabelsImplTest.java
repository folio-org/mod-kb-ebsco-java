package org.folio.rest.impl.integrationsuite;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNPROCESSABLE_ENTITY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import static org.folio.test.util.TestUtil.mockGet;
import static org.folio.test.util.TestUtil.readFile;
import static org.folio.test.util.TestUtil.readJsonFile;
import static org.folio.util.KBTestUtil.mockDefaultConfiguration;
import static org.folio.util.KbCredentialsTestUtil.KB_CREDENTIALS_CUSTOM_LABELS_ENDPOINT;
import static org.folio.util.KbCredentialsTestUtil.STUB_CREDENTIALS_NAME;
import static org.folio.util.KbCredentialsTestUtil.getKbCredentials;
import static org.folio.util.KbCredentialsTestUtil.insertKbCredentials;

import java.io.IOException;
import java.net.URISyntaxException;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.matching.RegexPattern;
import com.github.tomakehurst.wiremock.matching.UrlPathPattern;

import io.vertx.ext.unit.junit.VertxUnitRunner;

import org.apache.http.HttpStatus;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.JSONAssert;

import org.folio.rest.impl.WireMockTestBase;
import org.folio.rest.jaxrs.model.CustomLabel;
import org.folio.rest.jaxrs.model.CustomLabelsCollection;
import org.folio.rest.jaxrs.model.JsonapiError;

@RunWith(VertxUnitRunner.class)
public class EholdingsCustomLabelsImplTest extends WireMockTestBase {

  private static final String CUSTOM_LABELS_PATH = "eholdings/custom-labels";

  private static final String RM_API_CUSTOMER_PATH = "/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/";
  private static final String KB_GET_CUSTOM_LABELS_RESPONSE = "responses/kb-ebsco/custom-labels/get-custom-labels-list.json";
  private static final String RM_GET_PROXY_CUSTOM_LABELS_RESPONSE = "responses/rmapi/proxiescustomlabels/"
    + "get-root-proxy-custom-labels-success-response.json";

  @Test
  public void shouldReturnCustomLabelsOnGet() throws IOException, URISyntaxException {
    String stubResponseFile = "responses/rmapi/proxiescustomlabels/get-root-proxy-custom-labels-success-response.json";
    mockCustomLabelsConfiguration(stubResponseFile);

    String labels = getWithOk(CUSTOM_LABELS_PATH).asString();
    JSONAssert.assertEquals(readFile("responses/kb-ebsco/custom-labels/get-custom-labels-list.json"), labels, false);
    verify(1, getRequestedFor(urlEqualTo("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/")));
  }

  @Test
  public void shouldReturnUnauthorizedOnGetWithResourcesWhenRMAPI401() throws IOException, URISyntaxException {
    mockDefaultConfiguration(getWiremockUrl());
    mockGet(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/"), HttpStatus.SC_UNAUTHORIZED);

    JsonapiError error = getWithStatus(CUSTOM_LABELS_PATH, SC_FORBIDDEN).as(JsonapiError.class);
    assertThat(error.getErrors().get(0).getTitle(), containsString("Unauthorized Access"));
  }

  @Test
  public void shouldReturnUnauthorizedOnGetWithResourcesWhenRMAPI403() throws IOException, URISyntaxException {
    mockDefaultConfiguration(getWiremockUrl());
    mockGet(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/"), SC_FORBIDDEN);

    JsonapiError error = getWithStatus(CUSTOM_LABELS_PATH, SC_FORBIDDEN).as(JsonapiError.class);
    assertThat(error.getErrors().get(0).getTitle(), containsString("Unauthorized Access"));
  }

  @Test
  public void shouldReturn422WhenIdNotInRange() throws IOException, URISyntaxException {
    String stubResponseFile = "responses/rmapi/proxiescustomlabels/get-root-proxy-custom-labels-success-response.json";
    mockCustomLabelsConfiguration(stubResponseFile);
    final String putBody = readFile("requests/kb-ebsco/custom-labels/put-custom-label-invalid-id.json");
    final JsonapiError jsonapiError = putWithStatus(CUSTOM_LABELS_PATH, putBody, SC_UNPROCESSABLE_ENTITY)
      .as(JsonapiError.class);
    assertEquals("Invalid Custom Label id", jsonapiError.getErrors().get(0).getTitle());
    assertEquals("Custom Label id should be in range 1 - 5", jsonapiError.getErrors().get(0).getDetail());
  }

  @Test
  public void shouldReturn422WhenInvalidNameLength() throws IOException, URISyntaxException {
    String stubResponseFile = "responses/rmapi/proxiescustomlabels/get-root-proxy-custom-labels-success-response.json";
    mockCustomLabelsConfiguration(stubResponseFile);
    final String putBody = readFile("requests/kb-ebsco/custom-labels/put-custom-label-invalid-name.json");
    final JsonapiError jsonapiError = putWithStatus(CUSTOM_LABELS_PATH, putBody, SC_UNPROCESSABLE_ENTITY)
      .as(JsonapiError.class);
    assertEquals("Invalid Custom Label Name", jsonapiError.getErrors().get(0).getTitle());
    assertEquals("Custom Label Name is too long (maximum is 50 characters)", jsonapiError.getErrors().get(0).getDetail());
  }

  @Test
  public void shouldReturn422WhenHasDuplicateIds() throws IOException, URISyntaxException {
    String stubResponseFile = "responses/rmapi/proxiescustomlabels/get-root-proxy-custom-labels-success-response.json";
    mockCustomLabelsConfiguration(stubResponseFile);
    final String putBody = readFile("requests/kb-ebsco/custom-labels/put-custom-labels-duplicate-id.json");
    final JsonapiError jsonapiError = putWithStatus(CUSTOM_LABELS_PATH, putBody, SC_UNPROCESSABLE_ENTITY)
      .as(JsonapiError.class);
    assertEquals("Invalid request body", jsonapiError.getErrors().get(0).getTitle());
    assertEquals("Each label in body must contain unique id", jsonapiError.getErrors().get(0).getDetail());
  }

  @Test
  public void shouldUpdateCustomLabelsWhenAllIsValidWithOneItem() throws IOException, URISyntaxException {
    String stubResponseFile = "responses/rmapi/custom-labels/get-custom-labels-two-elements.json";
    mockCustomLabelsConfiguration(stubResponseFile);
    stubFor(
      put(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/"), true))
        .willReturn(new ResponseDefinitionBuilder().withStatus(SC_NO_CONTENT)));

    final String putBody = readFile("requests/kb-ebsco/custom-labels/put-one-custom-label.json");
    final CustomLabelsCollection updatedCollection = putWithStatus(CUSTOM_LABELS_PATH, putBody, SC_OK)
      .as(CustomLabelsCollection.class);

    assertEquals(1, updatedCollection.getData().size());
    assertEquals((Integer) 1, updatedCollection.getMeta().getTotalResults());
    CustomLabel item = updatedCollection.getData().get(0);
    assertNotNull(item);
    assertEquals("test label 1 updated", item.getAttributes().getDisplayLabel());
    verify(1, putRequestedFor(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/"), true))
      .withRequestBody(equalToJson(readFile("requests/rmapi/custom-labels/put-custom-labels-one-item.json"))));
  }

  @Test
  public void shouldUpdateCustomLabelsWhenAllIsValidWithFiveItems() throws IOException, URISyntaxException {
    String stubResponseFile = "responses/rmapi/custom-labels/get-custom-labels-two-elements.json";
    mockCustomLabelsConfiguration(stubResponseFile);
    stubFor(
      put(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/"), true))
        .willReturn(new ResponseDefinitionBuilder().withStatus(SC_NO_CONTENT)));

    final String putBody = readFile("requests/kb-ebsco/custom-labels/put-five-custom-labels.json");
    final CustomLabelsCollection updatedCollection = putWithStatus(CUSTOM_LABELS_PATH, putBody, SC_OK)
      .as(CustomLabelsCollection.class);

    assertEquals(5, updatedCollection.getData().size());
    assertEquals((Integer) 5, updatedCollection.getMeta().getTotalResults());

    CustomLabel firstItem = updatedCollection.getData().get(0);
    assertNotNull(firstItem);
    assertEquals("test label 1", firstItem.getAttributes().getDisplayLabel());

    CustomLabel lastItem = updatedCollection.getData().get(4);
    assertNotNull(lastItem);
    assertEquals("test label 5", lastItem.getAttributes().getDisplayLabel());

    verify(1, putRequestedFor(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/"), true))
      .withRequestBody(equalToJson(readFile("requests/rmapi/custom-labels/put-custom-labels-five-items.json"))));
  }

  @Test
  public void shouldReturnCustomLabelsOnGetCustomLabels() throws IOException, URISyntaxException {
    insertKbCredentials(getWiremockUrl(), STUB_CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);
    String credentialsId = getKbCredentials(vertx).get(0).getId();
    CustomLabelsCollection expected = readJsonFile(KB_GET_CUSTOM_LABELS_RESPONSE, CustomLabelsCollection.class);
    expected.getData().forEach(customLabel -> customLabel.setCredentialsId(credentialsId));

    mockCustomLabelsConfiguration(RM_GET_PROXY_CUSTOM_LABELS_RESPONSE);
    String resourcePath = String.format(KB_CREDENTIALS_CUSTOM_LABELS_ENDPOINT, credentialsId);
    CustomLabelsCollection actual = getWithOk(resourcePath).as(CustomLabelsCollection.class);

    verify(1, getRequestedFor(urlEqualTo(RM_API_CUSTOMER_PATH)));
    assertEquals(expected, actual);
  }

  @Test
  public void shouldReturn403OnGetWithResourcesWhenRMAPI403() {
    insertKbCredentials(getWiremockUrl(), STUB_CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);
    String credentialsId = getKbCredentials(vertx).get(0).getId();

    mockGet(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/"), SC_FORBIDDEN);
    String resourcePath = String.format(KB_CREDENTIALS_CUSTOM_LABELS_ENDPOINT, credentialsId);
    JsonapiError error = getWithStatus(resourcePath, SC_FORBIDDEN).as(JsonapiError.class);
    assertThat(error.getErrors().get(0).getTitle(), containsString("Unauthorized Access"));
  }

  @Test
  public void shouldReturn404OnGetCustomLabelsWhenCredentialsAreMissing() {
    String resourcePath = String.format(KB_CREDENTIALS_CUSTOM_LABELS_ENDPOINT, "11111111-1111-1111-a111-111111111111");
    JsonapiError error = getWithStatus(resourcePath, SC_NOT_FOUND).as(JsonapiError.class);

    assertThat(error.getErrors().get(0).getTitle(), containsString("KbCredentials not found by id"));
  }

  private void mockCustomLabelsConfiguration(String stubResponseFile) throws IOException, URISyntaxException {
    mockDefaultConfiguration(getWiremockUrl());
    stubFor(
      get(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/"), true))
        .willReturn(new ResponseDefinitionBuilder().withBody(readFile(stubResponseFile))));
  }
}
