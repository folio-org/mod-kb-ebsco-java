package org.folio.rest.impl.integrationsuite;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CREATED;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_UNPROCESSABLE_ENTITY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import static org.folio.rest.util.RestConstants.OKAPI_USER_ID_HEADER;
import static org.folio.test.util.TestUtil.STUB_TENANT;
import static org.folio.test.util.TestUtil.STUB_TOKEN;
import static org.folio.test.util.TestUtil.readFile;
import static org.folio.util.AccessTypesTestUtil.insertAccessTypes;
import static org.folio.util.AccessTypesTestUtil.testData;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import com.github.tomakehurst.wiremock.matching.RegexPattern;
import com.github.tomakehurst.wiremock.matching.UrlPathPattern;
import io.restassured.RestAssured;
import io.restassured.http.Header;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.folio.repository.accesstypes.AccessTypesTableConstants;
import org.folio.rest.converter.accesstypes.AccessTypeCollectionConverter;
import org.folio.rest.impl.WireMockTestBase;
import org.folio.rest.jaxrs.model.AccessTypeCollection;
import org.folio.rest.jaxrs.model.AccessTypeCollectionItem;
import org.folio.rest.jaxrs.model.Errors;
import org.folio.rest.jaxrs.model.JsonapiError;
import org.folio.rest.util.RestConstants;
import org.folio.util.AccessTypesTestUtil;

@RunWith(VertxUnitRunner.class)
public class EholdingsAccessTypesImplTest extends WireMockTestBase {

  private static final String USER_8 = "88888888-8888-4888-8888-888888888888";
  private static final String USER_9 = "99999999-9999-4999-9999-999999999999";
  private static final String USER_2 = "22222222-2222-4222-2222-222222222222";
  private static final String USER_3 = "33333333-3333-4333-3333-333333333333";

  private static final Header USER8 = new Header(OKAPI_USER_ID_HEADER, USER_8);
  private static final Header USER9 = new Header(OKAPI_USER_ID_HEADER, USER_9);
  private static final Header USER2 = new Header(OKAPI_USER_ID_HEADER, USER_2);
  private static final Header USER3 = new Header(OKAPI_USER_ID_HEADER, USER_3);
  private static final String ACCESS_TYPES_PATH = "/eholdings/access-types";

