package org.folio.rest.impl.integrationsuite;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;

import static org.folio.repository.assigneduser.AssignedUsersConstants.ASSIGNED_USERS_TABLE_NAME;
import static org.folio.repository.kbcredentials.KbCredentialsTableConstants.KB_CREDENTIALS_TABLE_NAME;
import static org.folio.util.AssignedUsersTestUtil.KB_CREDENTIALS_ASSIGNED_USER_ENDPOINT;
import static org.folio.util.AssignedUsersTestUtil.getAssignedUsers;
import static org.folio.util.AssignedUsersTestUtil.insertAssignedUser;
import static org.folio.util.KBTestUtil.clearDataFromTable;
import static org.folio.util.KbCredentialsTestUtil.STUB_API_URL;
import static org.folio.util.KbCredentialsTestUtil.STUB_CREDENTIALS_NAME;
import static org.folio.util.KbCredentialsTestUtil.getKbCredentials;
import static org.folio.util.KbCredentialsTestUtil.insertKbCredentials;

import java.util.UUID;

import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.folio.rest.impl.WireMockTestBase;
import org.folio.rest.jaxrs.model.AssignedUser;
import org.folio.rest.jaxrs.model.AssignedUserCollection;
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
    insertKbCredentials(STUB_API_URL, STUB_CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);
    String credentialsId = getKbCredentials(vertx).get(0).getId();
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
  public void shouldReturn200AndAssignedUserOnGet() {
    insertKbCredentials(STUB_API_URL, STUB_CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);
    String credentialsId = getKbCredentials(vertx).get(0).getId();
    insertAssignedUser(credentialsId, "john_doe", "John", null, "Doe", "patron", vertx);

    AssignedUser expected = getAssignedUsers(vertx).get(0);

    String resourcePath = String.format(KB_CREDENTIALS_ASSIGNED_USER_ENDPOINT, credentialsId) + "/" + expected.getId();
    AssignedUser actual = getWithOk(resourcePath).as(AssignedUser.class);

    assertEquals(expected, actual);
  }

  @Test
  public void shouldReturn400OnGetWhenIdIsInvalid() {
    insertKbCredentials(STUB_API_URL, STUB_CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);
    String credentialsId = getKbCredentials(vertx).get(0).getId();

    String resourcePath = String.format(KB_CREDENTIALS_ASSIGNED_USER_ENDPOINT, credentialsId) + "/invalid-id";
    JsonapiError error = getWithStatus(resourcePath, SC_BAD_REQUEST).as(JsonapiError.class);

    assertThat(error.getErrors().get(0).getTitle(), containsString("'userId' parameter is incorrect"));
  }

  @Test
  public void shouldReturn404OnGetWhenAssignedUserIsMissing() {
    insertKbCredentials(STUB_API_URL, STUB_CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);
    String credentialsId = getKbCredentials(vertx).get(0).getId();

    String resourcePath = String.format(KB_CREDENTIALS_ASSIGNED_USER_ENDPOINT, credentialsId)
      + "/11111111-1111-1111-a111-111111111111";
    JsonapiError error = getWithStatus(resourcePath, SC_NOT_FOUND).as(JsonapiError.class);

    assertThat(error.getErrors().get(0).getTitle(), containsString("AssignedUser not found by id"));
  }

  @Test
  public void shouldReturn404OnGetWhenCredentialsIsMissingAndUserNot() {
    insertKbCredentials(STUB_API_URL, STUB_CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);
    String credentialsId = getKbCredentials(vertx).get(0).getId();
    insertAssignedUser(credentialsId, "john_doe", "John", null, "Doe", "patron", vertx);
    String userId = getAssignedUsers(vertx).get(0).getId();

    String missingCredentialsId = "11111111-1111-1111-a111-111111111111";
    String resourcePath = String.format(KB_CREDENTIALS_ASSIGNED_USER_ENDPOINT, missingCredentialsId) + "/" + userId;
    JsonapiError error = getWithStatus(resourcePath, SC_NOT_FOUND).as(JsonapiError.class);

    assertThat(error.getErrors().get(0).getTitle(), containsString("AssignedUser not found by id"));
  }
}
