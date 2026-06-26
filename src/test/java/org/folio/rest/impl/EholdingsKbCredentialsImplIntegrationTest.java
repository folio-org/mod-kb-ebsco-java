package org.folio.rest.impl;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CREATED;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNPROCESSABLE_ENTITY;
import static org.folio.repository.assigneduser.AssignedUsersConstants.ASSIGNED_USERS_TABLE_NAME;
import static org.folio.repository.kbcredentials.KbCredentialsTableConstants.KB_CREDENTIALS_TABLE_NAME;
import static org.folio.util.AssertTestUtil.assertErrorContainsDetail;
import static org.folio.util.AssertTestUtil.assertErrorContainsTitle;
import static org.folio.util.AssignedUsersTestUtil.saveAssignedUser;
import static org.folio.util.HoldingsRetryStatusTestUtil.getRetryStatus;
import static org.folio.util.HoldingsStatusUtil.getStatus;
import static org.folio.util.KbCredentialsTestUtil.API_URL;
import static org.folio.util.KbCredentialsTestUtil.CREDENTIALS_NAME;
import static org.folio.util.KbCredentialsTestUtil.KB_CREDENTIALS_ENDPOINT;
import static org.folio.util.KbCredentialsTestUtil.STUB_USERNAME;
import static org.folio.util.KbCredentialsTestUtil.STUB_USER_ID;
import static org.folio.util.KbCredentialsTestUtil.STUB_USER_ID_OTHER_HEADER;
import static org.folio.util.KbCredentialsTestUtil.STUB_USER_OTHER_ID;
import static org.folio.util.KbCredentialsTestUtil.USER_KB_CREDENTIAL_ENDPOINT;
import static org.folio.util.KbCredentialsTestUtil.getKbCredentials;
import static org.folio.util.KbCredentialsTestUtil.getKbCredentialsNonSecured;
import static org.folio.util.KbCredentialsTestUtil.saveKbCredentials;
import static org.folio.util.KbCredentialsTestUtil.stubPatchRequest;
import static org.folio.util.KbCredentialsTestUtil.stubbedCredentials;
import static org.folio.util.PackagesTestUtil.buildDbPackage;
import static org.folio.util.PackagesTestUtil.savePackage;
import static org.folio.util.ProvidersTestUtil.buildDbProvider;
import static org.folio.util.ProvidersTestUtil.saveProvider;
import static org.folio.util.ResourcesTestUtil.buildResource;
import static org.folio.util.ResourcesTestUtil.saveResource;
import static org.folio.util.TestUtil.STUB_TENANT;
import static org.folio.util.TestUtil.clearDataFromTable;
import static org.folio.util.TitlesTestUtil.buildTitle;
import static org.folio.util.TitlesTestUtil.saveTitle;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.vertx.core.json.Json;
import java.util.UUID;
import org.apache.commons.collections4.map.CaseInsensitiveMap;
import org.apache.commons.lang3.StringUtils;
import org.folio.okapi.common.XOkapiHeaders;
import org.folio.rest.jaxrs.model.JsonapiError;
import org.folio.rest.jaxrs.model.KbCredentials;
import org.folio.rest.jaxrs.model.KbCredentialsCollection;
import org.folio.rest.jaxrs.model.KbCredentialsKey;
import org.folio.rest.jaxrs.model.KbCredentialsPostRequest;
import org.folio.rest.jaxrs.model.KbCredentialsPutRequest;
import org.folio.rest.jaxrs.model.LoadStatusNameEnum;
import org.folio.service.kbcredentials.KbCredentialsService;
import org.folio.util.IntegrationTestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

class EholdingsKbCredentialsImplIntegrationTest extends IntegrationTestBase {

  private static final String STARS_256 = "*".repeat(256);
  private static final String OTHER_CUST_ID = "OTHER_CUST_ID";

  @Autowired
  @Qualifier("nonSecuredCredentialsService")
  private KbCredentialsService nonSecuredCredentialsService;

  @Autowired
  private KbCredentialsService securedCredentialsService;

  @AfterEach
  void tearDown() {
    clearDataFromTable(vertx, ASSIGNED_USERS_TABLE_NAME);
    clearDataFromTable(vertx, KB_CREDENTIALS_TABLE_NAME);
  }

