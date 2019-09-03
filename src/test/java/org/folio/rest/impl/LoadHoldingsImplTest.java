package org.folio.rest.impl;

import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;

import static org.folio.repository.holdings.status.HoldingsStatusTableConstants.HOLDINGS_STATUS_TABLE;
import static org.folio.rest.jaxrs.model.LoadStatusNameEnum.COMPLETED;
import static org.folio.service.holdings.HoldingConstants.HOLDINGS_SERVICE_ADDRESS;
import static org.folio.service.holdings.HoldingConstants.LOAD_FACADE_ADDRESS;
import static org.folio.service.holdings.HoldingConstants.SAVE_HOLDINGS_ACTION;
import static org.folio.service.holdings.HoldingConstants.SNAPSHOT_CREATED_ACTION;
import static org.folio.service.holdings.HoldingConstants.SNAPSHOT_FAILED_ACTION;
import static org.folio.test.util.TestUtil.STUB_TENANT;
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
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import com.github.tomakehurst.wiremock.matching.RegexPattern;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import com.github.tomakehurst.wiremock.matching.UrlPathPattern;

import io.vertx.core.Handler;
import io.vertx.core.eventbus.SendContext;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;

import org.folio.holdingsiq.model.Configuration;
import org.folio.repository.holdings.HoldingInfoInDB;
import org.folio.repository.holdings.status.HoldingsStatusRepositoryImpl;
import org.folio.repository.holdings.status.RetryStatusRepository;
import org.folio.rest.jaxrs.model.LoadStatusNameEnum;
import org.folio.service.holdings.HoldingsMessage;
import org.folio.service.holdings.HoldingsService;
import org.folio.service.holdings.LoadServiceFacade;
import org.folio.service.holdings.message.LoadHoldingsMessage;
import org.folio.util.HoldingsStatusUtil;
import org.folio.util.HoldingsTestUtil;
import org.folio.util.KBTestUtil;

@RunWith(VertxUnitRunner.class)
public class LoadHoldingsImplTest extends WireMockTestBase {
  static final String HOLDINGS_STATUS_ENDPOINT = "/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/holdings/status";
  static final String HOLDINGS_POST_HOLDINGS_ENDPOINT = "/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/holdings";
  static final String HOLDINGS_GET_ENDPOINT = "/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/holdings";
  static final String LOAD_HOLDINGS_ENDPOINT = "loadHoldings";
  private static final int TIMEOUT = 180000;
  private static final int EXPECTED_LOADED_PAGES = 2;
  private static final int TEST_SNAPSHOT_RETRY_COUNT = 2;
  private static final String STUB_HOLDINGS_TITLE = "java-test-one";
  @InjectMocks
  @Autowired
  HoldingsService holdingsService;
  @Spy
  @Autowired
  HoldingsStatusRepositoryImpl holdingsStatusRepository;
  @Autowired
  RetryStatusRepository retryStatusRepository;
  private Configuration stubConfiguration;
  private Handler<SendContext> interceptor;

  @Before
  public void setUp() throws Exception {
    super.setUp();
    MockitoAnnotations.initMocks(this);
    stubConfiguration = Configuration.builder()
      .apiKey(STUB_API_KEY)
      .customerId(STUB_CUSTOMER_ID)
      .url(getWiremockUrl())
      .build();
    KBTestUtil.clearDataFromTable(vertx, HOLDINGS_STATUS_TABLE);
    HoldingsStatusUtil.insertStatusNotStarted(vertx);
  }

  @After
  public void tearDown() {
    if (interceptor != null) {
      vertx.eventBus().removeInterceptor(interceptor);
    }
  }