  private static final RegexPattern CONFIG_ACCESS_TYPE_LIMIT_URL_PATTERN =
    new RegexPattern("/configurations/entries.*");


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
      get(new UrlPathPattern(new EqualToPattern("/users/" + USER_2), false))
        .willReturn(new ResponseDefinitionBuilder()
          .withStatus(404)
        ));

    stubFor(
      get(new UrlPathPattern(CONFIG_ACCESS_TYPE_LIMIT_URL_PATTERN, true))
        .willReturn(new ResponseDefinitionBuilder()
          .withStatus(200)
          .withBody(readFile("responses/configuration/access-types-limit.json"))
        ));
  }

  @After
  public void tearDown() {
    AccessTypesTestUtil.clearAccessTypes(vertx);
  }

  @Test
  public void shouldReturnAccessTypeCollectionOnGet() {
    List<AccessTypeCollectionItem> testAccessTypes = testData();
    List<AccessTypeCollectionItem> accessTypeCollectionItems = insertAccessTypes(testAccessTypes, vertx);

    AccessTypeCollection expected = new AccessTypeCollectionConverter().convert(accessTypeCollectionItems);
    AccessTypeCollection actual = getWithOk(ACCESS_TYPES_PATH).as(AccessTypeCollection.class);

    assertEquals(expected, actual);
  }

  @Test
  public void shouldReturnAccessTypeOnGet() {
    List<AccessTypeCollectionItem> accessTypeCollectionItems = insertAccessTypes(testData(), vertx);

    AccessTypeCollectionItem expected = accessTypeCollectionItems.get(0);
    AccessTypeCollectionItem actual = getWithOk(ACCESS_TYPES_PATH + "/" + expected.getId())
      .as(AccessTypeCollectionItem.class);

    assertEquals(expected, actual);
  }

  @Test
  public void shouldReturn404OnGetIfAccessTypeIsMissing() {
    String id = "99999999-9999-9999-9999-999999999999";
    JsonapiError error = getWithStatus(ACCESS_TYPES_PATH + "/" + id, SC_NOT_FOUND).as(JsonapiError.class);

    assertEquals(1, error.getErrors().size());
    assertEquals(String.format("Access type with id '%s' not found", id), error.getErrors().get(0).getTitle());
  }

  @Test
  public void shouldReturn422OnGetIfIdIsInvalid() {
    String id = "99999999-9999-2-9999-999999999999";
    JsonapiError error = getWithStatus(ACCESS_TYPES_PATH + "/" + id, SC_UNPROCESSABLE_ENTITY).as(JsonapiError.class);

    assertEquals(1, error.getErrors().size());
    assertEquals(String.format("Invalid id '%s'", id), error.getErrors().get(0).getTitle());
  }

  @Test
  public void shouldReturn204OnDelete() {
    List<AccessTypeCollectionItem> accessTypesBeforeDelete = insertAccessTypes(testData(), vertx);
    String accessTypeIdToDelete = accessTypesBeforeDelete.get(0).getId();
    deleteWithNoContent(ACCESS_TYPES_PATH + "/" + accessTypeIdToDelete);

    List<AccessTypeCollectionItem> accessTypesAfterDelete = AccessTypesTestUtil.getAccessTypes(vertx);

    assertEquals(accessTypesBeforeDelete.size() - 1, accessTypesAfterDelete.size());
    assertThat(accessTypesAfterDelete, not(hasItem(
      hasProperty(AccessTypesTableConstants.ID_COLUMN, equalTo(accessTypeIdToDelete)))));
  }

  @Test
  public void shouldReturn422OnDeleteIfIdIsInvalid() {
    String id = "99999999-9999-2-9999-999999999999";
    JsonapiError error = deleteWithStatus(ACCESS_TYPES_PATH + "/" + id, SC_UNPROCESSABLE_ENTITY).as(JsonapiError.class);

    assertEquals(1, error.getErrors().size());
    assertEquals(String.format("Invalid id '%s'", id), error.getErrors().get(0).getTitle());
  }

  @Test
  public void shouldReturn404IfNotFound() {
    String id = "11111111-1111-1111-a111-111111111111";
    JsonapiError error = deleteWithStatus(ACCESS_TYPES_PATH + "/" + id, SC_NOT_FOUND).as(JsonapiError.class);

    assertEquals(1, error.getErrors().size());
    assertEquals(String.format("Access type with id '%s' not found", id), error.getErrors().get(0).getTitle());
  }

  @Test
  public void shouldReturnAccessTypeWhenDataIsValid() throws IOException, URISyntaxException {

    String postBody = readFile("requests/kb-ebsco/access-types/access-type-1.json");
    final AccessTypeCollectionItem accessType = postWithStatus(ACCESS_TYPES_PATH, postBody, SC_CREATED, USER8)
      .as(AccessTypeCollectionItem.class);

    assertEquals("firstname_test", accessType.getCreator().getFirstName());
    assertEquals("lastname_test", accessType.getCreator().getLastName());
    assertEquals("accessTypes", accessType.getType().value());
    assertNull(accessType.getUpdater());
  }

  @Test
  public void shouldReturn500WhenSaveWithDuplicateIdObject() throws IOException, URISyntaxException {

    String postBody = readFile("requests/kb-ebsco/access-types/access-type-1.json");
    postWithStatus(ACCESS_TYPES_PATH, postBody, SC_CREATED, USER8);
    final JsonapiError errors = postWithStatus(ACCESS_TYPES_PATH, postBody, SC_BAD_REQUEST, USER8)
      .as(JsonapiError.class);
    assertThat(errors.getErrors().get(0).getTitle(), containsString("duplicate key value violates unique constraint"));
  }

  @Test
  public void shouldReturn400WhenReachedMaximumAccessTypesSize() throws IOException, URISyntaxException {

    String postBody1 = readFile("requests/kb-ebsco/access-types/access-type-1.json");
    String postBody2 = readFile("requests/kb-ebsco/access-types/access-type-2.json");
    String postBody3 = readFile("requests/kb-ebsco/access-types/access-type-3.json");

    postWithStatus(ACCESS_TYPES_PATH, postBody1, SC_CREATED, USER8);
    postWithStatus(ACCESS_TYPES_PATH, postBody2, SC_CREATED, USER8);

    final JsonapiError errors = postWithStatus(ACCESS_TYPES_PATH, postBody3, SC_BAD_REQUEST, USER8)
      .as(JsonapiError.class);

    assertEquals("Maximum number of access types allowed is 2", errors.getErrors().get(0).getTitle());
  }

  @Test
  public void shouldReturn404WhenUserNoFound() throws IOException, URISyntaxException {

    String postBody = readFile("requests/kb-ebsco/access-types/access-type-1.json");
    final JsonapiError errors = postWithStatus(ACCESS_TYPES_PATH, postBody, SC_NOT_FOUND, USER2).as(JsonapiError.class);
    assertEquals("User not found", errors.getErrors().get(0).getTitle());
  }

  @Test
  public void shouldReturn422WhenRequestHasUnrecognizedField() throws IOException, URISyntaxException {

    String postBody = readFile("requests/kb-ebsco/access-types/access-type-1.json");
    String badRequestBody = postBody.replaceFirst("type", "BadType");
    final String response = postWithStatus(ACCESS_TYPES_PATH, badRequestBody, SC_UNPROCESSABLE_ENTITY, USER8).asString();
    assertThat(response, containsString("Unrecognized field"));
  }

  @Test
  public void shouldReturn422WhenPostHasInvalidUUID() throws IOException, URISyntaxException {

    String postBody = readFile("requests/kb-ebsco/access-types/access-type-1.json");
    String badRequestBody = postBody.replaceAll("-1111-", "-2-");  // make bad UUID
    Errors errors = postWithStatus(ACCESS_TYPES_PATH, badRequestBody, SC_UNPROCESSABLE_ENTITY, USER8).as(Errors.class);
    assertEquals("id", errors.getErrors().get(0).getParameters().get(0).getKey());
  }

  @Test
  public void shouldReturn400WhenContentTypeHeaderIsMissing() {
    RestAssured.given()
      .spec(givenWithUrl())
      .header(RestConstants.OKAPI_TENANT_HEADER, STUB_TENANT) // no content-type header
      .body("NOT_JSON")
      .when()
      .post(ACCESS_TYPES_PATH)
      .then()
      .log().ifValidationFails()
      .statusCode(SC_BAD_REQUEST)
      .body(containsString("Content-type"));
  }

  @Test
  public void shouldReturn400WhenJsonIsInvalid() {
    RestAssured.given()
      .spec(givenWithUrl())
      .header(RestConstants.OKAPI_TENANT_HEADER, STUB_TENANT)
      .header(RestConstants.OKAPI_TOKEN_HEADER, STUB_TOKEN)
      .header(CONTENT_TYPE_HEADER)
      .body("This is not json")
      .when()
      .post(ACCESS_TYPES_PATH)
      .then()
      .log().ifValidationFails()
      .statusCode(SC_BAD_REQUEST)
      .body(containsString("Json content error"));
  }

  @Test
  public void shouldUpdateAccessTypeWhenValidData() throws IOException, URISyntaxException {
    String postBody = readFile("requests/kb-ebsco/access-types/access-type-1.json");
    postWithStatus(ACCESS_TYPES_PATH, postBody, SC_CREATED, USER8);

    String putBody = readFile("requests/kb-ebsco/access-types/access-type-1-updated.json");
    String accessTypeId = "/11111111-1111-1111-a111-111111111111";
    putWithNoContent(ACCESS_TYPES_PATH + accessTypeId, putBody, USER9);

    final List<AccessTypeCollectionItem> accessTypes = AccessTypesTestUtil.getAccessTypes(vertx);
    assertEquals(1, accessTypes.size());
    assertNotNull(accessTypes.get(0).getCreator());
    assertEquals("firstname_test", accessTypes.get(0).getCreator().getFirstName());
    assertEquals("cedrick", accessTypes.get(0).getMetadata().getCreatedByUsername());
    assertNotNull(accessTypes.get(0).getUpdater());
    assertEquals("John", accessTypes.get(0).getUpdater().getFirstName());
    assertEquals("john_doe", accessTypes.get(0).getMetadata().getUpdatedByUsername());
  }

  @Test
  public void shouldReturn404WhenNoAccessType() throws IOException, URISyntaxException {
    String putBody = readFile("requests/kb-ebsco/access-types/access-type-1-updated.json");
    String accessTypeId = "/33333333-3333-3333-a333-333333333333";
    final JsonapiError errors = putWithStatus(ACCESS_TYPES_PATH + accessTypeId,
      putBody.replaceAll("1", "3"), SC_NOT_FOUND, USER9)
      .as(JsonapiError.class);
    assertThat(errors.getErrors().get(0).getTitle(), containsString("not found"));

  }

  @Test
  public void shouldReturn400WhenNoUserHeader() throws IOException, URISyntaxException {
    String putBody = readFile("requests/kb-ebsco/access-types/access-type-1-updated.json");
    String accessTypeId = "/33333333-3333-3333-a333-333333333333";
    final JsonapiError errors = putWithStatus(ACCESS_TYPES_PATH + accessTypeId,
      putBody.replaceAll("1", "3"), SC_BAD_REQUEST)
      .as(JsonapiError.class);
    assertThat(errors.getErrors().get(0).getTitle(), containsString("Missing user id header"));
  }

  @Test
  public void shouldReturn422WhenInvalidId() throws IOException, URISyntaxException {
    String putBody = readFile("requests/kb-ebsco/access-types/access-type-1-updated.json");
    String accessTypeId = "/c0af6d39-6705-43d7-b91e-c01c3549ddww";
    final JsonapiError errors = putWithStatus(ACCESS_TYPES_PATH + accessTypeId,
      putBody, SC_UNPROCESSABLE_ENTITY, USER9)
      .as(JsonapiError.class);
    assertThat(errors.getErrors().get(0).getTitle(), containsString("Invalid id"));
  }

  @Test
  public void shouldReturn403WhenUnAuthorized() throws IOException, URISyntaxException {
    String postBody = readFile("requests/kb-ebsco/access-types/access-type-1.json");
    postWithStatus(ACCESS_TYPES_PATH, postBody, SC_CREATED, USER8);

    String putBody = readFile("requests/kb-ebsco/access-types/access-type-1-updated.json");
    String accessTypeId = "11111111-1111-1111-a111-111111111111";
    putWithStatus(ACCESS_TYPES_PATH + accessTypeId, putBody, SC_BAD_REQUEST, USER3);
  }
}
