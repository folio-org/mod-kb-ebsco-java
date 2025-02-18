package org.folio.rest.impl.integrationsuite;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.folio.repository.kbcredentials.KbCredentialsTableConstants.KB_CREDENTIALS_TABLE_NAME;
import static org.folio.test.util.TestUtil.STUB_TENANT;
import static org.folio.test.util.TestUtil.STUB_TOKEN;
import static org.folio.test.util.TestUtil.readFile;
import static org.folio.util.KbCredentialsTestUtil.STUB_USER_ID_HEADER;
import static org.folio.util.KbTestUtil.clearDataFromTable;
import static org.folio.util.KbTestUtil.setupDefaultKbConfiguration;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.matching.RegexPattern;
import com.github.tomakehurst.wiremock.matching.UrlPathPattern;
import io.restassured.RestAssured;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import java.io.IOException;
import java.net.URISyntaxException;
import org.folio.okapi.common.XOkapiHeaders;
import org.folio.rest.impl.WireMockTestBase;
import org.folio.rest.jaxrs.model.ConfigurationStatus;
import org.folio.rest.jaxrs.model.JsonapiError;
import org.folio.util.AssertTestUtil;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class EholdingsStatusTest extends WireMockTestBase {

  public static final String EHOLDINGS_STATUS_PATH = "eholdings/status";

  @After
  public void tearDown() {
    clearDataFromTable(vertx, KB_CREDENTIALS_TABLE_NAME);
  }

  @Test
  public void shouldReturnTrueWhenRmApiRequestCompletesWith200Status() throws IOException, URISyntaxException {
    setupDefaultKbConfiguration(getWiremockUrl(), vertx);

    stubFor(
      get(new UrlPathPattern(new RegexPattern("/rm/rmaccounts.*"), true))
        .willReturn(new ResponseDefinitionBuilder()
          .withBody(readFile("responses/rmapi/vendors/get-zero-vendors-response.json"))));

    final ConfigurationStatus status =
      getWithOk(EHOLDINGS_STATUS_PATH, STUB_USER_ID_HEADER).as(ConfigurationStatus.class);
    assertThat(status.getData().getAttributes().getIsConfigurationValid(), equalTo(true));

  }

  @Test
  public void shouldReturnFalseWhenRmApiRequestCompletesWithErrorStatus() {
    setupDefaultKbConfiguration(getWiremockUrl(), vertx);

    stubFor(
      get(new UrlPathPattern(new RegexPattern("/rm/rmaccounts.*"), true))
        .willReturn(new ResponseDefinitionBuilder().withStatus(401)));

    final ConfigurationStatus status =
      getWithOk(EHOLDINGS_STATUS_PATH, STUB_USER_ID_HEADER).as(ConfigurationStatus.class);
    assertThat(status.getData().getAttributes().getIsConfigurationValid(), equalTo(false));
  }

  @Test
  public void shouldReturnErrorWhenRmApiRequestCompletesWith429() {
    setupDefaultKbConfiguration(getWiremockUrl(), vertx);

    stubFor(
      get(new UrlPathPattern(new RegexPattern("/rm/rmaccounts.*"), true))
        .willReturn(new ResponseDefinitionBuilder().withStatus(429).withBody("{\n"
          + "  \"Errors\": [\n"
          + "    {\n"
          + "      \"Code\": 1010,\n"
          + "      \"Message\": \"Too Many Requests.\",\n"
          + "      \"SubCode\": 0\n"
          + "    }\n"
          + "  ]\n"
          + "}")));

    final JsonapiError error = getWithStatus(EHOLDINGS_STATUS_PATH, 429, STUB_USER_ID_HEADER).as(JsonapiError.class);
    AssertTestUtil.assertErrorContainsTitle(error, "Too Many Requests");
  }

  @Test
  public void shouldReturn500OnInvalidOkapiUrl() {
    RestAssured.given()
      .header(XOkapiHeaders.TENANT, STUB_TENANT)
      .header(XOkapiHeaders.TOKEN, STUB_TOKEN)
      .header(XOkapiHeaders.URL, "wrongUrl^")
      .baseUri("http://localhost")
      .port(port)
      .when()
      .get(EHOLDINGS_STATUS_PATH)
      .then()
      .statusCode(500);
  }

  @Test
  public void shouldReturnFalseIfEmptyConfig() {
    final ConfigurationStatus status =
      getWithOk(EHOLDINGS_STATUS_PATH, STUB_USER_ID_HEADER).as(ConfigurationStatus.class);

    assertThat(status.getData().getAttributes().getIsConfigurationValid(), equalTo(false));
  }
}
