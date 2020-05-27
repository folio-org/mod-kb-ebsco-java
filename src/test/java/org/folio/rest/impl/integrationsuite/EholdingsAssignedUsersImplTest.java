package org.folio.rest.impl.integrationsuite;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_UNPROCESSABLE_ENTITY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;

import static org.folio.repository.assigneduser.AssignedUsersConstants.ASSIGNED_USERS_TABLE_NAME;
import static org.folio.repository.kbcredentials.KbCredentialsTableConstants.KB_CREDENTIALS_TABLE_NAME;
import static org.folio.util.AssignedUsersTestUtil.getAssignedUsers;
import static org.folio.util.AssignedUsersTestUtil.insertAssignedUser;
import static org.folio.util.KBTestUtil.clearDataFromTable;
import static org.folio.util.KbCredentialsTestUtil.KB_CREDENTIALS_ENDPOINT;
import static org.folio.util.KbCredentialsTestUtil.STUB_API_URL;
import static org.folio.util.KbCredentialsTestUtil.STUB_CREDENTIALS_NAME;
import static org.folio.util.KbCredentialsTestUtil.insertKbCredentials;

import java.util.List;
import java.util.UUID;

import io.vertx.core.json.Json;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.folio.rest.impl.WireMockTestBase;
import org.folio.rest.jaxrs.model.AssignedUser;
import org.folio.rest.jaxrs.model.AssignedUserCollection;
import org.folio.rest.jaxrs.model.AssignedUserDataAttributes;
import org.folio.rest.jaxrs.model.AssignedUserPostRequest;
import org.folio.rest.jaxrs.model.Errors;
import org.folio.rest.jaxrs.model.JsonapiError;

@RunWith(VertxUnitRunner.class)
public class EholdingsAssignedUsersImplTest extends WireMockTestBase {

  private static final String ASSIGN_USER_PATH = KB_CREDENTIALS_ENDPOINT + "/%s/users";
  private static final String KB_CREDENTIALS_ASSIGNED_USER_PATH = KB_CREDENTIALS_ENDPOINT + "/%s/users/%s";

  @After
  public void tearDown() {
    clearDataFromTable(vertx, ASSIGNED_USERS_TABLE_NAME);
    clearDataFromTable(vertx, KB_CREDENTIALS_TABLE_NAME);
  }

  @Test
  public void shouldReturn200WithCollection() {
    String credentialsId = insertKbCredentials(STUB_API_URL, STUB_CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);
    insertAssignedUser(credentialsId, "john_doe", "John", null, "Doe", "patron", vertx);
    insertAssignedUser(credentialsId, "jane_doe", "Jane", null, "Doe", "patron", vertx);

    final AssignedUserCollection assignedUsers = getWithOk(String.format(ASSIGN_USER_PATH, credentialsId)).as(AssignedUserCollection.class);
    assertEquals(2,  (int)assignedUsers.getMeta().getTotalResults());
    assertEquals(2,  assignedUsers.getData().size());
  }

  @Test
  public void shouldReturn200WithEmptyCollection() {
    String assignedUsersPath = String.format(ASSIGN_USER_PATH, UUID.randomUUID().toString());

    final AssignedUserCollection assignedUsers = getWithOk(assignedUsersPath).as(AssignedUserCollection.class);
    assertEquals(0, (int) assignedUsers.getMeta().getTotalResults());
    assertEquals(0, assignedUsers.getData().size());
  }

  @Test
  public void shouldReturn400WhenInvalidCredentialsId() {
    String assignedUsersPath = String.format(ASSIGN_USER_PATH, "invalid-id");
    final JsonapiError error = getWithStatus(assignedUsersPath, SC_BAD_REQUEST).as(JsonapiError.class);
    assertThat(error.getErrors().get(0).getTitle(), containsString("'id' parameter is incorrect"));
  }

  @Test
  public void shouldReturn201OnPostWhenAssignedUserIsValid() {
    String credentialsId = insertKbCredentials(STUB_API_URL, STUB_CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);

    AssignedUser expected = new AssignedUser()
      .withId(UUID.randomUUID().toString())
      .withType(AssignedUser.Type.ASSIGNED_USERS)
      .withAttributes(new AssignedUserDataAttributes()
        .withCredentialsId(credentialsId)
        .withFirstName("John")
        .withLastName("Doe")
        .withUserName("johndoe")
        .withPatronGroup("staff"));

    AssignedUserPostRequest assignedUserPostRequest = new AssignedUserPostRequest().withData(expected);
    String postBody = Json.encode(assignedUserPostRequest);
    String endpoint = String.format(ASSIGN_USER_PATH, credentialsId);
    AssignedUser actual = postWithCreated(endpoint, postBody).as(AssignedUser.class);

    assertEquals(expected, actual);

    List<AssignedUser> assignedUsersInDb = getAssignedUsers(vertx);
    assertThat(assignedUsersInDb, hasSize(1));
    assertEquals(expected, assignedUsersInDb.get(0));
  }

