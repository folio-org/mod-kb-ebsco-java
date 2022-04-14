package org.folio.rest.impl.integrationsuite;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_UNPROCESSABLE_ENTITY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;

import static org.folio.repository.assigneduser.AssignedUsersConstants.ASSIGNED_USERS_TABLE_NAME;
import static org.folio.repository.kbcredentials.KbCredentialsTableConstants.KB_CREDENTIALS_TABLE_NAME;
import static org.folio.util.AssertTestUtil.assertErrorContainsTitle;
import static org.folio.util.AssignedUsersTestUtil.getAssignedUsers;
import static org.folio.util.AssignedUsersTestUtil.saveAssignedUser;
import static org.folio.util.KBTestUtil.clearDataFromTable;
import static org.folio.util.KBTestUtil.randomId;
import static org.folio.util.KbCredentialsTestUtil.KB_CREDENTIALS_ENDPOINT;
import static org.folio.util.KbCredentialsTestUtil.STUB_API_URL;
import static org.folio.util.KbCredentialsTestUtil.STUB_CREDENTIALS_NAME;
import static org.folio.util.KbCredentialsTestUtil.saveKbCredentials;

import java.util.List;

import io.vertx.core.json.Json;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.folio.rest.impl.WireMockTestBase;
import org.folio.rest.jaxrs.model.AssignedUser;
import org.folio.rest.jaxrs.model.AssignedUserCollection;
import org.folio.rest.jaxrs.model.AssignedUserId;
import org.folio.rest.jaxrs.model.AssignedUserPostRequest;
import org.folio.rest.jaxrs.model.Errors;
import org.folio.rest.jaxrs.model.JsonapiError;

@RunWith(VertxUnitRunner.class)
public class EholdingsAssignedUsersImplTest extends WireMockTestBase {

