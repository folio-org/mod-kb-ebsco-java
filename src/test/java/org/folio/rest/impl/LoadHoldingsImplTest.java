package org.folio.rest.impl;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import static org.folio.util.TestUtil.mockDefaultConfiguration;
import static org.folio.util.TestUtil.mockGet;
import static org.folio.util.TestUtil.readFile;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import com.github.tomakehurst.wiremock.matching.RegexPattern;
import com.github.tomakehurst.wiremock.matching.UrlPathPattern;
import io.vertx.core.json.Json;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.folio.repository.holdings.DbHolding;
import org.folio.util.HoldingsTestUtil;

@RunWith(VertxUnitRunner.class)
public class LoadHoldingsImplTest extends WireMockTestBase {

  private static final String HOLDINGS_STATUS_ENDPOINT = "/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/holdings/status";
  private static final String HOLDINGS_POST_HOLDINGS_ENDPOINT = "/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/holdings";
  private static final String HOLDINGS_GET_ENDPOINT = "/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/holdings";
  private static final String LOAD_HOLDINGS_ENDPOINT = "loadHoldings";
  private static final String GET_HOLDINGS_SCENARIO = "Get holdings";
  private static final String COMPLETED_STATE = "Completed state";

  @Test
  public void shouldSaveHoldings() throws IOException, URISyntaxException {

    mockDefaultConfiguration(getWiremockUrl());

    mockGet(new EqualToPattern(HOLDINGS_STATUS_ENDPOINT), "responses/rmapi/holdings/status/get-status-completed.json");

    stubFor(
      post(new UrlPathPattern(new EqualToPattern(HOLDINGS_POST_HOLDINGS_ENDPOINT), false))
        .willReturn(new ResponseDefinitionBuilder()
          .withBody("")
          .withStatus(202)));

    mockGet(new RegexPattern(HOLDINGS_GET_ENDPOINT), "responses/rmapi/holdings/holdings/get-holdings.json");

    postWithStatus(LOAD_HOLDINGS_ENDPOINT, "", SC_NO_CONTENT);

    final List<DbHolding> holdingsList = HoldingsTestUtil.getHoldings(vertx);
    assertThat(holdingsList.size(), Matchers.notNullValue());

  }

  @Test
  public void shouldWaitForCompleteStatusAndLoadHoldings() throws IOException, URISyntaxException {
    mockDefaultConfiguration(getWiremockUrl());

    stubFor(get(new UrlPathPattern(new RegexPattern(HOLDINGS_STATUS_ENDPOINT), true))
      .inScenario(GET_HOLDINGS_SCENARIO)
      .whenScenarioStateIs(STARTED)
      .willReturn(new ResponseDefinitionBuilder()
        .withBody(readFile("responses/rmapi/holdings/status/get-status-in-progress.json")))
      .willSetStateTo(COMPLETED_STATE));

    stubFor(get(new UrlPathPattern(new RegexPattern(HOLDINGS_STATUS_ENDPOINT), true))
      .inScenario(GET_HOLDINGS_SCENARIO)
      .whenScenarioStateIs(COMPLETED_STATE)
      .willReturn(new ResponseDefinitionBuilder()
        .withBody(readFile("responses/rmapi/holdings/status/get-status-completed.json"))));

    mockGet(new RegexPattern(HOLDINGS_GET_ENDPOINT), "responses/rmapi/holdings/holdings/get-holdings.json");

    postWithStatus(LOAD_HOLDINGS_ENDPOINT, "", SC_NO_CONTENT);

    final List<DbHolding> holdingsList = HoldingsTestUtil.getHoldings(vertx);
    assertThat(holdingsList.size(), equalTo(2));
  }

  @Test
  public void shouldSaveHoldingsAndClearOldEntries() throws IOException, URISyntaxException {
      mockDefaultConfiguration(getWiremockUrl());
      HoldingsTestUtil.addHolding(vertx, Json.decodeValue(readFile("responses/kb-ebsco/holdings/custom-holding.json"),
          DbHolding.class), Instant.now().minus(Duration.ofDays(2)));

      mockGet(new EqualToPattern(HOLDINGS_STATUS_ENDPOINT), "responses/rmapi/holdings/status/get-status-completed.json");

      stubFor(post(new UrlPathPattern(new EqualToPattern(HOLDINGS_POST_HOLDINGS_ENDPOINT), false))
        .willReturn(new ResponseDefinitionBuilder()
          .withBody("")
          .withStatus(202)));

      mockGet(new RegexPattern(HOLDINGS_GET_ENDPOINT), "responses/rmapi/holdings/holdings/get-holdings.json");

      postWithStatus(LOAD_HOLDINGS_ENDPOINT, "", SC_NO_CONTENT);

      final List<DbHolding> holdingsList = HoldingsTestUtil.getHoldings(vertx);
      assertThat(holdingsList.size(), equalTo(2));
  }

}
