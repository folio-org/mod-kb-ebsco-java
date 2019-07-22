package org.folio.rest.impl;

import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static java.time.ZonedDateTime.parse;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.folio.repository.holdings.status.HoldingsStatusTableConstants.HOLDINGS_STATUS_TABLE;
import static org.folio.rest.impl.LoadHoldingsImplTest.HOLDINGS_GET_ENDPOINT;
import static org.folio.rest.impl.LoadHoldingsImplTest.HOLDINGS_POST_HOLDINGS_ENDPOINT;
import static org.folio.rest.impl.LoadHoldingsImplTest.LOAD_HOLDINGS_ENDPOINT;
import static org.folio.service.holdings.HoldingsServiceImpl.POSTGRES_TIMESTAMP_FORMATTER;
import static org.folio.util.TestUtil.mockDefaultConfiguration;
import static org.folio.util.TestUtil.mockGet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.equalToIgnoringWhiteSpace;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;

import org.folio.rest.jaxrs.model.HoldingsLoadingStatus;
import org.folio.util.HoldingsStatusUtil;
import org.folio.util.TestUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import com.github.tomakehurst.wiremock.matching.RegexPattern;
import com.github.tomakehurst.wiremock.matching.UrlPathPattern;

import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class LoadHoldingsStatusImplTest extends WireMockTestBase {

  public static final String HOLDINGS_STATUS_ENDPOINT = LOAD_HOLDINGS_ENDPOINT + "/status";
  private static final int SLEEP_TIME = 400;

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    TestUtil.clearDataFromTable(vertx, HOLDINGS_STATUS_TABLE);
    HoldingsStatusUtil.insertStatusNotStarted(vertx);
  }

  @Test
  public void shouldReturnStatusNotStarted() throws IOException, URISyntaxException {
    mockDefaultConfiguration(getWiremockUrl());
    final HoldingsLoadingStatus status = getWithOk(HOLDINGS_STATUS_ENDPOINT).body().as(HoldingsLoadingStatus.class);
    assertThat(status.getData().getAttributes().getStatus().getName().value(), equalToIgnoringWhiteSpace("Not Started"));
  }

  @Test
  public void shouldReturnStatusPopulatingStagingArea() throws IOException, URISyntaxException, InterruptedException {
    mockDefaultConfiguration(getWiremockUrl());
    mockGet(new EqualToPattern(LoadHoldingsImplTest.HOLDINGS_STATUS_ENDPOINT), "responses/rmapi/holdings/status/get-status-in-progress.json");
    postWithStatus(LOAD_HOLDINGS_ENDPOINT, "", SC_NO_CONTENT);
    Thread.sleep(50);
    final HoldingsLoadingStatus status = getWithOk(HOLDINGS_STATUS_ENDPOINT).body().as(HoldingsLoadingStatus.class);
    assertThat(status.getData().getAttributes().getStatus().getDetail().value(), equalToIgnoringWhiteSpace("Populating staging area"));
    Thread.sleep(300);
  }

  @Test
  public void shouldReturnStatusCompleted() throws IOException, URISyntaxException, InterruptedException {
    mockDefaultConfiguration(getWiremockUrl());

    mockGet(new EqualToPattern(LoadHoldingsImplTest.HOLDINGS_STATUS_ENDPOINT), "responses/rmapi/holdings/status/get-status-completed-one-page.json");

    stubFor(post(new UrlPathPattern(new EqualToPattern(HOLDINGS_POST_HOLDINGS_ENDPOINT), false))
      .willReturn(new ResponseDefinitionBuilder()
        .withBody("")
        .withStatus(202)));

    mockGet(new RegexPattern(HOLDINGS_GET_ENDPOINT), "responses/rmapi/holdings/holdings/get-holdings.json");

    postWithStatus(LOAD_HOLDINGS_ENDPOINT, "", SC_NO_CONTENT);
    Thread.sleep(SLEEP_TIME);
    final HoldingsLoadingStatus status = getWithOk(HOLDINGS_STATUS_ENDPOINT).body().as(HoldingsLoadingStatus.class);

    assertThat(status.getData().getType(), equalTo("status"));
    assertThat(status.getData().getAttributes().getTotalCount(), equalTo(2));
    assertThat(status.getData().getAttributes().getStatus().getName().value(), equalToIgnoringCase("Completed"));

    assertTrue(parse(status.getData().getAttributes().getStarted(), POSTGRES_TIMESTAMP_FORMATTER)
      .isBefore(parse(status.getData().getAttributes().getFinished(), POSTGRES_TIMESTAMP_FORMATTER)));
  }

  @Test
  public void shouldReturnErrorWhenRMAPIReturnsError() throws IOException, URISyntaxException, InterruptedException {
    mockDefaultConfiguration(getWiremockUrl());

    mockGet(new EqualToPattern(LoadHoldingsImplTest.HOLDINGS_STATUS_ENDPOINT), SC_INTERNAL_SERVER_ERROR);

    postWithStatus(LOAD_HOLDINGS_ENDPOINT, "", SC_NO_CONTENT);

    Thread.sleep(SLEEP_TIME);
    final HoldingsLoadingStatus status = getWithOk(HOLDINGS_STATUS_ENDPOINT).body().as(HoldingsLoadingStatus.class);
    assertThat(status.getData().getAttributes().getStatus().getName().value(), equalToIgnoringCase("Failed"));
  }
}
