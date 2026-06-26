package org.folio.rest.impl;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_UNPROCESSABLE_ENTITY;
import static org.folio.repository.assigneduser.AssignedUsersConstants.ASSIGNED_USERS_TABLE_NAME;
import static org.folio.repository.kbcredentials.KbCredentialsTableConstants.KB_CREDENTIALS_TABLE_NAME;
import static org.folio.util.AssertTestUtil.assertErrorContainsTitle;
import static org.folio.util.AssignedUsersTestUtil.getAssignedUsers;
import static org.folio.util.AssignedUsersTestUtil.saveAssignedUser;
import static org.folio.util.KbCredentialsTestUtil.API_URL;
import static org.folio.util.KbCredentialsTestUtil.CREDENTIALS_NAME;
import static org.folio.util.KbCredentialsTestUtil.KB_CREDENTIALS_ENDPOINT;
import static org.folio.util.KbCredentialsTestUtil.saveKbCredentials;
import static org.folio.util.TestUtil.clearDataFromTable;
import static org.folio.util.TestUtil.randomId;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.vertx.core.json.Json;
import java.util.Map;
import java.util.UUID;
import lombok.SneakyThrows;
import org.folio.rest.jaxrs.model.AssignedUserCollection;
import org.folio.rest.jaxrs.model.AssignedUserId;
import org.folio.rest.jaxrs.model.AssignedUserPostRequest;
import org.folio.rest.jaxrs.model.Errors;
import org.folio.rest.jaxrs.model.JsonapiError;
import org.folio.util.IntegrationTestBase;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class EholdingsAssignedUsersImplIntegrationTest extends IntegrationTestBase {

  private static final String ASSIGN_USER_PATH = KB_CREDENTIALS_ENDPOINT + "/%s/users";
  private static final String KB_CREDENTIALS_ASSIGNED_USER_PATH = KB_CREDENTIALS_ENDPOINT + "/%s/users/%s";

  @AfterEach
  void tearDown() {
    clearDataFromTable(vertx, ASSIGNED_USERS_TABLE_NAME);
    clearDataFromTable(vertx, KB_CREDENTIALS_TABLE_NAME);
  }

  @Test
  void shouldReturn200WithCollection() {
    var credentialsId = saveKbCredentials(API_URL, CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);
    saveAssignedUser(JOHN_ID, credentialsId, vertx);
    saveAssignedUser(JANE_ID, credentialsId, vertx);

    var assignedUsers = getWithOk(assignedUserPath(credentialsId)).as(AssignedUserCollection.class);
    assertEquals(2, (int) assignedUsers.getMeta().getTotalResults());
    assertEquals(2, assignedUsers.getData().size());
  }

  @Test
  void shouldReturn200WithEmptyCollection() {
    var assignedUsersPath = assignedUserPath(randomId());

    var assignedUsers = getWithOk(assignedUsersPath).as(AssignedUserCollection.class);
    assertEquals(0, (int) assignedUsers.getMeta().getTotalResults());
    assertEquals(0, assignedUsers.getData().size());
  }

  @Test
  void shouldReturn400WhenInvalidCredentialsId() {
    var assignedUsersPath = assignedUserPath("invalid-id");
    var error = getWithStatus(assignedUsersPath, SC_BAD_REQUEST).as(JsonapiError.class);
    assertErrorContainsTitle(error, "parameter value {invalid-id} is not valid: must match");
  }

  @Test
  @SneakyThrows
  void shouldReturn201OnPostWhenAssignedUserIsValid() {
    var credentialsId = saveKbCredentials(API_URL, CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);

    var expected = stubAssignedUserId(credentialsId);
    var assignedUserPostRequest = new AssignedUserPostRequest().withData(expected);

    mockUserById(expected.getId());

    var postBody = Json.encode(assignedUserPostRequest);
    var actual = postWithCreated(assignedUserPath(credentialsId), postBody).as(AssignedUserId.class);

    assertEquals(expected, actual);

    var assignedUsersInDb = getAssignedUsers(vertx);
    assertEquals(1, assignedUsersInDb.size());
    assertEquals(expected.getId(), assignedUsersInDb.getFirst().getId());
    assertEquals(expected.getCredentialsId(), assignedUsersInDb.getFirst().getAttributes().getCredentialsId());
  }

  @Test
  void shouldReturn400OnPostWhenAssignedUserIsAlreadyAssigned() {
    var credentialsId = saveKbCredentials(API_URL, CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);
    saveAssignedUser(JOHN_ID, credentialsId, vertx);
    mockUserById(JOHN_ID);

    var assignedUserPostRequest = new AssignedUserPostRequest().withData(stubAssignedUserId(credentialsId));
    var postBody = Json.encode(assignedUserPostRequest);
    var error = postWithStatus(assignedUserPath(credentialsId), postBody, SC_BAD_REQUEST).as(JsonapiError.class);

    assertErrorContainsTitle(error, "The user is already assigned");
  }

  @Test
  void shouldReturn404OnPostWhenAssignedUserToMissingCredentials() {
    var credentialsId = randomId();
    var expected = stubAssignedUserId(credentialsId);
    mockUserById(expected.getId());

    var assignedUserPostRequest = new AssignedUserPostRequest().withData(expected);
    var postBody = Json.encode(assignedUserPostRequest);
    var error = postWithStatus(assignedUserPath(credentialsId), postBody, SC_NOT_FOUND).as(JsonapiError.class);

    assertErrorContainsTitle(error, "not found");
  }

  @Test
  void shouldReturn422OnPostWhenAssignedUserDoesNotHaveRequiredParameters() {
    var credentialsId = randomId();
    var expected = new AssignedUserId();

    var assignedUserPostRequest = new AssignedUserPostRequest().withData(expected);
    var postBody = Json.encode(assignedUserPostRequest);
    var errors = postWithStatus(assignedUserPath(credentialsId), postBody, SC_UNPROCESSABLE_ENTITY).as(Errors.class);

    assertEquals(1, errors.getErrors().size());
    assertThat(errors.getErrors(), everyItem(hasProperty("message", equalTo("must not be null"))));
  }

  @Test
  void shouldReturn422OnPostWhenUserDoesNotExist() {
    var credentialsId = randomId();
    var expected = new AssignedUserId()
      .withId(UUID.randomUUID().toString())
      .withCredentialsId(credentialsId);

    mockUserNotFound(expected.getId());

    var assignedUserPostRequest = new AssignedUserPostRequest().withData(expected);
    var postBody = Json.encode(assignedUserPostRequest);
    var errors = postWithStatus(assignedUserPath(credentialsId), postBody, SC_UNPROCESSABLE_ENTITY).as(Errors.class);

    assertEquals(1, errors.getErrors().size());
    assertThat(errors.getErrors(), everyItem(hasProperty("additionalProperties",
      equalTo(Map.of("title", "Unable to assign user", "detail", "User doesn't exist")))));
  }

  @Test
  void shouldReturn204OnDeleteUserAssignment() {
    var credentialsId = saveKbCredentials(API_URL, CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);
    saveAssignedUser(JOHN_ID, credentialsId, vertx);
    saveAssignedUser(JANE_ID, credentialsId, vertx);

    deleteWithNoContent(assignedUserByIdPath(credentialsId, JOHN_ID));

    var assignedUsers = getWithOk(assignedUserPath(credentialsId)).as(AssignedUserCollection.class);
    assertEquals(2, (int) assignedUsers.getMeta().getTotalResults());
    assertEquals(2, assignedUsers.getData().size());
  }

  @Test
  void shouldReturn400OnDeleteWhenInvalidUserId() {
    var credentialsId = saveKbCredentials(API_URL, CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);
    var error = deleteWithStatus(assignedUserByIdPath(credentialsId, "invalid-id"), SC_BAD_REQUEST)
      .as(JsonapiError.class);

    assertErrorContainsTitle(error, "parameter value {invalid-id} is not valid: must match");
  }

  @Test
  void shouldReturn404OnDeleteWhenUserNotFound() {
    var credentialsId = saveKbCredentials(API_URL, CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);
    var error = deleteWithStatus(assignedUserByIdPath(credentialsId, randomId()), SC_NOT_FOUND).as(JsonapiError.class);

    assertErrorContainsTitle(error, "Assigned User not found by id");
  }

  private @NonNull String assignedUserByIdPath(String credentialsId, String userId) {
    return String.format(KB_CREDENTIALS_ASSIGNED_USER_PATH, credentialsId, userId);
  }

  private @NonNull String assignedUserPath(String credentialsId) {
    return String.format(ASSIGN_USER_PATH, credentialsId);
  }

  private AssignedUserId stubAssignedUserId(String credentialsId) {
    return new AssignedUserId()
      .withId(JOHN_ID)
      .withCredentialsId(credentialsId);
  }
}