  @Test
  void shouldReturnKbCredentialsCollectionOnGet() {
    saveKbCredentials(API_URL, CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);
    var actual = getWithOk(KB_CREDENTIALS_ENDPOINT).as(KbCredentialsCollection.class);

    assertEquals(1, actual.getData().size());
    assertEquals(Integer.valueOf(1), actual.getMeta().getTotalResults());
    var credentials = actual.getData().getFirst();
    assertNotNull(credentials);
    assertNotNull(credentials.getId());

    assertEquals(API_URL, credentials.getAttributes().getUrl());
    assertEquals(CREDENTIALS_NAME, credentials.getAttributes().getName());
    assertEquals(STUB_CUSTOMER_ID, credentials.getAttributes().getCustomerId());
    assertEquals(StringUtils.repeat("*", 40), credentials.getAttributes().getApiKey());

    assertEquals(STUB_USERNAME, credentials.getMeta().getCreatedByUsername());
    assertEquals(STUB_USER_ID, credentials.getMeta().getCreatedByUserId());
    assertNotNull(credentials.getMeta().getCreatedDate());
  }

  @Test
  void shouldReturnEmptyKbCredentialsCollectionOnGet() {
    var actual = getWithOk(KB_CREDENTIALS_ENDPOINT).as(KbCredentialsCollection.class);

    assertNotNull(actual.getData());
    assertEquals(0, actual.getData().size());
    assertEquals(Integer.valueOf(0), actual.getMeta().getTotalResults());
  }

  @Test
  void shouldReturn201OnPostIfCredentialsAreValid() {
    var kbCredentialsPostRequest = new KbCredentialsPostRequest().withData(stubbedCredentials(getWiremockUrl()));
    var postBody = Json.encode(kbCredentialsPostRequest);

    mockVerifyValidCredentialsRequest();
    var actual = postWithStatus(KB_CREDENTIALS_ENDPOINT, postBody, SC_CREATED).as(KbCredentials.class);

    assertNotNull(actual);
    assertNotNull(actual.getId());
    assertNotNull(actual.getType());
    assertEquals(getWiremockUrl(), actual.getAttributes().getUrl());
    assertEquals(CREDENTIALS_NAME, actual.getAttributes().getName());
    assertEquals(STUB_CUSTOMER_ID, actual.getAttributes().getCustomerId());
    assertEquals(STUB_USER_ID, actual.getMeta().getCreatedByUserId());
    assertNotNull(actual.getMeta().getCreatedDate());
  }

  @Test
  void shouldReturn422OnPostWhenCredentialsAreInvalid() {
    var kbCredentialsPostRequest = new KbCredentialsPostRequest().withData(stubbedCredentials(getWiremockUrl()));
    var postBody = Json.encode(kbCredentialsPostRequest);

    mockVerifyFailedCredentialsRequest();
    var error = postWithStatus(KB_CREDENTIALS_ENDPOINT, postBody, SC_UNPROCESSABLE_ENTITY).as(JsonapiError.class);

    assertErrorContainsTitle(error, "KB API Credentials are invalid");
  }

  @Test
  void shouldReturn422OnPostWhenCredentialsNameIsLongerThen255() {
    var creds = stubbedCredentials(getWiremockUrl());
    creds.getAttributes().setName(STARS_256);

    var kbCredentialsPostRequest = new KbCredentialsPostRequest().withData(creds);
    var postBody = Json.encode(kbCredentialsPostRequest);

    var error = postWithStatus(KB_CREDENTIALS_ENDPOINT, postBody, SC_UNPROCESSABLE_ENTITY).as(JsonapiError.class);

    assertErrorContainsTitle(error, "Invalid name");
    assertErrorContainsDetail(error, "name is too long (maximum is 255 characters)");
  }

  @Test
  void shouldReturn422OnPostWhenCredentialsNameIsEmpty() {
    var creds = stubbedCredentials(getWiremockUrl());
    creds.getAttributes().setName("");

    var kbCredentialsPostRequest = new KbCredentialsPostRequest().withData(creds);
    var postBody = Json.encode(kbCredentialsPostRequest);

    var error = postWithStatus(KB_CREDENTIALS_ENDPOINT, postBody, SC_UNPROCESSABLE_ENTITY).as(JsonapiError.class);

    assertErrorContainsTitle(error, "Invalid name");
    assertErrorContainsDetail(error, "name must not be empty");
  }