  @Test
  public void shouldSaveHoldings(TestContext context) throws IOException, URISyntaxException {
    mockDefaultConfiguration(getWiremockUrl());

    Async async = context.async();
    handleStatusChange(COMPLETED, holdingsStatusRepository, o -> async.complete());

    mockGet(new EqualToPattern(HOLDINGS_STATUS_ENDPOINT), "responses/rmapi/holdings/status/get-status-completed.json");
    StringValuePattern urlPattern = new EqualToPattern(HOLDINGS_POST_HOLDINGS_ENDPOINT);
    stubFor(post(new UrlPathPattern(urlPattern, false))
      .willReturn(new ResponseDefinitionBuilder()
        .withBody("")
        .withStatus(202)));
    mockGet(new RegexPattern(HOLDINGS_GET_ENDPOINT), "responses/rmapi/holdings/holdings/get-holdings.json");

    postWithStatus(LOAD_HOLDINGS_ENDPOINT, "", SC_NO_CONTENT);

    async.await(TIMEOUT);

    final List<HoldingInfoInDB> holdingsList = HoldingsTestUtil.getHoldings(vertx);
    assertThat(holdingsList.size(), Matchers.notNullValue());
  }

  @Test
  public void shouldRetryCreationOfSnapshotWhenItFails(TestContext context) throws IOException, URISyntaxException {
    mockDefaultConfiguration(getWiremockUrl());

    stubFor(
      post(new UrlPathPattern(new EqualToPattern(HOLDINGS_POST_HOLDINGS_ENDPOINT), false))
        .willReturn(new ResponseDefinitionBuilder()
          .withBody("")
          .withStatus(202)));
    ResponseDefinitionBuilder failedResponse = new ResponseDefinitionBuilder().withStatus(500);
    ResponseDefinitionBuilder successfulResponse = new ResponseDefinitionBuilder()
      .withBody(readFile("responses/rmapi/holdings/status/get-status-completed.json"));
    mockResponseList(new UrlPathPattern(new EqualToPattern(HOLDINGS_STATUS_ENDPOINT), false),
      failedResponse,
      successfulResponse,
      successfulResponse);

    Async async = context.async();
    interceptor = interceptAndStop(HOLDINGS_SERVICE_ADDRESS, SNAPSHOT_CREATED_ACTION, message -> async.complete());
    vertx.eventBus().addInterceptor(interceptor);

    postWithStatus(LOAD_HOLDINGS_ENDPOINT, "", SC_NO_CONTENT);

    async.await(TIMEOUT);
    assertTrue(async.isSucceeded());
  }

  @Test
  public void shouldStopRetryingAfterMultipleFailures(TestContext context) throws IOException, URISyntaxException{
    mockDefaultConfiguration(getWiremockUrl());
    mockGet(new EqualToPattern(HOLDINGS_STATUS_ENDPOINT), "responses/rmapi/holdings/status/get-status-completed.json");

    UrlPathPattern urlPattern = new UrlPathPattern(new EqualToPattern(HOLDINGS_POST_HOLDINGS_ENDPOINT), false);
    stubFor(
      post(urlPattern)
        .willReturn(new ResponseDefinitionBuilder()
          .withStatus(500)));

    Async async = context.async(TEST_SNAPSHOT_RETRY_COUNT);
    interceptor = interceptAndContinue(HOLDINGS_SERVICE_ADDRESS, SNAPSHOT_FAILED_ACTION, o -> async.countDown());
    vertx.eventBus().addInterceptor(interceptor);

    postWithStatus(LOAD_HOLDINGS_ENDPOINT, "", SC_NO_CONTENT);

    async.await(TIMEOUT);

    Async retryStatusAsync = context.async();
    retryStatusRepository.get(STUB_TENANT)
      .thenAccept(status -> {
        boolean timerExists = vertx.cancelTimer(status.getTimerId());
        context.assertEquals(0, status.getRetryAttemptsLeft());
        context.assertFalse(timerExists);
        retryStatusAsync.complete();
      });
    retryStatusAsync.await(TIMEOUT);

    verify(TEST_SNAPSHOT_RETRY_COUNT,
      RequestPatternBuilder.newRequestPattern(RequestMethod.POST, urlPattern));
  }

