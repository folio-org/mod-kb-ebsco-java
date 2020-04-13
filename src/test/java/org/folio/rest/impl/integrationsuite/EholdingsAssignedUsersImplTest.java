package org.folio.rest.impl.integrationsuite;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;

import static org.folio.repository.assigneduser.AssignedUsersConstants.ASSIGNED_USERS_TABLE_NAME;
import static org.folio.repository.kbcredentials.KbCredentialsTableConstants.KB_CREDENTIALS_TABLE_NAME;
import static org.folio.util.AssignedUsersTestUtil.insertAssignedUsers;
import static org.folio.util.KBTestUtil.clearDataFromTable;
import static org.folio.util.KbCredentialsTestUtil.KB_CREDENTIALS_ENDPOINT;
import static org.folio.util.KbCredentialsTestUtil.STUB_API_URL;
import static org.folio.util.KbCredentialsTestUtil.STUB_CREDENTIALS_NAME;
import static org.folio.util.KbCredentialsTestUtil.getKbCredentials;
import static org.folio.util.KbCredentialsTestUtil.insertKbCredentials;

import java.util.UUID;

import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.folio.rest.impl.WireMockTestBase;
import org.folio.rest.jaxrs.model.AssignedUserCollection;
import org.folio.rest.jaxrs.model.JsonapiError;

@RunWith(VertxUnitRunner.class)
public class EholdingsAssignedUsersImplTest extends WireMockTestBase {

  @Test
  public void shouldReturn200WithCollection() {
    try {
      insertKbCredentials(STUB_API_URL, STUB_CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);
      String credentialsId = getKbCredentials(vertx).get(0).getId();
      insertAssignedUsers(credentialsId, "john_doe", "John", null, "Doe", "patron", vertx);
      insertAssignedUsers(credentialsId, "jane_doe", "Jane", null, "Doe", "patron", vertx);
      String assignedUsersPath = KB_CREDENTIALS_ENDPOINT + "/" + credentialsId + "/users";

      final AssignedUserCollection assignedUsers = getWithOk(assignedUsersPath).as(AssignedUserCollection.class);
      assertEquals(2,  (int)assignedUsers.getMeta().getTotalResults());
      assertEquals(2,  assignedUsers.getData().size());
    } finally {
      clearDataFromTable(vertx, ASSIGNED_USERS_TABLE_NAME);
      clearDataFromTable(vertx, KB_CREDENTIALS_TABLE_NAME);
    }
  }

  @Test
  public void shouldReturn200WithEmptyCollection() {
    String assignedUsersPath = KB_CREDENTIALS_ENDPOINT + "/" + UUID.randomUUID().toString() + "/users";

    final AssignedUserCollection assignedUsers = getWithOk(assignedUsersPath).as(AssignedUserCollection.class);
    assertEquals(0,  (int)assignedUsers.getMeta().getTotalResults());
    assertEquals(0,  assignedUsers.getData().size());
  }

  @Test
  public void shouldReturn400WhenInvalidCredentialsId(){
    String assignedUsersPath = KB_CREDENTIALS_ENDPOINT + "/invalid-id" + "/users";
    final JsonapiError error = getWithStatus(assignedUsersPath, SC_BAD_REQUEST).as(JsonapiError.class);
    assertThat(error.getErrors().get(0).getTitle(), containsString("invalid input syntax for type uuid: \"invalid-id\""));
  }
}