  @Test
  void shouldReturn422OnPostWhenCredentialsWithProvidedNameAlreadyExist() {
    saveKbCredentials(API_URL, CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);
    var kbCredentialsPostRequest = new KbCredentialsPostRequest().withData(stubbedCredentials(getWiremockUrl()));
    var postBody = Json.encode(kbCredentialsPostRequest);

    mockVerifyValidCredentialsRequest();
    var error = postWithStatus(KB_CREDENTIALS_ENDPOINT, postBody, SC_UNPROCESSABLE_ENTITY).as(JsonapiError.class);

    assertErrorContainsTitle(error, "Duplicate name");
    assertErrorContainsDetail(error, String.format("Credentials with name '%s' already exist", CREDENTIALS_NAME));
  }

  @Test
  void shouldReturn422OnPostWhenCredentialsWithProvidedCustIdAndUrlAlreadyExist() {
    saveKbCredentials(getWiremockUrl(), CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);

    var creds = stubbedCredentials(getWiremockUrl());
    creds.getAttributes().setName("Other KB");

    var kbCredentialsPostRequest = new KbCredentialsPostRequest().withData(creds);
    var postBody = Json.encode(kbCredentialsPostRequest);

    mockVerifyValidCredentialsRequest();
    var error = postWithStatus(KB_CREDENTIALS_ENDPOINT, postBody, SC_UNPROCESSABLE_ENTITY).as(JsonapiError.class);

    assertErrorContainsTitle(error, "Duplicate credentials");
    assertErrorContainsDetail(error, String.format("Credentials with customer id '%s' and url '%s' already exist",
      STUB_CUSTOMER_ID, getWiremockUrl()));
  }

  @Test
  void shouldReturnKbCredentialsOnGet() {
    var credentialsId =
      saveKbCredentials(API_URL, CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);

    var resourcePath = KB_CREDENTIALS_ENDPOINT + "/" + credentialsId;
    var actual = getWithOk(resourcePath).as(KbCredentials.class);

    assertEquals(getKbCredentials(vertx).getFirst(), actual);
  }

  @Test
  void shouldReturnKbCredentialsKeyOnGet() {
    var credentialsId =
      saveKbCredentials(API_URL, CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);

    var resourcePath = KB_CREDENTIALS_ENDPOINT + "/" + credentialsId + "/key";
    var actual = getWithOk(resourcePath).as(KbCredentialsKey.class);

    assertEquals(STUB_API_KEY, actual.getAttributes().getApiKey());
  }

  @Test
  void shouldReturn400OnGetWhenIdIsInvalid() {
    var resourcePath = KB_CREDENTIALS_ENDPOINT + "/invalid-id";
    var error = getWithStatus(resourcePath, SC_BAD_REQUEST).as(JsonapiError.class);

    assertErrorContainsTitle(error, "parameter value {invalid-id} is not valid: must match");
  }

  @Test
  void shouldReturn404OnGetWhenCredentialsAreMissing() {
    var resourcePath = KB_CREDENTIALS_ENDPOINT + "/11111111-1111-1111-a111-111111111111";
    var error = getWithStatus(resourcePath, SC_NOT_FOUND).as(JsonapiError.class);

    assertErrorContainsTitle(error, "KbCredentials not found by id");
  }

  @Test
  void shouldReturn204OnPatchIfCredentialsAreValid() {
    var credentialsId = saveKbCredentials(getWiremockUrl(), CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);

    var kbCredentialsPatchRequest = stubPatchRequest();
    kbCredentialsPatchRequest.getData().getAttributes().setCustomerId("updated");

    var patchBody = Json.encode(kbCredentialsPatchRequest);

    mockVerifyValidCredentialsRequest();
    var resourcePath = KB_CREDENTIALS_ENDPOINT + "/" + credentialsId;
    patchWithNoContent(resourcePath, patchBody);

    var actual = getKbCredentialsNonSecured(vertx).getFirst();

    assertNotNull(actual);
    assertNotNull(actual.getId());
    assertNotNull(actual.getType());
    assertEquals(getWiremockUrl(), actual.getAttributes().getUrl());
    assertEquals(STUB_API_KEY, actual.getAttributes().getApiKey());
    assertEquals(CREDENTIALS_NAME, actual.getAttributes().getName());
    assertEquals("updated", actual.getAttributes().getCustomerId());
    assertEquals(STUB_USER_ID, actual.getMeta().getCreatedByUserId());
    assertNotNull(actual.getMeta().getCreatedDate());
    assertEquals(STUB_USER_ID, actual.getMeta().getUpdatedByUserId());
    assertNotNull(actual.getMeta().getUpdatedDate());
  }

