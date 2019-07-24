package org.folio.rest.impl;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.folio.repository.holdings.status.HoldingsStatusTableConstants.HOLDINGS_STATUS_TABLE;
import static org.folio.service.holdings.HoldingConstants.HOLDINGS_SERVICE_ADDRESS;
import static org.folio.service.holdings.HoldingConstants.LOAD_FACADE_ADDRESS;
import static org.folio.util.TestUtil.STUB_TENANT;
import static org.folio.util.TestUtil.mockDefaultConfiguration;
import static org.folio.util.TestUtil.mockGet;
import static org.folio.util.TestUtil.readFile;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.folio.holdingsiq.model.Configuration;
import org.folio.repository.holdings.HoldingInfoInDB;
import org.folio.service.holdings.HoldingsMessage;
import org.folio.service.holdings.LoadServiceFacade;
import org.folio.service.holdings.message.LoadHoldingsMessage;
import org.folio.util.HoldingsStatusUtil;
import org.folio.util.HoldingsTestUtil;
import org.folio.util.TestUtil;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import com.github.tomakehurst.wiremock.matching.RegexPattern;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import com.github.tomakehurst.wiremock.matching.UrlPathPattern;

import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.SendContext;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class LoadHoldingsImplTest extends WireMockTestBase {

  public static final String SAVE_HOLDINGS_ACTION = "saveHolding";
  public static final String SNAPSHOT_CREATED_ACTION = "snapshotCreated";
  public static final String SNAPSHOT_FAILED_ACTION = "snapshotFailed";
  public static final String CREATE_SNAPSHOT_ACTION = "createSnapshot";

  private static final int TIMEOUT = 180000;
  private static final int EXPECTED_LOADED_PAGES = 2;
  private static final int TEST_SNAPSHOT_RETRY_COUNT = 2;
  private static final String STUB_HOLDINGS_TITLE = "java-test-one";
  static final String HOLDINGS_STATUS_ENDPOINT = "/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/holdings/status";
  static final String HOLDINGS_POST_HOLDINGS_ENDPOINT = "/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/holdings";
  static final String HOLDINGS_GET_ENDPOINT = "/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/holdings";
  static final String LOAD_HOLDINGS_ENDPOINT = "loadHoldings";

  private static final String GET_HOLDINGS_SCENARIO = "Get holdings";
  private static final String COMPLETED_STATE = "Completed state";
  private static final String RETRY_SCENARIO = "Retry scenario";
  private Configuration stubConfiguration;
  private Handler<SendContext> interceptor;

  @Before
  public void setUp() throws Exception {
    super.setUp();
    stubConfiguration = Configuration.builder()
      .apiKey(STUB_API_KEY)
      .customerId(STUB_CUSTOMER_ID)
      .url(getWiremockUrl())
      .build();
    TestUtil.clearDataFromTable(vertx, HOLDINGS_STATUS_TABLE);
    HoldingsStatusUtil.insertStatusNotStarted(vertx);
  }

  @After
  public void tearDown() {
    if(interceptor!= null){
      vertx.eventBus().removeInterceptor(interceptor);
    }
  }

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

    final List<HoldingInfoInDB> holdingsList = HoldingsTestUtil.getHoldings(vertx);
    assertThat(holdingsList.size(), Matchers.notNullValue());
  }

  @Test
  public void shouldRetryCreationOfSnapshotWhenItFails(TestContext context) throws IOException, URISyntaxException {
    mockDefaultConfiguration(getWiremockUrl());
    mockGet(new EqualToPattern(HOLDINGS_STATUS_ENDPOINT), "responses/rmapi/holdings/status/get-status-completed.json");
    stubFor(
      post(new UrlPathPattern(new EqualToPattern(HOLDINGS_POST_HOLDINGS_ENDPOINT), false))
        .willReturn(new ResponseDefinitionBuilder()
          .withBody("")
          .withStatus(202)));
    mockResponseList(new UrlPathPattern(new EqualToPattern(HOLDINGS_GET_ENDPOINT), false),
      new ResponseDefinitionBuilder().withStatus(500),
      new ResponseDefinitionBuilder()
        .withBody("")
        .withStatus(202)
    );

    Async async = context.async();
    interceptor = interceptAndBlock(HOLDINGS_SERVICE_ADDRESS, SNAPSHOT_CREATED_ACTION, message -> async.complete());
    vertx.eventBus().addInterceptor(interceptor);

    postWithStatus(LOAD_HOLDINGS_ENDPOINT, "", SC_NO_CONTENT);

    async.await(TIMEOUT);
  }

  @Test
  public void shouldStopRetryingAfterMultipleFailures() throws IOException, URISyntaxException, InterruptedException {
    mockDefaultConfiguration(getWiremockUrl());
    mockGet(new EqualToPattern(HOLDINGS_STATUS_ENDPOINT), "responses/rmapi/holdings/status/get-status-completed.json");

    UrlPathPattern urlPattern = new UrlPathPattern(new EqualToPattern(HOLDINGS_POST_HOLDINGS_ENDPOINT), false);
    stubFor(
      post(urlPattern)
        .willReturn(new ResponseDefinitionBuilder()
          .withStatus(500)));

    postWithStatus(LOAD_HOLDINGS_ENDPOINT, "", SC_NO_CONTENT);

    Thread.sleep(200);
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
    interceptor = interceptAndBlock(HOLDINGS_SERVICE_ADDRESS, SAVE_HOLDINGS_ACTION, message -> async.countDown());
    vertx.eventBus().addInterceptor(interceptor);

    postWithStatus(LOAD_HOLDINGS_ENDPOINT, "", SC_NO_CONTENT);

    async.await(TIMEOUT);
  }

  @Test
  public void shouldSendSaveHoldingsEventForEachLoadedPage(TestContext context) throws IOException, URISyntaxException {
    mockGet(new EqualToPattern(HOLDINGS_STATUS_ENDPOINT), "responses/rmapi/holdings/status/get-status-completed.json");
    mockGet(new RegexPattern(HOLDINGS_GET_ENDPOINT), "responses/rmapi/holdings/holdings/get-holdings.json");

    List<HoldingsMessage> messages = new ArrayList<>();
    Async async = context.async(EXPECTED_LOADED_PAGES);
    interceptor = interceptAndBlock(HOLDINGS_SERVICE_ADDRESS, SAVE_HOLDINGS_ACTION,
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
    interceptor = interceptAndBlock(HOLDINGS_SERVICE_ADDRESS, SAVE_HOLDINGS_ACTION,
      message -> async.complete());
    vertx.eventBus().addInterceptor(interceptor);

    LoadServiceFacade proxy = LoadServiceFacade.createProxy(vertx, LOAD_FACADE_ADDRESS);
    proxy.loadHoldings(new LoadHoldingsMessage(stubConfiguration, STUB_TENANT, 2, 1));
    async.await(TIMEOUT);
  }

  @Ignore("loadHoldings endpoint was changed to return response immediately, " +
    "instead of returning it after loading is complete")
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

    final List<HoldingInfoInDB> holdingsList = HoldingsTestUtil.getHoldings(vertx);
    assertThat(holdingsList.size(), equalTo(2));
  }

  public static void mockResponseList(UrlPathPattern urlPattern, ResponseDefinitionBuilder... responses) {
    int scenarioStep = 0;
    for (ResponseDefinitionBuilder response : responses) {
      if(scenarioStep == 0){
        stubFor(
          get(urlPattern)
            .inScenario(RETRY_SCENARIO)
            .willSetStateTo(String.valueOf(++scenarioStep))
            .willReturn(response));
      }
      else{
        stubFor(
          get(urlPattern)
            .inScenario(RETRY_SCENARIO)
            .whenScenarioStateIs(String.valueOf(scenarioStep))
            .willSetStateTo(String.valueOf(++scenarioStep))
            .willReturn(response));
      }
    }
  }

  public static Handler<SendContext> intercept(String serviceAddress, String serviceMethodName,
                                               Consumer<Message> messageConsumer) {
    return messageContext -> {
      Message message = messageContext.message();
      if (messageMatches(serviceAddress, serviceMethodName, message)) {
        messageConsumer.accept(message);
        messageContext.next();
      } else {
        messageContext.next();
      }
    };
  }

  public static Handler<SendContext> interceptAndBlock(String serviceAddress, String serviceMethodName,
                                                       Consumer<Message> messageConsumer) {
    return messageContext -> {
      Message message = messageContext.message();
      if (messageMatches(serviceAddress, serviceMethodName, message)) {
        messageConsumer.accept(message);
      } else {
        messageContext.next();
      }
    };
  }

  private static boolean messageMatches(String serviceAddress, String serviceMethodName, Message message) {
    return serviceAddress.equals(message.address())
      && serviceMethodName.equals(message.headers().get("action"));
  }
}
