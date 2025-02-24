package org.folio.rest.impl.integrationsuite;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CREATED;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.apache.http.HttpStatus.SC_UNPROCESSABLE_ENTITY;
import static org.folio.repository.assigneduser.AssignedUsersConstants.ASSIGNED_USERS_TABLE_NAME;
import static org.folio.repository.kbcredentials.KbCredentialsTableConstants.KB_CREDENTIALS_TABLE_NAME;
import static org.folio.rest.impl.PackagesTestData.FULL_PACKAGE_ID;
import static org.folio.rest.impl.PackagesTestData.STUB_PACKAGE_NAME;
import static org.folio.rest.impl.ProvidersTestData.STUB_VENDOR_ID;
import static org.folio.rest.impl.ProvidersTestData.STUB_VENDOR_NAME;
import static org.folio.rest.impl.ResourcesTestData.STUB_MANAGED_RESOURCE_ID;
import static org.folio.rest.impl.TitlesTestData.STUB_TITLE_ID;
import static org.folio.rest.impl.TitlesTestData.STUB_TITLE_NAME;
import static org.folio.test.util.TestUtil.STUB_TENANT;
import static org.folio.util.AssertTestUtil.assertErrorContainsDetail;
import static org.folio.util.AssertTestUtil.assertErrorContainsTitle;
import static org.folio.util.AssignedUsersTestUtil.saveAssignedUser;
import static org.folio.util.HoldingsRetryStatusTestUtil.getRetryStatus;
import static org.folio.util.HoldingsStatusUtil.getStatus;
import static org.folio.util.KbCredentialsTestUtil.KB_CREDENTIALS_ENDPOINT;
import static org.folio.util.KbCredentialsTestUtil.STUB_API_URL;
import static org.folio.util.KbCredentialsTestUtil.STUB_CREDENTIALS_NAME;
import static org.folio.util.KbCredentialsTestUtil.STUB_USERNAME;
import static org.folio.util.KbCredentialsTestUtil.STUB_USER_ID;
import static org.folio.util.KbCredentialsTestUtil.STUB_USER_ID_HEADER;
import static org.folio.util.KbCredentialsTestUtil.STUB_USER_ID_OTHER_HEADER;
import static org.folio.util.KbCredentialsTestUtil.STUB_USER_OTHER_ID;
import static org.folio.util.KbCredentialsTestUtil.USER_KB_CREDENTIAL_ENDPOINT;
import static org.folio.util.KbCredentialsTestUtil.getKbCredentials;
import static org.folio.util.KbCredentialsTestUtil.getKbCredentialsNonSecured;
import static org.folio.util.KbCredentialsTestUtil.saveKbCredentials;
import static org.folio.util.KbTestUtil.clearDataFromTable;
import static org.folio.util.PackagesTestUtil.buildDbPackage;
import static org.folio.util.PackagesTestUtil.savePackage;
import static org.folio.util.ProvidersTestUtil.buildDbProvider;
import static org.folio.util.ProvidersTestUtil.saveProvider;
import static org.folio.util.ResourcesTestUtil.buildResource;
import static org.folio.util.ResourcesTestUtil.saveResource;
import static org.folio.util.TitlesTestUtil.buildTitle;
import static org.folio.util.TitlesTestUtil.saveTitle;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import io.vertx.core.json.Json;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.collections4.map.CaseInsensitiveMap;
import org.apache.commons.lang3.StringUtils;
import org.folio.okapi.common.XOkapiHeaders;
import org.folio.repository.holdings.status.retry.RetryStatus;
import org.folio.rest.impl.WireMockTestBase;
import org.folio.rest.jaxrs.model.HoldingsLoadingStatus;
import org.folio.rest.jaxrs.model.JsonapiError;
import org.folio.rest.jaxrs.model.KbCredentials;
import org.folio.rest.jaxrs.model.KbCredentialsCollection;
import org.folio.rest.jaxrs.model.KbCredentialsDataAttributes;
import org.folio.rest.jaxrs.model.KbCredentialsKey;
import org.folio.rest.jaxrs.model.KbCredentialsPatchRequest;
import org.folio.rest.jaxrs.model.KbCredentialsPatchRequestData;
import org.folio.rest.jaxrs.model.KbCredentialsPatchRequestDataAttributes;
import org.folio.rest.jaxrs.model.KbCredentialsPostRequest;
import org.folio.rest.jaxrs.model.KbCredentialsPutRequest;
import org.folio.rest.jaxrs.model.LoadStatusNameEnum;
import org.folio.service.kbcredentials.KbCredentialsService;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@RunWith(VertxUnitRunner.class)
public class EholdingsKbCredentialsImplTest extends WireMockTestBase {

  private static final String STARS_256 = "*".repeat(256);
  private static final String OTHER_CUST_ID = "OTHER_CUST_ID";
  @Autowired
  @Qualifier("nonSecuredCredentialsService")
  private KbCredentialsService nonSecuredCredentialsService;