  @Test
  void shouldReturn422OnPatchWhenCredentialsAreInvalid() {
    var credentialsId = saveKbCredentials(getWiremockUrl(), CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);

    var kbCredentialsPatchRequest = stubPatchRequest();
    kbCredentialsPatchRequest.getData().getAttributes().setCustomerId("updated");

    var patchBody = Json.encode(kbCredentialsPatchRequest);

    mockVerifyFailedCredentialsRequest();
    var resourcePath = KB_CREDENTIALS_ENDPOINT + "/" + credentialsId;
    var error = patchWithStatus(resourcePath, patchBody, SC_UNPROCESSABLE_ENTITY).as(JsonapiError.class);

    assertErrorContainsTitle(error, "KB API Credentials are invalid");
  }

  @Test
  void shouldReturn422OnPatchWhenCredentialsNameIsLongerThen255() {
    var kbCredentialsPatchRequest = stubPatchRequest();
    kbCredentialsPatchRequest.getData().getAttributes().setName(STARS_256);

    var patchBody = Json.encode(kbCredentialsPatchRequest);

    var resourcePath = KB_CREDENTIALS_ENDPOINT + "/11111111-1111-1111-a111-111111111111";
    var error = patchWithStatus(resourcePath, patchBody, SC_UNPROCESSABLE_ENTITY).as(JsonapiError.class);

    assertErrorContainsTitle(error, "Invalid name");
    assertErrorContainsDetail(error, "name is too long (maximum is 255 characters)");
  }

  @Test
  void shouldReturn422OnPatchWhenCredentialsNameIsEmpty() {
    var kbCredentialsPatchRequest = stubPatchRequest();
    kbCredentialsPatchRequest.getData().getAttributes().setName("");
    kbCredentialsPatchRequest.getData().getAttributes().setCustomerId(STUB_CUSTOMER_ID);

    var patchBody = Json.encode(kbCredentialsPatchRequest);

    var resourcePath = KB_CREDENTIALS_ENDPOINT + "/11111111-1111-1111-a111-111111111111";
    var error = patchWithStatus(resourcePath, patchBody, SC_UNPROCESSABLE_ENTITY).as(JsonapiError.class);

    assertErrorContainsTitle(error, "Invalid name");
    assertErrorContainsDetail(error, "name must not be empty");
  }

  @Test
  void shouldReturn422OnPatchWhenAllFieldsAreEmpty() {
    var kbCredentialsPatchRequest = stubPatchRequest();
    var patchBody = Json.encode(kbCredentialsPatchRequest);

    var resourcePath = KB_CREDENTIALS_ENDPOINT + "/11111111-1111-1111-a111-111111111111";
    var error = patchWithStatus(resourcePath, patchBody, SC_UNPROCESSABLE_ENTITY).as(JsonapiError.class);

    assertErrorContainsTitle(error, "Invalid attributes");
    assertErrorContainsDetail(error, "At least one of attributes must not be empty");
  }

  @Test
  void shouldReturn422OnPatchWhenCredentialsWithProvidedNameAlreadyExist() {
    saveKbCredentials(getWiremockUrl(), CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);
    var credentialsId = saveKbCredentials(getWiremockUrl(), CREDENTIALS_NAME + "2",
      STUB_API_KEY, OTHER_CUST_ID, vertx);

    var kbCredentialsPatchRequest = stubPatchRequest();
    kbCredentialsPatchRequest.getData().getAttributes().setName(CREDENTIALS_NAME);
    var patchBody = Json.encode(kbCredentialsPatchRequest);

    mockVerifyValidCredentialsRequest();
    var resourcePath = KB_CREDENTIALS_ENDPOINT + "/" + credentialsId;
    var error = patchWithStatus(resourcePath, patchBody, SC_UNPROCESSABLE_ENTITY).as(JsonapiError.class);

    assertErrorContainsTitle(error, "Duplicate name");
    assertErrorContainsDetail(error, String.format("Credentials with name '%s' already exist", CREDENTIALS_NAME));
  }

