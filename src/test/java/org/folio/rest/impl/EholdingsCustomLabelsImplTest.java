package org.folio.rest.impl;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_NOT_IMPLEMENTED;
import static org.apache.http.HttpStatus.SC_OK;
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

import org.folio.rest.jaxrs.model.JsonapiError;

@RunWith(VertxUnitRunner.class)
public class EholdingsCustomLabelsImplTest extends WireMockTestBase {

  private static final String CUSTOM_LABELS_PATH = "eholdings/custom-labels";

  @Test
  public void shouldReturnCustomLabelsOnGet() throws IOException, URISyntaxException {
    String stubResponseFile = "responses/rmapi/proxiescustomlabels/get-root-proxy-custom-labels-success-response.json";

    mockDefaultConfiguration(getWiremockUrl());
    stubFor(
      get(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/"), true))
        .willReturn(new ResponseDefinitionBuilder()
          .withBody(readFile(stubResponseFile))));

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
    mockDefaultConfiguration(getWiremockUrl());
    stubFor(
      get(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/"), true))
        .willReturn(new ResponseDefinitionBuilder()
          .withBody(readFile(stubResponseFile))));
    final String label = getWithStatus(CUSTOM_LABELS_PATH + "/1", SC_OK).asString();
    JSONAssert.assertEquals(readFile("responses/kb-ebsco/custom-labels/get-custom-label-single-element.json"),
      label, false);
  }

  @Test
  public void shouldReturn404WhenCustomLabelIsNotFound() throws IOException, URISyntaxException {
    String stubResponseFile = "responses/rmapi/proxiescustomlabels/get-root-proxy-custom-labels-success-response.json";
    mockDefaultConfiguration(getWiremockUrl());
    stubFor(
      get(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/"), true))
        .willReturn(new ResponseDefinitionBuilder()
          .withBody(readFile(stubResponseFile))));

    final JsonapiError jsonapiError = getWithStatus(CUSTOM_LABELS_PATH + "/8", SC_NOT_FOUND).as(JsonapiError.class);
    assertEquals("Label not found", jsonapiError.getErrors().get(0).getTitle());
    assertEquals("Label with id: '8' does not exist", jsonapiError.getErrors().get(0).getDetail());
  }

  @Test
  public void shouldReturn404WhenCustomLabelIdIsZero() throws IOException, URISyntaxException {
    String stubResponseFile = "responses/rmapi/proxiescustomlabels/get-root-proxy-custom-labels-success-response.json";
    mockDefaultConfiguration(getWiremockUrl());
    stubFor(
      get(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/"), true))
        .willReturn(new ResponseDefinitionBuilder()
          .withBody(readFile(stubResponseFile))));

    final JsonapiError jsonapiError = getWithStatus(CUSTOM_LABELS_PATH + "/0", SC_NOT_FOUND).as(JsonapiError.class);
    assertEquals("Label not found", jsonapiError.getErrors().get(0).getTitle());
    assertEquals("Label with id: '0' does not exist", jsonapiError.getErrors().get(0).getDetail());
  }

  @Test
  public void shouldReturn400WhenCustomLabelIsInvalid() throws IOException, URISyntaxException {
    String stubResponseFile = "responses/rmapi/proxiescustomlabels/get-root-proxy-custom-labels-success-response.json";
    mockDefaultConfiguration(getWiremockUrl());
    stubFor(
      get(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/"), true))
        .willReturn(new ResponseDefinitionBuilder()
          .withBody(readFile(stubResponseFile))));

    final JsonapiError error = getWithStatus(CUSTOM_LABELS_PATH + "/a", SC_BAD_REQUEST).as(JsonapiError.class);
    assertEquals("Invalid format for Custom Label id: 'a'", error.getErrors().get(0).getTitle());
  }

  @Test
  public void shouldReturn501WhenPutByIdIsNotImplemented() throws IOException, URISyntaxException {
    final int expectedCode = SC_NOT_IMPLEMENTED;
    final int code =  putWithStatus(CUSTOM_LABELS_PATH + "/1",
      readFile("requests/kb-ebsco/custom-labels/put-custom-label.json"),expectedCode).response().statusCode();
    assertEquals(expectedCode, code);
  }

  @Test
  public void shouldReturn501WhenDeleteByIdIsNotImplemented(){
    final int expectedCode = SC_NOT_IMPLEMENTED;
    final int code =  deleteWithStatus(CUSTOM_LABELS_PATH + "/1", expectedCode).response().statusCode();
    assertEquals(expectedCode, code);
  }
}
