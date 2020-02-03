package org.folio.rest.impl.integrationsuite;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CREATED;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_UNPROCESSABLE_ENTITY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import static org.folio.rest.util.RestConstants.OKAPI_USER_ID_HEADER;
import static org.folio.test.util.TestUtil.STUB_TENANT;
import static org.folio.test.util.TestUtil.STUB_TOKEN;
import static org.folio.test.util.TestUtil.readFile;

import java.io.IOException;
import java.net.URISyntaxException;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import com.github.tomakehurst.wiremock.matching.UrlPathPattern;
import io.restassured.RestAssured;
import io.restassured.http.Header;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.folio.rest.impl.WireMockTestBase;
import org.folio.rest.jaxrs.model.AccessTypeCollectionItem;
import org.folio.rest.jaxrs.model.Errors;
import org.folio.rest.jaxrs.model.JsonapiError;
import org.folio.rest.util.RestConstants;
import org.folio.util.AccessTypesTestUtil;

@RunWith(VertxUnitRunner.class)
public class EholdingsAccessTypesImplTest extends WireMockTestBase {

  private static final String USER_8 = "88888888-8888-4888-8888-888888888888";
  private static final String USER_2 = "22222222-2222-4222-2222-222222222222";
  private static final Header USER8 = new Header(OKAPI_USER_ID_HEADER, USER_8);
  private static final Header USER2 = new Header(OKAPI_USER_ID_HEADER, USER_2);
  private static final String ACCESS_TYPES_PATH = "/eholdings/access-types";


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
      get(new UrlPathPattern(new EqualToPattern("/users/" + USER_2), false))
        .willReturn(new ResponseDefinitionBuilder()
          .withStatus(404)
        ));
  }

  @After
  public void tearDown() {
    AccessTypesTestUtil.clearAccessTypes(vertx);
  }

  @Test
  public void shouldReturnAccessTypeWhenDataIsValid() throws IOException, URISyntaxException {

    String postBody  = readFile("requests/kb-ebsco/access-types/access-type-1.json");
    final AccessTypeCollectionItem accessType = postWithStatus(ACCESS_TYPES_PATH, postBody, SC_CREATED, USER8)
      .as(AccessTypeCollectionItem.class);

    assertEquals("firstname_test", accessType.getCreator().getFirstName());
    assertEquals("lastname_test", accessType.getCreator().getLastName());
    assertEquals("accessTypes", accessType.getType().value());
    assertNull(accessType.getUpdater());
  }

  @Test
  public void shouldReturn500WhenSaveWithDuplicateIdObject() throws IOException, URISyntaxException {

    String postBody  = readFile("requests/kb-ebsco/access-types/access-type-1.json");
    postWithStatus(ACCESS_TYPES_PATH, postBody, SC_CREATED, USER8);
    postWithStatus(ACCESS_TYPES_PATH, postBody, SC_BAD_REQUEST, USER8);
  }

  @Test
  public void shouldReturn400WhenReachedMaximumAccessTypesSize() throws IOException, URISyntaxException {

    String postBody1  = readFile("requests/kb-ebsco/access-types/access-type-1.json");
    String postBody2  = readFile("requests/kb-ebsco/access-types/access-type-2.json");
    String postBody3  = readFile("requests/kb-ebsco/access-types/access-type-3.json");

    postWithStatus(ACCESS_TYPES_PATH, postBody1, SC_CREATED, USER8);
    postWithStatus(ACCESS_TYPES_PATH, postBody2, SC_CREATED, USER8);

    final JsonapiError errors = postWithStatus(ACCESS_TYPES_PATH, postBody3, SC_BAD_REQUEST, USER8)
      .as(JsonapiError.class);

    assertEquals("Maximum number of access types allowed is 2", errors.getErrors().get(0).getTitle());
  }

  @Test
  public void shouldReturn404WhenUserNoFound() throws IOException, URISyntaxException {

    String postBody  = readFile("requests/kb-ebsco/access-types/access-type-1.json");
    final JsonapiError errors = postWithStatus(ACCESS_TYPES_PATH, postBody, SC_NOT_FOUND, USER2).as(JsonapiError.class);
    assertEquals("User not found", errors.getErrors().get(0).getTitle());
  }

  @Test
  public void shouldReturn422WhenRequestHasUnrecognizedField() throws IOException, URISyntaxException {

    String postBody  = readFile("requests/kb-ebsco/access-types/access-type-1.json");
    String badRequestBody = postBody.replaceFirst("type", "BadType");
    final String response = postWithStatus(ACCESS_TYPES_PATH, badRequestBody, SC_UNPROCESSABLE_ENTITY, USER8).asString();
    assertThat(response, containsString("Unrecognized field"));
  }

  @Test
  public void shouldReturn422WhenPostHasInvalidUUID() throws IOException, URISyntaxException {

    String postBody  = readFile("requests/kb-ebsco/access-types/access-type-1.json");
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

}