  @Test
  void shouldReturn422OnPatchWhenCredentialsWithProvidedCustIdAndUrlAlreadyExist() {
    saveKbCredentials(getWiremockUrl(), CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);

    var kbCredentialsPatchRequest = stubPatchRequest();
    kbCredentialsPatchRequest.getData().getAttributes().setCustomerId(STUB_CUSTOMER_ID);
    kbCredentialsPatchRequest.getData().getAttributes().setUrl(getWiremockUrl());
    var patchBody = Json.encode(kbCredentialsPatchRequest);

    mockVerifyValidCredentialsRequest();
    var credentialsId = saveKbCredentials(getWiremockUrl(), CREDENTIALS_NAME + "2",
      STUB_API_KEY, OTHER_CUST_ID, vertx);
    var resourcePath = KB_CREDENTIALS_ENDPOINT + "/" + credentialsId;
    var error = patchWithStatus(resourcePath, patchBody, SC_UNPROCESSABLE_ENTITY).as(JsonapiError.class);

    assertErrorContainsTitle(error, "Duplicate credentials");
    assertErrorContainsDetail(error, String.format("Credentials with customer id '%s' and url '%s' already exist",
      STUB_CUSTOMER_ID, getWiremockUrl()));
  }

  @Test
  void shouldReturn400OnPatchWhenIdIsInvalid() {
    var kbCredentialsPatchRequest = stubPatchRequest();
    kbCredentialsPatchRequest.getData().getAttributes().setName(CREDENTIALS_NAME);

    var patchBody = Json.encode(kbCredentialsPatchRequest);

    var resourcePath = KB_CREDENTIALS_ENDPOINT + "/invalid-id";
    var error = patchWithStatus(resourcePath, patchBody, SC_BAD_REQUEST).as(JsonapiError.class);

    assertErrorContainsTitle(error, "parameter value {invalid-id} is not valid: must match");
  }

  @Test
  void shouldReturn404OnPatchWhenCredentialsAreMissing() {
    var kbCredentialsPatchRequest = stubPatchRequest();
    kbCredentialsPatchRequest.getData().getAttributes().setName(CREDENTIALS_NAME);

    var patchBody = Json.encode(kbCredentialsPatchRequest);

    mockVerifyValidCredentialsRequest();
    var resourcePath = KB_CREDENTIALS_ENDPOINT + "/11111111-1111-1111-a111-111111111111";
    var error =
      patchWithStatus(resourcePath, patchBody, SC_NOT_FOUND).as(JsonapiError.class);

    assertErrorContainsTitle(error, "KbCredentials not found by id");
  }

  @Test
  void shouldReturn204OnPutIfCredentialsAreValid() {
    var credentialsId =
      saveKbCredentials(API_URL, CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);

    var creds = stubbedCredentials(getWiremockUrl());
    creds.getAttributes()
      .withName(CREDENTIALS_NAME + "updated")
      .withCustomerId(STUB_CUSTOMER_ID + "updated");

    var kbCredentialsPutRequest = new KbCredentialsPutRequest().withData(creds);
    var putBody = Json.encode(kbCredentialsPutRequest);

    mockVerifyValidCredentialsRequest();
    var resourcePath = KB_CREDENTIALS_ENDPOINT + "/" + credentialsId;
    putWithNoContent(resourcePath, putBody);

    var actual = getKbCredentials(vertx).getFirst();

    assertNotNull(actual);
    assertNotNull(actual.getId());
    assertNotNull(actual.getType());
    assertEquals(getWiremockUrl(), actual.getAttributes().getUrl());
    assertEquals(CREDENTIALS_NAME + "updated", actual.getAttributes().getName());
    assertEquals(STUB_CUSTOMER_ID + "updated", actual.getAttributes().getCustomerId());
    assertEquals(STUB_USER_ID, actual.getMeta().getCreatedByUserId());
    assertNotNull(actual.getMeta().getCreatedDate());
    assertEquals(STUB_USER_ID, actual.getMeta().getUpdatedByUserId());
    assertNotNull(actual.getMeta().getUpdatedDate());
  }