  @Autowired
  private KbCredentialsService securedCredentialsService;

  @After
  public void tearDown() {
    clearDataFromTable(vertx, ASSIGNED_USERS_TABLE_NAME);
    clearDataFromTable(vertx, KB_CREDENTIALS_TABLE_NAME);
  }

  @Test
  public void shouldReturnKbCredentialsCollectionOnGet() {
    saveKbCredentials(STUB_API_URL, STUB_CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);
    KbCredentialsCollection actual = getWithOk(KB_CREDENTIALS_ENDPOINT).as(KbCredentialsCollection.class);

    assertEquals(1, actual.getData().size());
    assertEquals(Integer.valueOf(1), actual.getMeta().getTotalResults());
    var credentials = actual.getData().getFirst();
    assertNotNull(credentials);
    assertNotNull(credentials.getId());

    assertEquals(STUB_API_URL, credentials.getAttributes().getUrl());
    assertEquals(STUB_CREDENTIALS_NAME, credentials.getAttributes().getName());
    assertEquals(STUB_CUSTOMER_ID, credentials.getAttributes().getCustomerId());
    assertEquals(StringUtils.repeat("*", 40), credentials.getAttributes().getApiKey());

    assertEquals(STUB_USERNAME, credentials.getMeta().getCreatedByUsername());
    assertEquals(STUB_USER_ID, credentials.getMeta().getCreatedByUserId());
    assertNotNull(credentials.getMeta().getCreatedDate());
  }

  @Test
  public void shouldReturnEmptyKbCredentialsCollectionOnGet() {
    KbCredentialsCollection actual = getWithOk(KB_CREDENTIALS_ENDPOINT).as(KbCredentialsCollection.class);

    assertNotNull(actual.getData());
    assertEquals(0, actual.getData().size());
    assertEquals(Integer.valueOf(0), actual.getMeta().getTotalResults());
  }

  @Test
  public void shouldReturn201OnPostIfCredentialsAreValid() {
    KbCredentialsPostRequest kbCredentialsPostRequest = new KbCredentialsPostRequest()
      .withData(stubbedCredentials());
    String postBody = Json.encode(kbCredentialsPostRequest);

    mockVerifyValidCredentialsRequest();
    KbCredentials actual = postWithStatus(KB_CREDENTIALS_ENDPOINT, postBody, SC_CREATED, STUB_USER_ID_HEADER)
      .as(KbCredentials.class);

    assertNotNull(actual);
    assertNotNull(actual.getId());
    assertNotNull(actual.getType());
    assertEquals(getWiremockUrl(), actual.getAttributes().getUrl());
    assertEquals(STUB_CREDENTIALS_NAME, actual.getAttributes().getName());
    assertEquals(STUB_CUSTOMER_ID, actual.getAttributes().getCustomerId());
    assertEquals(STUB_USER_ID, actual.getMeta().getCreatedByUserId());
    assertNotNull(actual.getMeta().getCreatedDate());
  }

  @Test
  public void shouldReturn422OnPostWhenCredentialsAreInvalid() {
    KbCredentialsPostRequest kbCredentialsPostRequest = new KbCredentialsPostRequest()
      .withData(stubbedCredentials());
    String postBody = Json.encode(kbCredentialsPostRequest);

    mockVerifyFailedCredentialsRequest();
    JsonapiError error = postWithStatus(KB_CREDENTIALS_ENDPOINT, postBody, SC_UNPROCESSABLE_ENTITY, STUB_USER_ID_HEADER)
      .as(JsonapiError.class);

    assertErrorContainsTitle(error, "KB API Credentials are invalid");
  }

  @Test
  public void shouldReturn422OnPostWhenCredentialsNameIsLongerThen255() {
    KbCredentials creds = stubbedCredentials();
    creds.getAttributes().setName(STARS_256);

    KbCredentialsPostRequest kbCredentialsPostRequest = new KbCredentialsPostRequest().withData(creds);
    String postBody = Json.encode(kbCredentialsPostRequest);

    JsonapiError error = postWithStatus(KB_CREDENTIALS_ENDPOINT, postBody, SC_UNPROCESSABLE_ENTITY, STUB_USER_ID_HEADER)
      .as(JsonapiError.class);

    assertErrorContainsTitle(error, "Invalid name");
    assertErrorContainsDetail(error, "name is too long (maximum is 255 characters)");
  }

  @Test
  public void shouldReturn422OnPostWhenCredentialsNameIsEmpty() {
    KbCredentials creds = stubbedCredentials();
    creds.getAttributes().setName("");

    KbCredentialsPostRequest kbCredentialsPostRequest = new KbCredentialsPostRequest().withData(creds);
    String postBody = Json.encode(kbCredentialsPostRequest);

    JsonapiError error = postWithStatus(KB_CREDENTIALS_ENDPOINT, postBody, SC_UNPROCESSABLE_ENTITY, STUB_USER_ID_HEADER)
      .as(JsonapiError.class);

    assertErrorContainsTitle(error, "Invalid name");
    assertErrorContainsDetail(error, "name must not be empty");
  }