  @Test
  public void shouldReturn400OnPostWhenAssignedUserToMissingCredentials() {
    String credentialsId = insertKbCredentials(STUB_API_URL, STUB_CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);
    String userId = insertAssignedUser(credentialsId, "username", "John", null, "Doe", "patron", vertx);

    AssignedUserPostRequest assignedUserPostRequest = new AssignedUserPostRequest()
      .withData(new AssignedUser()
        .withId(userId)
        .withType(AssignedUser.Type.ASSIGNED_USERS)
        .withAttributes(new AssignedUserDataAttributes()
          .withCredentialsId(credentialsId)
          .withFirstName("John")
          .withLastName("Doe")
          .withUserName("johndoe")
          .withPatronGroup("staff")));

    String postBody = Json.encode(assignedUserPostRequest);
    String endpoint = String.format(ASSIGN_USER_PATH, credentialsId);
    JsonapiError error = postWithStatus(endpoint, postBody, SC_BAD_REQUEST).as(JsonapiError.class);

    assertThat(error.getErrors().get(0).getTitle(), containsString("The user is already assigned"));
  }

  @Test
  public void shouldReturn404OnPostWhenAssignedUserToMissingCredentials() {
    String credentialsId = UUID.randomUUID().toString();
    AssignedUser expected = new AssignedUser()
      .withId(UUID.randomUUID().toString())
      .withType(AssignedUser.Type.ASSIGNED_USERS)
      .withAttributes(new AssignedUserDataAttributes()
        .withCredentialsId(credentialsId)
        .withFirstName("John")
        .withLastName("Doe")
        .withUserName("johndoe")
        .withPatronGroup("staff"));

    AssignedUserPostRequest assignedUserPostRequest = new AssignedUserPostRequest().withData(expected);
    String postBody = Json.encode(assignedUserPostRequest);
    String endpoint = String.format(ASSIGN_USER_PATH, credentialsId);
    JsonapiError error = postWithStatus(endpoint, postBody, SC_NOT_FOUND).as(JsonapiError.class);

    assertThat(error.getErrors().get(0).getTitle(), containsString("not found"));
  }

  @Test
  public void shouldReturn422OnPostWhenAssignedUserDoesNotHaveRequiredParameters() {
    String credentialsId = UUID.randomUUID().toString();
    AssignedUser expected = new AssignedUser()
      .withType(AssignedUser.Type.ASSIGNED_USERS)
      .withAttributes(new AssignedUserDataAttributes());

    AssignedUserPostRequest assignedUserPostRequest = new AssignedUserPostRequest().withData(expected);
    String postBody = Json.encode(assignedUserPostRequest);
    String endpoint = String.format(ASSIGN_USER_PATH, credentialsId);
    Errors errors = postWithStatus(endpoint, postBody, SC_UNPROCESSABLE_ENTITY).as(Errors.class);

    assertThat(errors.getErrors(), hasSize(5));
    assertThat(errors.getErrors(), everyItem(hasProperty("message", equalTo("may not be null"))));
  }

  @Test
  public void shouldReturn204OnDeleteUserAssignment(){
    String credentialsId = insertKbCredentials(STUB_API_URL, STUB_CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);
    final String userId1 = insertAssignedUser(credentialsId, "john_doe", "John", null, "Doe", "patron", vertx);
    final String userId2 = insertAssignedUser(credentialsId, "jane_doe", "Jane", null, "Doe", "patron", vertx);
    deleteWithNoContent(String.format(KB_CREDENTIALS_ASSIGNED_USER_PATH,  credentialsId, userId1));

    final AssignedUserCollection assignedUsers = getWithOk(String.format(ASSIGN_USER_PATH, credentialsId))
      .as(AssignedUserCollection.class);
    assertEquals(1,  (int)assignedUsers.getMeta().getTotalResults());
    assertEquals(1,  assignedUsers.getData().size());
    assertEquals(userId2, assignedUsers.getData().get(0).getId());
    assertEquals("jane_doe", assignedUsers.getData().get(0).getAttributes().getUserName());
  }

  @Test
  public void shouldReturn400OnDeleteWhenInvalidUserId() {
    String credentialsId = insertKbCredentials(STUB_API_URL, STUB_CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);
    final JsonapiError error = deleteWithStatus(String.format(KB_CREDENTIALS_ASSIGNED_USER_PATH, credentialsId, "invalid-id"), SC_BAD_REQUEST).as(JsonapiError.class);
    assertThat(error.getErrors().get(0).getTitle(), containsString("'userId' parameter is incorrect."));
  }

