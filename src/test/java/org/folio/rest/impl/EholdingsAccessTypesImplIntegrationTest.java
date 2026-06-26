package org.folio.rest.impl;

import static org.folio.HttpStatus.SC_BAD_REQUEST;
import static org.folio.HttpStatus.SC_CREATED;
import static org.folio.HttpStatus.SC_NOT_FOUND;
import static org.folio.HttpStatus.SC_NO_CONTENT;
import static org.folio.HttpStatus.SC_OK;
import static org.folio.HttpStatus.SC_UNPROCESSABLE_CONTENT;
import static org.folio.repository.accesstypes.AccessTypeMappingsTableConstants.ACCESS_TYPES_MAPPING_TABLE_NAME;
import static org.folio.repository.accesstypes.AccessTypesTableConstants.ACCESS_TYPES_TABLE_NAME;
import static org.folio.repository.kbcredentials.KbCredentialsTableConstants.KB_CREDENTIALS_TABLE_NAME;
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
import static org.folio.util.KbCredentialsTestUtil.setupDefaultKbConfiguration;
import static org.folio.util.TestUtil.STUB_TENANT;
import static org.folio.util.TestUtil.STUB_TOKEN;
import static org.folio.util.TestUtil.clearDataFromTable;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.restassured.RestAssured;
import io.vertx.core.json.Json;
import java.util.List;
import java.util.UUID;
import org.folio.okapi.common.XOkapiHeaders;
import org.folio.repository.RecordType;
import org.folio.rest.jaxrs.model.AccessType;
import org.folio.rest.jaxrs.model.AccessTypeCollection;
import org.folio.rest.jaxrs.model.AccessTypeDataAttributes;
import org.folio.rest.jaxrs.model.AccessTypePostRequest;
import org.folio.rest.jaxrs.model.AccessTypePutRequest;
import org.folio.rest.jaxrs.model.Errors;
import org.folio.rest.jaxrs.model.JsonapiError;
import org.folio.util.IntegrationTestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EholdingsAccessTypesImplIntegrationTest extends IntegrationTestBase {

  private String credentialsId;

  @BeforeEach
  void setUp() {
    credentialsId = setupDefaultKbConfiguration(getWiremockUrl(), vertx);
  }

  @AfterEach
  void tearDown() {
    clearDataFromTable(vertx, ACCESS_TYPES_MAPPING_TABLE_NAME);
    clearDataFromTable(vertx, ACCESS_TYPES_TABLE_NAME);
    clearDataFromTable(vertx, KB_CREDENTIALS_TABLE_NAME);
  }

  @Test
  void shouldReturnAccessTypeCollectionOnGet() {
    List<AccessType> testAccessTypes = testData(credentialsId);
    final String id0 = insertAccessType(testAccessTypes.get(0), vertx);
    final String id1 = insertAccessType(testAccessTypes.get(1), vertx);

    AccessTypeCollection actual = getWithStatus(ACCESS_TYPES_PATH, SC_OK)
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
  void shouldReturnAccessTypeCollectionOnGetByCredentialsId() {
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
  void shouldReturnAccessTypeCollectionWithUsageNumberOnGetByCredentialsId() {
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

    assertEquals(3, findAccessTypeWithId(actual, id0).getUsageNumber());
    assertEquals(2, findAccessTypeWithId(actual, id1).getUsageNumber());
    assertEquals(0, findAccessTypeWithId(actual, id2).getUsageNumber());
  }

  @Test
  void shouldReturnAccessTypeOnGetByIdAndUser() {
    List<AccessType> accessTypes = testData(credentialsId);
    AccessType expected = accessTypes.getFirst();
    String id = insertAccessType(expected, vertx);

    insertAccessTypeMapping("11111111-1111", RecordType.RESOURCE, id, vertx);
    insertAccessTypeMapping("11111111-1112", RecordType.PACKAGE, id, vertx);
    insertAccessTypeMapping("11111111-1113", RecordType.PACKAGE, id, vertx);

    String resourcePath = ACCESS_TYPES_PATH + "/" + id;
    AccessType actual = getWithStatus(resourcePath, SC_OK).as(AccessType.class);

    assertEquals(id, actual.getId());
    assertEquals(expected.getAttributes(), actual.getAttributes());
    assertEquals(Integer.valueOf(3), actual.getUsageNumber());
  }

  @Test
  void shouldReturnAccessTypeOnGetByIdAndCredentialsId() {
    List<AccessType> accessTypes = testData(credentialsId);
    AccessType expected = accessTypes.getFirst();
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
  void shouldReturn404OnGetByIdAndUserIfAccessTypeIsMissing() {
    String id = "11111111-1111-1111-a111-111111111111";
    String resourcePath = ACCESS_TYPES_PATH + "/" + id;
    JsonapiError error = getWithStatus(resourcePath, SC_NOT_FOUND).as(JsonapiError.class);

    assertErrorContainsTitle(error, String.format("Access type not found: id = %s", id));
  }

  @Test
  void shouldReturn404OnGetByIdAndCredentialsIfCredentialsIsMissing() {
    String id = UUID.randomUUID().toString();
    String resourcePath = String.format(KB_CREDENTIALS_ACCESS_TYPE_ID_ENDPOINT,
      UUID.randomUUID(), id);
    JsonapiError error = getWithStatus(resourcePath, SC_NOT_FOUND).as(JsonapiError.class);

    assertErrorContainsTitle(error, String.format("Access type not found: id = %s", id));
  }

  @Test
  void shouldReturn400OnGetByIdAndUserIfIdIsInvalid() {
    String id = "invalid-id";
    String resourcePath = ACCESS_TYPES_PATH + "/" + id;
    JsonapiError error = getWithStatus(resourcePath, SC_BAD_REQUEST).as(JsonapiError.class);

    assertErrorContainsTitle(error, "parameter value {invalid-id} is not valid: must match");
  }

  @Test
  void shouldReturn204OnDelete() {
    List<AccessType> accessTypes = testData(credentialsId);
    String accessTypeIdToDelete = insertAccessType(accessTypes.getFirst(), vertx);

    String resourcePath = String.format(KB_CREDENTIALS_ACCESS_TYPE_ID_ENDPOINT, credentialsId, accessTypeIdToDelete);
    deleteWithNoContent(resourcePath);

    List<AccessType> accessTypesAfterDelete = getAccessTypes(vertx);

    assertEquals(0, accessTypesAfterDelete.size());
    assertThat(accessTypesAfterDelete, not(hasItem(hasProperty("id", equalTo(accessTypeIdToDelete)))));
  }

  @Test
  void shouldReturn400OnDeleteIfIdIsInvalid() {
    String resourcePath = String.format(KB_CREDENTIALS_ACCESS_TYPE_ID_ENDPOINT, credentialsId, "invalid-id");
    JsonapiError error = deleteWithStatus(resourcePath, SC_BAD_REQUEST).as(JsonapiError.class);

    assertErrorContainsTitle(error, "parameter value {invalid-id} is not valid: must match");
  }

  @Test
  void shouldReturn204OnDeleteIfNotFound() {
    String resourcePath = String.format(KB_CREDENTIALS_ACCESS_TYPE_ID_ENDPOINT, credentialsId, UUID.randomUUID());
    int statusCode = deleteWithNoContent(resourcePath).response().statusCode();

    assertEquals(SC_NO_CONTENT, statusCode);
  }

  @Test
  void shouldReturn204OnDeleteWhenAssignedToRecords() {
    List<AccessType> accessTypes = testData(credentialsId);
    String id = insertAccessType(accessTypes.getFirst(), vertx);
    insertAccessTypeMapping("11111111-1111", RecordType.PACKAGE, id, vertx);

    String resourcePath = String.format(KB_CREDENTIALS_ACCESS_TYPE_ID_ENDPOINT, credentialsId, id);
    int statusCode = deleteWithNoContent(resourcePath).response().statusCode();

    assertEquals(SC_NO_CONTENT, statusCode);
  }

  @Test
  void shouldReturnAccessTypeWhenDataIsValid() {
    String postBody = Json.encode(new AccessTypePostRequest().withData(stubbedAccessType()));

    String resourcePath = String.format(KB_CREDENTIALS_ACCESS_TYPES_ENDPOINT, credentialsId);
    AccessType actual = postWithStatus(resourcePath, postBody, SC_CREATED).as(AccessType.class);

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
  void shouldReturn400WhenReachedMaximumAccessTypesSize() {
    List<AccessType> accessTypes = testData(credentialsId);
    insertAccessType(accessTypes.get(0), vertx);
    insertAccessType(accessTypes.get(1), vertx);

    var accessType = stubbedAccessType();
    accessType.getAttributes().setName(STUB_ACCESS_TYPE_NAME_3);
    var postBody = Json.encode(new AccessTypePostRequest().withData(accessType));

    var resourcePath = String.format(KB_CREDENTIALS_ACCESS_TYPES_ENDPOINT, credentialsId);
    var error = postWithStatus(resourcePath, postBody, SC_BAD_REQUEST).as(JsonapiError.class);

    assertErrorContainsTitle(error, "Maximum number of access types allowed is 2");
    List<AccessType> accessTypesInDb = getAccessTypes(vertx);
    assertEquals(2, accessTypesInDb.size());
  }

  @Test
  void shouldReturn400WhenPostAccessTypeWithDuplicateName() {
    List<AccessType> accessTypes = testData(credentialsId);
    insertAccessType(accessTypes.getFirst(), vertx);

    var accessType = stubbedAccessType();
    stubbedAccessType().getAttributes().setName(accessTypes.getFirst().getAttributes().getName());
    String postBody = Json.encode(new AccessTypePostRequest().withData(accessType));

    String resourcePath = String.format(KB_CREDENTIALS_ACCESS_TYPES_ENDPOINT, credentialsId);
    var error =
      postWithStatus(resourcePath, postBody, SC_UNPROCESSABLE_CONTENT).as(JsonapiError.class);

    assertErrorContainsTitle(error, "Duplicate name");
  }

  @Test
  void shouldReturn404WhenUserNotFound() {
    String postBody = Json.encode(new AccessTypePostRequest().withData(stubbedAccessType()));

    String resourcePath = String.format(KB_CREDENTIALS_ACCESS_TYPES_ENDPOINT, credentialsId);
    var error =
      postWithStatus(resourcePath, postBody, SC_NOT_FOUND, USER_NOT_FOUND_USER_ID_HEADER).as(JsonapiError.class);

    assertErrorContainsTitle(error, "User not found");
  }

  @Test
  void shouldReturn422WhenRequestHasUnrecognizedField() {
    String postBody = Json.encode(new AccessTypePostRequest().withData(stubbedAccessType()))
      .replaceFirst("type", "BadType");

    String resourcePath = String.format(KB_CREDENTIALS_ACCESS_TYPES_ENDPOINT, credentialsId);
    String response = postWithStatus(resourcePath, postBody, SC_UNPROCESSABLE_CONTENT).asString();
    assertTrue(response.contains("Unrecognized field"));
  }

  @Test
  void shouldReturn422WhenPostHasInvalidUuid() {
    AccessType accessType = stubbedAccessType();
    accessType.setId("-2-");
    String postBody = Json.encode(new AccessTypePostRequest().withData(accessType));

    var resourcePath = String.format(KB_CREDENTIALS_ACCESS_TYPES_ENDPOINT, credentialsId);
    var errors = postWithStatus(resourcePath, postBody, SC_UNPROCESSABLE_CONTENT).as(Errors.class);
    assertEquals("data.id", errors.getErrors().getFirst().getParameters().getFirst().getKey());
  }

  @Test
  void shouldReturn400WhenContentTypeHeaderIsMissing() {
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

    assertTrue(error.contains("Content-type"));
  }

  @Test
  void shouldReturn400WhenJsonIsInvalid() {
    var resourcePath = String.format(KB_CREDENTIALS_ACCESS_TYPES_ENDPOINT, credentialsId);
    var error = RestAssured.given()
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

    assertTrue(error.contains("Unrecognized token"));
  }

  @Test
  void shouldReturn204OnPutByCredentialsAndAccessTypeId() {
    List<AccessType> accessTypes = testData(credentialsId);
    String id = insertAccessType(accessTypes.getFirst(), vertx);

    AccessType accessType = stubbedAccessType();
    String updatedName = "UpdatedName";
    String updatedDescription = "UpdatedDescription";
    accessType.getAttributes().setName(updatedName);
    accessType.getAttributes().setDescription(updatedDescription);

    String putBody = Json.encode(new AccessTypePutRequest().withData(accessType));
    String resourcePath = String.format(KB_CREDENTIALS_ACCESS_TYPE_ID_ENDPOINT, credentialsId, id);
    putWithNoContent(resourcePath, putBody);

    AccessType actual = getAccessTypes(vertx).getFirst();
    assertEquals(id, actual.getId());
    assertEquals(updatedName, actual.getAttributes().getName());
    assertEquals(updatedDescription, actual.getAttributes().getDescription());
    assertEquals(credentialsId, actual.getAttributes().getCredentialsId());
    assertNotNull(actual.getMetadata());
  }

  @Test
  void shouldReturn404OnPutByCredentialsAndAccessTypeIdWhenAccessTypeIsMissing() {
    String putBody = Json.encode(new AccessTypePutRequest().withData(stubbedAccessType()));
    String resourcePath = String.format(KB_CREDENTIALS_ACCESS_TYPE_ID_ENDPOINT, credentialsId, UUID.randomUUID());
    JsonapiError error = putWithStatus(resourcePath, putBody, SC_NOT_FOUND).as(JsonapiError.class);

    assertErrorContainsTitle(error, "not found");
  }

  @Test
  void shouldReturn422OnPutByCredentialsAndAccessTypeIdWhenInvalidId() {
    AccessType accessType = stubbedAccessType();
    accessType.setId("invalid-id-format");

    String putBody = Json.encode(new AccessTypePutRequest().withData(accessType));
    String resourcePath = String.format(KB_CREDENTIALS_ACCESS_TYPE_ID_ENDPOINT, credentialsId, UUID.randomUUID());
    Errors errors = putWithStatus(resourcePath, putBody, SC_UNPROCESSABLE_CONTENT).as(Errors.class);

    assertEquals("data.id", errors.getErrors().getFirst().getParameters().getFirst().getKey());
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
}
