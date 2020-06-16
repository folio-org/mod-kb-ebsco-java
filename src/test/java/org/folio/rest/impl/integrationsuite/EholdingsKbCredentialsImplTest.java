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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import static org.folio.repository.assigneduser.AssignedUsersConstants.ASSIGNED_USERS_TABLE_NAME;
import static org.folio.repository.kbcredentials.KbCredentialsTableConstants.KB_CREDENTIALS_TABLE_NAME;
import static org.folio.rest.util.AssertTestUtil.assertErrorContainsDetail;
import static org.folio.rest.util.AssertTestUtil.assertErrorContainsTitle;
import static org.folio.rest.util.RestConstants.OKAPI_TENANT_HEADER;
import static org.folio.test.util.TestUtil.STUB_TENANT;
import static org.folio.util.AssignedUsersTestUtil.insertAssignedUser;
import static org.folio.util.HoldingsRetryStatusTestUtil.getRetryStatus;
import static org.folio.util.HoldingsStatusUtil.getStatus;
import static org.folio.util.KBTestUtil.clearDataFromTable;
import static org.folio.util.KbCredentialsTestUtil.KB_CREDENTIALS_ENDPOINT;
import static org.folio.util.KbCredentialsTestUtil.STUB_API_URL;
import static org.folio.util.KbCredentialsTestUtil.STUB_CREDENTIALS_NAME;
import static org.folio.util.KbCredentialsTestUtil.STUB_INVALID_TOKEN_HEADER;
import static org.folio.util.KbCredentialsTestUtil.STUB_TOKEN_HEADER;
import static org.folio.util.KbCredentialsTestUtil.STUB_TOKEN_OTHER_HEADER;
import static org.folio.util.KbCredentialsTestUtil.STUB_USERNAME;
import static org.folio.util.KbCredentialsTestUtil.STUB_USER_ID;
import static org.folio.util.KbCredentialsTestUtil.USER_KB_CREDENTIAL_ENDPOINT;
import static org.folio.util.KbCredentialsTestUtil.getKbCredentials;
import static org.folio.util.KbCredentialsTestUtil.getKbCredentialsNonSecured;
import static org.folio.util.KbCredentialsTestUtil.saveKbCredentials;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.vertx.core.json.Json;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import joptsimple.internal.Strings;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import org.folio.repository.holdings.status.retry.RetryStatus;
import org.folio.rest.impl.WireMockTestBase;
import org.folio.rest.jaxrs.model.HoldingsLoadingStatus;
import org.folio.rest.jaxrs.model.JsonapiError;
import org.folio.rest.jaxrs.model.KbCredentials;
import org.folio.rest.jaxrs.model.KbCredentialsCollection;
import org.folio.rest.jaxrs.model.KbCredentialsDataAttributes;
import org.folio.rest.jaxrs.model.KbCredentialsPatchRequest;
import org.folio.rest.jaxrs.model.KbCredentialsPostRequest;
import org.folio.rest.jaxrs.model.KbCredentialsPutRequest;
import org.folio.rest.jaxrs.model.LoadStatusNameEnum;
import org.folio.service.kbcredentials.KbCredentialsService;

@RunWith(VertxUnitRunner.class)
public class EholdingsKbCredentialsImplTest extends WireMockTestBase {

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
    assertNotNull(actual.getData().get(0));
    assertNotNull(actual.getData().get(0).getId());

    assertEquals(STUB_API_URL, actual.getData().get(0).getAttributes().getUrl());
    assertEquals(STUB_CREDENTIALS_NAME, actual.getData().get(0).getAttributes().getName());
    assertEquals(STUB_CUSTOMER_ID, actual.getData().get(0).getAttributes().getCustomerId());
    assertEquals(StringUtils.repeat("*", 40), actual.getData().get(0).getAttributes().getApiKey());

    assertEquals(STUB_USERNAME, actual.getData().get(0).getMeta().getCreatedByUsername());
    assertEquals(STUB_USER_ID, actual.getData().get(0).getMeta().getCreatedByUserId());
    assertNotNull(actual.getData().get(0).getMeta().getCreatedDate());
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
    KbCredentials actual = postWithStatus(KB_CREDENTIALS_ENDPOINT, postBody, SC_CREATED, STUB_TOKEN_HEADER)
      .as(KbCredentials.class);