  @Test
  public void shouldReturn422OnPostWhenCredentialsWithProvidedNameAlreadyExist() {
    saveKbCredentials(STUB_API_URL, STUB_CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);
    KbCredentialsPostRequest kbCredentialsPostRequest = new KbCredentialsPostRequest()
      .withData(stubbedCredentials());
    String postBody = Json.encode(kbCredentialsPostRequest);

    mockVerifyValidCredentialsRequest();
    JsonapiError error = postWithStatus(KB_CREDENTIALS_ENDPOINT, postBody, SC_UNPROCESSABLE_ENTITY, STUB_USER_ID_HEADER)
      .as(JsonapiError.class);

    assertErrorContainsTitle(error, "Duplicate name");
    assertErrorContainsDetail(error, String.format("Credentials with name '%s' already exist", STUB_CREDENTIALS_NAME));
  }

  @Test
  public void shouldReturn422OnPostWhenCredentialsWithProvidedCustIdAndUrlAlreadyExist() {
    saveKbCredentials(getWiremockUrl(), STUB_CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);

    KbCredentials creds = stubbedCredentials();
    creds.getAttributes().setName("Other KB");

    KbCredentialsPostRequest kbCredentialsPostRequest = new KbCredentialsPostRequest().withData(creds);
    String postBody = Json.encode(kbCredentialsPostRequest);

    mockVerifyValidCredentialsRequest();
    JsonapiError error = postWithStatus(KB_CREDENTIALS_ENDPOINT, postBody, SC_UNPROCESSABLE_ENTITY, STUB_USER_ID_HEADER)
      .as(JsonapiError.class);

    assertErrorContainsTitle(error, "Duplicate credentials");
    assertErrorContainsDetail(error, String.format("Credentials with customer id '%s' and url '%s' already exist",
      STUB_CUSTOMER_ID, getWiremockUrl()));
  }

  @Test
  public void shouldReturnKbCredentialsOnGet() {
    String credentialsId =
      saveKbCredentials(STUB_API_URL, STUB_CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);

    String resourcePath = KB_CREDENTIALS_ENDPOINT + "/" + credentialsId;
    KbCredentials actual = getWithOk(resourcePath).as(KbCredentials.class);

    assertEquals(getKbCredentials(vertx).getFirst(), actual);
  }

  @Test
  public void shouldReturnKbCredentialsKeyOnGet() {
    String credentialsId =
      saveKbCredentials(STUB_API_URL, STUB_CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);

    String resourcePath = KB_CREDENTIALS_ENDPOINT + "/" + credentialsId + "/key";
    KbCredentialsKey actual = getWithOk(resourcePath).as(KbCredentialsKey.class);

    assertEquals(STUB_API_KEY, actual.getAttributes().getApiKey());
  }

  @Test
  public void shouldReturn400OnGetWhenIdIsInvalid() {
    String resourcePath = KB_CREDENTIALS_ENDPOINT + "/invalid-id";
    JsonapiError error = getWithStatus(resourcePath, SC_BAD_REQUEST).as(JsonapiError.class);

    assertErrorContainsTitle(error, "parameter value {invalid-id} is not valid: must match");
  }

  @Test
  public void shouldReturn404OnGetWhenCredentialsAreMissing() {
    String resourcePath = KB_CREDENTIALS_ENDPOINT + "/11111111-1111-1111-a111-111111111111";
    JsonapiError error = getWithStatus(resourcePath, SC_NOT_FOUND).as(JsonapiError.class);

    assertErrorContainsTitle(error, "KbCredentials not found by id");
  }

  @Test
  public void shouldReturn204OnPatchIfCredentialsAreValid() {
    String credentialsId =
      saveKbCredentials(getWiremockUrl(), STUB_CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);

    KbCredentialsPatchRequest kbCredentialsPatchRequest = stubPatchRequest();
    kbCredentialsPatchRequest.getData().getAttributes().setCustomerId("updated");

    String patchBody = Json.encode(kbCredentialsPatchRequest);

    mockVerifyValidCredentialsRequest();
    String resourcePath = KB_CREDENTIALS_ENDPOINT + "/" + credentialsId;
    patchWithNoContent(resourcePath, patchBody, STUB_USER_ID_HEADER);

    KbCredentials actual = getKbCredentialsNonSecured(vertx).getFirst();

    assertNotNull(actual);
    assertNotNull(actual.getId());
    assertNotNull(actual.getType());
    assertEquals(getWiremockUrl(), actual.getAttributes().getUrl());
    assertEquals(STUB_API_KEY, actual.getAttributes().getApiKey());
    assertEquals(STUB_CREDENTIALS_NAME, actual.getAttributes().getName());
    assertEquals("updated", actual.getAttributes().getCustomerId());
    assertEquals(STUB_USER_ID, actual.getMeta().getCreatedByUserId());
    assertNotNull(actual.getMeta().getCreatedDate());
    assertEquals(STUB_USER_ID, actual.getMeta().getUpdatedByUserId());
    assertNotNull(actual.getMeta().getUpdatedDate());
  }

