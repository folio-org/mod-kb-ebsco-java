package org.folio.rest.impl.integrationsuite;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
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
import static org.folio.test.util.TestUtil.STUB_TENANT;
import static org.folio.util.AssertTestUtil.assertErrorContainsTitle;
import static org.folio.util.AssignedUsersTestUtil.getAssignedUsers;
import static org.folio.util.AssignedUsersTestUtil.saveAssignedUser;
import static org.folio.util.KBTestUtil.clearDataFromTable;
import static org.folio.util.KBTestUtil.randomId;
import static org.folio.util.KbCredentialsTestUtil.KB_CREDENTIALS_ENDPOINT;
import static org.folio.util.KbCredentialsTestUtil.STUB_API_URL;
import static org.folio.util.KbCredentialsTestUtil.STUB_CREDENTIALS_NAME;
import static org.folio.util.KbCredentialsTestUtil.saveKbCredentials;
import static org.folio.util.StringUtil.urlEncode;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.RegexPattern;
import com.github.tomakehurst.wiremock.matching.UrlPathPattern;
import io.vertx.core.json.Json;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.folio.okapi.common.XOkapiHeaders;
import org.folio.rest.impl.WireMockTestBase;
import org.folio.rest.jaxrs.model.AssignedUser;
import org.folio.rest.jaxrs.model.AssignedUserCollection;
import org.folio.rest.jaxrs.model.AssignedUserId;
import org.folio.rest.jaxrs.model.AssignedUserPostRequest;
import org.folio.rest.jaxrs.model.Errors;
import org.folio.rest.jaxrs.model.JsonapiError;
import org.folio.test.util.TestUtil;
import org.folio.util.StringUtil;

@RunWith(VertxUnitRunner.class)
public class EholdingsAssignedUsersImplTest extends WireMockTestBase {

  private static final String ASSIGN_USER_PATH = KB_CREDENTIALS_ENDPOINT + "/%s/users";
  private static final String KB_CREDENTIALS_ASSIGNED_USER_PATH = KB_CREDENTIALS_ENDPOINT + "/%s/users/%s";

  private static final Map<String, String> OKAPI_HEADERS = new HashMap<>();
  private static final String USERDATA_COLLECTION_INFO_STUB_FILE = "responses/userlookup/mock_user_collection_response_200.json";
  private static final String GROUP_INFO_STUB_FILE = "responses/userlookup/mock_group_collection_response_200.json";

  private final String QUERY_PARAM = "query";
  private final String GET_USERS_ENDPOINT = "/users";


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
  public void shouldReturn200WithCollection() throws IOException, URISyntaxException {
    String credentialsId = saveKbCredentials(STUB_API_URL, STUB_CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);
    saveAssignedUser(JOHN_ID, credentialsId, vertx);
    saveAssignedUser(JANE_ID, credentialsId, vertx);

    List<UUID> ids = List.of(UUID.fromString(JOHN_ID), UUID.fromString(JANE_ID));

    wireMockUsers(ids);

    final AssignedUserCollection assignedUsers =
      getWithOk(String.format(ASSIGN_USER_PATH, credentialsId)).as(AssignedUserCollection.class);
    assertEquals(2,(int) assignedUsers.getMeta().getTotalResults());
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

    AssignedUserId expected = stubAssignedUserId(JOHN_ID, credentialsId);

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
      .withData(stubAssignedUserId(JOHN_ID, credentialsId));

    String postBody = Json.encode(assignedUserPostRequest);
    String endpoint = String.format(ASSIGN_USER_PATH, credentialsId);
    JsonapiError error = postWithStatus(endpoint, postBody, SC_BAD_REQUEST).as(JsonapiError.class);

    assertErrorContainsTitle(error, "The user is already assigned");
  }

  @Test
  public void shouldReturn404OnPostWhenAssignedUserToMissingCredentials() {
    String credentialsId = randomId();
    AssignedUserId expected = stubAssignedUserId(JOHN_ID, credentialsId);

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
  public void shouldReturn204OnDeleteUserAssignment() throws IOException, URISyntaxException {
    String credentialsId = saveKbCredentials(STUB_API_URL, STUB_CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);
    String userId1 = saveAssignedUser(JOHN_ID, credentialsId, vertx);
    String userId2 = saveAssignedUser(JANE_ID, credentialsId, vertx);

    List<UUID> ids = List.of(UUID.fromString(userId2));
    wireMockUsers(ids);

    deleteWithNoContent(String.format(KB_CREDENTIALS_ASSIGNED_USER_PATH, credentialsId, userId1));

    final AssignedUserCollection assignedUsers = getWithOk(String.format(ASSIGN_USER_PATH, credentialsId))
      .as(AssignedUserCollection.class);
    assertEquals(2, (int) assignedUsers.getMeta().getTotalResults());
    assertEquals(2, assignedUsers.getData().size());
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

  private String cqlQueryConverter(List<UUID> ids) {
    return "id=(" + ids.stream().map(UUID::toString)
      .map(StringUtil::cqlEncode).collect(Collectors.joining(" OR ")) + ")";
  }

  private void wireMockUsers(List<UUID> ids) throws IOException, URISyntaxException {
    OKAPI_HEADERS.put(XOkapiHeaders.TENANT, STUB_TENANT);
    OKAPI_HEADERS.put(XOkapiHeaders.URL, getWiremockUrl());

    String query = cqlQueryConverter(ids);

    stubFor(
      get(new UrlPathPattern(new RegexPattern(GET_USERS_ENDPOINT), true))
        .withQueryParam(QUERY_PARAM, WireMock.equalTo(urlEncode(query)))
        .willReturn(new ResponseDefinitionBuilder()
          .withBody(TestUtil.readFile(USERDATA_COLLECTION_INFO_STUB_FILE))));

    UUID janeGroupId = UUID.fromString(JANE_GROUP_ID);
    UUID johnGroupId = UUID.fromString(JOHN_GROUP_ID);
    List<UUID> groupIds = List.of(johnGroupId, janeGroupId);
    String cqlQuery = cqlQueryConverter(groupIds);

    stubFor(
      get(new UrlPathPattern(new RegexPattern("/groups"), true))
        .withQueryParam(QUERY_PARAM, WireMock.equalTo(urlEncode(cqlQuery)))
        .willReturn(new ResponseDefinitionBuilder()
          .withBody(TestUtil.readFile(GROUP_INFO_STUB_FILE))));
  }

  private AssignedUserId stubAssignedUserId(String userId, String credentialsId) {
    return new AssignedUserId()
      .withId(userId)
      .withCredentialsId(credentialsId);
  }
}
