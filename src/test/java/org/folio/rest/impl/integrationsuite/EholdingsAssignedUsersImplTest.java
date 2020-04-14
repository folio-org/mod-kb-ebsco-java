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
import static org.folio.util.AssignedUsersTestUtil.KB_CREDENTIALS_ASSIGNED_USER_ENDPOINT;
import static org.folio.util.AssignedUsersTestUtil.getAssignedUsers;
import static org.folio.util.AssignedUsersTestUtil.insertAssignedUser;
import static org.folio.util.KBTestUtil.clearDataFromTable;
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
    String assignedUsersPath = String.format(KB_CREDENTIALS_ASSIGNED_USER_ENDPOINT, credentialsId);

    final AssignedUserCollection assignedUsers = getWithOk(assignedUsersPath).as(AssignedUserCollection.class);
    assertEquals(2, (int) assignedUsers.getMeta().getTotalResults());
    assertEquals(2, assignedUsers.getData().size());
  }

  @Test
  public void shouldReturn200WithEmptyCollection() {
    String assignedUsersPath = String.format(KB_CREDENTIALS_ASSIGNED_USER_ENDPOINT, UUID.randomUUID().toString());

    final AssignedUserCollection assignedUsers = getWithOk(assignedUsersPath).as(AssignedUserCollection.class);
    assertEquals(0, (int) assignedUsers.getMeta().getTotalResults());
    assertEquals(0, assignedUsers.getData().size());
  }

  @Test
  public void shouldReturn400WhenInvalidCredentialsId() {
    String assignedUsersPath = String.format(KB_CREDENTIALS_ASSIGNED_USER_ENDPOINT, "invalid-id");
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
    String endpoint = String.format(KB_CREDENTIALS_ASSIGNED_USER_ENDPOINT, credentialsId);
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
    String endpoint = String.format(KB_CREDENTIALS_ASSIGNED_USER_ENDPOINT, credentialsId);
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
    String endpoint = String.format(KB_CREDENTIALS_ASSIGNED_USER_ENDPOINT, credentialsId);
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
    String endpoint = String.format(KB_CREDENTIALS_ASSIGNED_USER_ENDPOINT, credentialsId);
    Errors errors = postWithStatus(endpoint, postBody, SC_UNPROCESSABLE_ENTITY).as(Errors.class);

    assertThat(errors.getErrors(), hasSize(5));
    assertThat(errors.getErrors(), everyItem(hasProperty("message", equalTo("may not be null"))));
  }
}
