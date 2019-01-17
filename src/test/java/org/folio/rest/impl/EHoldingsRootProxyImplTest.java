package org.folio.rest.impl;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.matching.RegexPattern;
import com.github.tomakehurst.wiremock.matching.UrlPathPattern;
import io.restassured.RestAssured;
import io.vertx.ext.unit.junit.VertxUnitRunner;

import org.apache.http.HttpStatus;
import org.folio.rest.jaxrs.model.JsonapiError;
import org.folio.rest.jaxrs.model.RootProxy;
import org.folio.rest.jaxrs.model.RootProxyData;
import org.folio.rest.jaxrs.model.RootProxyDataAttributes;
import org.folio.rest.util.RestConstants;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.URISyntaxException;

import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.folio.util.TestUtil.mockConfiguration;
import static org.folio.util.TestUtil.readFile;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

@RunWith(VertxUnitRunner.class)
public class EHoldingsRootProxyImplTest extends WireMockTestBase {
  private static final String ROOT_PROXY_ID = "root-proxy";
  private static final String ROOT_PROXY_TYPE = "rootProxies";
  private static final String UPDATEROOT_PROXY_ENDPOINT = "eholdings/root-proxy";
  
  @Test
  public void shouldReturnRootProxyWhenCustIdAndAPIKeyAreValid() throws IOException, URISyntaxException {
    String stubResponseFile = "responses/rmapi/proxiescustomlabels/get-root-proxy-custom-labels-success-response.json";

    String expectedRootProxyID = "<n>";

    mockConfiguration(CONFIGURATION_STUB_FILE, getWiremockUrl());
    stubFor(
      get(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/"), true))
        .willReturn(new ResponseDefinitionBuilder()
          .withBody(readFile(stubResponseFile))));

    RestAssured.given()
      .spec(getRequestSpecification())
      .when()
      .get(UPDATEROOT_PROXY_ENDPOINT)
      .then()
      .statusCode(HttpStatus.SC_OK)
      .body("data.id", equalTo(ROOT_PROXY_ID))
      .body("data.type", equalTo(ROOT_PROXY_TYPE))
      .body("data.attributes.id", equalTo(ROOT_PROXY_ID))
      .body("data.attributes.proxyTypeId", equalTo(expectedRootProxyID));
  }
  
  @Test
  public void shouldReturnUnauthorizedWhenRMAPIRequestCompletesWith401ErrorStatus() throws IOException, URISyntaxException {
    mockConfiguration(CONFIGURATION_STUB_FILE, getWiremockUrl());
    stubFor(
      get(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/"), true))
        .willReturn(new ResponseDefinitionBuilder().withStatus(HttpStatus.SC_UNAUTHORIZED)));
    
    RestAssured.given()
      .spec(getRequestSpecification())
      .when()
      .get(UPDATEROOT_PROXY_ENDPOINT)
      .then()
      .statusCode(HttpStatus.SC_FORBIDDEN);
  }
  
  @Test
  public void shouldReturnUnauthorizedWhenRMAPIRequestCompletesWith403ErrorStatus() throws IOException, URISyntaxException {
    mockConfiguration(CONFIGURATION_STUB_FILE, getWiremockUrl());
    stubFor(
      get(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/"), true))
        .willReturn(new ResponseDefinitionBuilder().withStatus(HttpStatus.SC_FORBIDDEN)));
    
    RestAssured.given()
      .spec(getRequestSpecification())
      .when()
      .get(UPDATEROOT_PROXY_ENDPOINT)
      .then()
      .statusCode(HttpStatus.SC_FORBIDDEN);
  }
  
  @Test
  public void shouldReturnUpdatedProxyOnSuccessfulPut() throws IOException, URISyntaxException {
    String stubResponseFile = "responses/rmapi/proxiescustomlabels/get-root-proxy-custom-labels-updated-response.json";

    mockConfiguration(CONFIGURATION_STUB_FILE, getWiremockUrl());

    stubFor(
      get(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/"), true))
        .willReturn(new ResponseDefinitionBuilder().withBody(readFile(stubResponseFile))));

    stubFor(
      put(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/"), true))
        .willReturn(new ResponseDefinitionBuilder().withStatus(HttpStatus.SC_NO_CONTENT)));

    RootProxy expected = getUpdatedRootProxy();

    RootProxy rootProxy = RestAssured
      .given()
      .spec(getRequestSpecification())
      .header(CONTENT_TYPE_HEADER)
      .body(readFile("requests/kb-ebsco/put-root-proxy.json"))
      .when()
      .put(UPDATEROOT_PROXY_ENDPOINT)
      .then()
      .statusCode(HttpStatus.SC_OK)
      .extract().as(RootProxy.class);
    
    assertThat(rootProxy.getData().getId(), equalTo(expected.getData().getId()));
    assertThat(rootProxy.getData().getType(), equalTo(expected.getData().getType()));
    assertThat(rootProxy.getData().getAttributes().getId(), equalTo(expected.getData().getAttributes().getId()));
    assertThat(rootProxy.getData().getAttributes().getProxyTypeId(), equalTo(expected.getData().getAttributes().getProxyTypeId()));

    verify(1, putRequestedFor(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/"), true))
      .withRequestBody(equalToJson(readFile("requests/rmapi/proxiescustomlabels/put-root-proxy-custom-labels.json"))));
  }
  
  @Test
  public void shouldReturn400WhenInvalidProxyIDAndRMAPIErrorOnPut() throws IOException, URISyntaxException {
    String stubGetResponseFile = "responses/rmapi/proxiescustomlabels/get-root-proxy-custom-labels-updated-response.json";
    String stubPutResponseFile = "responses/rmapi/proxiescustomlabels/put-root-proxy-custom-labels-400-error-response.json";

    mockConfiguration(CONFIGURATION_STUB_FILE, getWiremockUrl());
    
    stubFor(
        get(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/"), true))
          .willReturn(new ResponseDefinitionBuilder().withBody(readFile(stubGetResponseFile))));

    stubFor(
      put(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/"), true))
        .willReturn(new ResponseDefinitionBuilder().withBody(readFile(stubPutResponseFile)).withStatus(HttpStatus.SC_BAD_REQUEST)));

    JsonapiError error = RestAssured
      .given()
      .spec(getRequestSpecification())
      .header(CONTENT_TYPE_HEADER)
      .body(readFile("requests/kb-ebsco/put-root-proxy.json"))
      .when()
      .put(UPDATEROOT_PROXY_ENDPOINT)
      .then()
      .statusCode(HttpStatus.SC_BAD_REQUEST)
      .extract().as(JsonapiError.class);

    assertThat(error.getErrors().get(0).getTitle(), equalTo("Invalid Proxy ID"));
  }
  
  private RootProxy getUpdatedRootProxy() {
    return new RootProxy()
      .withData(new RootProxyData()
          .withId(ROOT_PROXY_ID)
          .withType(ROOT_PROXY_TYPE)
          .withAttributes(new RootProxyDataAttributes()
              .withId(ROOT_PROXY_ID)
              .withProxyTypeId("Test-Proxy-ID-123")))
      .withJsonapi(RestConstants.JSONAPI);
  }
}

