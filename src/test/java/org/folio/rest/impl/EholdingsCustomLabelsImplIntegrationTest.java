package org.folio.rest.impl;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static org.folio.HttpStatus.SC_FORBIDDEN;
import static org.folio.HttpStatus.SC_NOT_FOUND;
import static org.folio.HttpStatus.SC_NO_CONTENT;
import static org.folio.HttpStatus.SC_UNAUTHORIZED;
import static org.folio.HttpStatus.SC_UNPROCESSABLE_CONTENT;
import static org.folio.repository.kbcredentials.KbCredentialsTableConstants.KB_CREDENTIALS_TABLE_NAME;
import static org.folio.util.AssertTestUtil.assertErrorContainsDetail;
import static org.folio.util.AssertTestUtil.assertErrorContainsTitle;
import static org.folio.util.AssertTestUtil.assertJsonEqual;
import static org.folio.util.KbCredentialsTestUtil.KB_CREDENTIALS_CUSTOM_LABELS_ENDPOINT;
import static org.folio.util.KbCredentialsTestUtil.setupDefaultKbConfiguration;
import static org.folio.util.TestUtil.clearDataFromTable;
import static org.folio.util.TestUtil.readFile;
import static org.folio.util.TestUtil.readJsonFile;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.tomakehurst.wiremock.matching.RegexPattern;
import java.util.UUID;
import org.folio.rest.jaxrs.model.CustomLabelsCollection;
import org.folio.rest.jaxrs.model.JsonapiError;
import org.folio.util.IntegrationTestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EholdingsCustomLabelsImplIntegrationTest extends IntegrationTestBase {

  private static final String KB_CUSTOM_LABELS_PATH = "eholdings/custom-labels";

  // RM API responses
  private static final String RM_GET_LABELS_RESPONSE = "responses/rmapi/proxiescustomlabels/get-success-response.json";
  private static final String RM_PUT_ONE_LABEL_REQUEST = "requests/rmapi/proxiescustomlabels/put-one-label.json";
  private static final String RM_PUT_FIVE_LABEL_REQUEST = "requests/rmapi/proxiescustomlabels/put-five-labels.json";

  // KB-EBSCO expected responses
  private static final String KB_GET_CUSTOM_LABELS_RESPONSE =
    "responses/kb-ebsco/custom-labels/get-custom-labels-list.json";

  // Request payloads
  private static final String REQUESTS_PATH = "requests/kb-ebsco/custom-labels";
  private static final String PUT_ONE_LABEL_REQUEST = REQUESTS_PATH + "/put-one-custom-label.json";
  private static final String PUT_FIVE_LABEL_REQUEST = REQUESTS_PATH + "/put-five-custom-labels.json";
  private static final String PUT_WITH_INVALID_ID_REQUEST = REQUESTS_PATH + "/put-custom-label-invalid-id.json";
  private static final String PUT_WITH_INVALID_NAME_REQUEST = REQUESTS_PATH + "/put-custom-label-invalid-name.json";
  private static final String PUT_WITH_DUPLICATE_ID_REQUEST = REQUESTS_PATH + "/put-custom-labels-duplicate-id.json";

  private String credentialsId;

  @BeforeEach
  void setUp() {
    credentialsId = setupDefaultKbConfiguration(getWiremockUrl(), vertx);
  }

  @AfterEach
  void tearDown() {
    clearDataFromTable(vertx, KB_CREDENTIALS_TABLE_NAME);
  }

  @Test
  void shouldReturnCustomLabelsOnGet() {
    mockCustomLabelsConfiguration();

    var actual = getWithOk(KB_CUSTOM_LABELS_PATH).asString();
    assertJsonEqual(readFile(KB_GET_CUSTOM_LABELS_RESPONSE), actual, false);
    verifyGet(equalTo(rootProxyCustomLabelsRmApi()), 1);
  }

  @Test
  void shouldReturn403OnGetWithResourcesWhenRmApi401() {
    mockGet(new RegexPattern(rootProxyCustomLabelsRmApi()), SC_UNAUTHORIZED);

    var error = getWithStatus(KB_CUSTOM_LABELS_PATH, SC_FORBIDDEN).as(JsonapiError.class);
    assertErrorContainsTitle(error, "Unauthorized Access");
  }

  @Test
  void shouldReturn403OnGetWithResourcesWhenRmApi403() {
    mockGet(new RegexPattern(rootProxyCustomLabelsRmApi()), SC_FORBIDDEN);

    var error = getWithStatus(KB_CUSTOM_LABELS_PATH, SC_FORBIDDEN).as(JsonapiError.class);
    assertErrorContainsTitle(error, "Unauthorized Access");
  }

  @Test
  void shouldReturnCustomLabelsOnGetByCredentials() {
    var expected = readJsonFile(KB_GET_CUSTOM_LABELS_RESPONSE, CustomLabelsCollection.class);
    expected.getData().forEach(customLabel -> customLabel.setCredentialsId(credentialsId));

    mockCustomLabelsConfiguration();
    var resourcePath = String.format(KB_CREDENTIALS_CUSTOM_LABELS_ENDPOINT, credentialsId);
    var actual = getWithOk(resourcePath).as(CustomLabelsCollection.class);

    verifyGet(equalTo(rootProxyCustomLabelsRmApi()), 1);
    assertEquals(expected, actual);
  }

  @Test
  void shouldReturn403OnGetByCredentialsWithResourcesWhenRmApi403() {
    mockGet(new RegexPattern(rootProxyCustomLabelsRmApi()), SC_FORBIDDEN);
    var resourcePath = String.format(KB_CREDENTIALS_CUSTOM_LABELS_ENDPOINT, credentialsId);
    var error = getWithStatus(resourcePath, SC_FORBIDDEN).as(JsonapiError.class);

    assertErrorContainsTitle(error, "Unauthorized Access");
  }

  @Test
  void shouldReturn404OnGetByCredentialsWhenCredentialsAreMissing() {
    var resourcePath = String.format(KB_CREDENTIALS_CUSTOM_LABELS_ENDPOINT, UUID.randomUUID());
    var error = getWithStatus(resourcePath, SC_NOT_FOUND).as(JsonapiError.class);

    assertErrorContainsTitle(error, "KbCredentials not found by id");
  }

  @Test
  void shouldUpdateCustomLabelsOnPutWhenAllIsValidWithOneItem() {
    mockCustomLabelsSuccessPutRequest();

    var putBody = readFile(PUT_ONE_LABEL_REQUEST);
    var resourcePath = String.format(KB_CREDENTIALS_CUSTOM_LABELS_ENDPOINT, credentialsId);
    var actual = putWithOk(resourcePath, putBody).asString();

    assertJsonEqual(readFile(PUT_ONE_LABEL_REQUEST), actual, false);
    verifyPut(equalTo(rootProxyCustomLabelsRmApi()), equalToJson(readFile(RM_PUT_ONE_LABEL_REQUEST)));
  }

  @Test
  void shouldUpdateCustomLabelsOnPutWhenAllIsValidWithFiveItems() {
    mockCustomLabelsSuccessPutRequest();

    var putBody = readFile(PUT_FIVE_LABEL_REQUEST);
    var resourcePath = String.format(KB_CREDENTIALS_CUSTOM_LABELS_ENDPOINT, credentialsId);
    var actual = putWithOk(resourcePath, putBody).asString();

    assertJsonEqual(readFile(PUT_FIVE_LABEL_REQUEST), actual, false);
    verifyPut(equalTo(rootProxyCustomLabelsRmApi()), equalToJson(readFile(RM_PUT_FIVE_LABEL_REQUEST)));
  }

  @Test
  void shouldReturn422OnPutWhenIdNotInRange() {
    var putBody = readFile(PUT_WITH_INVALID_ID_REQUEST);
    var resourcePath = String.format(KB_CREDENTIALS_CUSTOM_LABELS_ENDPOINT, UUID.randomUUID());
    var error = putWithStatus(resourcePath, putBody, SC_UNPROCESSABLE_CONTENT)
      .as(JsonapiError.class);

    assertErrorContainsTitle(error, "Invalid Custom Label id");
    assertErrorContainsDetail(error, "Custom Label id should be in range 1 - 5");
  }

  @Test
  void shouldReturn422OnPutWhenInvalidNameLength() {
    var putBody = readFile(PUT_WITH_INVALID_NAME_REQUEST);
    var resourcePath = String.format(KB_CREDENTIALS_CUSTOM_LABELS_ENDPOINT, UUID.randomUUID());
    var error = putWithStatus(resourcePath, putBody, SC_UNPROCESSABLE_CONTENT)
      .as(JsonapiError.class);

    assertErrorContainsTitle(error, "Invalid Custom Label Name");
    assertErrorContainsDetail(error, "Custom Label Name is too long (maximum is 50 characters)");
  }

  @Test
  void shouldReturn422OnPutWhenHasDuplicateIds() {
    var putBody = readFile(PUT_WITH_DUPLICATE_ID_REQUEST);
    var resourcePath = String.format(KB_CREDENTIALS_CUSTOM_LABELS_ENDPOINT, UUID.randomUUID());
    var error = putWithStatus(resourcePath, putBody, SC_UNPROCESSABLE_CONTENT)
      .as(JsonapiError.class);

    assertErrorContainsTitle(error, "Invalid request body");
    assertErrorContainsDetail(error, "Each label in body must contain unique id");
  }

  private void mockCustomLabelsConfiguration() {
    mockGet(equalTo(rootProxyCustomLabelsRmApi()), readFile(RM_GET_LABELS_RESPONSE));
  }

  private void mockCustomLabelsSuccessPutRequest() {
    mockCustomLabelsConfiguration();
    mockPut(equalTo(rootProxyCustomLabelsRmApi()), SC_NO_CONTENT);
  }
}