  @Test
  public void shouldReturn422OnPatchWhenCredentialsAreInvalid() {
    String credentialsId =
      saveKbCredentials(getWiremockUrl(), STUB_CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);

    KbCredentialsPatchRequest kbCredentialsPatchRequest = stubPatchRequest();
    kbCredentialsPatchRequest.getData().getAttributes().setCustomerId("updated");

    String patchBody = Json.encode(kbCredentialsPatchRequest);

    mockVerifyFailedCredentialsRequest();
    String resourcePath = KB_CREDENTIALS_ENDPOINT + "/" + credentialsId;
    JsonapiError error = patchWithStatus(resourcePath, patchBody, SC_UNPROCESSABLE_ENTITY, STUB_USER_ID_HEADER)
      .as(JsonapiError.class);

    assertErrorContainsTitle(error, "KB API Credentials are invalid");
  }

  @Test
  public void shouldReturn422OnPatchWhenCredentialsNameIsLongerThen255() {
    KbCredentialsPatchRequest kbCredentialsPatchRequest = stubPatchRequest();
    kbCredentialsPatchRequest.getData().getAttributes().setName(STARS_256);

    String patchBody = Json.encode(kbCredentialsPatchRequest);

    String resourcePath = KB_CREDENTIALS_ENDPOINT + "/11111111-1111-1111-a111-111111111111";
    JsonapiError error = patchWithStatus(resourcePath, patchBody, SC_UNPROCESSABLE_ENTITY, STUB_USER_ID_HEADER)
      .as(JsonapiError.class);

    assertErrorContainsTitle(error, "Invalid name");
    assertErrorContainsDetail(error, "name is too long (maximum is 255 characters)");
  }

  @Test
  public void shouldReturn422OnPatchWhenCredentialsNameIsEmpty() {
    KbCredentialsPatchRequest kbCredentialsPatchRequest = stubPatchRequest();
    kbCredentialsPatchRequest.getData().getAttributes().setName("");
    kbCredentialsPatchRequest.getData().getAttributes().setCustomerId(STUB_CUSTOMER_ID);

    String patchBody = Json.encode(kbCredentialsPatchRequest);

    String resourcePath = KB_CREDENTIALS_ENDPOINT + "/11111111-1111-1111-a111-111111111111";
    JsonapiError error = patchWithStatus(resourcePath, patchBody, SC_UNPROCESSABLE_ENTITY, STUB_USER_ID_HEADER)
      .as(JsonapiError.class);

    assertErrorContainsTitle(error, "Invalid name");
    assertErrorContainsDetail(error, "name must not be empty");
  }

  @Test
  public void shouldReturn422OnPatchWhenAllFieldsAreEmpty() {
    KbCredentialsPatchRequest kbCredentialsPatchRequest = stubPatchRequest();
    String patchBody = Json.encode(kbCredentialsPatchRequest);

    String resourcePath = KB_CREDENTIALS_ENDPOINT + "/11111111-1111-1111-a111-111111111111";
    JsonapiError error = patchWithStatus(resourcePath, patchBody, SC_UNPROCESSABLE_ENTITY, STUB_USER_ID_HEADER)
      .as(JsonapiError.class);

    assertErrorContainsTitle(error, "Invalid attributes");
    assertErrorContainsDetail(error, "At least one of attributes must not be empty");
  }

  @Test
  public void shouldReturn422OnPatchWhenCredentialsWithProvidedNameAlreadyExist() {
    saveKbCredentials(getWiremockUrl(), STUB_CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);
    String credentialsId = saveKbCredentials(getWiremockUrl(), STUB_CREDENTIALS_NAME + "2",
      STUB_API_KEY, OTHER_CUST_ID, vertx);

    KbCredentialsPatchRequest kbCredentialsPatchRequest = stubPatchRequest();
    kbCredentialsPatchRequest.getData().getAttributes().setName(STUB_CREDENTIALS_NAME);
    String patchBody = Json.encode(kbCredentialsPatchRequest);

    mockVerifyValidCredentialsRequest();
    String resourcePath = KB_CREDENTIALS_ENDPOINT + "/" + credentialsId;
    JsonapiError error = patchWithStatus(resourcePath, patchBody, SC_UNPROCESSABLE_ENTITY, STUB_USER_ID_HEADER)
      .as(JsonapiError.class);

    assertErrorContainsTitle(error, "Duplicate name");
    assertErrorContainsDetail(error, String.format("Credentials with name '%s' already exist", STUB_CREDENTIALS_NAME));
  }

