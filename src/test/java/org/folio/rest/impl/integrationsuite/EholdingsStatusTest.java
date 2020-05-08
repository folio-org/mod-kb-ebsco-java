package org.folio.rest.impl.integrationsuite;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import static org.folio.repository.kbcredentials.KbCredentialsTableConstants.KB_CREDENTIALS_TABLE_NAME;
import static org.folio.test.util.TestUtil.STUB_TENANT;
import static org.folio.test.util.TestUtil.STUB_TOKEN;
import static org.folio.test.util.TestUtil.readFile;
import static org.folio.util.KBTestUtil.clearDataFromTable;
import static org.folio.util.KBTestUtil.mockEmptyConfiguration;
import static org.folio.util.KBTestUtil.setupDefaultKBConfiguration;
import static org.folio.util.KbCredentialsTestUtil.STUB_TOKEN_HEADER;

import java.io.IOException;
import java.net.URISyntaxException;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.matching.RegexPattern;
import com.github.tomakehurst.wiremock.matching.UrlPathPattern;
import io.restassured.RestAssured;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.folio.rest.impl.WireMockTestBase;
import org.folio.rest.jaxrs.model.ConfigurationStatus;
import org.folio.rest.util.RestConstants;

@RunWith(VertxUnitRunner.class)
public class EholdingsStatusTest extends WireMockTestBase {

  public static final String EHOLDINGS_STATUS_PATH = "eholdings/status";

  @After
  public void tearDown() {
    clearDataFromTable(vertx, KB_CREDENTIALS_TABLE_NAME);
  }

  @Test
  public void shouldReturnTrueWhenRMAPIRequestCompletesWith200Status() throws IOException, URISyntaxException {

    setupDefaultKBConfiguration(getWiremockUrl(), vertx);

    stubFor(
      get(new UrlPathPattern(new RegexPattern("/rm/rmaccounts.*"), true))
        .willReturn(new ResponseDefinitionBuilder()
          .withBody(readFile("responses/rmapi/vendors/get-zero-vendors-response.json"))));

    final ConfigurationStatus status = getWithOk(EHOLDINGS_STATUS_PATH, STUB_TOKEN_HEADER).as(ConfigurationStatus.class);
    assertThat(status.getData().getAttributes().getIsConfigurationValid(), equalTo(true));

  }

  @Test
  public void shouldReturnFalseWhenRMAPIRequestCompletesWithErrorStatus() {

    setupDefaultKBConfiguration(getWiremockUrl(), vertx);

    stubFor(
      get(new UrlPathPattern(new RegexPattern("/rm/rmaccounts.*"), true))
        .willReturn(new ResponseDefinitionBuilder().withStatus(401)));

    final ConfigurationStatus status = getWithOk(EHOLDINGS_STATUS_PATH, STUB_TOKEN_HEADER).as(ConfigurationStatus.class);
    assertThat(status.getData().getAttributes().getIsConfigurationValid(), equalTo(false));
  }

  @Test
  public void shouldReturn500OnInvalidOkapiUrl() {
    RestAssured.given()
      .header(RestConstants.OKAPI_TENANT_HEADER, STUB_TENANT)
      .header(RestConstants.OKAPI_TOKEN_HEADER, STUB_TOKEN)
      .header(RestConstants.OKAPI_URL_HEADER, "wrongUrl^")
      .baseUri("http://localhost")
      .port(port)
      .when()
      .get(EHOLDINGS_STATUS_PATH)
      .then()
      .statusCode(500);
  }

  @Test
  @Ignore
  public void shouldReturnFalseIfEmptyConfig() throws IOException, URISyntaxException {
    mockEmptyConfiguration(null);

    final ConfigurationStatus status = getWithOk(EHOLDINGS_STATUS_PATH, STUB_TOKEN_HEADER).as(ConfigurationStatus.class);

    assertThat(status.getData().getAttributes().getIsConfigurationValid(), equalTo(false));
  }
}