  @Test
  void shouldReturn422OnPutWhenCredentialsAreInvalid() {
    var credentialsId =
      saveKbCredentials(API_URL, CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);

    var kbCredentialsPutRequest = new KbCredentialsPutRequest().withData(stubbedCredentials(getWiremockUrl()));
    var putBody = Json.encode(kbCredentialsPutRequest);

    mockVerifyFailedCredentialsRequest();
    var resourcePath = KB_CREDENTIALS_ENDPOINT + "/" + credentialsId;
    var error = putWithStatus(resourcePath, putBody, SC_UNPROCESSABLE_ENTITY).as(JsonapiError.class);

    assertErrorContainsTitle(error, "KB API Credentials are invalid");
  }

  @Test
  void shouldReturn422OnPutWhenCredentialsNameIsLongerThen255() {
    var creds = stubbedCredentials(getWiremockUrl());
    creds.getAttributes().setName(STARS_256);

    var kbCredentialsPutRequest = new KbCredentialsPutRequest().withData(creds);
    var putBody = Json.encode(kbCredentialsPutRequest);

    var resourcePath = KB_CREDENTIALS_ENDPOINT + "/11111111-1111-1111-a111-111111111111";
    var error = putWithStatus(resourcePath, putBody, SC_UNPROCESSABLE_ENTITY).as(JsonapiError.class);

    assertErrorContainsTitle(error, "Invalid name");
    assertErrorContainsDetail(error, "name is too long (maximum is 255 characters)");
  }

  @Test
  void shouldReturn422OnPutWhenCredentialsNameIsEmpty() {
    var creds = stubbedCredentials(getWiremockUrl());
    creds.getAttributes().setName("");

    var kbCredentialsPutRequest = new KbCredentialsPutRequest().withData(creds);
    var putBody = Json.encode(kbCredentialsPutRequest);

    var resourcePath = KB_CREDENTIALS_ENDPOINT + "/11111111-1111-1111-a111-111111111111";
    var error = putWithStatus(resourcePath, putBody, SC_UNPROCESSABLE_ENTITY).as(JsonapiError.class);

    assertErrorContainsTitle(error, "Invalid name");
    assertErrorContainsDetail(error, "name must not be empty");
  }

  @Test
  void shouldReturn422OnPutWhenCredentialsWithProvidedNameAlreadyExist() {
    saveKbCredentials(API_URL, CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);
    var credentialsId = saveKbCredentials(API_URL, CREDENTIALS_NAME + "2", STUB_API_KEY, OTHER_CUST_ID, vertx);

    var kbCredentialsPutRequest = new KbCredentialsPutRequest().withData(stubbedCredentials(getWiremockUrl()));
    var putBody = Json.encode(kbCredentialsPutRequest);

    mockVerifyValidCredentialsRequest();
    var resourcePath = KB_CREDENTIALS_ENDPOINT + "/" + credentialsId;
    var error = putWithStatus(resourcePath, putBody, SC_UNPROCESSABLE_ENTITY).as(JsonapiError.class);

    assertErrorContainsTitle(error, "Duplicate name");
    assertErrorContainsDetail(error, String.format("Credentials with name '%s' already exist", CREDENTIALS_NAME));
  }

  @Test
  void shouldReturn422OnPutWhenCredentialsWithProvidedCustIdAndUrlAlreadyExist() {
    saveKbCredentials(getWiremockUrl(), CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);
    var credentialsId = saveKbCredentials(getWiremockUrl(), CREDENTIALS_NAME + "2", STUB_API_KEY, OTHER_CUST_ID, vertx);

    var creds = stubbedCredentials(getWiremockUrl());
    creds.getAttributes().setName(CREDENTIALS_NAME + "2");
    var kbCredentialsPutRequest = new KbCredentialsPutRequest().withData(creds);
    var putBody = Json.encode(kbCredentialsPutRequest);

    mockVerifyValidCredentialsRequest();
    var resourcePath = KB_CREDENTIALS_ENDPOINT + "/" + credentialsId;
    var error = putWithStatus(resourcePath, putBody, SC_UNPROCESSABLE_ENTITY).as(JsonapiError.class);

    assertErrorContainsTitle(error, "Duplicate credentials");
    assertErrorContainsDetail(error, String.format("Credentials with customer id '%s' and url '%s' already exist",
      STUB_CUSTOMER_ID, getWiremockUrl()));
  }