  @Test
  public void shouldRetryLoadingHoldingsFromStartWhenPageFailsToLoad(TestContext context) throws IOException, URISyntaxException {
    mockDefaultConfiguration(getWiremockUrl());
    mockGet(new EqualToPattern(HOLDINGS_STATUS_ENDPOINT), "responses/rmapi/holdings/status/get-status-completed.json");

    stubFor(
      post(new UrlPathPattern(new EqualToPattern(HOLDINGS_POST_HOLDINGS_ENDPOINT), false))
        .willReturn(new ResponseDefinitionBuilder()
          .withBody("")
          .withStatus(202)));

    ResponseDefinitionBuilder successfulResponse = new ResponseDefinitionBuilder()
      .withBody(readFile("responses/rmapi/holdings/holdings/get-holdings.json"))
      .withStatus(200);
    ResponseDefinitionBuilder failedResponse = new ResponseDefinitionBuilder().withStatus(500);
    mockResponseList(new UrlPathPattern(new EqualToPattern(HOLDINGS_GET_ENDPOINT), false),
      successfulResponse,
      failedResponse,
      failedResponse,
      successfulResponse
    );

    int firstTryPages = 1;
    int secondTryPages = 2;
    Async async = context.async(firstTryPages + secondTryPages);
    interceptor = interceptAndStop(HOLDINGS_SERVICE_ADDRESS, SAVE_HOLDINGS_ACTION, message -> async.countDown());
    vertx.eventBus().addInterceptor(interceptor);

    postWithStatus(LOAD_HOLDINGS_ENDPOINT, "", SC_NO_CONTENT);

    async.await(TIMEOUT);
    assertTrue(async.isSucceeded());
  }

  @Test
  public void shouldSendSaveHoldingsEventForEachLoadedPage(TestContext context) throws IOException, URISyntaxException {
    mockGet(new EqualToPattern(HOLDINGS_STATUS_ENDPOINT), "responses/rmapi/holdings/status/get-status-completed.json");
    mockGet(new RegexPattern(HOLDINGS_GET_ENDPOINT), "responses/rmapi/holdings/holdings/get-holdings.json");

    List<HoldingsMessage> messages = new ArrayList<>();
    Async async = context.async(EXPECTED_LOADED_PAGES);
    interceptor = interceptAndStop(HOLDINGS_SERVICE_ADDRESS, SAVE_HOLDINGS_ACTION,
      message -> {
        messages.add(((JsonObject) message.body()).getJsonObject("holdings").mapTo(HoldingsMessage.class));
        async.countDown();
      });
    vertx.eventBus().addInterceptor(interceptor);

    LoadServiceFacade proxy = LoadServiceFacade.createProxy(vertx, LOAD_FACADE_ADDRESS);
    proxy.loadHoldings(new LoadHoldingsMessage(stubConfiguration, STUB_TENANT, 5001, 2));

    async.await(TIMEOUT);
    assertEquals(2, messages.size());
    assertEquals(STUB_HOLDINGS_TITLE, messages.get(0).getHoldingList().get(0).getPublicationTitle());
  }

  @Test
  public void shouldRetryLoadingPageWhenPageFails(TestContext context) throws IOException, URISyntaxException {
    mockGet(new EqualToPattern(HOLDINGS_STATUS_ENDPOINT), "responses/rmapi/holdings/status/get-status-completed-one-page.json");

    mockResponseList(new UrlPathPattern(new EqualToPattern(HOLDINGS_GET_ENDPOINT), false),
      new ResponseDefinitionBuilder().withStatus(SC_INTERNAL_SERVER_ERROR),
      new ResponseDefinitionBuilder()
        .withBody(readFile("responses/rmapi/holdings/holdings/get-holdings.json"))
    );

    Async async = context.async();
    interceptor = interceptAndStop(HOLDINGS_SERVICE_ADDRESS, SAVE_HOLDINGS_ACTION,
      message -> async.complete());
    vertx.eventBus().addInterceptor(interceptor);

    LoadServiceFacade proxy = LoadServiceFacade.createProxy(vertx, LOAD_FACADE_ADDRESS);
    proxy.loadHoldings(new LoadHoldingsMessage(stubConfiguration, STUB_TENANT, 2, 1));
    async.await(TIMEOUT);
    assertTrue(async.isSucceeded());
  }

  public static void handleStatusChange(LoadStatusNameEnum status, HoldingsStatusRepositoryImpl repositorySpy, Consumer<Void> handler) {
    when(repositorySpy.update(
      argThat(argument -> argument.getData().getAttributes().getStatus().getName() == status), anyString()))
      .thenAnswer(invocationOnMock -> ((CompletableFuture<Void>) invocationOnMock.callRealMethod())
        .thenAccept(handler));
  }
}
