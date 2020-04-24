package org.folio.rest.impl.integrationsuite;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CREATED;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.apache.http.HttpStatus.SC_UNPROCESSABLE_ENTITY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import static org.folio.repository.accesstypes.AccessTypeMappingsTableConstants.ACCESS_TYPES_MAPPING_TABLE_NAME;
import static org.folio.repository.accesstypes.AccessTypesTableConstants.ACCESS_TYPES_TABLE_NAME;
import static org.folio.repository.accesstypes.AccessTypesTableConstants.ACCESS_TYPES_TABLE_NAME_OLD;
import static org.folio.repository.kbcredentials.KbCredentialsTableConstants.KB_CREDENTIALS_TABLE_NAME;
import static org.folio.test.util.TestUtil.STUB_TENANT;
import static org.folio.test.util.TestUtil.STUB_TOKEN;
import static org.folio.test.util.TestUtil.readFile;
import static org.folio.util.AccessTypesTestUtil.ACCESS_TYPES_PATH;
import static org.folio.util.AccessTypesTestUtil.KB_CREDENTIALS_ACCESS_TYPES_ENDPOINT;
import static org.folio.util.AccessTypesTestUtil.STUB_ACCESS_TYPE_NAME;
import static org.folio.util.AccessTypesTestUtil.STUB_ACCESS_TYPE_NAME_3;
import static org.folio.util.AccessTypesTestUtil.getAccessTypes;
import static org.folio.util.AccessTypesTestUtil.getAccessTypesOld;
import static org.folio.util.AccessTypesTestUtil.insertAccessType;
import static org.folio.util.AccessTypesTestUtil.insertAccessTypeMapping;
import static org.folio.util.AccessTypesTestUtil.insertAccessTypes;
import static org.folio.util.AccessTypesTestUtil.testData;
import static org.folio.util.KBTestUtil.clearDataFromTable;
import static org.folio.util.KbCredentialsTestUtil.STUB_API_URL;
import static org.folio.util.KbCredentialsTestUtil.STUB_CREDENTIALS_NAME;
import static org.folio.util.KbCredentialsTestUtil.STUB_TOKEN_HEADER;
import static org.folio.util.KbCredentialsTestUtil.insertKbCredentials;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import com.github.tomakehurst.wiremock.matching.RegexPattern;
import com.github.tomakehurst.wiremock.matching.UrlPathPattern;
import io.restassured.RestAssured;
import io.restassured.http.Header;
import io.vertx.core.json.Json;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.folio.okapi.common.XOkapiHeaders;
import org.folio.repository.RecordType;
import org.folio.repository.accesstypes.AccessTypesTableConstants;
import org.folio.rest.impl.WireMockTestBase;
import org.folio.rest.jaxrs.model.AccessType;
import org.folio.rest.jaxrs.model.AccessTypeCollection;
import org.folio.rest.jaxrs.model.AccessTypeDataAttributes;
import org.folio.rest.jaxrs.model.AccessTypePostRequest;
import org.folio.rest.jaxrs.model.Errors;
import org.folio.rest.jaxrs.model.JsonapiError;
import org.folio.rest.util.RestConstants;

@RunWith(VertxUnitRunner.class)
public class EholdingsAccessTypesImplTest extends WireMockTestBase {

  private static final String USER_8 = "88888888-8888-4888-8888-888888888888";
  private static final String USER_9 = "99999999-9999-4999-9999-999999999999";
  private static final String USER_2 = "22222222-2222-4222-2222-222222222222";
  private static final String USER_3 = "33333333-3333-4333-3333-333333333333";

