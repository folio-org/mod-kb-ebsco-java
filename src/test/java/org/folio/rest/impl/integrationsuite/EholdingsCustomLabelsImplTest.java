package org.folio.rest.impl.integrationsuite;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.apache.http.HttpStatus.SC_UNPROCESSABLE_ENTITY;
import static org.folio.repository.kbcredentials.KbCredentialsTableConstants.KB_CREDENTIALS_TABLE_NAME;
import static org.folio.test.util.TestUtil.mockGet;
import static org.folio.test.util.TestUtil.readFile;
import static org.folio.test.util.TestUtil.readJsonFile;
import static org.folio.util.AssertTestUtil.assertErrorContainsDetail;
import static org.folio.util.AssertTestUtil.assertErrorContainsTitle;
import static org.folio.util.KbCredentialsTestUtil.KB_CREDENTIALS_CUSTOM_LABELS_ENDPOINT;
import static org.folio.util.KbCredentialsTestUtil.STUB_USER_ID_HEADER;
import static org.folio.util.KbTestUtil.clearDataFromTable;
import static org.folio.util.KbTestUtil.getDefaultKbConfiguration;
import static org.folio.util.KbTestUtil.setupDefaultKbConfiguration;
import static org.junit.Assert.assertEquals;

import com.github.tomakehurst.wiremock.matching.RegexPattern;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.UUID;
import org.folio.rest.impl.WireMockTestBase;
import org.folio.rest.jaxrs.model.CustomLabelsCollection;
import org.folio.rest.jaxrs.model.JsonapiError;
import org.folio.rest.jaxrs.model.KbCredentials;
import org.json.JSONException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.JSONAssert;

@RunWith(VertxUnitRunner.class)
public class EholdingsCustomLabelsImplTest extends WireMockTestBase {

  private static final String KB_CUSTOM_LABELS_PATH = "eholdings/custom-labels";
  private static final String RM_API_CUSTOMER_PATH = "/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/";

  private static final String REQUESTS_PATH = "requests/kb-ebsco/custom-labels";
  private static final String PUT_ONE_LABEL_REQUEST = REQUESTS_PATH + "/put-one-custom-label.json";
  private static final String PUT_FIVE_LABEL_REQUEST = REQUESTS_PATH + "/put-five-custom-labels.json";
  private static final String PUT_WITH_INVALID_ID_REQUEST = REQUESTS_PATH + "/put-custom-label-invalid-id.json";
  private static final String PUT_WITH_INVALID_NAME_REQUEST = REQUESTS_PATH + "/put-custom-label-invalid-name.json";
  private static final String PUT_WITH_DUPLICATE_ID_REQUEST = REQUESTS_PATH + "/put-custom-labels-duplicate-id.json";

  private static final String KB_GET_CUSTOM_LABELS_RESPONSE =
    "responses/kb-ebsco/custom-labels/get-custom-labels-list.json";

  private static final String RM_GET_LABELS_RESPONSE = "responses/rmapi/proxiescustomlabels/get-success-response.json";
  private static final String RM_PUT_ONE_LABEL_REQUEST = "requests/rmapi/proxiescustomlabels/put-one-label.json";
  private static final String RM_PUT_FIVE_LABEL_REQUEST = "requests/rmapi/proxiescustomlabels/put-five-labels.json";

