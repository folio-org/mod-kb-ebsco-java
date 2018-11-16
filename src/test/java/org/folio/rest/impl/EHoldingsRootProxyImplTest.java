package org.folio.rest.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.RegexPattern;
import com.github.tomakehurst.wiremock.matching.UrlPathPattern;
import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;
import io.vertx.ext.unit.junit.VertxUnitRunner;

import org.apache.http.HttpStatus;
import org.folio.rest.jaxrs.model.JsonapiError;
import org.folio.rest.jaxrs.model.RootProxy;
import org.folio.rest.jaxrs.model.RootProxyData;
import org.folio.rest.jaxrs.model.RootProxyDataAttributes;
import org.folio.rest.jaxrs.model.RootProxyPutRequest;
import org.folio.rest.util.RestConstants;
import org.folio.util.TestUtil;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.URISyntaxException;

import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

@RunWith(VertxUnitRunner.class)
public class EHoldingsRootProxyImplTest extends WireMockTestBase {
  private static final String ROOT_PROXY_ID = "root-proxy";
  private static final String ROOT_PROXY_TYPE = "rootProxies";
  
  @Test
  public void shouldReturnRootProxyWhenCustIdAndAPIKeyAreValid() throws IOException, URISyntaxException {
    String stubResponseFile = "responses/rmapi/proxiescustomlabels/get-root-proxy-custom-labels-success-response.json";

    String expectedRootProxyID = "<n>";

    String wiremockUrl = getWiremockUrl();
    TestUtil.mockConfiguration(CONFIGURATION_STUB_FILE, wiremockUrl);
    WireMock.stubFor(
      WireMock.get(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/"), true))
        .willReturn(new ResponseDefinitionBuilder()
          .withBody(TestUtil.readFile(stubResponseFile))));

    RequestSpecification requestSpecification = getRequestSpecification();

    RestAssured.given()
      .spec(requestSpecification)
      .when()
      .get("eholdings/root-proxy")
      .then()
      .statusCode(200)
      .body("data.id", equalTo(ROOT_PROXY_ID))
      .body("data.type", equalTo(ROOT_PROXY_TYPE))
      .body("data.attributes.id", equalTo(ROOT_PROXY_ID))
      .body("data.attributes.proxyTypeId", equalTo(expectedRootProxyID));
  }
  
  @Test
  public void shouldReturnUnauthorizedWhenRMAPIRequestCompletesWith401ErrorStatus() throws IOException, URISyntaxException {
    String wiremockUrl = getWiremockUrl();
    TestUtil.mockConfiguration(CONFIGURATION_STUB_FILE, wiremockUrl);
    WireMock.stubFor(
      WireMock.get(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/"), true))
        .willReturn(new ResponseDefinitionBuilder().withStatus(401)));
    
    RequestSpecification requestSpecification = getRequestSpecification();

    RestAssured.given()
      .spec(requestSpecification)
      .when()
      .get("eholdings/root-proxy")
      .then()
      .statusCode(403);
  }
  
  @Test
  public void shouldReturnUnauthorizedWhenRMAPIRequestCompletesWith403ErrorStatus() throws IOException, URISyntaxException {
    String wiremockUrl = getWiremockUrl();
    TestUtil.mockConfiguration(CONFIGURATION_STUB_FILE, wiremockUrl);
    WireMock.stubFor(
      WireMock.get(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/"), true))
        .willReturn(new ResponseDefinitionBuilder().withStatus(403)));
    
    RequestSpecification requestSpecification = getRequestSpecification();

    RestAssured.given()
      .spec(requestSpecification)
      .when()
      .get("eholdings/root-proxy")
      .then()
      .statusCode(403);
  }
  
  @Test
  public void shouldReturnUpdatedProxyOnSuccessfulPut() throws IOException, URISyntaxException {
    String stubResponseFile = "responses/rmapi/proxiescustomlabels/get-root-proxy-custom-labels-updated-response.json";

    String wiremockUrl = getWiremockUrl();
    TestUtil.mockConfiguration(CONFIGURATION_STUB_FILE, wiremockUrl);

    WireMock.stubFor(
      WireMock.get(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/"), true))
        .willReturn(new ResponseDefinitionBuilder().withBody(TestUtil.readFile(stubResponseFile))));

    WireMock.stubFor(
      WireMock.put(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/"), true))
        .willReturn(new ResponseDefinitionBuilder().withStatus(204)));

    ObjectMapper mapper = new ObjectMapper();
    RootProxyPutRequest rootProxyToBeUpdated = mapper.readValue(TestUtil.getFile("requests/kb-ebsco/put-root-proxy.json"),
        RootProxyPutRequest.class);

    RootProxy expected = getUpdatedRootProxy();
    RequestSpecification requestSpecification = getRequestSpecification();

    String updateRootProxyEndpoint = "eholdings/root-proxy";

    RootProxy rootProxy = RestAssured
      .given()
      .spec(requestSpecification)
      .header(CONTENT_TYPE_HEADER)
      .body(mapper.writeValueAsString(rootProxyToBeUpdated))
      .when()
      .put(updateRootProxyEndpoint)
      .then()
      .statusCode(HttpStatus.SC_OK)
      .extract().as(RootProxy.class);
    
    assertThat(rootProxy.getData().getId(), equalTo(expected.getData().getId()));
    assertThat(rootProxy.getData().getType(), equalTo(expected.getData().getType()));
    assertThat(rootProxy.getData().getAttributes().getId(), equalTo(expected.getData().getAttributes().getId()));
    assertThat(rootProxy.getData().getAttributes().getProxyTypeId(), equalTo(expected.getData().getAttributes().getProxyTypeId()));

    WireMock.verify(1, putRequestedFor(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/"), true))
      .withRequestBody(equalToJson(TestUtil.readFile("requests/rmapi/proxiescustomlabels/put-root-proxy-custom-labels.json"))));
  }
  
  @Test
  public void shouldReturn400WhenInvalidProxyIDAndRMAPIErrorOnPut() throws IOException, URISyntaxException {
    String stubGetResponseFile = "responses/rmapi/proxiescustomlabels/get-root-proxy-custom-labels-updated-response.json";
    String stubPutResponseFile = "responses/rmapi/proxiescustomlabels/put-root-proxy-custom-labels-400-error-response.json";

    String wiremockUrl = getWiremockUrl();
    TestUtil.mockConfiguration(CONFIGURATION_STUB_FILE, wiremockUrl);
    
    WireMock.stubFor(
        WireMock.get(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/"), true))
          .willReturn(new ResponseDefinitionBuilder().withBody(TestUtil.readFile(stubGetResponseFile))));

    WireMock.stubFor(
      WireMock.put(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/"), true))
        .willReturn(new ResponseDefinitionBuilder().withBody(TestUtil.readFile(stubPutResponseFile)).withStatus(400)));

    ObjectMapper mapper = new ObjectMapper();
    RootProxyPutRequest proxyToBeUpdated = mapper.readValue(TestUtil.getFile("requests/kb-ebsco/put-root-proxy.json"),
        RootProxyPutRequest.class);

    RequestSpecification requestSpecification = getRequestSpecification();

    String rootProxyEndpoint = "eholdings/root-proxy";

    JsonapiError error = RestAssured
      .given()
      .spec(requestSpecification)
      .header(CONTENT_TYPE_HEADER)
      .body(mapper.writeValueAsString(proxyToBeUpdated))
      .when()
      .put(rootProxyEndpoint)
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