  private static final Header USER8_TOKEN = new Header(XOkapiHeaders.TOKEN,
    "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJjZWRyaWNrIiwidXNlcl9pZCI6Ijg4ODg4ODg4LTg4ODgtNDg4OC04ODg4LTg4ODg4ODg4ODg4OCIsImlhdCI6MTU4NTg5NTE0NCwidGVuYW50IjoiZGlrdSJ9.xxJIwZRYzYkYjCX1fe-fFaC90iW2GEQotSZNoswXbXg");
  private static final Header USER9_TOKEN = new Header(XOkapiHeaders.TOKEN,
    "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJqb2huX2RvZSIsInVzZXJfaWQiOiI5OTk5OTk5OS05OTk5LTQ5OTktOTk5OS05OTk5OTk5OTk5OTkiLCJpYXQiOjE1ODU4OTUxNDQsInRlbmFudCI6ImRpa3UifQ.lkyJer68DZ2kclmS4z79knVc24UjYeIfAJSAFEX8zNs");
  private static final Header USER2_TOKEN = new Header(XOkapiHeaders.TOKEN,
    "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJqb2huX2RvZSIsInVzZXJfaWQiOiIyMjIyMjIyMi0yMjIyLTQyMjItMjIyMi0yMjIyMjIyMjIyMjIiLCJpYXQiOjE1ODU4OTUxNDQsInRlbmFudCI6ImRpa3UifQ.s9rQ-2NAvAwDRJaGk6k-JeE1cRN9LUOvaEopenKeQGs");
  private static final Header USER3_TOKEN = new Header(XOkapiHeaders.TOKEN,
    "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJqb2huX2RvZSIsInVzZXJfaWQiOiIzMzMzMzMzMy0zMzMzLTQzMzMtMzMzMy0zMzMzMzMzMzMzMzMiLCJpYXQiOjE1ODU4OTUxNDQsInRlbmFudCI6ImRpa3UifQ.7xGCAAwXMD8PrhWulanfSdtRF4A14W8Z2XBpVt9K2e4");

  private static final RegexPattern CONFIG_ACCESS_TYPE_LIMIT_URL_PATTERN =
    new RegexPattern("/configurations/entries.*");

  private String credentialsId;

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    stubFor(
      get(new UrlPathPattern(new EqualToPattern("/users/" + USER_8), false))
        .willReturn(new ResponseDefinitionBuilder()
          .withStatus(200)
          .withBody(readFile("responses/userlookup/mock_user_response_200.json"))
        ));

    stubFor(
      get(new UrlPathPattern(new EqualToPattern("/users/" + USER_9), false))
        .willReturn(new ResponseDefinitionBuilder()
          .withStatus(200)
          .withBody(readFile("responses/userlookup/mock_user_response_2_200.json"))
        ));

    stubFor(
      get(new UrlPathPattern(new EqualToPattern("/users/" + USER_2), false))
        .willReturn(new ResponseDefinitionBuilder()
          .withStatus(404)
        ));

    stubFor(
      get(new UrlPathPattern(new EqualToPattern("/users/" + USER_3), false))
        .willReturn(new ResponseDefinitionBuilder()
          .withStatus(403)
        ));