  private KbCredentials configuration;

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    setupDefaultKbConfiguration(getWiremockUrl(), vertx);
    configuration = getDefaultKbConfiguration(vertx);
  }

  @After
  public void tearDown() {
    clearDataFromTable(vertx, KB_CREDENTIALS_TABLE_NAME);
  }

  @Test
  public void shouldReturnCustomLabelsOnGet() throws IOException, URISyntaxException, JSONException {
    mockCustomLabelsConfiguration();

    String actual = getWithOk(KB_CUSTOM_LABELS_PATH, STUB_USER_ID_HEADER).asString();
    JSONAssert.assertEquals(readFile(KB_GET_CUSTOM_LABELS_RESPONSE), actual, false);
    verify(1, getRequestedFor(urlEqualTo(RM_API_CUSTOMER_PATH)));
  }

  @Test
  public void shouldReturn403OnGetWithResourcesWhenRmApi401() {
    mockGet(new RegexPattern(RM_API_CUSTOMER_PATH), SC_UNAUTHORIZED);

    JsonapiError error = getWithStatus(KB_CUSTOM_LABELS_PATH, SC_FORBIDDEN, STUB_USER_ID_HEADER).as(JsonapiError.class);
    assertErrorContainsTitle(error, "Unauthorized Access");
  }

  @Test
  public void shouldReturn403OnGetWithResourcesWhenRmApi403() {
    mockGet(new RegexPattern(RM_API_CUSTOMER_PATH), SC_FORBIDDEN);

    JsonapiError error = getWithStatus(KB_CUSTOM_LABELS_PATH, SC_FORBIDDEN, STUB_USER_ID_HEADER).as(JsonapiError.class);
    assertErrorContainsTitle(error, "Unauthorized Access");
  }

  @Test
  public void shouldReturnCustomLabelsOnGetByCredentials() throws IOException, URISyntaxException {
    CustomLabelsCollection expected = readJsonFile(KB_GET_CUSTOM_LABELS_RESPONSE, CustomLabelsCollection.class);
    expected.getData().forEach(customLabel -> customLabel.setCredentialsId(configuration.getId()));

    mockCustomLabelsConfiguration();
    String resourcePath = String.format(KB_CREDENTIALS_CUSTOM_LABELS_ENDPOINT, configuration.getId());
    CustomLabelsCollection actual = getWithOk(resourcePath, STUB_USER_ID_HEADER).as(CustomLabelsCollection.class);

    verify(1, getRequestedFor(urlEqualTo(RM_API_CUSTOMER_PATH)));
    assertEquals(expected, actual);
  }

  @Test
  public void shouldReturn403OnGetByCredentialsWithResourcesWhenRmApi403() {
    mockGet(new RegexPattern(RM_API_CUSTOMER_PATH), SC_FORBIDDEN);
    String resourcePath = String.format(KB_CREDENTIALS_CUSTOM_LABELS_ENDPOINT, configuration.getId());
    JsonapiError error = getWithStatus(resourcePath, SC_FORBIDDEN, STUB_USER_ID_HEADER).as(JsonapiError.class);

    assertErrorContainsTitle(error, "Unauthorized Access");
  }

  @Test
  public void shouldReturn404OnGetByCredentialsWhenCredentialsAreMissing() {
    String resourcePath = String.format(KB_CREDENTIALS_CUSTOM_LABELS_ENDPOINT, UUID.randomUUID());
    JsonapiError error = getWithStatus(resourcePath, SC_NOT_FOUND, STUB_USER_ID_HEADER).as(JsonapiError.class);

    assertErrorContainsTitle(error, "KbCredentials not found by id");
  }

  @Test
  public void shouldUpdateCustomLabelsOnPutWhenAllIsValidWithOneItem()
    throws IOException, URISyntaxException, JSONException {
    mockCustomLabelsSuccessPutRequest();

    String putBody = readFile(PUT_ONE_LABEL_REQUEST);
    String resourcePath = String.format(KB_CREDENTIALS_CUSTOM_LABELS_ENDPOINT, configuration.getId());
    String actual = putWithOk(resourcePath, putBody, STUB_USER_ID_HEADER).asString();

    JSONAssert.assertEquals(readFile(PUT_ONE_LABEL_REQUEST), actual, false);

    verify(1, putRequestedFor(urlEqualTo(RM_API_CUSTOMER_PATH))
      .withRequestBody(equalToJson(readFile(RM_PUT_ONE_LABEL_REQUEST))));
  }

  @Test
  public void shouldUpdateCustomLabelsOnPutWhenAllIsValidWithFiveItems()
    throws IOException, URISyntaxException, JSONException {
    mockCustomLabelsSuccessPutRequest();

    String putBody = readFile(PUT_FIVE_LABEL_REQUEST);
    String resourcePath = String.format(KB_CREDENTIALS_CUSTOM_LABELS_ENDPOINT, configuration.getId());
    String actual = putWithOk(resourcePath, putBody, STUB_USER_ID_HEADER).asString();

    JSONAssert.assertEquals(readFile(PUT_FIVE_LABEL_REQUEST), actual, false);

    verify(1, putRequestedFor(urlEqualTo(RM_API_CUSTOMER_PATH))
      .withRequestBody(equalToJson(readFile(RM_PUT_FIVE_LABEL_REQUEST))));
  }

  @Test
  public void shouldReturn422OnPutWhenIdNotInRange() throws IOException, URISyntaxException {
    String putBody = readFile(PUT_WITH_INVALID_ID_REQUEST);
    String resourcePath = String.format(KB_CREDENTIALS_CUSTOM_LABELS_ENDPOINT, UUID.randomUUID());
    JsonapiError error = putWithStatus(resourcePath, putBody, SC_UNPROCESSABLE_ENTITY, STUB_USER_ID_HEADER)
      .as(JsonapiError.class);

    assertErrorContainsTitle(error, "Invalid Custom Label id");
    assertErrorContainsDetail(error, "Custom Label id should be in range 1 - 5");
  }

  @Test
  public void shouldReturn422OnPutWhenInvalidNameLength() throws IOException, URISyntaxException {
    String putBody = readFile(PUT_WITH_INVALID_NAME_REQUEST);
    String resourcePath = String.format(KB_CREDENTIALS_CUSTOM_LABELS_ENDPOINT, UUID.randomUUID());
    JsonapiError error = putWithStatus(resourcePath, putBody, SC_UNPROCESSABLE_ENTITY, STUB_USER_ID_HEADER)
      .as(JsonapiError.class);

    assertErrorContainsTitle(error, "Invalid Custom Label Name");
    assertErrorContainsDetail(error, "Custom Label Name is too long (maximum is 50 characters)");
  }

  @Test
  public void shouldReturn422OnPutWhenHasDuplicateIds() throws IOException, URISyntaxException {
    String putBody = readFile(PUT_WITH_DUPLICATE_ID_REQUEST);
    String resourcePath = String.format(KB_CREDENTIALS_CUSTOM_LABELS_ENDPOINT, UUID.randomUUID());
    JsonapiError error = putWithStatus(resourcePath, putBody, SC_UNPROCESSABLE_ENTITY, STUB_USER_ID_HEADER)
      .as(JsonapiError.class);

    assertErrorContainsTitle(error, "Invalid request body");
    assertErrorContainsDetail(error, "Each label in body must contain unique id");
  }

  private void mockCustomLabelsConfiguration() throws IOException, URISyntaxException {
    stubFor(get(urlEqualTo(RM_API_CUSTOMER_PATH))
      .willReturn(aResponse().withStatus(SC_OK).withBody(readFile(RM_GET_LABELS_RESPONSE))));
  }

  private void mockCustomLabelsSuccessPutRequest() throws IOException, URISyntaxException {
    mockCustomLabelsConfiguration();
    stubFor(put(urlEqualTo(RM_API_CUSTOMER_PATH))
      .willReturn(aResponse().withStatus(SC_NO_CONTENT)));
  }
}