  @Test
  public void shouldReturn422OnPatchWhenCredentialsWithProvidedCustIdAndUrlAlreadyExist() {
    saveKbCredentials(getWiremockUrl(), STUB_CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);
    final String credentialsId = saveKbCredentials(getWiremockUrl(), STUB_CREDENTIALS_NAME + "2",
      STUB_API_KEY, "OTHER_CUST_ID", vertx);

    KbCredentialsPatchRequest kbCredentialsPatchRequest = stubPatchRequest();
    kbCredentialsPatchRequest.getData().getAttributes().setCustomerId(STUB_CUSTOMER_ID);
    kbCredentialsPatchRequest.getData().getAttributes().setUrl(getWiremockUrl());
    String patchBody = Json.encode(kbCredentialsPatchRequest);

    mockVerifyValidCredentialsRequest();
    String resourcePath = KB_CREDENTIALS_ENDPOINT + "/" + credentialsId;
    JsonapiError error = patchWithStatus(resourcePath, patchBody, SC_UNPROCESSABLE_ENTITY, STUB_USER_ID_HEADER)
      .as(JsonapiError.class);

    assertErrorContainsTitle(error, "Duplicate credentials");
    assertErrorContainsDetail(error, String.format("Credentials with customer id '%s' and url '%s' already exist",
      STUB_CUSTOMER_ID, getWiremockUrl()));
  }

  @Test
  public void shouldReturn400OnPatchWhenIdIsInvalid() {
    KbCredentialsPatchRequest kbCredentialsPatchRequest = stubPatchRequest();
    kbCredentialsPatchRequest.getData().getAttributes().setName(STUB_CREDENTIALS_NAME);

    String patchBody = Json.encode(kbCredentialsPatchRequest);

    String resourcePath = KB_CREDENTIALS_ENDPOINT + "/invalid-id";
    JsonapiError error = patchWithStatus(resourcePath, patchBody, SC_BAD_REQUEST).as(JsonapiError.class);

    assertErrorContainsTitle(error, "parameter value {invalid-id} is not valid: must match");
  }

  @Test
  public void shouldReturn404OnPatchWhenCredentialsAreMissing() {
    KbCredentialsPatchRequest kbCredentialsPatchRequest = stubPatchRequest();
    kbCredentialsPatchRequest.getData().getAttributes().setName(STUB_CREDENTIALS_NAME);

    String patchBody = Json.encode(kbCredentialsPatchRequest);

    mockVerifyValidCredentialsRequest();
    String resourcePath = KB_CREDENTIALS_ENDPOINT + "/11111111-1111-1111-a111-111111111111";
    JsonapiError error =
      patchWithStatus(resourcePath, patchBody, SC_NOT_FOUND, STUB_USER_ID_HEADER).as(JsonapiError.class);

    assertErrorContainsTitle(error, "KbCredentials not found by id");
  }

  @Test
  public void shouldReturn204OnPutIfCredentialsAreValid() {
    String credentialsId =
      saveKbCredentials(STUB_API_URL, STUB_CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);

    KbCredentials creds = stubbedCredentials();
    creds.getAttributes()
      .withName(STUB_CREDENTIALS_NAME + "updated")
      .withCustomerId(STUB_CUSTOMER_ID + "updated");

    KbCredentialsPutRequest kbCredentialsPutRequest = new KbCredentialsPutRequest().withData(creds);
    String putBody = Json.encode(kbCredentialsPutRequest);

    mockVerifyValidCredentialsRequest();
    String resourcePath = KB_CREDENTIALS_ENDPOINT + "/" + credentialsId;
    putWithNoContent(resourcePath, putBody, STUB_USER_ID_HEADER);

    KbCredentials actual = getKbCredentials(vertx).getFirst();

    assertNotNull(actual);
    assertNotNull(actual.getId());
    assertNotNull(actual.getType());
    assertEquals(getWiremockUrl(), actual.getAttributes().getUrl());
    assertEquals(STUB_CREDENTIALS_NAME + "updated", actual.getAttributes().getName());
    assertEquals(STUB_CUSTOMER_ID + "updated", actual.getAttributes().getCustomerId());
    assertEquals(STUB_USER_ID, actual.getMeta().getCreatedByUserId());
    assertNotNull(actual.getMeta().getCreatedDate());
    assertEquals(STUB_USER_ID, actual.getMeta().getUpdatedByUserId());
    assertNotNull(actual.getMeta().getUpdatedDate());
  }