  private static final String ASSIGN_USER_PATH = KB_CREDENTIALS_ENDPOINT + "/%s/users";
  private static final String KB_CREDENTIALS_ASSIGNED_USER_PATH = KB_CREDENTIALS_ENDPOINT + "/%s/users/%s";

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    setUpTestUsers();
  }

  @After
  public void tearDown() {
    clearDataFromTable(vertx, ASSIGNED_USERS_TABLE_NAME);
    clearDataFromTable(vertx, KB_CREDENTIALS_TABLE_NAME);
    tearDownTestUsers();
  }

  @Test
  public void shouldReturn200WithCollection() {
    String credentialsId = saveKbCredentials(STUB_API_URL, STUB_CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);
    saveAssignedUser(JOHN_ID, credentialsId, vertx);
    saveAssignedUser(JANE_ID, credentialsId, vertx);

    final AssignedUserCollection assignedUsers =
      getWithOk(String.format(ASSIGN_USER_PATH, credentialsId)).as(AssignedUserCollection.class);
    assertEquals(2, (int) assignedUsers.getMeta().getTotalResults());
    assertEquals(2, assignedUsers.getData().size());
  }

  @Test
  public void shouldReturn200WithEmptyCollection() {
    String assignedUsersPath = String.format(ASSIGN_USER_PATH, randomId());

    final AssignedUserCollection assignedUsers = getWithOk(assignedUsersPath).as(AssignedUserCollection.class);
    assertEquals(0, (int) assignedUsers.getMeta().getTotalResults());
    assertEquals(0, assignedUsers.getData().size());
  }

  @Test
  public void shouldReturn400WhenInvalidCredentialsId() {
    String assignedUsersPath = String.format(ASSIGN_USER_PATH, "invalid-id");
    final JsonapiError error = getWithStatus(assignedUsersPath, SC_BAD_REQUEST).as(JsonapiError.class);
    assertErrorContainsTitle(error, "'credentialsId' parameter is incorrect");
  }

  @Test
  public void shouldReturn201OnPostWhenAssignedUserIsValid() {
    String credentialsId = saveKbCredentials(STUB_API_URL, STUB_CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);

    AssignedUserId expected = stubAssignedUser(JOHN_ID, credentialsId);

    AssignedUserPostRequest assignedUserPostRequest = new AssignedUserPostRequest().withData(expected);
    String postBody = Json.encode(assignedUserPostRequest);
    String endpoint = String.format(ASSIGN_USER_PATH, credentialsId);
    AssignedUserId actual = postWithCreated(endpoint, postBody).as(AssignedUserId.class);

    assertEquals(expected, actual);

    List<AssignedUser> assignedUsersInDb = getAssignedUsers(vertx);
    assertThat(assignedUsersInDb, hasSize(1));
    assertEquals(expected.getId(), assignedUsersInDb.get(0).getId());
    assertEquals(expected.getCredentialsId(), assignedUsersInDb.get(0).getAttributes().getCredentialsId());
  }

  @Test
  public void shouldReturn400OnPostWhenAssignedUserIsAlreadyAssigned() {
    String credentialsId = saveKbCredentials(STUB_API_URL, STUB_CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);
    saveAssignedUser(JOHN_ID, credentialsId, vertx);

    AssignedUserPostRequest assignedUserPostRequest = new AssignedUserPostRequest()
      .withData(stubAssignedUser(JOHN_ID, credentialsId));

    String postBody = Json.encode(assignedUserPostRequest);
    String endpoint = String.format(ASSIGN_USER_PATH, credentialsId);
    JsonapiError error = postWithStatus(endpoint, postBody, SC_BAD_REQUEST).as(JsonapiError.class);

    assertErrorContainsTitle(error, "The user is already assigned");
  }

  @Test
  public void shouldReturn404OnPostWhenAssignedUserToMissingCredentials() {
    String credentialsId = randomId();
    AssignedUserId expected = stubAssignedUser(JOHN_ID, credentialsId);

    AssignedUserPostRequest assignedUserPostRequest = new AssignedUserPostRequest().withData(expected);
    String postBody = Json.encode(assignedUserPostRequest);
    String endpoint = String.format(ASSIGN_USER_PATH, credentialsId);
    JsonapiError error = postWithStatus(endpoint, postBody, SC_NOT_FOUND).as(JsonapiError.class);

    assertErrorContainsTitle(error, "not found");
  }

  @Test
  public void shouldReturn422OnPostWhenAssignedUserDoesNotHaveRequiredParameters() {
    String credentialsId = randomId();
    AssignedUserId expected = new AssignedUserId();

    AssignedUserPostRequest assignedUserPostRequest = new AssignedUserPostRequest().withData(expected);
    String postBody = Json.encode(assignedUserPostRequest);
    String endpoint = String.format(ASSIGN_USER_PATH, credentialsId);
    Errors errors = postWithStatus(endpoint, postBody, SC_UNPROCESSABLE_ENTITY).as(Errors.class);

    assertThat(errors.getErrors(), hasSize(1));
    assertThat(errors.getErrors(), everyItem(hasProperty("message", equalTo("must not be null"))));
  }

  @Test
  public void shouldReturn204OnDeleteUserAssignment() {
    String credentialsId = saveKbCredentials(STUB_API_URL, STUB_CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);
    String userId1 = saveAssignedUser(JOHN_ID, credentialsId, vertx);
    String userId2 = saveAssignedUser(JANE_ID, credentialsId, vertx);

    deleteWithNoContent(String.format(KB_CREDENTIALS_ASSIGNED_USER_PATH, credentialsId, userId1));

    final AssignedUserCollection assignedUsers = getWithOk(String.format(ASSIGN_USER_PATH, credentialsId))
      .as(AssignedUserCollection.class);
    assertEquals(1, (int) assignedUsers.getMeta().getTotalResults());
    assertEquals(1, assignedUsers.getData().size());
    assertEquals(userId2, assignedUsers.getData().get(0).getId());
    assertEquals(JANE_USERNAME, assignedUsers.getData().get(0).getAttributes().getUserName());
  }

  @Test
  public void shouldReturn400OnDeleteWhenInvalidUserId() {
    String credentialsId = saveKbCredentials(STUB_API_URL, STUB_CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);
    final JsonapiError error =
      deleteWithStatus(String.format(KB_CREDENTIALS_ASSIGNED_USER_PATH, credentialsId, "invalid-id"), SC_BAD_REQUEST)
        .as(JsonapiError.class);

    assertErrorContainsTitle(error, "'userId' parameter is incorrect.");
  }

  @Test
  public void shouldReturn404OnDeleteWhenUserNotFound() {
    String credentialsId = saveKbCredentials(STUB_API_URL, STUB_CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);
    final JsonapiError error =
      deleteWithStatus(String.format(KB_CREDENTIALS_ASSIGNED_USER_PATH, credentialsId, randomId()),
        SC_NOT_FOUND).as(JsonapiError.class);

    assertErrorContainsTitle(error, "Assigned User not found by id");
  }

  @Test
  public void shouldReturn204OnPutWhenAssignedUserIsValid() {
    String credentialsId = saveKbCredentials(STUB_API_URL, STUB_CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);
    String userId = saveAssignedUser(JANE_ID, credentialsId, vertx);

    AssignedUserId expected = stubAssignedUser(userId, credentialsId);

    String putBody = Json.encode(new AssignedUserPostRequest().withData(expected));
    putWithNoContent(String.format(KB_CREDENTIALS_ASSIGNED_USER_PATH, credentialsId, userId), putBody);

    List<AssignedUser> assignedUsersInDb = getAssignedUsers(vertx);
    assertThat(assignedUsersInDb, hasSize(1));
    assertEquals(expected.getId(), assignedUsersInDb.get(0).getId());
    assertEquals(expected.getCredentialsId(), assignedUsersInDb.get(0).getAttributes().getCredentialsId());
  }

  @Test
  public void shouldReturn404OnPutWhenAssignedUserNotFound() {
    String credentialsId = saveKbCredentials(STUB_API_URL, STUB_CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);
    String userId = randomId();

    AssignedUserId expected = stubAssignedUser(userId, credentialsId);

    String putBody = Json.encode(new AssignedUserPostRequest().withData(expected));
    JsonapiError error =
      putWithStatus(String.format(KB_CREDENTIALS_ASSIGNED_USER_PATH, credentialsId, userId), putBody, SC_NOT_FOUND)
        .as(JsonapiError.class);

    assertErrorContainsTitle(error, "not found");
    assertThat(getAssignedUsers(vertx), hasSize(0));
  }

  @Test
  public void shouldReturn404OnPutWhenAssignedUserAndCredentialsNotFound() {
    String credentialsId = randomId();
    String userId = randomId();

    AssignedUserId expected = stubAssignedUser(userId, credentialsId);

    String putBody = Json.encode(new AssignedUserPostRequest().withData(expected));
    JsonapiError error =
      putWithStatus(String.format(KB_CREDENTIALS_ASSIGNED_USER_PATH, credentialsId, userId), putBody, SC_NOT_FOUND)
        .as(JsonapiError.class);

    assertErrorContainsTitle(error, "not found");
    assertThat(getAssignedUsers(vertx), hasSize(0));
  }

  @Test
  public void shouldReturn400OnPutWhenRequestAndPathIdsNotMatch() {
    String credentialsId = randomId();
    String userId = randomId();

    AssignedUserId expected = stubAssignedUser(randomId(), randomId());

    String putBody = Json.encode(new AssignedUserPostRequest().withData(expected));
    JsonapiError error =
      putWithStatus(String.format(KB_CREDENTIALS_ASSIGNED_USER_PATH, credentialsId, userId), putBody, SC_BAD_REQUEST)
        .as(JsonapiError.class);

    assertErrorContainsTitle(error, "Credentials ID and user ID can't be updated");
    assertThat(getAssignedUsers(vertx), hasSize(0));
  }

  private AssignedUserId stubAssignedUser(String userId, String credentialsId) {
    return new AssignedUserId()
      .withId(userId)
      .withCredentialsId(credentialsId);
  }
}
