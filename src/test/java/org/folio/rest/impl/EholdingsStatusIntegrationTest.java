package org.folio.rest.impl;

import static org.folio.repository.kbcredentials.KbCredentialsTableConstants.KB_CREDENTIALS_TABLE_NAME;
import static org.folio.util.KbCredentialsTestUtil.setupDefaultKbConfiguration;
import static org.folio.util.TestUtil.STUB_TENANT;
import static org.folio.util.TestUtil.STUB_TOKEN;
import static org.folio.util.TestUtil.clearDataFromTable;
import static org.folio.util.TestUtil.readFile;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.tomakehurst.wiremock.matching.RegexPattern;
import io.restassured.RestAssured;
import org.folio.okapi.common.XOkapiHeaders;
import org.folio.rest.jaxrs.model.ConfigurationStatus;
import org.folio.rest.jaxrs.model.JsonapiError;
import org.folio.util.AssertTestUtil;
import org.folio.util.IntegrationTestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class EholdingsStatusIntegrationTest extends IntegrationTestBase {

  private static final String EHOLDINGS_STATUS_PATH = "eholdings/status";

  // RM API responses
  private static final String GET_ZERO_VENDORS_RESPONSE = "responses/rmapi/vendors/get-zero-vendors-response.json";

  // Request/response bodies
  private static final String TOO_MANY_REQUESTS_BODY = """
    {
      "Errors": [
        {
          "Code": 1010,
          "Message": "Too Many Requests.",
          "SubCode": 0
        }
      ]
    }""";

  @AfterEach
  void tearDown() {
    clearDataFromTable(vertx, KB_CREDENTIALS_TABLE_NAME);
  }

  @Test
  void shouldReturnTrueWhenRmApiRequestCompletesWith200Status() {
    setupDefaultKbConfiguration(getWiremockUrl(), vertx);
    mockGet(new RegexPattern(RM_ACCOUNTS_ANY_PATH_REGEX), readFile(GET_ZERO_VENDORS_RESPONSE));

    var status = getWithOk(EHOLDINGS_STATUS_PATH).as(ConfigurationStatus.class);
    assertEquals(true, status.getData().getAttributes().getIsConfigurationValid());
  }

  @Test
  void shouldReturnFalseWhenRmApiRequestCompletesWithErrorStatus() {
    setupDefaultKbConfiguration(getWiremockUrl(), vertx);
    mockGet(new RegexPattern(RM_ACCOUNTS_ANY_PATH_REGEX), 401);

    var status = getWithOk(EHOLDINGS_STATUS_PATH).as(ConfigurationStatus.class);
    assertEquals(false, status.getData().getAttributes().getIsConfigurationValid());
  }

  @Test
  void shouldReturnErrorWhenRmApiRequestCompletesWith429() {
    setupDefaultKbConfiguration(getWiremockUrl(), vertx);
    mockGet(new RegexPattern(RM_ACCOUNTS_ANY_PATH_REGEX), TOO_MANY_REQUESTS_BODY, 429);

    var error = getWithStatus(EHOLDINGS_STATUS_PATH, 429).as(JsonapiError.class);
    AssertTestUtil.assertErrorContainsTitle(error, "Too Many Requests");
  }

  @Test
  void shouldReturn500OnInvalidOkapiUrl() {
    RestAssured.given()
      .header(XOkapiHeaders.TENANT, STUB_TENANT)
      .header(XOkapiHeaders.TOKEN, STUB_TOKEN)
      .header(XOkapiHeaders.URL, "wrongUrl^")
      .baseUri(moduleUrl)
      .when()
      .get(EHOLDINGS_STATUS_PATH)
      .then()
      .statusCode(500);
  }

  @Test
  void shouldReturnFalseIfEmptyConfig() {
    var status = getWithOk(EHOLDINGS_STATUS_PATH).as(ConfigurationStatus.class);
    assertEquals(false, status.getData().getAttributes().getIsConfigurationValid());
  }
}