  @Test
  public void shouldReturn422OnPutWhenCredentialsAreInvalid() {
    String credentialsId =
      saveKbCredentials(STUB_API_URL, STUB_CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);

    KbCredentialsPutRequest kbCredentialsPutRequest = new KbCredentialsPutRequest()
      .withData(stubbedCredentials());
    String putBody = Json.encode(kbCredentialsPutRequest);

    mockVerifyFailedCredentialsRequest();
    String resourcePath = KB_CREDENTIALS_ENDPOINT + "/" + credentialsId;
    JsonapiError error = putWithStatus(resourcePath, putBody, SC_UNPROCESSABLE_ENTITY, STUB_USER_ID_HEADER)
      .as(JsonapiError.class);

    assertErrorContainsTitle(error, "KB API Credentials are invalid");
  }

  @Test
  public void shouldReturn422OnPutWhenCredentialsNameIsLongerThen255() {
    KbCredentials creds = stubbedCredentials();
    creds.getAttributes().setName(STARS_256);

    KbCredentialsPutRequest kbCredentialsPutRequest = new KbCredentialsPutRequest().withData(creds);
    String putBody = Json.encode(kbCredentialsPutRequest);

    String resourcePath = KB_CREDENTIALS_ENDPOINT + "/11111111-1111-1111-a111-111111111111";
    JsonapiError error = putWithStatus(resourcePath, putBody, SC_UNPROCESSABLE_ENTITY, STUB_USER_ID_HEADER)
      .as(JsonapiError.class);

    assertErrorContainsTitle(error, "Invalid name");
    assertErrorContainsDetail(error, "name is too long (maximum is 255 characters)");
  }

  @Test
  public void shouldReturn422OnPutWhenCredentialsNameIsEmpty() {
    KbCredentials creds = stubbedCredentials();
    creds.getAttributes().setName("");

    KbCredentialsPutRequest kbCredentialsPutRequest = new KbCredentialsPutRequest().withData(creds);
    String putBody = Json.encode(kbCredentialsPutRequest);

    String resourcePath = KB_CREDENTIALS_ENDPOINT + "/11111111-1111-1111-a111-111111111111";
    JsonapiError error = putWithStatus(resourcePath, putBody, SC_UNPROCESSABLE_ENTITY, STUB_USER_ID_HEADER)
      .as(JsonapiError.class);

    assertErrorContainsTitle(error, "Invalid name");
    assertErrorContainsDetail(error, "name must not be empty");
  }

  @Test
  public void shouldReturn422OnPutWhenCredentialsWithProvidedNameAlreadyExist() {
    saveKbCredentials(STUB_API_URL, STUB_CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);
    String credentialsId = saveKbCredentials(STUB_API_URL, STUB_CREDENTIALS_NAME + "2",
      STUB_API_KEY, OTHER_CUST_ID, vertx);

    KbCredentialsPutRequest kbCredentialsPutRequest = new KbCredentialsPutRequest()
      .withData(stubbedCredentials());
    String putBody = Json.encode(kbCredentialsPutRequest);

    mockVerifyValidCredentialsRequest();
    String resourcePath = KB_CREDENTIALS_ENDPOINT + "/" + credentialsId;
    JsonapiError error = putWithStatus(resourcePath, putBody, SC_UNPROCESSABLE_ENTITY, STUB_USER_ID_HEADER)
      .as(JsonapiError.class);

    assertErrorContainsTitle(error, "Duplicate name");
    assertErrorContainsDetail(error, String.format("Credentials with name '%s' already exist", STUB_CREDENTIALS_NAME));
  }

  @Test
  public void shouldReturn422OnPutWhenCredentialsWithProvidedCustIdAndUrlAlreadyExist() {
    saveKbCredentials(getWiremockUrl(), STUB_CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);
    String credentialsId = saveKbCredentials(getWiremockUrl(), STUB_CREDENTIALS_NAME + "2",
      STUB_API_KEY, OTHER_CUST_ID, vertx);

    KbCredentials creds = stubbedCredentials();
    creds.getAttributes().setName(STUB_CREDENTIALS_NAME + "2");
    KbCredentialsPutRequest kbCredentialsPutRequest = new KbCredentialsPutRequest().withData(creds);
    String putBody = Json.encode(kbCredentialsPutRequest);

    mockVerifyValidCredentialsRequest();
    String resourcePath = KB_CREDENTIALS_ENDPOINT + "/" + credentialsId;
    JsonapiError error = putWithStatus(resourcePath, putBody, SC_UNPROCESSABLE_ENTITY, STUB_USER_ID_HEADER)
      .as(JsonapiError.class);

    assertErrorContainsTitle(error, "Duplicate credentials");
    assertErrorContainsDetail(error, String.format("Credentials with customer id '%s' and url '%s' already exist",
      STUB_CUSTOMER_ID, getWiremockUrl()));
  }

  @Test
  public void shouldReturn400OnPutWhenIdIsInvalid() {
    KbCredentials creds = stubbedCredentials();
    creds.setId(UUID.randomUUID().toString());

    KbCredentialsPutRequest kbCredentialsPutRequest = new KbCredentialsPutRequest().withData(creds);
    String putBody = Json.encode(kbCredentialsPutRequest);

    String resourcePath = KB_CREDENTIALS_ENDPOINT + "/invalid-id";
    JsonapiError error = putWithStatus(resourcePath, putBody, SC_BAD_REQUEST).as(JsonapiError.class);

    assertErrorContainsTitle(error, "parameter value {invalid-id} is not valid: must match");
  }