    assertNotNull(actual);
    assertNotNull(actual.getId());
    assertNotNull(actual.getType());
    assertEquals(getWiremockUrl(), actual.getAttributes().getUrl());
    assertEquals(STUB_CREDENTIALS_NAME, actual.getAttributes().getName());
    assertEquals(STUB_CUSTOMER_ID, actual.getAttributes().getCustomerId());
    assertEquals(STUB_USERNAME, actual.getMeta().getCreatedByUsername());
    assertEquals(STUB_USER_ID, actual.getMeta().getCreatedByUserId());
    assertNotNull(actual.getMeta().getCreatedDate());
  }

  @Test
  public void shouldReturn422OnPostWhenCredentialsAreInvalid() {
    KbCredentialsPostRequest kbCredentialsPostRequest = new KbCredentialsPostRequest()
      .withData(stubbedCredentials());
    String postBody = Json.encode(kbCredentialsPostRequest);

    mockVerifyFailedCredentialsRequest();
    JsonapiError error = postWithStatus(KB_CREDENTIALS_ENDPOINT, postBody, SC_UNPROCESSABLE_ENTITY, STUB_TOKEN_HEADER)
      .as(JsonapiError.class);

    assertErrorContainsTitle(error, "KB API Credentials are invalid");
  }

  @Test
  public void shouldReturn422OnPostWhenCredentialsNameIsLongerThen255() {
    KbCredentials creds = stubbedCredentials();
    creds.getAttributes().setName(Strings.repeat('*', 256));

    KbCredentialsPostRequest kbCredentialsPostRequest = new KbCredentialsPostRequest().withData(creds);
    String postBody = Json.encode(kbCredentialsPostRequest);

    JsonapiError error = postWithStatus(KB_CREDENTIALS_ENDPOINT, postBody, SC_UNPROCESSABLE_ENTITY, STUB_TOKEN_HEADER)
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

    JsonapiError error = postWithStatus(KB_CREDENTIALS_ENDPOINT, postBody, SC_UNPROCESSABLE_ENTITY, STUB_TOKEN_HEADER)
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
    JsonapiError error = postWithStatus(KB_CREDENTIALS_ENDPOINT, postBody, SC_UNPROCESSABLE_ENTITY, STUB_TOKEN_HEADER)
      .as(JsonapiError.class);

    assertErrorContainsTitle(error, "Duplicate name");
    assertErrorContainsDetail(error, String.format("Credentials with name '%s' already exist", STUB_CREDENTIALS_NAME));
  }

  @Test
  public void shouldReturnKbCredentialsOnGet() {
    String credentialsId = saveKbCredentials(STUB_API_URL, STUB_CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);

    String resourcePath = KB_CREDENTIALS_ENDPOINT + "/" + credentialsId;
    KbCredentials actual = getWithOk(resourcePath).as(KbCredentials.class);

    assertEquals(getKbCredentials(vertx).get(0), actual);
  }

  @Test
  public void shouldReturn400OnGetWhenIdIsInvalid() {
    String resourcePath = KB_CREDENTIALS_ENDPOINT + "/invalid-id";
    JsonapiError error = getWithStatus(resourcePath, SC_BAD_REQUEST).as(JsonapiError.class);

    assertErrorContainsTitle(error, "'id' parameter is incorrect.");
  }

  @Test
  public void shouldReturn404OnGetWhenCredentialsAreMissing() {
    String resourcePath = KB_CREDENTIALS_ENDPOINT + "/11111111-1111-1111-a111-111111111111";
    JsonapiError error = getWithStatus(resourcePath, SC_NOT_FOUND).as(JsonapiError.class);

    assertErrorContainsTitle(error, "KbCredentials not found by id");
  }

  @Test
  public void shouldReturn204OnPatchIfCredentialsAreValid() {
    String credentialsId = saveKbCredentials(getWiremockUrl(), STUB_CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);

    KbCredentials creds = stubbedCredentials();
    creds.getAttributes()
      .withUrl(null)
      .withApiKey(null)
      .withName(STUB_CREDENTIALS_NAME + "updated")
      .withCustomerId(STUB_CUSTOMER_ID + "updated");

    KbCredentialsPatchRequest kbCredentialsPatchRequest = new KbCredentialsPatchRequest().withData(creds);
    String patchBody = Json.encode(kbCredentialsPatchRequest);

    mockVerifyValidCredentialsRequest();
    String resourcePath = KB_CREDENTIALS_ENDPOINT + "/" + credentialsId;
    patchWithNoContent(resourcePath, patchBody, STUB_TOKEN_HEADER);

    KbCredentials actual = getKbCredentialsNonSecured(vertx).get(0);

    assertNotNull(actual);
    assertNotNull(actual.getId());
    assertNotNull(actual.getType());
    assertEquals(getWiremockUrl(), actual.getAttributes().getUrl());
    assertEquals(STUB_API_KEY, actual.getAttributes().getApiKey());
    assertEquals(STUB_CREDENTIALS_NAME + "updated", actual.getAttributes().getName());
    assertEquals(STUB_CUSTOMER_ID + "updated", actual.getAttributes().getCustomerId());
    assertEquals(STUB_USERNAME, actual.getMeta().getCreatedByUsername());
    assertEquals(STUB_USER_ID, actual.getMeta().getCreatedByUserId());
    assertNotNull(actual.getMeta().getCreatedDate());
    assertEquals(STUB_USERNAME, actual.getMeta().getUpdatedByUsername());
    assertEquals(STUB_USER_ID, actual.getMeta().getUpdatedByUserId());
    assertNotNull(actual.getMeta().getUpdatedDate());
  }

  @Test
  public void shouldReturn422OnPatchWhenCredentialsAreInvalid() {
    String credentialsId = saveKbCredentials(STUB_API_URL, STUB_CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);

    KbCredentialsPutRequest kbCredentialsPutRequest = new KbCredentialsPutRequest()
      .withData(stubbedCredentials());
    String putBody = Json.encode(kbCredentialsPutRequest);

    mockVerifyFailedCredentialsRequest();
    String resourcePath = KB_CREDENTIALS_ENDPOINT + "/" + credentialsId;
    JsonapiError error = patchWithStatus(resourcePath, putBody, SC_UNPROCESSABLE_ENTITY, STUB_TOKEN_HEADER)
      .as(JsonapiError.class);

    assertEquals("KB API Credentials are invalid", error.getErrors().get(0).getTitle());
  }

  @Test
  public void shouldReturn422OnPatchWhenCredentialsNameIsLongerThen255() {
    KbCredentials creds = stubbedCredentials();
    creds.getAttributes().setName(Strings.repeat('*', 256));

    KbCredentialsPutRequest kbCredentialsPutRequest = new KbCredentialsPutRequest().withData(creds);
    String putBody = Json.encode(kbCredentialsPutRequest);

    String resourcePath = KB_CREDENTIALS_ENDPOINT + "/11111111-1111-1111-a111-111111111111";
    JsonapiError error = patchWithStatus(resourcePath, putBody, SC_UNPROCESSABLE_ENTITY, STUB_TOKEN_HEADER)
      .as(JsonapiError.class);

    assertEquals("Invalid name", error.getErrors().get(0).getTitle());
    assertEquals("name is too long (maximum is 255 characters)", error.getErrors().get(0).getDetail());
  }

  @Test
  public void shouldReturn422OnPatchWhenCredentialsNameIsEmpty() {
    KbCredentials creds = stubbedCredentials();
    creds.getAttributes().setName("");

    KbCredentialsPutRequest kbCredentialsPutRequest = new KbCredentialsPutRequest().withData(creds);
    String putBody = Json.encode(kbCredentialsPutRequest);

    String resourcePath = KB_CREDENTIALS_ENDPOINT + "/11111111-1111-1111-a111-111111111111";
    JsonapiError error = patchWithStatus(resourcePath, putBody, SC_UNPROCESSABLE_ENTITY, STUB_TOKEN_HEADER)
      .as(JsonapiError.class);

    assertEquals("Invalid name", error.getErrors().get(0).getTitle());
    assertEquals("name must not be empty", error.getErrors().get(0).getDetail());
  }

  @Test
  public void shouldReturn422OnPatchWhenCredentialsWithProvidedNameAlreadyExist() {
    saveKbCredentials(STUB_API_URL, STUB_CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);
    String credentialsId = saveKbCredentials(STUB_API_URL, STUB_CREDENTIALS_NAME + "2",
      STUB_API_KEY, STUB_CUSTOMER_ID, vertx);

    KbCredentialsPutRequest kbCredentialsPutRequest = new KbCredentialsPutRequest()
      .withData(stubbedCredentials());
    String putBody = Json.encode(kbCredentialsPutRequest);

    mockVerifyValidCredentialsRequest();
    String resourcePath = KB_CREDENTIALS_ENDPOINT + "/" + credentialsId;
    JsonapiError error = patchWithStatus(resourcePath, putBody, SC_UNPROCESSABLE_ENTITY, STUB_TOKEN_HEADER)
      .as(JsonapiError.class);

    assertEquals("Duplicate name", error.getErrors().get(0).getTitle());
    assertEquals(String.format("Credentials with name '%s' already exist", STUB_CREDENTIALS_NAME),
      error.getErrors().get(0).getDetail());
  }

  @Test
  public void shouldReturn400OnPatchWhenIdIsInvalid() {
    KbCredentials creds = stubbedCredentials();
    creds.setId(UUID.randomUUID().toString());

    KbCredentialsPutRequest kbCredentialsPutRequest = new KbCredentialsPutRequest().withData(creds);
    String putBody = Json.encode(kbCredentialsPutRequest);

    String resourcePath = KB_CREDENTIALS_ENDPOINT + "/invalid-id";
    JsonapiError error = patchWithStatus(resourcePath, putBody, SC_BAD_REQUEST).as(JsonapiError.class);

    assertThat(error.getErrors().get(0).getTitle(), containsString("'id' parameter is incorrect."));
  }

  @Test
  public void shouldReturn404OnPatchWhenCredentialsAreMissing() {
    KbCredentials creds = stubbedCredentials();
    creds.setId(UUID.randomUUID().toString());

    KbCredentialsPutRequest kbCredentialsPutRequest = new KbCredentialsPutRequest().withData(creds);
    String putBody = Json.encode(kbCredentialsPutRequest);

    mockVerifyValidCredentialsRequest();
    String resourcePath = KB_CREDENTIALS_ENDPOINT + "/11111111-1111-1111-a111-111111111111";
    JsonapiError error = patchWithStatus(resourcePath, putBody, SC_NOT_FOUND, STUB_TOKEN_HEADER).as(JsonapiError.class);

    assertThat(error.getErrors().get(0).getTitle(), containsString("KbCredentials not found by id"));
  }

  @Test
  public void shouldReturn204OnPutIfCredentialsAreValid() {
    String credentialsId = saveKbCredentials(STUB_API_URL, STUB_CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);

    KbCredentials creds = stubbedCredentials();
    creds.getAttributes()
      .withName(STUB_CREDENTIALS_NAME + "updated")
      .withCustomerId(STUB_CUSTOMER_ID + "updated");

    KbCredentialsPutRequest kbCredentialsPutRequest = new KbCredentialsPutRequest().withData(creds);
    String putBody = Json.encode(kbCredentialsPutRequest);

    mockVerifyValidCredentialsRequest();
    String resourcePath = KB_CREDENTIALS_ENDPOINT + "/" + credentialsId;
    putWithNoContent(resourcePath, putBody, STUB_TOKEN_HEADER);

    KbCredentials actual = getKbCredentials(vertx).get(0);

    assertNotNull(actual);
    assertNotNull(actual.getId());
    assertNotNull(actual.getType());
    assertEquals(getWiremockUrl(), actual.getAttributes().getUrl());
    assertEquals(STUB_CREDENTIALS_NAME + "updated", actual.getAttributes().getName());
    assertEquals(STUB_CUSTOMER_ID + "updated", actual.getAttributes().getCustomerId());
    assertEquals(STUB_USERNAME, actual.getMeta().getCreatedByUsername());
    assertEquals(STUB_USER_ID, actual.getMeta().getCreatedByUserId());
    assertNotNull(actual.getMeta().getCreatedDate());
    assertEquals(STUB_USERNAME, actual.getMeta().getUpdatedByUsername());
    assertEquals(STUB_USER_ID, actual.getMeta().getUpdatedByUserId());
    assertNotNull(actual.getMeta().getUpdatedDate());
  }

  @Test
  public void shouldReturn422OnPutWhenCredentialsAreInvalid() {
    String credentialsId = saveKbCredentials(STUB_API_URL, STUB_CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);

    KbCredentialsPutRequest kbCredentialsPutRequest = new KbCredentialsPutRequest()
      .withData(stubbedCredentials());
    String putBody = Json.encode(kbCredentialsPutRequest);

    mockVerifyFailedCredentialsRequest();
    String resourcePath = KB_CREDENTIALS_ENDPOINT + "/" + credentialsId;
    JsonapiError error = putWithStatus(resourcePath, putBody, SC_UNPROCESSABLE_ENTITY, STUB_TOKEN_HEADER)
      .as(JsonapiError.class);

    assertErrorContainsTitle(error, "KB API Credentials are invalid");
  }

  @Test
  public void shouldReturn422OnPutWhenCredentialsNameIsLongerThen255() {
    KbCredentials creds = stubbedCredentials();
    creds.getAttributes().setName(Strings.repeat('*', 256));

    KbCredentialsPutRequest kbCredentialsPutRequest = new KbCredentialsPutRequest().withData(creds);
    String putBody = Json.encode(kbCredentialsPutRequest);

    String resourcePath = KB_CREDENTIALS_ENDPOINT + "/11111111-1111-1111-a111-111111111111";
    JsonapiError error = putWithStatus(resourcePath, putBody, SC_UNPROCESSABLE_ENTITY, STUB_TOKEN_HEADER)
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
    JsonapiError error = putWithStatus(resourcePath, putBody, SC_UNPROCESSABLE_ENTITY, STUB_TOKEN_HEADER)
      .as(JsonapiError.class);

    assertErrorContainsTitle(error, "Invalid name");
    assertErrorContainsDetail(error, "name must not be empty");
  }

  @Test
  public void shouldReturn422OnPutWhenCredentialsWithProvidedNameAlreadyExist() {
    saveKbCredentials(STUB_API_URL, STUB_CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);
    String credentialsId = saveKbCredentials(STUB_API_URL, STUB_CREDENTIALS_NAME + "2",
      STUB_API_KEY, STUB_CUSTOMER_ID, vertx);

    KbCredentialsPutRequest kbCredentialsPutRequest = new KbCredentialsPutRequest()
      .withData(stubbedCredentials());
    String putBody = Json.encode(kbCredentialsPutRequest);

    mockVerifyValidCredentialsRequest();
    String resourcePath = KB_CREDENTIALS_ENDPOINT + "/" + credentialsId;
    JsonapiError error = putWithStatus(resourcePath, putBody, SC_UNPROCESSABLE_ENTITY, STUB_TOKEN_HEADER)
      .as(JsonapiError.class);

    assertErrorContainsTitle(error, "Duplicate name");
    assertErrorContainsDetail(error, String.format("Credentials with name '%s' already exist", STUB_CREDENTIALS_NAME));
  }

  @Test
  public void shouldReturn400OnPutWhenIdIsInvalid() {
    KbCredentials creds = stubbedCredentials();
    creds.setId(UUID.randomUUID().toString());

    KbCredentialsPutRequest kbCredentialsPutRequest = new KbCredentialsPutRequest().withData(creds);
    String putBody = Json.encode(kbCredentialsPutRequest);

    String resourcePath = KB_CREDENTIALS_ENDPOINT + "/invalid-id";
    JsonapiError error = putWithStatus(resourcePath, putBody, SC_BAD_REQUEST).as(JsonapiError.class);

    assertErrorContainsTitle(error, "'id' parameter is incorrect.");
  }

  @Test
  public void shouldReturn404OnPutWhenCredentialsAreMissing() {
    KbCredentials creds = stubbedCredentials();
    creds.setId(UUID.randomUUID().toString());

    KbCredentialsPutRequest kbCredentialsPutRequest = new KbCredentialsPutRequest().withData(creds);
    String putBody = Json.encode(kbCredentialsPutRequest);

    mockVerifyValidCredentialsRequest();
    String resourcePath = KB_CREDENTIALS_ENDPOINT + "/11111111-1111-1111-a111-111111111111";
    JsonapiError error = putWithStatus(resourcePath, putBody, SC_NOT_FOUND, STUB_TOKEN_HEADER).as(JsonapiError.class);

    assertErrorContainsTitle(error, "KbCredentials not found by id");
  }

  @Test
  public void shouldReturn204OnDelete() {
    String credentialsId = saveKbCredentials(STUB_API_URL, STUB_CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);

    String resourcePath = KB_CREDENTIALS_ENDPOINT + "/" + credentialsId;
    deleteWithNoContent(resourcePath);

    List<KbCredentials> kbCredentialsInDb = getKbCredentials(vertx);
    assertTrue(kbCredentialsInDb.isEmpty());
  }

  @Test
  public void shouldReturn400OnDeleteWhenHasRelatedRecords() {
    String credentialsId = saveKbCredentials(STUB_API_URL, STUB_CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);
    insertAssignedUser(credentialsId, "username", "John", null, "Doe", "patron", vertx);

    String resourcePath = KB_CREDENTIALS_ENDPOINT + "/" + credentialsId;
    JsonapiError error = deleteWithStatus(resourcePath, SC_BAD_REQUEST).as(JsonapiError.class);

    assertErrorContainsTitle(error, "Credentials have related records and can't be deleted");

    List<KbCredentials> kbCredentialsInDb = getKbCredentials(vertx);
    assertFalse(kbCredentialsInDb.isEmpty());
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

    assertErrorContainsTitle(error, "'id' parameter is incorrect.");
  }

  @Test
  public void shouldReturn200AndKbCredentialsOnGetByUser() {
    String credentialsId = saveKbCredentials(STUB_API_URL, STUB_CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);
    insertAssignedUser(STUB_USER_ID, credentialsId, "username", "John", null, "Doe", "patron",
      vertx);

    KbCredentials actual = getWithStatus(USER_KB_CREDENTIAL_ENDPOINT, SC_OK, STUB_TOKEN_HEADER).as(KbCredentials.class);
    assertEquals(getKbCredentialsNonSecured(vertx).get(0), actual);
  }

  @Test
  public void shouldReturn200AndKbCredentialsOnGetByUserWhenSingleKbCredentialsPresent() {
    saveKbCredentials(STUB_API_URL, STUB_CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);

    KbCredentials actual = getWithStatus(USER_KB_CREDENTIAL_ENDPOINT, SC_OK, STUB_TOKEN_HEADER).as(KbCredentials.class);
    assertEquals(getKbCredentialsNonSecured(vertx).get(0), actual);
  }

  @Test
  public void shouldReturn401OnGetByUserWhenTokenIsInvalid() {
    JsonapiError error = getWithStatus(USER_KB_CREDENTIAL_ENDPOINT, SC_UNAUTHORIZED, STUB_INVALID_TOKEN_HEADER)
      .as(JsonapiError.class);

    assertErrorContainsTitle(error, "Invalid token");
  }

  @Test
  public void shouldReturn404OnGetByUserWhenAssignedUserIsMissingAndNoKBCredentialsAtAll() {
    JsonapiError error = getWithStatus(USER_KB_CREDENTIAL_ENDPOINT, SC_NOT_FOUND, STUB_TOKEN_HEADER).as(JsonapiError.class);

    assertErrorContainsTitle(error, "User credentials not found");
  }

  @Test
  public void shouldReturn404OnGetByUserWhenUserIsNotTheOneWhoIsAssigned() {
    String credentialsId = saveKbCredentials(STUB_API_URL, STUB_CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);
    insertAssignedUser(STUB_USER_ID, credentialsId, "username", "John", null, "Doe", "patron", vertx);

    JsonapiError error = getWithStatus(USER_KB_CREDENTIAL_ENDPOINT, SC_NOT_FOUND, STUB_TOKEN_OTHER_HEADER).as(
      JsonapiError.class);

    assertErrorContainsTitle(error, "User credentials not found");
  }

  @Test
  public void shouldReturnStatusNotStartedOnKbCredentialsCreation() {
    KbCredentialsPostRequest kbCredentialsPostRequest = new KbCredentialsPostRequest()
      .withData(stubbedCredentials());
    String postBody = Json.encode(kbCredentialsPostRequest);

    mockVerifyValidCredentialsRequest();
    KbCredentials actual = postWithStatus(KB_CREDENTIALS_ENDPOINT, postBody, SC_CREATED, STUB_TOKEN_HEADER)
      .as(KbCredentials.class);

    final HoldingsLoadingStatus status = getStatus(actual.getId(), vertx);
    assertEquals(LoadStatusNameEnum.NOT_STARTED, status.getData().getAttributes().getStatus().getName());

    final RetryStatus retryStatus = getRetryStatus(actual.getId(), vertx);
    assertNotNull(retryStatus);
  }

  @Test
  public void shouldReturnSecuredCollection() {
    saveKbCredentials(STUB_API_URL, STUB_CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);
    Map<String, String> headers = new HashMap<>();
    headers.put(OKAPI_TENANT_HEADER, STUB_TENANT);
    KbCredentialsCollection collection = securedCredentialsService.findAll(headers).join();
    assertThat(collection.getMeta().getTotalResults(), equalTo(1));
    assertThat(collection.getData().get(0).getType(), equalTo(KbCredentials.Type.KB_CREDENTIALS));
    assertThat(collection.getData().get(0).getAttributes().getName(), equalTo(STUB_CREDENTIALS_NAME));
    assertThat(collection.getData().get(0).getAttributes().getApiKey(), containsString("*"));
    assertThat(collection.getData().get(0).getAttributes().getCustomerId(), equalTo(STUB_CUSTOMER_ID));
    assertThat(collection.getData().get(0).getAttributes().getUrl(), equalTo(STUB_API_URL));
  }

  @Test
  public void shouldReturnNonSecuredCollection() {
    saveKbCredentials(STUB_API_URL, STUB_CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);
    Map<String, String> headers = new HashMap<>();
    headers.put(OKAPI_TENANT_HEADER, STUB_TENANT);
    KbCredentialsCollection collection = nonSecuredCredentialsService.findAll(headers).join();
    assertThat(collection.getMeta().getTotalResults(), equalTo(1));
    assertThat(collection.getData().get(0).getType(), equalTo(KbCredentials.Type.KB_CREDENTIALS));
    assertThat(collection.getData().get(0).getAttributes().getName(), equalTo(STUB_CREDENTIALS_NAME));
    assertThat(collection.getData().get(0).getAttributes().getApiKey(), equalTo(STUB_API_KEY));
    assertThat(collection.getData().get(0).getAttributes().getCustomerId(), equalTo(STUB_CUSTOMER_ID));
    assertThat(collection.getData().get(0).getAttributes().getUrl(), equalTo(STUB_API_URL));
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

  private void mockVerifyValidCredentialsRequest() {
    stubFor(
      get(urlPathMatching("/rm/rmaccounts/.*"))
        .willReturn(aResponse().withStatus(SC_OK).withBody("{\"totalResults\": 0, \"vendors\": []}")));
  }

  private void mockVerifyFailedCredentialsRequest() {
    stubFor(
      get(urlPathMatching("/rm/rmaccounts/.*"))
        .willReturn(aResponse().withStatus(SC_UNPROCESSABLE_ENTITY)));
  }

}
