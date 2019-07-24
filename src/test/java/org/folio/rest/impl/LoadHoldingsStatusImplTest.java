package org.folio.rest.impl;

import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static java.time.ZonedDateTime.parse;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.folio.repository.holdings.status.HoldingsStatusTableConstants.HOLDINGS_STATUS_TABLE;
import static org.folio.rest.impl.LoadHoldingsImplTest.CREATE_SNAPSHOT_ACTION;
import static org.folio.rest.impl.LoadHoldingsImplTest.HOLDINGS_GET_ENDPOINT;
import static org.folio.rest.impl.LoadHoldingsImplTest.HOLDINGS_POST_HOLDINGS_ENDPOINT;
import static org.folio.rest.impl.LoadHoldingsImplTest.LOAD_HOLDINGS_ENDPOINT;
import static org.folio.rest.impl.LoadHoldingsImplTest.SNAPSHOT_CREATED_ACTION;
import static org.folio.rest.impl.LoadHoldingsImplTest.SNAPSHOT_FAILED_ACTION;
import static org.folio.rest.impl.LoadHoldingsImplTest.intercept;
import static org.folio.rest.impl.LoadHoldingsImplTest.interceptAndBlock;
import static org.folio.rest.impl.LoadHoldingsImplTest.mockResponseList;
import static org.folio.service.holdings.HoldingConstants.HOLDINGS_SERVICE_ADDRESS;
import static org.folio.service.holdings.HoldingConstants.LOAD_FACADE_ADDRESS;
import static org.folio.service.holdings.HoldingsServiceImpl.POSTGRES_TIMESTAMP_FORMATTER;
import static org.folio.util.TestUtil.mockDefaultConfiguration;
import static org.folio.util.TestUtil.mockGet;
import static org.folio.util.TestUtil.readFile;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.equalToIgnoringWhiteSpace;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.folio.rest.jaxrs.model.HoldingsLoadingStatus;
import org.folio.util.HoldingsStatusUtil;
import org.folio.util.TestUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import com.github.tomakehurst.wiremock.matching.RegexPattern;
import com.github.tomakehurst.wiremock.matching.UrlPathPattern;

import io.vertx.core.Handler;
import io.vertx.core.eventbus.SendContext;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class LoadHoldingsStatusImplTest extends WireMockTestBase {

  public static final String HOLDINGS_STATUS_ENDPOINT = LOAD_HOLDINGS_ENDPOINT + "/status";
  private static final int SLEEP_TIME = 200;
  private static final int TIMEOUT = 300;
  public static final int SNAPSHOT_RETRIES = 2;
  private List<Handler<SendContext>> interceptors = new ArrayList<>();

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    TestUtil.clearDataFromTable(vertx, HOLDINGS_STATUS_TABLE);
    HoldingsStatusUtil.insertStatusNotStarted(vertx);
  }

  @After
  public void tearDown() {
    interceptors.forEach(interceptor ->
      vertx.eventBus().removeInterceptor(interceptor));
  }

  @Test
  public void shouldReturnStatusNotStarted() throws IOException, URISyntaxException {
    mockDefaultConfiguration(getWiremockUrl());
    final HoldingsLoadingStatus status = getWithOk(HOLDINGS_STATUS_ENDPOINT).body().as(HoldingsLoadingStatus.class);
    assertThat(status.getData().getAttributes().getStatus().getName().value(), equalToIgnoringWhiteSpace("Not Started"));
  }

  @Test
  public void shouldReturnStatusPopulatingStagingArea(TestContext context) throws IOException, URISyntaxException {
    mockDefaultConfiguration(getWiremockUrl());

    mockResponseList(
      new UrlPathPattern(new EqualToPattern(LoadHoldingsImplTest.HOLDINGS_STATUS_ENDPOINT), false),
      new ResponseDefinitionBuilder()
        .withBody(readFile("responses/rmapi/holdings/status/get-status-in-progress.json"))
        .withStatus(200),
      new ResponseDefinitionBuilder()
        .withBody(readFile("responses/rmapi/holdings/status/get-status-completed.json"))
        .withStatus(200)
    );

    Async startedAsync = context.async();
    Handler<SendContext> interceptor = intercept(LOAD_FACADE_ADDRESS, CREATE_SNAPSHOT_ACTION, message -> startedAsync.complete());
    vertx.eventBus().addInterceptor(interceptor);
    interceptors.add(interceptor);

    Async finishedAsync = context.async();
    interceptor = interceptAndBlock(HOLDINGS_SERVICE_ADDRESS, SNAPSHOT_CREATED_ACTION, message -> finishedAsync.complete());
    vertx.eventBus().addInterceptor(interceptor);
    interceptors.add(interceptor);


    postWithStatus(LOAD_HOLDINGS_ENDPOINT, "", SC_NO_CONTENT);

    startedAsync.await(TIMEOUT);
    final HoldingsLoadingStatus status = getWithOk(HOLDINGS_STATUS_ENDPOINT).body().as(HoldingsLoadingStatus.class);
    assertThat(status.getData().getAttributes().getStatus().getDetail().value(), equalToIgnoringWhiteSpace("Populating staging area"));

    finishedAsync.await(TIMEOUT);
  }

  @Test
  public void shouldReturnStatusCompleted(TestContext context) throws IOException, URISyntaxException, InterruptedException {
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
  public void shouldReturnErrorWhenRMAPIReturnsError(TestContext context) throws IOException, URISyntaxException {
    mockDefaultConfiguration(getWiremockUrl());

    mockGet(new EqualToPattern(LoadHoldingsImplTest.HOLDINGS_STATUS_ENDPOINT), SC_INTERNAL_SERVER_ERROR);

    Async finishedAsync = context.async(SNAPSHOT_RETRIES);
    Handler<SendContext> interceptor = intercept(HOLDINGS_SERVICE_ADDRESS, SNAPSHOT_FAILED_ACTION, message -> finishedAsync.countDown());
    vertx.eventBus().addInterceptor(interceptor);
    interceptors.add(interceptor);

    postWithStatus(LOAD_HOLDINGS_ENDPOINT, "", SC_NO_CONTENT);

    finishedAsync.await(TIMEOUT);

    final HoldingsLoadingStatus status = getWithOk(HOLDINGS_STATUS_ENDPOINT).body().as(HoldingsLoadingStatus.class);
    assertThat(status.getData().getAttributes().getStatus().getName().value(), equalToIgnoringCase("Failed"));
  }
}