  @Test
  public void shouldReturn404OnPutWhenCredentialsAreMissing() {
    KbCredentials creds = stubbedCredentials();
    creds.setId(UUID.randomUUID().toString());

    KbCredentialsPutRequest kbCredentialsPutRequest = new KbCredentialsPutRequest().withData(creds);
    String putBody = Json.encode(kbCredentialsPutRequest);

    mockVerifyValidCredentialsRequest();
    String resourcePath = KB_CREDENTIALS_ENDPOINT + "/11111111-1111-1111-a111-111111111111";
    JsonapiError error = putWithStatus(resourcePath, putBody, SC_NOT_FOUND, STUB_USER_ID_HEADER).as(JsonapiError.class);

    assertErrorContainsTitle(error, "KbCredentials not found by id");
  }

  @Test
  public void shouldReturn204OnDelete() {
    String credentialsId =
      saveKbCredentials(STUB_API_URL, STUB_CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);

    String resourcePath = KB_CREDENTIALS_ENDPOINT + "/" + credentialsId;
    deleteWithNoContent(resourcePath);

    List<KbCredentials> kbCredentialsInDb = getKbCredentials(vertx);
    assertTrue(kbCredentialsInDb.isEmpty());
  }

  @Test
  public void shouldReturn204OnDeleteWhenHasRelatedRecords() {
    String credentialsId = saveKbCredentials(STUB_API_URL, STUB_CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID,
      vertx);

    saveAssignedUser(STUB_USER_ID, credentialsId, vertx);
    saveProvider(buildDbProvider(STUB_VENDOR_ID, credentialsId, STUB_VENDOR_NAME), vertx);
    savePackage(buildDbPackage(FULL_PACKAGE_ID, credentialsId, STUB_PACKAGE_NAME), vertx);
    saveResource(buildResource(STUB_MANAGED_RESOURCE_ID, credentialsId, STUB_TITLE_NAME), vertx);
    saveTitle(buildTitle(STUB_TITLE_ID, credentialsId, STUB_TITLE_NAME), vertx);

    String resourcePath = KB_CREDENTIALS_ENDPOINT + "/" + credentialsId;
    deleteWithNoContent(resourcePath);

    List<KbCredentials> kbCredentialsInDb = getKbCredentials(vertx);
    assertTrue(kbCredentialsInDb.isEmpty());
  }

  @Test
  public void shouldReturn204OnDeleteWhenCredentialsAreMissing() {
    String resourcePath = KB_CREDENTIALS_ENDPOINT + "/11111111-1111-1111-a111-111111111111";
    deleteWithNoContent(resourcePath);

    List<KbCredentials> kbCredentialsInDb = getKbCredentials(vertx);
    assertTrue(kbCredentialsInDb.isEmpty());
  }

  @Test
  public void shouldReturn400OnDeleteWhenIdIsInvalid() {
    String resourcePath = KB_CREDENTIALS_ENDPOINT + "/invalid-id";
    JsonapiError error = deleteWithStatus(resourcePath, SC_BAD_REQUEST).as(JsonapiError.class);

    assertErrorContainsTitle(error, "parameter value {invalid-id} is not valid: must match");
  }

  @Test
  public void shouldReturn200AndKbCredentialsOnGetByUser() {
    String credentialsId =
      saveKbCredentials(STUB_API_URL, STUB_CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);
    saveAssignedUser(STUB_USER_ID, credentialsId, vertx);

    KbCredentials actual =
      getWithStatus(USER_KB_CREDENTIAL_ENDPOINT, SC_OK, STUB_USER_ID_HEADER).as(KbCredentials.class);
    assertEquals(getKbCredentialsNonSecured(vertx).getFirst(), actual);
  }

  @Test
  public void shouldReturn200AndKbCredentialsOnGetByUserWhenSingleKbCredentialsPresent() {
    saveKbCredentials(STUB_API_URL, STUB_CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);

    KbCredentials actual =
      getWithStatus(USER_KB_CREDENTIAL_ENDPOINT, SC_OK, STUB_USER_ID_HEADER).as(KbCredentials.class);
    assertEquals(getKbCredentialsNonSecured(vertx).getFirst(), actual);
  }

  @Test
  public void shouldReturn404OnGetByUserWhenAssignedUserIsMissingAndNoKbCredentialsAtAll() {
    JsonapiError error =
      getWithStatus(USER_KB_CREDENTIAL_ENDPOINT, SC_NOT_FOUND, STUB_USER_ID_HEADER).as(JsonapiError.class);

    assertErrorContainsTitle(error, "KB Credentials do not exist ");
  }

