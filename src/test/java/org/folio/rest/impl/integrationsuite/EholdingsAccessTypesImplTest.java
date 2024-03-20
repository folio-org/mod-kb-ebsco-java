package org.folio.rest.impl.integrationsuite;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CREATED;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNPROCESSABLE_ENTITY;
import static org.folio.repository.accesstypes.AccessTypeMappingsTableConstants.ACCESS_TYPES_MAPPING_TABLE_NAME;
import static org.folio.repository.accesstypes.AccessTypesTableConstants.ACCESS_TYPES_TABLE_NAME;
import static org.folio.repository.kbcredentials.KbCredentialsTableConstants.KB_CREDENTIALS_TABLE_NAME;
import static org.folio.test.util.TestUtil.STUB_TENANT;
import static org.folio.test.util.TestUtil.STUB_TOKEN;
import static org.folio.test.util.TestUtil.readFile;
import static org.folio.util.AccessTypesTestUtil.ACCESS_TYPES_PATH;
import static org.folio.util.AccessTypesTestUtil.KB_CREDENTIALS_ACCESS_TYPES_ENDPOINT;
import static org.folio.util.AccessTypesTestUtil.KB_CREDENTIALS_ACCESS_TYPE_ID_ENDPOINT;
import static org.folio.util.AccessTypesTestUtil.STUB_ACCESS_TYPE_NAME;
import static org.folio.util.AccessTypesTestUtil.STUB_ACCESS_TYPE_NAME_3;
import static org.folio.util.AccessTypesTestUtil.getAccessTypes;
import static org.folio.util.AccessTypesTestUtil.insertAccessType;
import static org.folio.util.AccessTypesTestUtil.insertAccessTypeMapping;
import static org.folio.util.AccessTypesTestUtil.testData;
import static org.folio.util.AssertTestUtil.assertErrorContainsTitle;
import static org.folio.util.KbCredentialsTestUtil.STUB_API_URL;
import static org.folio.util.KbCredentialsTestUtil.STUB_CREDENTIALS_NAME;
import static org.folio.util.KbCredentialsTestUtil.STUB_TOKEN_HEADER;
import static org.folio.util.KbCredentialsTestUtil.saveKbCredentials;
import static org.folio.util.KbTestUtil.clearDataFromTable;
import static org.folio.util.TokenTestUtils.generateToken;
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

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import com.github.tomakehurst.wiremock.matching.RegexPattern;
import com.github.tomakehurst.wiremock.matching.UrlPathPattern;
import io.restassured.RestAssured;
import io.restassured.http.Header;
import io.vertx.core.json.Json;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.UUID;
import org.folio.okapi.common.XOkapiHeaders;
import org.folio.repository.RecordType;
import org.folio.rest.impl.WireMockTestBase;
import org.folio.rest.jaxrs.model.AccessType;
import org.folio.rest.jaxrs.model.AccessTypeCollection;
import org.folio.rest.jaxrs.model.AccessTypeDataAttributes;
import org.folio.rest.jaxrs.model.AccessTypePostRequest;
import org.folio.rest.jaxrs.model.AccessTypePutRequest;
import org.folio.rest.jaxrs.model.Errors;
import org.folio.rest.jaxrs.model.JsonapiError;
import org.folio.test.util.TestUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class EholdingsAccessTypesImplTest extends WireMockTestBase {

  private static final String USER_8 = "88888888-8888-4888-8888-888888888888";
  private static final String USER_9 = "99999999-9999-4999-9999-999999999999";
  private static final String USER_2 = "22222222-2222-4222-2222-222222222222";
  private static final String USER_3 = "33333333-3333-4333-3333-333333333333";

  private static final Header USER8_TOKEN = new Header(XOkapiHeaders.TOKEN, generateToken("username", USER_8));
  private static final Header USER8_ID = new Header(XOkapiHeaders.USER_ID, USER_8);
  private static final Header USER9_TOKEN = new Header(XOkapiHeaders.TOKEN, generateToken("username", USER_9));
  private static final Header USER9_ID = new Header(XOkapiHeaders.USER_ID, USER_9);
  private static final Header USER2_TOKEN = new Header(XOkapiHeaders.TOKEN, generateToken("username", USER_2));
  private static final Header USER2_ID = new Header(XOkapiHeaders.USER_ID, USER_2);
  private static final String USERDATA_COLLECTION_INFO_STUB_FILE =
    "responses/userlookup/mock_user_collection_response_200.json";

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

    stubFor(
      get(new UrlPathPattern(new EqualToPattern("/users"), false))
        .withQueryParam("query", new RegexPattern("id.*"))
        .willReturn(new ResponseDefinitionBuilder()
          .withBody(TestUtil.readFile(USERDATA_COLLECTION_INFO_STUB_FILE))));

    credentialsId = saveKbCredentials(STUB_API_URL, STUB_CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);
  }

  @After
  public void tearDown() {
    clearDataFromTable(vertx, ACCESS_TYPES_MAPPING_TABLE_NAME);
    clearDataFromTable(vertx, ACCESS_TYPES_TABLE_NAME);
    clearDataFromTable(vertx, KB_CREDENTIALS_TABLE_NAME);
  }

  @Test
  public void shouldReturnAccessTypeCollectionOnGet() {
    List<AccessType> testAccessTypes = testData(credentialsId);
    final String id0 = insertAccessType(testAccessTypes.get(0), vertx);
    final String id1 = insertAccessType(testAccessTypes.get(1), vertx);

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
    final String id0 = insertAccessType(testAccessTypes.get(0), vertx);
    final String id1 = insertAccessType(testAccessTypes.get(1), vertx);

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
    final String id0 = insertAccessType(testAccessTypes.get(0), vertx);
    final String id1 = insertAccessType(testAccessTypes.get(1), vertx);
    final String id2 = insertAccessType(testAccessTypes.get(2), vertx);

    insertAccessTypeMapping("11111111-1111", RecordType.RESOURCE, id0, vertx);
    insertAccessTypeMapping("11111111-1112", RecordType.PACKAGE, id0, vertx);
    insertAccessTypeMapping("11111111-1113", RecordType.PACKAGE, id0, vertx);
    insertAccessTypeMapping("11111111-1114", RecordType.PACKAGE, id1, vertx);
    insertAccessTypeMapping("11111111-1115", RecordType.PACKAGE, id1, vertx);

    String resourcePath = String.format(KB_CREDENTIALS_ACCESS_TYPES_ENDPOINT, credentialsId);
    AccessTypeCollection actual = getWithOk(resourcePath).as(AccessTypeCollection.class);

    assertThat(findAccessTypeWithId(actual, id0).getUsageNumber(), equalTo(3));
    assertThat(findAccessTypeWithId(actual, id1).getUsageNumber(), equalTo(2));
    assertThat(findAccessTypeWithId(actual, id2).getUsageNumber(), equalTo(0));
  }

  @Test
  public void shouldReturnAccessTypeOnGetByIdAndUser() {
    List<AccessType> accessTypes = testData(credentialsId);
    AccessType expected = accessTypes.get(0);
    String id = insertAccessType(expected, vertx);

    insertAccessTypeMapping("11111111-1111", RecordType.RESOURCE, id, vertx);
    insertAccessTypeMapping("11111111-1112", RecordType.PACKAGE, id, vertx);
    insertAccessTypeMapping("11111111-1113", RecordType.PACKAGE, id, vertx);

    String resourcePath = ACCESS_TYPES_PATH + "/" + id;
    AccessType actual = getWithStatus(resourcePath, SC_OK, STUB_TOKEN_HEADER).as(AccessType.class);

    assertEquals(id, actual.getId());
    assertEquals(expected.getAttributes(), actual.getAttributes());
    assertEquals(Integer.valueOf(3), actual.getUsageNumber());
  }

  @Test
  public void shouldReturnAccessTypeOnGetByIdAndCredentialsId() {
    List<AccessType> accessTypes = testData(credentialsId);
    AccessType expected = accessTypes.get(0);
    String id = insertAccessType(expected, vertx);

    insertAccessTypeMapping("11111111-1111", RecordType.RESOURCE, id, vertx);
    insertAccessTypeMapping("11111111-1112", RecordType.PACKAGE, id, vertx);
    insertAccessTypeMapping("11111111-1113", RecordType.PACKAGE, id, vertx);

    String resourcePath = String.format(KB_CREDENTIALS_ACCESS_TYPE_ID_ENDPOINT, credentialsId, id);
    AccessType actual = getWithOk(resourcePath).as(AccessType.class);

    assertEquals(id, actual.getId());
    assertEquals(expected.getAttributes(), actual.getAttributes());
    assertEquals(Integer.valueOf(3), actual.getUsageNumber());
  }

  @Test
  public void shouldReturn404OnGetByIdAndUserIfAccessTypeIsMissing() {
    String id = "11111111-1111-1111-a111-111111111111";
    String resourcePath = ACCESS_TYPES_PATH + "/" + id;
    JsonapiError error = getWithStatus(resourcePath, SC_NOT_FOUND, STUB_TOKEN_HEADER).as(JsonapiError.class);

    assertErrorContainsTitle(error, String.format("Access type not found: id = %s", id));
  }

  @Test
  public void shouldReturn404OnGetByIdAndCredentialsIfCredentialsIsMissing() {
    String id = UUID.randomUUID().toString();
    String resourcePath = String.format(KB_CREDENTIALS_ACCESS_TYPE_ID_ENDPOINT,
      UUID.randomUUID(), id);
    JsonapiError error = getWithStatus(resourcePath, SC_NOT_FOUND, STUB_TOKEN_HEADER).as(JsonapiError.class);

    assertErrorContainsTitle(error, String.format("Access type not found: id = %s", id));
  }

  @Test
  public void shouldReturn400OnGetByIdAndUserIfIdIsInvalid() {
    String id = "invalid-id";
    String resourcePath = ACCESS_TYPES_PATH + "/" + id;
    JsonapiError error = getWithStatus(resourcePath, SC_BAD_REQUEST, STUB_TOKEN_HEADER).as(JsonapiError.class);

    assertErrorContainsTitle(error, "parameter value {invalid-id} is not valid: must match");
  }

  @Test
  public void shouldReturn204OnDelete() {
    List<AccessType> accessTypes = testData(credentialsId);
    String accessTypeIdToDelete = insertAccessType(accessTypes.get(0), vertx);

    String resourcePath = String.format(KB_CREDENTIALS_ACCESS_TYPE_ID_ENDPOINT, credentialsId, accessTypeIdToDelete);
    deleteWithNoContent(resourcePath);

    List<AccessType> accessTypesAfterDelete = getAccessTypes(vertx);

    assertEquals(0, accessTypesAfterDelete.size());
    assertThat(accessTypesAfterDelete, not(hasItem(hasProperty("id", equalTo(accessTypeIdToDelete)))));
  }

  @Test
  public void shouldReturn400OnDeleteIfIdIsInvalid() {
    String resourcePath = String.format(KB_CREDENTIALS_ACCESS_TYPE_ID_ENDPOINT, credentialsId, "invalid-id");
    JsonapiError error = deleteWithStatus(resourcePath, SC_BAD_REQUEST).as(JsonapiError.class);

    assertErrorContainsTitle(error, "parameter value {invalid-id} is not valid: must match");
  }

  @Test
  public void shouldReturn204OnDeleteIfNotFound() {
    String resourcePath = String.format(KB_CREDENTIALS_ACCESS_TYPE_ID_ENDPOINT, credentialsId, UUID.randomUUID());
    int statusCode = deleteWithNoContent(resourcePath).response().statusCode();

    assertEquals(SC_NO_CONTENT, statusCode);
  }

  @Test
  public void shouldReturn204OnDeleteWhenAssignedToRecords() {
    List<AccessType> accessTypes = testData(credentialsId);
    String id = insertAccessType(accessTypes.get(0), vertx);
    insertAccessTypeMapping("11111111-1111", RecordType.PACKAGE, id, vertx);

    String resourcePath = String.format(KB_CREDENTIALS_ACCESS_TYPE_ID_ENDPOINT, credentialsId, id);
    int statusCode = deleteWithNoContent(resourcePath).response().statusCode();

    assertEquals(SC_NO_CONTENT, statusCode);
  }

  @Test
  public void shouldReturnAccessTypeWhenDataIsValid() throws IOException, URISyntaxException {
    String postBody = Json.encode(new AccessTypePostRequest().withData(stubbedAccessType()));

    mockValidAccessTypesLimit();
    String resourcePath = String.format(KB_CREDENTIALS_ACCESS_TYPES_ENDPOINT, credentialsId);
    AccessType actual = postWithStatus(resourcePath, postBody, SC_CREATED, USER8_TOKEN, USER8_ID).as(AccessType.class);

    assertNotNull(actual.getId());
    assertNotNull(actual.getAttributes());
    assertNotNull(actual.getCreator());
    assertNotNull(actual.getMetadata());
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
    JsonapiError error = postWithStatus(resourcePath, postBody, SC_BAD_REQUEST, USER8_TOKEN).as(JsonapiError.class);

    assertErrorContainsTitle(error, "Maximum number of access types allowed is 2");
  }

  @Test
  public void shouldReturn400WhenPostAccessTypeWithDuplicateName() throws IOException, URISyntaxException {
    List<AccessType> accessTypes = testData(credentialsId);
    insertAccessType(accessTypes.get(0), vertx);

    AccessType accessType = stubbedAccessType();
    stubbedAccessType().getAttributes().setName(accessTypes.get(0).getAttributes().getName());
    String postBody = Json.encode(new AccessTypePostRequest().withData(accessType));

    mockValidAccessTypesLimit();
    String resourcePath = String.format(KB_CREDENTIALS_ACCESS_TYPES_ENDPOINT, credentialsId);
    JsonapiError error =
      postWithStatus(resourcePath, postBody, SC_UNPROCESSABLE_ENTITY, USER8_TOKEN, USER8_ID).as(JsonapiError.class);

    assertErrorContainsTitle(error, "Duplicate name");
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
    JsonapiError error = postWithStatus(resourcePath, postBody, SC_BAD_REQUEST, USER8_TOKEN).as(JsonapiError.class);

    assertErrorContainsTitle(error, "Maximum number of access types allowed is 3");
    List<AccessType> accessTypesInDb = getAccessTypes(vertx);
    assertEquals(3, accessTypesInDb.size());
  }

  @Test
  public void shouldReturn404WhenUserNotFound() throws IOException, URISyntaxException {
    String postBody = Json.encode(new AccessTypePostRequest().withData(stubbedAccessType()));

    mockValidAccessTypesLimit();
    String resourcePath = String.format(KB_CREDENTIALS_ACCESS_TYPES_ENDPOINT, credentialsId);
    JsonapiError error =
      postWithStatus(resourcePath, postBody, SC_NOT_FOUND, USER2_TOKEN, USER2_ID).as(JsonapiError.class);

    assertErrorContainsTitle(error, "User not found");
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
  public void shouldReturn422WhenPostHasInvalidUuid() {
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
    String error = RestAssured.given()
      .spec(givenWithUrl())
      .header(XOkapiHeaders.TENANT, STUB_TENANT)
      .body("NOT_JSON")
      .when()
      .post(resourcePath)
      .then()
      .log().ifValidationFails()
      .statusCode(SC_BAD_REQUEST)
      .extract()
      .asString();

    assertThat(error, containsString("Content-type"));
  }

  @Test
  public void shouldReturn400WhenJsonIsInvalid() {
    String resourcePath = String.format(KB_CREDENTIALS_ACCESS_TYPES_ENDPOINT, credentialsId);
    String error = RestAssured.given()
      .spec(givenWithUrl())
      .header(XOkapiHeaders.TENANT, STUB_TENANT)
      .header(XOkapiHeaders.TOKEN, STUB_TOKEN)
      .header(CONTENT_TYPE_HEADER)
      .body("NOT_JSON")
      .when()
      .post(resourcePath)
      .then()
      .log().ifValidationFails()
      .statusCode(SC_BAD_REQUEST)
      .extract()
      .asString();

    assertThat(error, containsString("Unrecognized token"));
  }

  @Test
  public void shouldReturn204OnPutByCredentialsAndAccessTypeId() {
    List<AccessType> accessTypes = testData(credentialsId);
    String id = insertAccessType(accessTypes.get(0), vertx);

    AccessType accessType = stubbedAccessType();
    String updatedName = "UpdatedName";
    String updatedDescription = "UpdatedDescription";
    accessType.getAttributes().setName(updatedName);
    accessType.getAttributes().setDescription(updatedDescription);

    String putBody = Json.encode(new AccessTypePutRequest().withData(accessType));
    String resourcePath = String.format(KB_CREDENTIALS_ACCESS_TYPE_ID_ENDPOINT, credentialsId, id);
    putWithNoContent(resourcePath, putBody, USER9_TOKEN, USER9_ID);

    AccessType actual = getAccessTypes(vertx).get(0);
    assertEquals(id, actual.getId());
    assertEquals(updatedName, actual.getAttributes().getName());
    assertEquals(updatedDescription, actual.getAttributes().getDescription());
    assertEquals(credentialsId, actual.getAttributes().getCredentialsId());
    assertNotNull(actual.getMetadata());
  }

  @Test
  public void shouldReturn404OnPutByCredentialsAndAccessTypeIdWhenAccessTypeIsMissing() {
    String putBody = Json.encode(new AccessTypePutRequest().withData(stubbedAccessType()));
    String resourcePath = String.format(KB_CREDENTIALS_ACCESS_TYPE_ID_ENDPOINT, credentialsId, UUID.randomUUID());
    JsonapiError error = putWithStatus(resourcePath, putBody, SC_NOT_FOUND, USER9_TOKEN).as(JsonapiError.class);

    assertErrorContainsTitle(error, "not found");
  }

  @Test
  public void shouldReturn422OnPutByCredentialsAndAccessTypeIdWhenInvalidId() {
    AccessType accessType = stubbedAccessType();
    accessType.setId("invalid-id-format");

    String putBody = Json.encode(new AccessTypePutRequest().withData(accessType));
    String resourcePath = String.format(KB_CREDENTIALS_ACCESS_TYPE_ID_ENDPOINT, credentialsId, UUID.randomUUID());
    Errors errors = putWithStatus(resourcePath, putBody, SC_UNPROCESSABLE_ENTITY, USER9_TOKEN).as(Errors.class);

    assertThat(errors.getErrors().get(0).getParameters().get(0).getKey(), equalTo("data.id"));
  }

  private AccessType findAccessTypeWithId(AccessTypeCollection collection, String id) {
    return collection.getData().stream()
      .filter(accessType -> accessType.getId().equals(id))
      .findFirst().orElse(null);
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
      get(urlPathMatching("/configurations/entries.*"))
        .willReturn(aResponse()
          .withStatus(200)
          .withBody(readFile("responses/configuration/access-types-limit.json"))
        ));
  }

  private void mockInvalidAccessTypesLimit() throws IOException, URISyntaxException {
    stubFor(
      get(urlPathMatching("/configurations/entries.*"))
        .willReturn(aResponse()
          .withStatus(200)
          .withBody(readFile("responses/configuration/access-types-limit-invalid.json"))
        ));
  }
}
