package org.folio.rest.impl;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
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
import org.folio.service.holdings.ConfigurationMessage;
import org.folio.service.holdings.HoldingsMessage;
import org.folio.service.holdings.LoadServiceFacade;
import org.folio.util.HoldingsTestUtil;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import com.github.tomakehurst.wiremock.matching.RegexPattern;
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
  private static final int TIMEOUT = 500;
  private static final int EXPECTED_LOADED_PAGES = 2;
  private static final String STUB_HOLDINGS_TITLE = "java-test-one";
  static final String HOLDINGS_STATUS_ENDPOINT = "/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/holdings/status";
  static final String HOLDINGS_POST_HOLDINGS_ENDPOINT = "/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/holdings";
  static final String HOLDINGS_GET_ENDPOINT = "/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/holdings";
  static final String LOAD_HOLDINGS_ENDPOINT = "loadHoldings";

  private static final String GET_HOLDINGS_SCENARIO = "Get holdings";
  private static final String COMPLETED_STATE = "Completed state";
  private static final String RETRY_SCENARIO = "Retry scenario";
  private static final String SECOND_TRY = "Second try";
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

//  @Test
//  public void shouldRetryCreationOfSnapshotWhenItFails(TestContext context) throws IOException, URISyntaxException {
//    mockGet(new EqualToPattern(HOLDINGS_STATUS_ENDPOINT), "responses/rmapi/holdings/status/get-status-completed.json");
//
//    stubFor(
//      post(new UrlPathPattern(new EqualToPattern(HOLDINGS_POST_HOLDINGS_ENDPOINT), false))
//        .inScenario(RETRY_SCENARIO)
//        .willSetStateTo(SECOND_TRY)
//        .willReturn(new ResponseDefinitionBuilder()
//          .withStatus(500)));
//
//    stubFor(
//      post(new UrlPathPattern(new EqualToPattern(HOLDINGS_POST_HOLDINGS_ENDPOINT), false))
//        .inScenario(RETRY_SCENARIO)
//        .whenScenarioStateIs(SECOND_TRY)
//        .willReturn(new ResponseDefinitionBuilder()
//          .withBody("")
//          .withStatus(202)));
//
//    Async async = context.async();
//    interceptor = intercept(HOLDINGS_SERVICE_ADDRESS, SNAPSHOT_CREATED_ACTION, message -> async.complete());
//    vertx.eventBus().addInterceptor(interceptor);
//
//    LoadServiceFacade proxy = LoadServiceFacade.createProxy(vertx, LOAD_FACADE_ADDRESS);
//    proxy.createSnapshot(new ConfigurationMessage(stubConfiguration, STUB_TENANT));
//
//    async.await(TIMEOUT);
//  }

//  @Test
//  public void shouldStopRetryingAfterMultipleFailures() throws IOException, URISyntaxException, InterruptedException {
//    mockGet(new EqualToPattern(HOLDINGS_STATUS_ENDPOINT), "responses/rmapi/holdings/status/get-status-completed.json");
//
//    UrlPathPattern urlPattern = new UrlPathPattern(new EqualToPattern(HOLDINGS_POST_HOLDINGS_ENDPOINT), false);
//    stubFor(
//      post(urlPattern)
//        .willReturn(new ResponseDefinitionBuilder()
//          .withStatus(500)));
//
//    LoadServiceFacade proxy = LoadServiceFacade.createProxy(vertx, LOAD_FACADE_ADDRESS);
//    proxy.createSnapshot(new ConfigurationMessage(stubConfiguration, STUB_TENANT));
//
//    Thread.sleep(200);
//    verify(TEST_SNAPSHOT_RETRY_COUNT,
//      RequestPatternBuilder.newRequestPattern(RequestMethod.POST, urlPattern));
//  }

  @Test
  public void shouldSendSaveHoldingsEventForEachLoadedPage(TestContext context) throws IOException, URISyntaxException {
    mockGet(new EqualToPattern(HOLDINGS_STATUS_ENDPOINT), "responses/rmapi/holdings/status/get-status-completed.json");
    mockGet(new RegexPattern(HOLDINGS_GET_ENDPOINT), "responses/rmapi/holdings/holdings/get-holdings.json");

    List<HoldingsMessage> messages = new ArrayList<>();
    Async async = context.async(EXPECTED_LOADED_PAGES);
    interceptor = intercept(HOLDINGS_SERVICE_ADDRESS, SAVE_HOLDINGS_ACTION,
      message -> {
        messages.add(((JsonObject) message.body()).getJsonObject("holdings").mapTo(HoldingsMessage.class));
        async.countDown();
      });
    vertx.eventBus().addInterceptor(interceptor);

    LoadServiceFacade proxy = LoadServiceFacade.createProxy(vertx, LOAD_FACADE_ADDRESS);
    proxy.loadHoldings(new ConfigurationMessage(stubConfiguration, STUB_TENANT));

    async.await(TIMEOUT);
    assertEquals(2, messages.size());
    assertEquals(STUB_HOLDINGS_TITLE, messages.get(0).getHoldingList().get(0).getPublicationTitle());
  }

  @Test
  public void shouldRetryLoadingPageWhenPageFails(TestContext context) throws IOException, URISyntaxException {
    mockGet(new EqualToPattern(HOLDINGS_STATUS_ENDPOINT), "responses/rmapi/holdings/status/get-status-completed-one-page.json");
    UrlPathPattern urlPattern = new UrlPathPattern(new RegexPattern(HOLDINGS_GET_ENDPOINT), true);
    stubFor(get(urlPattern)
      .inScenario(RETRY_SCENARIO)
      .willSetStateTo(SECOND_TRY)
      .willReturn(new ResponseDefinitionBuilder().withStatus(SC_INTERNAL_SERVER_ERROR)
      ));
    stubFor(get(urlPattern)
      .inScenario(RETRY_SCENARIO)
      .whenScenarioStateIs(SECOND_TRY)
      .willReturn(new ResponseDefinitionBuilder()
        .withBody(readFile("responses/rmapi/holdings/holdings/get-holdings.json"))));

    Async async = context.async();
    interceptor = intercept(HOLDINGS_SERVICE_ADDRESS, SAVE_HOLDINGS_ACTION,
      message -> async.complete());
    vertx.eventBus().addInterceptor(interceptor);

    LoadServiceFacade proxy = LoadServiceFacade.createProxy(vertx, LOAD_FACADE_ADDRESS);
    proxy.loadHoldings(new ConfigurationMessage(stubConfiguration, STUB_TENANT));
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

  public static Handler<SendContext> intercept(String serviceAddress, String serviceMethodName,
                                               Consumer<Message> messageConsumer) {
    return messageContext -> {
      Message message = messageContext.message();
      if (serviceAddress.equals(message.address())
        && serviceMethodName.equals(message.headers().get("action"))) {
        messageConsumer.accept(message);
        messageContext.next();
      } else {
        messageContext.next();
      }
    };
  }
}