  @Test
  void shouldReturn400OnPutWhenIdIsInvalid() {
    var creds = stubbedCredentials(getWiremockUrl());
    creds.setId(UUID.randomUUID().toString());

    var kbCredentialsPutRequest = new KbCredentialsPutRequest().withData(creds);
    var putBody = Json.encode(kbCredentialsPutRequest);

    var resourcePath = KB_CREDENTIALS_ENDPOINT + "/invalid-id";
    var error = putWithStatus(resourcePath, putBody, SC_BAD_REQUEST).as(JsonapiError.class);

    assertErrorContainsTitle(error, "parameter value {invalid-id} is not valid: must match");
  }

  @Test
  void shouldReturn404OnPutWhenCredentialsAreMissing() {
    var creds = stubbedCredentials(getWiremockUrl());
    creds.setId(UUID.randomUUID().toString());

    var kbCredentialsPutRequest = new KbCredentialsPutRequest().withData(creds);
    var putBody = Json.encode(kbCredentialsPutRequest);

    mockVerifyValidCredentialsRequest();
    var resourcePath = KB_CREDENTIALS_ENDPOINT + "/11111111-1111-1111-a111-111111111111";
    var error = putWithStatus(resourcePath, putBody, SC_NOT_FOUND).as(JsonapiError.class);

    assertErrorContainsTitle(error, "KbCredentials not found by id");
  }

  @Test
  void shouldReturn204OnDelete() {
    var credentialsId = saveKbCredentials(API_URL, CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);

    var resourcePath = KB_CREDENTIALS_ENDPOINT + "/" + credentialsId;
    deleteWithNoContent(resourcePath);

    var kbCredentialsInDb = getKbCredentials(vertx);
    assertTrue(kbCredentialsInDb.isEmpty());
  }

  @Test
  void shouldReturn204OnDeleteWhenHasRelatedRecords() {
    var credentialsId = saveKbCredentials(API_URL, CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);

    saveAssignedUser(STUB_USER_ID, credentialsId, vertx);
    saveProvider(buildDbProvider(STUB_VENDOR_ID, credentialsId, STUB_VENDOR_NAME), vertx);
    savePackage(buildDbPackage(FULL_PACKAGE_ID, credentialsId, STUB_PACKAGE_NAME), vertx);
    saveResource(buildResource(STUB_MANAGED_RESOURCE_ID, credentialsId, STUB_TITLE_NAME), vertx);
    saveTitle(buildTitle(STUB_TITLE_ID, credentialsId, STUB_TITLE_NAME), vertx);

    var resourcePath = KB_CREDENTIALS_ENDPOINT + "/" + credentialsId;
    deleteWithNoContent(resourcePath);

    var kbCredentialsInDb = getKbCredentials(vertx);
    assertTrue(kbCredentialsInDb.isEmpty());
  }

  @Test
  void shouldReturn204OnDeleteWhenCredentialsAreMissing() {
    var resourcePath = KB_CREDENTIALS_ENDPOINT + "/11111111-1111-1111-a111-111111111111";
    deleteWithNoContent(resourcePath);

    var kbCredentialsInDb = getKbCredentials(vertx);
    assertTrue(kbCredentialsInDb.isEmpty());
  }

  @Test
  void shouldReturn400OnDeleteWhenIdIsInvalid() {
    var resourcePath = KB_CREDENTIALS_ENDPOINT + "/invalid-id";
    var error = deleteWithStatus(resourcePath, SC_BAD_REQUEST).as(JsonapiError.class);

    assertErrorContainsTitle(error, "parameter value {invalid-id} is not valid: must match");
  }

