package org.folio.rest.impl;

import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static java.time.ZonedDateTime.parse;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.equalToIgnoringWhiteSpace;
import static org.junit.Assert.assertTrue;

import static org.folio.repository.holdings.status.HoldingsLoadingStatusFactory.getStatusCompleted;
import static org.folio.repository.holdings.status.HoldingsStatusTableConstants.HOLDINGS_STATUS_TABLE;
import static org.folio.rest.impl.LoadHoldingsImplTest.HOLDINGS_GET_ENDPOINT;
import static org.folio.rest.impl.LoadHoldingsImplTest.HOLDINGS_POST_HOLDINGS_ENDPOINT;
import static org.folio.rest.impl.LoadHoldingsImplTest.LOAD_HOLDINGS_ENDPOINT;
import static org.folio.rest.impl.LoadHoldingsImplTest.handleStatusChange;
import static org.folio.rest.jaxrs.model.LoadStatusNameEnum.COMPLETED;
import static org.folio.service.holdings.HoldingConstants.CREATE_SNAPSHOT_ACTION;
import static org.folio.service.holdings.HoldingConstants.HOLDINGS_SERVICE_ADDRESS;
import static org.folio.service.holdings.HoldingConstants.LOAD_FACADE_ADDRESS;
import static org.folio.service.holdings.HoldingConstants.SNAPSHOT_CREATED_ACTION;
import static org.folio.service.holdings.HoldingConstants.SNAPSHOT_FAILED_ACTION;
import static org.folio.service.holdings.HoldingsServiceImpl.POSTGRES_TIMESTAMP_FORMATTER;
import static org.folio.test.util.TestUtil.STUB_TENANT;
import static org.folio.test.util.TestUtil.STUB_TOKEN;
import static org.folio.test.util.TestUtil.mockGet;
import static org.folio.test.util.TestUtil.mockResponseList;
import static org.folio.test.util.TestUtil.readFile;
import static org.folio.util.KBTestUtil.interceptAndContinue;
import static org.folio.util.KBTestUtil.interceptAndStop;
import static org.folio.util.KBTestUtil.mockDefaultConfiguration;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import com.github.tomakehurst.wiremock.matching.RegexPattern;
import com.github.tomakehurst.wiremock.matching.UrlPathPattern;

import io.vertx.core.Handler;
import io.vertx.core.eventbus.SendContext;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;

import org.folio.repository.holdings.status.HoldingsStatusRepositoryImpl;
import org.folio.rest.client.TenantClient;
import org.folio.rest.jaxrs.model.HoldingsLoadingStatus;
import org.folio.service.holdings.HoldingsService;
import org.folio.util.HoldingsStatusUtil;
import org.folio.util.KBTestUtil;

@RunWith(VertxUnitRunner.class)
public class LoadHoldingsStatusImplTest extends WireMockTestBase {

  private static final String HOLDINGS_STATUS_ENDPOINT = LOAD_HOLDINGS_ENDPOINT + "/status";
  private static final int TIMEOUT = 300;
  private static final int SNAPSHOT_RETRIES = 2;
  private List<Handler<SendContext>> interceptors = new ArrayList<>();

  @InjectMocks
  @Autowired
  HoldingsService holdingsService;
  @Spy
  @Autowired
  HoldingsStatusRepositoryImpl holdingsStatusRepository;

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    MockitoAnnotations.initMocks(this);
    KBTestUtil.clearDataFromTable(vertx, HOLDINGS_STATUS_TABLE);
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
  public void shouldNotOverrideStatusOnSecondCallToTenantAPI(TestContext context) throws IOException, URISyntaxException {
    mockDefaultConfiguration(getWiremockUrl());
    KBTestUtil.clearDataFromTable(vertx, HOLDINGS_STATUS_TABLE);
    HoldingsStatusUtil.insertStatus(vertx, getStatusCompleted(1000));

    Async async = context.async();
    TenantClient tenantClient = new TenantClient(host + ":" + port, STUB_TENANT, STUB_TOKEN);
    try {
      tenantClient.postTenant(null, res2 -> async.complete());
    } catch (Exception e) {
      e.printStackTrace();
    }
    async.awaitSuccess();

    final HoldingsLoadingStatus status = getWithOk(HOLDINGS_STATUS_ENDPOINT).body().as(HoldingsLoadingStatus.class);
    assertThat(status.getData().getAttributes().getStatus().getName().value(), equalToIgnoringWhiteSpace("Completed"));
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
    Handler<SendContext> interceptor = interceptAndContinue(LOAD_FACADE_ADDRESS, CREATE_SNAPSHOT_ACTION, message -> startedAsync.complete());
    vertx.eventBus().addInterceptor(interceptor);
    interceptors.add(interceptor);

    Async finishedAsync = context.async();
    interceptor = interceptAndStop(HOLDINGS_SERVICE_ADDRESS, SNAPSHOT_CREATED_ACTION, message -> finishedAsync.complete());
    vertx.eventBus().addInterceptor(interceptor);
    interceptors.add(interceptor);

    postWithStatus(LOAD_HOLDINGS_ENDPOINT, "", SC_NO_CONTENT);

    startedAsync.await(TIMEOUT);
    final HoldingsLoadingStatus status = getWithOk(HOLDINGS_STATUS_ENDPOINT).body().as(HoldingsLoadingStatus.class);
    assertThat(status.getData().getAttributes().getStatus().getDetail().value(), equalToIgnoringWhiteSpace("Populating staging area"));

    finishedAsync.await(TIMEOUT);
  }

  @Test
  public void shouldReturnStatusCompleted(TestContext context) throws IOException, URISyntaxException {
    mockDefaultConfiguration(getWiremockUrl());

    mockGet(new EqualToPattern(LoadHoldingsImplTest.HOLDINGS_STATUS_ENDPOINT), "responses/rmapi/holdings/status/get-status-completed-one-page.json");

    stubFor(post(new UrlPathPattern(new EqualToPattern(HOLDINGS_POST_HOLDINGS_ENDPOINT), false))
      .willReturn(new ResponseDefinitionBuilder()
        .withBody("")
        .withStatus(202)));

    mockGet(new RegexPattern(HOLDINGS_GET_ENDPOINT), "responses/rmapi/holdings/holdings/get-holdings.json");

    Async async = context.async();
    handleStatusChange(COMPLETED, holdingsStatusRepository, o -> async.complete());

    postWithStatus(LOAD_HOLDINGS_ENDPOINT, "", SC_NO_CONTENT);
    async.await(TIMEOUT);
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
    Handler<SendContext> interceptor = interceptAndContinue(HOLDINGS_SERVICE_ADDRESS, SNAPSHOT_FAILED_ACTION, message -> finishedAsync.countDown());
    vertx.eventBus().addInterceptor(interceptor);
    interceptors.add(interceptor);

    postWithStatus(LOAD_HOLDINGS_ENDPOINT, "", SC_NO_CONTENT);

    finishedAsync.await(TIMEOUT);

    final HoldingsLoadingStatus status = getWithOk(HOLDINGS_STATUS_ENDPOINT).body().as(HoldingsLoadingStatus.class);
    assertThat(status.getData().getAttributes().getStatus().getName().value(), equalToIgnoringCase("Failed"));
  }
}
