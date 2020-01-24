package org.folio.rest.impl.integrationsuite;

import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNPROCESSABLE_ENTITY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;

import static org.folio.test.util.TestUtil.mockGet;
import static org.folio.test.util.TestUtil.readFile;
import static org.folio.util.KBTestUtil.mockDefaultConfiguration;

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
import org.folio.rest.jaxrs.model.JsonapiError;

@RunWith(VertxUnitRunner.class)
public class EholdingsCustomLabelsImplTest extends WireMockTestBase {

  private static final String CUSTOM_LABELS_PATH = "eholdings/custom-labels";

  @Test
  public void shouldReturnCustomLabelsOnGet() throws IOException, URISyntaxException {
    String stubResponseFile = "responses/rmapi/proxiescustomlabels/get-root-proxy-custom-labels-success-response.json";
    mockCustomLabelsConfiguration(stubResponseFile);

    String labels = getWithOk(CUSTOM_LABELS_PATH).asString();
    JSONAssert.assertEquals(readFile("responses/kb-ebsco/custom-labels/get-custom-labels-list.json"),
      labels, false);
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
  public void shouldReturnSingleCustomLabelWhenIdIsValid() throws IOException, URISyntaxException {
    String stubResponseFile = "responses/rmapi/proxiescustomlabels/get-root-proxy-custom-labels-success-response.json";
    mockCustomLabelsConfiguration(stubResponseFile);

    final String label = getWithStatus(CUSTOM_LABELS_PATH + "/1", SC_OK).asString();
    JSONAssert.assertEquals(readFile("responses/kb-ebsco/custom-labels/get-custom-label-single-element.json"),
      label, false);
  }

  @Test
  public void shouldReturnSingleCustomLabelWhenIdIsValidWhenTwoLabelsAvailable() throws IOException, URISyntaxException {
    String stubResponseFile = "responses/rmapi/custom-labels/get-custom-labels-two-elements.json";
    mockCustomLabelsConfiguration(stubResponseFile);

    final String label = getWithStatus(CUSTOM_LABELS_PATH + "/5", SC_OK).asString();
    JSONAssert.assertEquals(readFile("responses/kb-ebsco/custom-labels/get-custom-label-single-element-id-five.json"),
      label, false);
  }

  @Test
  public void shouldReturn404WhenCustomLabelIsNotFound() throws IOException, URISyntaxException {
    String stubResponseFile = "responses/rmapi/proxiescustomlabels/get-root-proxy-custom-labels-success-response.json";
    mockCustomLabelsConfiguration(stubResponseFile);

    final JsonapiError jsonapiError = getWithStatus(CUSTOM_LABELS_PATH + "/8", SC_NOT_FOUND).as(JsonapiError.class);
    assertEquals("Label not found", jsonapiError.getErrors().get(0).getTitle());
    assertEquals("Label with id: '8' does not exist", jsonapiError.getErrors().get(0).getDetail());
  }

  @Test
  public void shouldReturn404WhenCustomLabelIdIsZero() throws IOException, URISyntaxException {
    String stubResponseFile = "responses/rmapi/proxiescustomlabels/get-root-proxy-custom-labels-success-response.json";
    mockCustomLabelsConfiguration(stubResponseFile);

    final JsonapiError jsonapiError = getWithStatus(CUSTOM_LABELS_PATH + "/0", SC_NOT_FOUND).as(JsonapiError.class);
    assertEquals("Label not found", jsonapiError.getErrors().get(0).getTitle());
    assertEquals("Label with id: '0' does not exist", jsonapiError.getErrors().get(0).getDetail());
  }

  @Test
  public void shouldReturn400WhenCustomLabelIsInvalid() {

    final JsonapiError error = getWithStatus(CUSTOM_LABELS_PATH + "/a", SC_BAD_REQUEST).as(JsonapiError.class);
    assertEquals("Invalid format for Custom Label id: 'a'", error.getErrors().get(0).getTitle());
  }

  @Test
  public void shouldReturn422WhenIdNotInRange() throws IOException, URISyntaxException {
    String stubResponseFile = "responses/rmapi/proxiescustomlabels/get-root-proxy-custom-labels-success-response.json";
    mockCustomLabelsConfiguration(stubResponseFile);
    final String putCustomLabel = "requests/kb-ebsco/custom-labels/put-custom-label-invalid-id.json";
    final JsonapiError jsonapiError = putWithStatus(CUSTOM_LABELS_PATH + "/6",
      readFile(putCustomLabel), SC_UNPROCESSABLE_ENTITY).as(JsonapiError.class);
    assertEquals("Invalid Custom Label id", jsonapiError.getErrors().get(0).getTitle());
    assertEquals("Custom Label id should be in range 1 - 5", jsonapiError.getErrors().get(0).getDetail());
  }

  @Test
  public void shouldReturn400OnPutWhenCustomLabelIsInvalid() throws IOException, URISyntaxException {
    final String putCustomLabel = "requests/kb-ebsco/custom-labels/put-custom-label-invalid-id.json";
    final JsonapiError jsonapiError = putWithStatus(CUSTOM_LABELS_PATH + "/a",
      readFile(putCustomLabel), SC_BAD_REQUEST).as(JsonapiError.class);
    assertEquals("Invalid format for Custom Label id: 'a'", jsonapiError.getErrors().get(0).getTitle());
  }

  @Test
  public void shouldUpdateCustomLabelWhenIdIsValid() throws IOException, URISyntaxException {
    String stubResponseFile = "responses/rmapi/custom-labels/get-custom-labels-two-elements.json";
    mockCustomLabelsConfiguration(stubResponseFile);
    stubFor(
      put(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/"), true))
        .willReturn(new ResponseDefinitionBuilder().withStatus(SC_NO_CONTENT)));

    final String putbody = "requests/kb-ebsco/custom-labels/put-custom-label-id-five.json";
    final CustomLabel updatedLabel = putWithStatus(CUSTOM_LABELS_PATH + "/5",
      readFile(putbody), SC_OK).as(CustomLabel.class);

    assertEquals("test label 5 updated", updatedLabel.getData().getAttributes().getDisplayLabel());
    verify(1, putRequestedFor(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/"), true))
      .withRequestBody(equalToJson(readFile("requests/rmapi/custom-labels/put-custom-labels-two-elements.json"))));
  }

  private void mockCustomLabelsConfiguration(String stubResponseFile) throws IOException, URISyntaxException {
    mockDefaultConfiguration(getWiremockUrl());
    stubFor(
      get(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/"), true))
        .willReturn(new ResponseDefinitionBuilder()
          .withBody(readFile(stubResponseFile))));
  }

  @Test
  public void shouldDeleteCustomLabelWhenIdIsValid() throws IOException, URISyntaxException {
    String stubResponseFile = "responses/rmapi/custom-labels/get-custom-labels-two-elements.json";
    mockCustomLabelsConfiguration(stubResponseFile);
    stubFor(
      put(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/"), true))
        .willReturn(new ResponseDefinitionBuilder().withStatus(SC_NO_CONTENT)));

    deleteWithNoContent(CUSTOM_LABELS_PATH + "/1");

    verify(1, putRequestedFor(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/"), true))
      .withRequestBody(equalToJson(readFile("requests/rmapi/custom-labels/put-custom-label-one-element.json"))));
  }

  @Test
  public void shouldReturn404OnDeleteWhenCustomLabelNotFound() throws IOException, URISyntaxException {
    String stubResponseFile = "responses/rmapi/custom-labels/get-custom-labels-two-elements.json";
    mockCustomLabelsConfiguration(stubResponseFile);
    stubFor(
      put(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/"), true))
        .willReturn(new ResponseDefinitionBuilder().withStatus(SC_NO_CONTENT)));

    final JsonapiError jsonapiError = deleteWithStatus(CUSTOM_LABELS_PATH + "/2", SC_NOT_FOUND).as(JsonapiError.class);

    assertEquals("Label not found", jsonapiError.getErrors().get(0).getTitle());
    assertEquals("Label with id: '2' does not exist", jsonapiError.getErrors().get(0).getDetail());
  }
  @Test
  public void shouldReturn400OnDeleteWhenCustomLabelIdIsInvalid() throws IOException, URISyntaxException {
    String stubResponseFile = "responses/rmapi/custom-labels/get-custom-labels-two-elements.json";
    mockCustomLabelsConfiguration(stubResponseFile);
    stubFor(
      put(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/"), true))
        .willReturn(new ResponseDefinitionBuilder().withStatus(SC_NO_CONTENT)));

    final JsonapiError jsonapiError = deleteWithStatus(CUSTOM_LABELS_PATH + "/a", SC_BAD_REQUEST)
      .as(JsonapiError.class);

    assertEquals("Invalid format for Custom Label id: 'a'", jsonapiError.getErrors().get(0).getTitle());
  }
}