  @Test
  void shouldReturn200AndKbCredentialsOnGetByUser() {
    var credentialsId = saveKbCredentials(API_URL, CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);
    saveAssignedUser(STUB_USER_ID, credentialsId, vertx);

    var actual = getWithStatus(USER_KB_CREDENTIAL_ENDPOINT, SC_OK).as(KbCredentials.class);
    assertEquals(getKbCredentialsNonSecured(vertx).getFirst(), actual);
  }

  @Test
  void shouldReturn200AndKbCredentialsOnGetByUserWhenSingleKbCredentialsPresent() {
    saveKbCredentials(API_URL, CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);

    var actual = getWithStatus(USER_KB_CREDENTIAL_ENDPOINT, SC_OK).as(KbCredentials.class);
    assertEquals(getKbCredentialsNonSecured(vertx).getFirst(), actual);
  }

  @Test
  void shouldReturn404OnGetByUserWhenAssignedUserIsMissingAndNoKbCredentialsAtAll() {
    var error = getWithStatus(USER_KB_CREDENTIAL_ENDPOINT, SC_NOT_FOUND).as(JsonapiError.class);

    assertErrorContainsTitle(error, "KB Credentials do not exist ");
  }

  @Test
  void shouldReturn404OnGetByUserWhenUserIsNotTheOneWhoIsAssigned() {
    var credentialsId = saveKbCredentials(API_URL, CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);
    saveAssignedUser(STUB_USER_ID, credentialsId, vertx);

    var error =
      getWithStatus(USER_KB_CREDENTIAL_ENDPOINT, SC_NOT_FOUND, STUB_USER_ID_OTHER_HEADER).as(JsonapiError.class);

    assertErrorContainsTitle(error, "KB Credentials do not exist or user with userId = " + STUB_USER_OTHER_ID
                                    + " is not assigned to any available knowledgebase.");
  }

  @Test
  void shouldReturnStatusNotStartedOnKbCredentialsCreation() {
    var kbCredentialsPostRequest = new KbCredentialsPostRequest().withData(stubbedCredentials(getWiremockUrl()));
    var postBody = Json.encode(kbCredentialsPostRequest);

    mockVerifyValidCredentialsRequest();
    var actual = postWithStatus(KB_CREDENTIALS_ENDPOINT, postBody, SC_CREATED).as(KbCredentials.class);

    var status = getStatus(actual.getId(), vertx);
    assertEquals(LoadStatusNameEnum.NOT_STARTED, status.getData().getAttributes().getStatus().getName());

    var retryStatus = getRetryStatus(actual.getId(), vertx);
    assertNotNull(retryStatus);
  }

  @Test
  void shouldReturnSecuredCollection() {
    saveKbCredentials(API_URL, CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);
    var headers = new CaseInsensitiveMap<String, String>();
    headers.put(XOkapiHeaders.TENANT, STUB_TENANT);
    var collection = securedCredentialsService.findAll(headers).join();
    assertEquals(1, collection.getMeta().getTotalResults());
    var credentials = collection.getData().getFirst();
    assertEquals(KbCredentials.Type.KB_CREDENTIALS, credentials.getType());
    assertEquals(CREDENTIALS_NAME, credentials.getAttributes().getName());
    assertTrue(credentials.getAttributes().getApiKey().contains("*"));
    assertEquals(STUB_CUSTOMER_ID, credentials.getAttributes().getCustomerId());
    assertEquals(API_URL, credentials.getAttributes().getUrl());
  }

  @Test
  void shouldReturnNonSecuredCollection() {
    saveKbCredentials(API_URL, CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);
    var headers = new CaseInsensitiveMap<String, String>();
    headers.put(XOkapiHeaders.TENANT, STUB_TENANT);
    var collection = nonSecuredCredentialsService.findAll(headers).join();
    assertEquals(1, collection.getMeta().getTotalResults());
    var credentials = collection.getData().getFirst();
    assertEquals(KbCredentials.Type.KB_CREDENTIALS, credentials.getType());
    assertEquals(CREDENTIALS_NAME, credentials.getAttributes().getName());
    assertEquals(STUB_API_KEY, credentials.getAttributes().getApiKey());
    assertEquals(STUB_CUSTOMER_ID, credentials.getAttributes().getCustomerId());
    assertEquals(API_URL, credentials.getAttributes().getUrl());
  }
}