  @Test
  public void shouldReturn404OnGetByUserWhenUserIsNotTheOneWhoIsAssigned() {
    String credentialsId =
      saveKbCredentials(STUB_API_URL, STUB_CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);
    saveAssignedUser(STUB_USER_ID, credentialsId, vertx);

    JsonapiError error = getWithStatus(USER_KB_CREDENTIAL_ENDPOINT, SC_NOT_FOUND, STUB_USER_ID_OTHER_HEADER).as(
      JsonapiError.class);

    assertErrorContainsTitle(error, "KB Credentials do not exist or user with userId = " + STUB_USER_OTHER_ID
                                    + " is not assigned to any available knowledgebase.");
  }

  @Test
  public void shouldReturnStatusNotStartedOnKbCredentialsCreation() {
    KbCredentialsPostRequest kbCredentialsPostRequest = new KbCredentialsPostRequest()
      .withData(stubbedCredentials());
    String postBody = Json.encode(kbCredentialsPostRequest);

    mockVerifyValidCredentialsRequest();
    KbCredentials actual = postWithStatus(KB_CREDENTIALS_ENDPOINT, postBody, SC_CREATED, STUB_USER_ID_HEADER)
      .as(KbCredentials.class);

    final HoldingsLoadingStatus status = getStatus(actual.getId(), vertx);
    assertEquals(LoadStatusNameEnum.NOT_STARTED, status.getData().getAttributes().getStatus().getName());

    final RetryStatus retryStatus = getRetryStatus(actual.getId(), vertx);
    assertThat(retryStatus, notNullValue());
  }

  @Test
  public void shouldReturnSecuredCollection() {
    saveKbCredentials(STUB_API_URL, STUB_CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);
    Map<String, String> headers = new CaseInsensitiveMap<>();
    headers.put(XOkapiHeaders.TENANT, STUB_TENANT);
    KbCredentialsCollection collection = securedCredentialsService.findAll(headers).join();
    assertThat(collection.getMeta().getTotalResults(), equalTo(1));
    var credentials = collection.getData().getFirst();
    assertThat(credentials.getType(), equalTo(KbCredentials.Type.KB_CREDENTIALS));
    assertThat(credentials.getAttributes().getName(), equalTo(STUB_CREDENTIALS_NAME));
    assertThat(credentials.getAttributes().getApiKey(), containsString("*"));
    assertThat(credentials.getAttributes().getCustomerId(), equalTo(STUB_CUSTOMER_ID));
    assertThat(credentials.getAttributes().getUrl(), equalTo(STUB_API_URL));
  }

  @Test
  public void shouldReturnNonSecuredCollection() {
    saveKbCredentials(STUB_API_URL, STUB_CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);
    Map<String, String> headers = new CaseInsensitiveMap<>();
    headers.put(XOkapiHeaders.TENANT, STUB_TENANT);
    KbCredentialsCollection collection = nonSecuredCredentialsService.findAll(headers).join();
    assertThat(collection.getMeta().getTotalResults(), equalTo(1));
    var credentials = collection.getData().getFirst();
    assertThat(credentials.getType(), equalTo(KbCredentials.Type.KB_CREDENTIALS));
    assertThat(credentials.getAttributes().getName(), equalTo(STUB_CREDENTIALS_NAME));
    assertThat(credentials.getAttributes().getApiKey(), equalTo(STUB_API_KEY));
    assertThat(credentials.getAttributes().getCustomerId(), equalTo(STUB_CUSTOMER_ID));
    assertThat(credentials.getAttributes().getUrl(), equalTo(STUB_API_URL));
  }

  private KbCredentials stubbedCredentials() {
    return new KbCredentials()
      .withType(KbCredentials.Type.KB_CREDENTIALS)
      .withAttributes(new KbCredentialsDataAttributes()
        .withName(STUB_CREDENTIALS_NAME)
        .withCustomerId(STUB_CUSTOMER_ID)
        .withApiKey(STUB_API_KEY)
        .withUrl(getWiremockUrl()));
  }

  private KbCredentialsPatchRequest stubPatchRequest() {
    return new KbCredentialsPatchRequest().withData(
      new KbCredentialsPatchRequestData().withType(KbCredentialsPatchRequestData.Type.KB_CREDENTIALS)
        .withAttributes(new KbCredentialsPatchRequestDataAttributes())
    );
  }

  private void mockVerifyValidCredentialsRequest() {
    stubFor(
      get(urlPathMatching("/rm/rmaccounts/.*"))
        .willReturn(aResponse().withStatus(SC_OK).withBody("{\"totalResults\": 0, \"vendors\": []}")));
  }

  private void mockVerifyFailedCredentialsRequest() {
    stubFor(
      get(urlPathMatching("/rm/rmaccounts/.*"))
        .willReturn(aResponse().withStatus(SC_UNAUTHORIZED)));
  }

}
