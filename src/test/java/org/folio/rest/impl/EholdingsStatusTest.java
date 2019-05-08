package org.folio.rest.impl;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import static org.folio.util.TestUtil.getRequestSpecificationBuilder;
import static org.folio.util.TestUtil.mockDefaultConfiguration;
import static org.folio.util.TestUtil.mockEmptyConfiguration;
import static org.folio.util.TestUtil.readFile;

import java.io.IOException;
import java.net.URISyntaxException;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.matching.RegexPattern;
import com.github.tomakehurst.wiremock.matching.UrlPathPattern;
import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.folio.rest.jaxrs.model.ConfigurationStatus;
import org.folio.rest.util.RestConstants;

@RunWith(VertxUnitRunner.class)
public class EholdingsStatusTest extends WireMockTestBase {

  public static final String EHOLDINGS_STATUS_PATH = "eholdings/status";

  @Test
  public void shouldReturnTrueWhenRMAPIRequestCompletesWith200Status() throws IOException, URISyntaxException {

    mockDefaultConfiguration(getWiremockUrl());

    stubFor(
      get(new UrlPathPattern(new RegexPattern("/rm/rmaccounts.*"), true))
        .willReturn(new ResponseDefinitionBuilder()
          .withBody(readFile("responses/rmapi/vendors/get-zero-vendors-response.json"))));

    final ConfigurationStatus status = getWithOk(EHOLDINGS_STATUS_PATH).as(ConfigurationStatus.class);
    assertThat(status.getData().getAttributes().getIsConfigurationValid(), equalTo(true));

  }

  @Test
  public void shouldReturnFalseWhenRMAPIRequestCompletesWithErrorStatus() throws IOException, URISyntaxException {

    mockDefaultConfiguration(getWiremockUrl());

    stubFor(
      get(new UrlPathPattern(new RegexPattern("/rm/rmaccounts.*"), true))
        .willReturn(new ResponseDefinitionBuilder().withStatus(401)));

    final ConfigurationStatus status = getWithOk(EHOLDINGS_STATUS_PATH).as(ConfigurationStatus.class);
    assertThat(status.getData().getAttributes().getIsConfigurationValid(), equalTo(false));
  }

  @Test
  public void shouldReturn500OnInvalidOkapiUrl() {
    RequestSpecification spec = getRequestSpecificationBuilder("http://localhost")
      .addHeader(RestConstants.OKAPI_URL_HEADER, "wrongUrl^").build();
    RestAssured.given()
      .spec(spec).port(port)
      .when()
      .get(EHOLDINGS_STATUS_PATH)
      .then()
      .statusCode(500);
  }

  @Test
  public void shouldReturnFalseIfEmptyConfig() throws IOException, URISyntaxException {
    mockEmptyConfiguration(null);

    final ConfigurationStatus status = getWithOk(EHOLDINGS_STATUS_PATH).as(ConfigurationStatus.class);

    assertThat(status.getData().getAttributes().getIsConfigurationValid(), equalTo(false));
  }
}
