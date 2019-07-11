package org.folio.rest.impl;

import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static java.time.ZonedDateTime.parse;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.equalToIgnoringWhiteSpace;

import static org.folio.repository.holdings.status.HoldingsStatusTableConstants.HOLDINGS_STATUS_TABLE;
import static org.folio.rest.impl.LoadHoldingsImplTest.HOLDINGS_GET_ENDPOINT;
import static org.folio.rest.impl.LoadHoldingsImplTest.HOLDINGS_POST_HOLDINGS_ENDPOINT;
import static org.folio.rest.impl.LoadHoldingsImplTest.LOAD_HOLDINGS_ENDPOINT;
import static org.folio.util.TestUtil.mockDefaultConfiguration;
import static org.folio.util.TestUtil.mockEmptyConfiguration;
import static org.folio.util.TestUtil.mockGet;
import static org.folio.util.TestUtil.readFile;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import com.github.tomakehurst.wiremock.matching.RegexPattern;
import com.github.tomakehurst.wiremock.matching.UrlPathPattern;
import io.vertx.core.json.Json;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.folio.repository.holdings.HoldingInfoInDB;
import org.folio.rest.jaxrs.model.HoldingsLoadingStatus;
import org.folio.util.HoldingsStatusUtil;
import org.folio.util.HoldingsTestUtil;
import org.folio.util.TestUtil;

@RunWith(VertxUnitRunner.class)
public class LoadHoldingsStatusImplTest extends WireMockTestBase {

  public static final String HOLDINGS_STATUS_ENDPOINT = LOAD_HOLDINGS_ENDPOINT + "/status";

  @Test
  public void shouldReturnStatusNotStarted() throws IOException, URISyntaxException {
    mockDefaultConfiguration(getWiremockUrl());
    final HoldingsLoadingStatus status = getWithOk(HOLDINGS_STATUS_ENDPOINT).body().as(HoldingsLoadingStatus.class);
    assertThat(status.getData().getAttributes().getStatus().getName().value(), equalToIgnoringWhiteSpace("Not Started"));
  }

  @Test
  public void shouldReturnStatusCompleted() throws IOException, URISyntaxException {
    try {
      mockDefaultConfiguration(getWiremockUrl());

      HoldingsTestUtil.addHolding(vertx, Json.decodeValue(readFile("responses/kb-ebsco/holdings/custom-holding.json"),
        HoldingInfoInDB.class), Instant.now().minus(Duration.ofDays(2)));

      mockGet(new EqualToPattern(LoadHoldingsImplTest.HOLDINGS_STATUS_ENDPOINT), "responses/rmapi/holdings/status/get-status-completed.json");

      stubFor(post(new UrlPathPattern(new EqualToPattern(HOLDINGS_POST_HOLDINGS_ENDPOINT), false))
        .willReturn(new ResponseDefinitionBuilder()
          .withBody("")
          .withStatus(202)));

      mockGet(new RegexPattern(HOLDINGS_GET_ENDPOINT), "responses/rmapi/holdings/holdings/get-holdings.json");

      postWithStatus(LOAD_HOLDINGS_ENDPOINT, "", SC_NO_CONTENT);
      final HoldingsLoadingStatus status = getWithOk(HOLDINGS_STATUS_ENDPOINT).body().as(HoldingsLoadingStatus.class);

      assertThat(status.getData().getType(), equalTo("status"));
      assertThat(status.getData().getAttributes().getTotalCount(), equalTo(2));
      assertThat(status.getData().getAttributes().getStatus().getName().value(), equalToIgnoringCase("Completed"));
      DateTimeFormatter f = getFormatter();

      org.junit.Assert.assertTrue(parse(status.getData().getAttributes().getStarted().replace(" ", "T"), f)
        .isBefore(parse(status.getData().getAttributes().getFinished().replace(" ", "T"), f)));
    } finally {
      TestUtil.clearDataFromTable(vertx, HOLDINGS_STATUS_TABLE);
      HoldingsStatusUtil.insertStatusNotStarted(vertx);
    }
  }

  @Test
  public void shouldReturnErrorWhenNoConfiguration() throws IOException, URISyntaxException {
    try {
      mockEmptyConfiguration(getWiremockUrl());
      postWithStatus(LOAD_HOLDINGS_ENDPOINT, "", SC_INTERNAL_SERVER_ERROR);
      final HoldingsLoadingStatus status = getWithOk(HOLDINGS_STATUS_ENDPOINT).body().as(HoldingsLoadingStatus.class);
      assertThat(status.getData().getAttributes().getStatus().getName().value(), equalToIgnoringCase("Failed"));

    } finally {
      TestUtil.clearDataFromTable(vertx, HOLDINGS_STATUS_TABLE);
      HoldingsStatusUtil.insertStatusNotStarted(vertx);
    }
  }

  private DateTimeFormatter getFormatter() {
    return new DateTimeFormatterBuilder()
      .parseCaseInsensitive()
      .append(ISO_LOCAL_DATE_TIME)
      .appendOffset("+HH", "Z").toFormatter();
  }
}