  @Test
  public void shouldReturn404OnDeleteWhenUserNotFound() {
    String credentialsId = insertKbCredentials(STUB_API_URL, STUB_CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);
    final JsonapiError error = deleteWithStatus(String.format(KB_CREDENTIALS_ASSIGNED_USER_PATH, credentialsId, UUID.randomUUID().toString()), SC_NOT_FOUND).as(JsonapiError.class);
    assertThat(error.getErrors().get(0).getTitle(), containsString("Assigned User not found by id"));
  }

  @Test
  public void shouldReturn204OnPutWhenAssignedUserIsValid() {
    String credentialsId = insertKbCredentials(STUB_API_URL, STUB_CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);
    String userId = insertAssignedUser(credentialsId, "jane_doe", "Jane", null, "Doe", "patron", vertx);

    AssignedUser expected = new AssignedUser()
      .withId(userId)
      .withType(AssignedUser.Type.ASSIGNED_USERS)
      .withAttributes(new AssignedUserDataAttributes()
        .withCredentialsId(credentialsId)
        .withFirstName("John")
        .withLastName("Doe")
        .withUserName("johndoe")
        .withPatronGroup("staff"));

    String putBody = Json.encode(new AssignedUserPostRequest().withData(expected));
    putWithNoContent(String.format(KB_CREDENTIALS_ASSIGNED_USER_PATH, credentialsId, userId), putBody);


    List<AssignedUser> assignedUsersInDb = getAssignedUsers(vertx);
    assertThat(assignedUsersInDb, hasSize(1));
    assertEquals(expected, assignedUsersInDb.get(0));
  }

  @Test
  public void shouldReturn404OnPutWhenAssignedUserNotFound() {
    String credentialsId = insertKbCredentials(STUB_API_URL, STUB_CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);
    String userId = UUID.randomUUID().toString();

    AssignedUser expected = new AssignedUser()
      .withId(userId)
      .withType(AssignedUser.Type.ASSIGNED_USERS)
      .withAttributes(new AssignedUserDataAttributes()
        .withCredentialsId(credentialsId)
        .withFirstName("John")
        .withLastName("Doe")
        .withUserName("johndoe")
        .withPatronGroup("staff"));

    String putBody = Json.encode(new AssignedUserPostRequest().withData(expected));
    JsonapiError error = putWithStatus(String.format(KB_CREDENTIALS_ASSIGNED_USER_PATH, credentialsId, userId), putBody, SC_NOT_FOUND).as(JsonapiError.class);

    assertThat(error.getErrors().get(0).getTitle(), containsString("not found"));
    assertThat(getAssignedUsers(vertx), hasSize(0));
  }

  @Test
  public void shouldReturn404OnPutWhenAssignedUserAndCredentialsNotFound() {
    String credentialsId = UUID.randomUUID().toString();
    String userId = UUID.randomUUID().toString();

    AssignedUser expected = new AssignedUser()
      .withId(userId)
      .withType(AssignedUser.Type.ASSIGNED_USERS)
      .withAttributes(new AssignedUserDataAttributes()
        .withCredentialsId(credentialsId)
        .withFirstName("John")
        .withLastName("Doe")
        .withUserName("johndoe")
        .withPatronGroup("staff"));

    String putBody = Json.encode(new AssignedUserPostRequest().withData(expected));
    JsonapiError error = putWithStatus(String.format(KB_CREDENTIALS_ASSIGNED_USER_PATH,credentialsId, userId), putBody, SC_NOT_FOUND).as(JsonapiError.class);

    assertThat(error.getErrors().get(0).getTitle(), containsString("not found"));
    assertThat(getAssignedUsers(vertx), hasSize(0));
  }

  @Test
  public void shouldReturn400OnPutWhenRequestAndPathIdsNotMatch() {
    String credentialsId = UUID.randomUUID().toString();
    String userId = UUID.randomUUID().toString();

    AssignedUser expected = new AssignedUser()
      .withId(UUID.randomUUID().toString())
      .withType(AssignedUser.Type.ASSIGNED_USERS)
      .withAttributes(new AssignedUserDataAttributes()
        .withCredentialsId(UUID.randomUUID().toString())
        .withFirstName("John")
        .withLastName("Doe")
        .withUserName("johndoe")
        .withPatronGroup("staff"));

    String putBody = Json.encode(new AssignedUserPostRequest().withData(expected));
    JsonapiError error = putWithStatus(String.format(KB_CREDENTIALS_ASSIGNED_USER_PATH, credentialsId, userId), putBody, SC_BAD_REQUEST).as(JsonapiError.class);

    assertThat(error.getErrors().get(0).getTitle(), containsString("Credentials ID and user ID can't be updated"));
    assertThat(getAssignedUsers(vertx), hasSize(0));
  }
}