    credentialsId = insertKbCredentials(STUB_API_URL, STUB_CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);
  }

  @After
  public void tearDown() {
    clearDataFromTable(vertx, ACCESS_TYPES_TABLE_NAME_OLD);
    clearDataFromTable(vertx, ACCESS_TYPES_MAPPING_TABLE_NAME);
    clearDataFromTable(vertx, ACCESS_TYPES_TABLE_NAME);
    clearDataFromTable(vertx, KB_CREDENTIALS_TABLE_NAME);
  }

  @Test
  public void shouldReturnAccessTypeCollectionOnGet() {
    List<AccessType> testAccessTypes = testData(credentialsId);
    String id0 = insertAccessType(testAccessTypes.get(0), vertx);
    String id1 = insertAccessType(testAccessTypes.get(1), vertx);

    AccessTypeCollection actual = getWithStatus(ACCESS_TYPES_PATH, SC_OK, STUB_TOKEN_HEADER)
      .as(AccessTypeCollection.class);

    assertEquals(Integer.valueOf(2), actual.getMeta().getTotalResults());
    assertEquals(2, actual.getData().size());
    assertThat(actual.getData().get(0), allOf(
      hasProperty("id", equalTo(id0)),
      allOf(hasProperty("attributes", notNullValue()), hasProperty("metadata", notNullValue()))
    ));
    assertThat(actual.getData().get(1), allOf(
      hasProperty("id", equalTo(id1)),
      allOf(hasProperty("attributes", notNullValue()), hasProperty("metadata", notNullValue()))
    ));
  }

  @Test
  public void shouldReturnAccessTypeCollectionOnGetByCredentialsId() {
    List<AccessType> testAccessTypes = testData(credentialsId);
    String id0 = insertAccessType(testAccessTypes.get(0), vertx);
    String id1 = insertAccessType(testAccessTypes.get(1), vertx);

    String resourcePath = String.format(KB_CREDENTIALS_ACCESS_TYPES_ENDPOINT, credentialsId);
    AccessTypeCollection actual = getWithOk(resourcePath).as(AccessTypeCollection.class);

    assertEquals(Integer.valueOf(2), actual.getMeta().getTotalResults());
    assertEquals(2, actual.getData().size());
    assertThat(actual.getData().get(0), allOf(
      hasProperty("id", equalTo(id0)),
      allOf(hasProperty("attributes", notNullValue()), hasProperty("metadata", notNullValue()))
    ));
    assertThat(actual.getData().get(1), allOf(
      hasProperty("id", equalTo(id1)),
      allOf(hasProperty("attributes", notNullValue()), hasProperty("metadata", notNullValue()))
    ));
  }

  @Test
  public void shouldReturnAccessTypeCollectionWithUsageNumberOnGetByCredentialsId() {
    List<AccessType> testAccessTypes = testData(credentialsId);
    String id0 = insertAccessType(testAccessTypes.get(0), vertx);
    String id1 = insertAccessType(testAccessTypes.get(1), vertx);
    insertAccessType(testAccessTypes.get(2), vertx);

    insertAccessTypeMapping("11111111-1111", RecordType.RESOURCE, id0, vertx);
    insertAccessTypeMapping("11111111-1112", RecordType.PACKAGE, id0, vertx);
    insertAccessTypeMapping("11111111-1113", RecordType.PACKAGE, id0, vertx);
    insertAccessTypeMapping("11111111-1114", RecordType.PACKAGE, id1, vertx);
    insertAccessTypeMapping("11111111-1115", RecordType.PACKAGE, id1, vertx);

    String resourcePath = String.format(KB_CREDENTIALS_ACCESS_TYPES_ENDPOINT, credentialsId);
    AccessTypeCollection actual = getWithOk(resourcePath).as(AccessTypeCollection.class);

    assertThat(actual.getData().get(0).getUsageNumber(), equalTo(3));
    assertThat(actual.getData().get(1).getUsageNumber(), equalTo(2));
    assertThat(actual.getData().get(2).getUsageNumber(), equalTo(0));
  }

  @Test
  public void shouldReturnAccessTypeOnGet() {
    List<AccessType> accessTypes = insertAccessTypes(testData(), vertx);
    AccessType expected = accessTypes.get(0);
    expected.setUsageNumber(3);

    insertAccessTypeMapping("11111111-1111", RecordType.RESOURCE, expected.getId(), vertx);
    insertAccessTypeMapping("11111111-1112", RecordType.PACKAGE, expected.getId(), vertx);
    insertAccessTypeMapping("11111111-1113", RecordType.PACKAGE, expected.getId(), vertx);

    AccessType actual = getWithOk(ACCESS_TYPES_PATH + "/" + expected.getId())
      .as(AccessType.class);

    assertEquals(expected, actual);
  }

  @Test
  public void shouldReturn404OnGetIfAccessTypeIsMissing() {
    String id = "11111111-1111-1111-a111-111111111111";
    JsonapiError error = getWithStatus(ACCESS_TYPES_PATH + "/" + id, SC_NOT_FOUND).as(JsonapiError.class);

    assertEquals(1, error.getErrors().size());
    assertEquals(String.format("Access type not found by id: %s", id), error.getErrors().get(0).getTitle());
  }

  @Test
  public void shouldReturn400OnGetIfIdIsInvalid() {
    String id = "99999999-9999-2-9999-999999999999";
    JsonapiError error = getWithStatus(ACCESS_TYPES_PATH + "/" + id, SC_BAD_REQUEST).as(JsonapiError.class);

    assertEquals(1, error.getErrors().size());
    assertThat(error.getErrors().get(0).getTitle(), containsString("'id' parameter is incorrect."));
  }

  @Test
  public void shouldReturn204OnDelete() {
    List<AccessType> accessTypesBeforeDelete = insertAccessTypes(testData(), vertx);
    String accessTypeIdToDelete = accessTypesBeforeDelete.get(0).getId();
    deleteWithNoContent(ACCESS_TYPES_PATH + "/" + accessTypeIdToDelete);

    List<AccessType> accessTypesAfterDelete = getAccessTypesOld(vertx);

    assertEquals(accessTypesBeforeDelete.size() - 1, accessTypesAfterDelete.size());
    assertThat(accessTypesAfterDelete, not(hasItem(
      hasProperty(AccessTypesTableConstants.ID_COLUMN, equalTo(accessTypeIdToDelete)))));
  }

  @Test
  public void shouldReturn400OnDeleteIfIdIsInvalid() {
    String id = "99999999-9999-2-9999-999999999999";
    JsonapiError error = deleteWithStatus(ACCESS_TYPES_PATH + "/" + id, SC_BAD_REQUEST).as(JsonapiError.class);

    assertEquals(1, error.getErrors().size());
    assertThat(error.getErrors().get(0).getTitle(), containsString("'id' parameter is incorrect."));
  }

  @Test
  public void shouldReturn404OnDeleteIfNotFound() {
    String id = "11111111-1111-1111-a111-111111111111";
    JsonapiError error = deleteWithStatus(ACCESS_TYPES_PATH + "/" + id, SC_NOT_FOUND).as(JsonapiError.class);

    assertEquals(1, error.getErrors().size());
    assertEquals(String.format("Access type with id '%s' not found", id), error.getErrors().get(0).getTitle());
  }

  @Test
  public void shouldReturn400OnDeleteWhenAssignedToRecords() {
    try {
      List<AccessType> accessTypesBeforeDelete = insertAccessTypes(testData(), vertx);
      String id = accessTypesBeforeDelete.get(0).getId();
      insertAccessTypeMapping("11111111-1111", RecordType.PACKAGE, id, vertx);

      JsonapiError errors = deleteWithStatus(ACCESS_TYPES_PATH + "/" + id, SC_BAD_REQUEST).as(JsonapiError.class);
      assertEquals("Can't delete access type that has assigned records", errors.getErrors().get(0).getTitle());
    } finally {
      clearDataFromTable(vertx, ACCESS_TYPES_MAPPING_TABLE_NAME);
    }
  }

  @Test
  public void shouldReturnAccessTypeWhenDataIsValid() throws IOException, URISyntaxException {
    String postBody = Json.encode(new AccessTypePostRequest().withData(stubbedAccessType()));

    mockValidAccessTypesLimit();
    String resourcePath = String.format(KB_CREDENTIALS_ACCESS_TYPES_ENDPOINT, credentialsId);
    AccessType actual = postWithStatus(resourcePath, postBody, SC_CREATED, USER8_TOKEN).as(AccessType.class);

    assertNotNull((actual.getId()));
    assertNotNull((actual.getAttributes()));
    assertNotNull((actual.getCreator()));
    assertNotNull((actual.getMetadata()));
    assertEquals("firstname_test", actual.getCreator().getFirstName());
    assertEquals("lastname_test", actual.getCreator().getLastName());
    assertEquals("accessTypes", actual.getType().value());
    assertNull(actual.getUpdater());
  }

  @Test
  public void shouldReturn400WhenReachedMaximumAccessTypesSize() throws IOException, URISyntaxException {
    List<AccessType> accessTypes = testData(credentialsId);
    insertAccessType(accessTypes.get(0), vertx);
    insertAccessType(accessTypes.get(1), vertx);

    AccessType accessType = stubbedAccessType();
    accessType.getAttributes().setName(STUB_ACCESS_TYPE_NAME_3);
    String postBody = Json.encode(new AccessTypePostRequest().withData(accessType));

    mockValidAccessTypesLimit();
    String resourcePath = String.format(KB_CREDENTIALS_ACCESS_TYPES_ENDPOINT, credentialsId);
    JsonapiError errors = postWithStatus(resourcePath, postBody, SC_BAD_REQUEST, USER8_TOKEN).as(JsonapiError.class);

    assertEquals("Maximum number of access types allowed is 2", errors.getErrors().get(0).getTitle());
  }

  @Test
  public void shouldSaveOnlyAllowedNumberOfAccessTypes() throws IOException, URISyntaxException {
    List<AccessType> accessTypes = testData(credentialsId);
    insertAccessType(accessTypes.get(0), vertx);
    insertAccessType(accessTypes.get(1), vertx);
    insertAccessType(accessTypes.get(2), vertx);

    AccessType accessType = stubbedAccessType();
    accessType.getAttributes().setName("new");
    String postBody = Json.encode(new AccessTypePostRequest().withData(accessType));

    mockInvalidAccessTypesLimit();
    String resourcePath = String.format(KB_CREDENTIALS_ACCESS_TYPES_ENDPOINT, credentialsId);
    JsonapiError errors = postWithStatus(resourcePath, postBody, SC_BAD_REQUEST, USER8_TOKEN).as(JsonapiError.class);

    assertEquals("Maximum number of access types allowed is 3", errors.getErrors().get(0).getTitle());
    List<AccessType> accessTypesInDb = getAccessTypes(vertx);
    assertEquals(3, accessTypesInDb.size());
  }

  @Test
  public void shouldReturn404WhenUserNotFound() throws IOException, URISyntaxException {
    String postBody = Json.encode(new AccessTypePostRequest().withData(stubbedAccessType()));

    mockValidAccessTypesLimit();
    String resourcePath = String.format(KB_CREDENTIALS_ACCESS_TYPES_ENDPOINT, credentialsId);
    JsonapiError errors = postWithStatus(resourcePath, postBody, SC_NOT_FOUND, USER2_TOKEN).as(JsonapiError.class);
    assertEquals("User not found", errors.getErrors().get(0).getTitle());
  }

  @Test
  public void shouldReturn422WhenRequestHasUnrecognizedField() {
    String postBody = Json.encode(new AccessTypePostRequest().withData(stubbedAccessType()))
      .replaceFirst("type", "BadType");

    String resourcePath = String.format(KB_CREDENTIALS_ACCESS_TYPES_ENDPOINT, credentialsId);
    String response = postWithStatus(resourcePath, postBody, SC_UNPROCESSABLE_ENTITY, USER8_TOKEN).asString();
    assertThat(response, containsString("Unrecognized field"));
  }

  @Test
  public void shouldReturn422WhenPostHasInvalidUUID() {
    AccessType accessType = stubbedAccessType();
    accessType.setId("-2-");
    String postBody = Json.encode(new AccessTypePostRequest().withData(accessType));

    String resourcePath = String.format(KB_CREDENTIALS_ACCESS_TYPES_ENDPOINT, credentialsId);
    Errors errors = postWithStatus(resourcePath, postBody, SC_UNPROCESSABLE_ENTITY, USER8_TOKEN).as(Errors.class);
    assertEquals("data.id", errors.getErrors().get(0).getParameters().get(0).getKey());
  }

  @Test
  public void shouldReturn400WhenContentTypeHeaderIsMissing() {
    String resourcePath = String.format(KB_CREDENTIALS_ACCESS_TYPES_ENDPOINT, credentialsId);
    RestAssured.given()
      .spec(givenWithUrl())
      .header(RestConstants.OKAPI_TENANT_HEADER, STUB_TENANT)
      .body("NOT_JSON")
      .when()
      .post(resourcePath)
      .then()
      .log().ifValidationFails()
      .statusCode(SC_BAD_REQUEST)
      .body(containsString("Content-type"));
  }

  @Test
  public void shouldReturn400WhenJsonIsInvalid() {
    String resourcePath = String.format(KB_CREDENTIALS_ACCESS_TYPES_ENDPOINT, credentialsId);
    RestAssured.given()
      .spec(givenWithUrl())
      .header(RestConstants.OKAPI_TENANT_HEADER, STUB_TENANT)
      .header(RestConstants.OKAPI_TOKEN_HEADER, STUB_TOKEN)
      .header(CONTENT_TYPE_HEADER)
      .body("NOT_JSON")
      .when()
      .post(resourcePath)
      .then()
      .log().ifValidationFails()
      .statusCode(SC_BAD_REQUEST)
      .body(containsString("Json content error"));
  }

  @Test
  public void shouldUpdateAccessTypeWhenValidData() throws IOException, URISyntaxException {
    mockValidAccessTypesLimit();
    List<AccessType> accessTypes = insertAccessTypes(testData(), vertx);

    String putBody = readFile("requests/kb-ebsco/access-types/access-type-1-updated.json");
    String accessTypeId = accessTypes.get(0).getId();
    putWithNoContent(ACCESS_TYPES_PATH + "/" + accessTypeId, putBody, USER9_TOKEN);
  }

  @Test
  public void shouldReturn404WhenNoAccessType() throws IOException, URISyntaxException {
    String putBody = readFile("requests/kb-ebsco/access-types/access-type-1-updated.json");
    String accessTypeId = "/33333333-3333-3333-a333-333333333333";
    final JsonapiError errors = putWithStatus(ACCESS_TYPES_PATH + accessTypeId,
      putBody.replaceAll("1", "3"), SC_NOT_FOUND, USER9_TOKEN)
      .as(JsonapiError.class);
    assertThat(errors.getErrors().get(0).getTitle(), containsString("not found"));

  }

  @Test
  public void shouldReturn401WhenNoUserHeader() throws IOException, URISyntaxException {
    String putBody = readFile("requests/kb-ebsco/access-types/access-type-1-updated.json");
    String accessTypeId = "/33333333-3333-3333-a333-333333333333";
    final JsonapiError errors = putWithStatus(ACCESS_TYPES_PATH + accessTypeId,
      putBody.replaceAll("1", "3"), SC_UNAUTHORIZED)
      .as(JsonapiError.class);
    assertThat(errors.getErrors().get(0).getTitle(), containsString("Unauthorized"));
  }

  @Test
  public void shouldReturn400WhenInvalidId() throws IOException, URISyntaxException {
    String putBody = readFile("requests/kb-ebsco/access-types/access-type-1-updated.json");
    String accessTypeId = "/c0af6d39-6705-43d7-b91e-c01c3549ddww";
    final JsonapiError errors = putWithStatus(ACCESS_TYPES_PATH + accessTypeId,
      putBody, SC_BAD_REQUEST, USER9_TOKEN)
      .as(JsonapiError.class);
    assertThat(errors.getErrors().get(0).getTitle(), containsString("'id' parameter is incorrect."));
  }

  @Test
  public void shouldReturn401WhenUnAuthorized() throws IOException, URISyntaxException {
    List<AccessType> accessTypes = insertAccessTypes(testData(), vertx);

    String putBody = readFile("requests/kb-ebsco/access-types/access-type-1-updated.json");
    String accessTypeId = accessTypes.get(0).getId();
    putWithStatus(ACCESS_TYPES_PATH + "/" + accessTypeId, putBody, SC_UNAUTHORIZED, USER3_TOKEN);
  }

  private AccessType stubbedAccessType() {
    return new AccessType()
      .withType(AccessType.Type.ACCESS_TYPES)
      .withAttributes(new AccessTypeDataAttributes()
        .withName(STUB_ACCESS_TYPE_NAME)
        .withCredentialsId(credentialsId));
  }

  private void mockValidAccessTypesLimit() throws IOException, URISyntaxException {
    stubFor(
      get(new UrlPathPattern(CONFIG_ACCESS_TYPE_LIMIT_URL_PATTERN, true))
        .willReturn(new ResponseDefinitionBuilder()
          .withStatus(200)
          .withBody(readFile("responses/configuration/access-types-limit.json"))
        ));
  }

  private void mockInvalidAccessTypesLimit() throws IOException, URISyntaxException {
    stubFor(
      get(new UrlPathPattern(CONFIG_ACCESS_TYPE_LIMIT_URL_PATTERN, true))
        .willReturn(new ResponseDefinitionBuilder()
          .withStatus(200)
          .withBody(readFile("responses/configuration/access-types-limit-invalid.json"))
        ));
  }
}
