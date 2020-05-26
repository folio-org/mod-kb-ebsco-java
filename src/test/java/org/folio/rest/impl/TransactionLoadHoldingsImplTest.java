package org.folio.rest.impl;

import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;

import static org.folio.repository.holdings.status.HoldingsStatusAuditTableConstants.HOLDINGS_STATUS_AUDIT_TABLE;
import static org.folio.repository.holdings.status.HoldingsStatusTableConstants.HOLDINGS_STATUS_TABLE;
import static org.folio.repository.holdings.status.TransactionIdTableConstants.TRANSACTION_ID_TABLE;
import static org.folio.repository.kbcredentials.KbCredentialsTableConstants.KB_CREDENTIALS_TABLE_NAME;
import static org.folio.rest.jaxrs.model.LoadStatusNameEnum.COMPLETED;
import static org.folio.service.holdings.HoldingConstants.HOLDINGS_SERVICE_ADDRESS;
import static org.folio.service.holdings.HoldingConstants.LOAD_FACADE_ADDRESS;
import static org.folio.service.holdings.HoldingConstants.SAVE_HOLDINGS_ACTION;
import static org.folio.service.holdings.HoldingConstants.SNAPSHOT_CREATED_ACTION;
import static org.folio.service.holdings.HoldingConstants.SNAPSHOT_FAILED_ACTION;
import static org.folio.test.util.TestUtil.STUB_TENANT;
import static org.folio.test.util.TestUtil.mockGet;
import static org.folio.test.util.TestUtil.mockGetWithBody;
import static org.folio.test.util.TestUtil.mockResponseList;
import static org.folio.test.util.TestUtil.readFile;
import static org.folio.util.HoldingsTestUtil.addHolding;
import static org.folio.util.KBTestUtil.clearDataFromTable;
import static org.folio.util.KBTestUtil.interceptAndContinue;
import static org.folio.util.KBTestUtil.interceptAndStop;
import static org.folio.util.KBTestUtil.setupDefaultKBConfiguration;
import static org.folio.util.KbCredentialsTestUtil.STUB_TOKEN_HEADER;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import com.github.tomakehurst.wiremock.matching.RegexPattern;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import com.github.tomakehurst.wiremock.matching.UrlPathPattern;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.DeliveryContext;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;

import org.folio.holdingsiq.model.Configuration;
import org.folio.holdingsiq.model.HoldingsDownloadTransaction;
import org.folio.holdingsiq.model.HoldingsTransactionIdsList;
import org.folio.holdingsiq.model.TransactionId;
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
import org.folio.util.TransactionIdTestUtil;

@RunWith(VertxUnitRunner.class)
public class TransactionLoadHoldingsImplTest extends WireMockTestBase {
  static final String HOLDINGS_POST_HOLDINGS_ENDPOINT = "/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/reports/holdings";
  static final String HOLDINGS_POST_DELTA_REPORT_ENDPOINT = "/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/reports/holdings/deltas";
  static final String HOLDINGS_GET_ENDPOINT = "/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/reports/holdings/transactions/%s";
  static final String DELTA_GET_ENDPOINT = "/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/reports/holdings/deltas/%s";
  static final String HOLDINGS_TRANSACTIONS_ENDPOINT = "/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/reports/holdings/transactions";
  static final String HOLDINGS_STATUS_ENDPOINT = "/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/reports/holdings/transactions/%s/status";
  static final String DELTA_STATUS_ENDPOINT = "/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/reports/holdings/deltas/%s/status";
  static final String LOAD_HOLDINGS_ENDPOINT = "loadHoldings";
  private static final String PREVIOUS_TRANSACTION_ID = "abcd3ab0-da4b-4a1f-a004-a9d323e54cde";
  private static final String TRANSACTION_ID = "84113ab0-da4b-4a1f-a004-a9d686e54811";
  private static final String DELTA_ID = "7e3537a0-3f30-4ef8-9470-dd0a87ac1066";
  private static final String DELETED_HOLDING_ID = "123356-3157070-19412030";
  private static final String UPDATED_HOLDING_ID = "123357-3157072-19412032";
  private static final String ADDED_HOLDING_ID = "36-7191-2435667";
  private static final int TIMEOUT = 60000;
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
  private Handler<DeliveryContext<LoadHoldingsMessage>> interceptor;

  @BeforeClass
  public static void setUpClass(TestContext context) {
    System.setProperty("holdings.load.implementation.qualifier", "TransactionLoadServiceFacade");
    WireMockTestBase.setUpClass(context);
  }

  @AfterClass
  public static void tearDownPropertiesAfterClass(){
    System.clearProperty("holdings.load.implementation.qualifier");
  }

  public static void handleStatusChange(LoadStatusNameEnum status, HoldingsStatusRepositoryImpl repositorySpy, Consumer<Void> handler) {
    doAnswer(invocationOnMock -> {
      @SuppressWarnings("unchecked")
      CompletableFuture<Void> future = (CompletableFuture<Void>) invocationOnMock.callRealMethod();
      return future.thenAccept(handler);
    }).when(repositorySpy).update(
      argThat(argument -> argument.getData().getAttributes().getStatus().getName() == status), anyString());
  }

  @Before
  public void setUp() throws Exception {
    super.setUp();
    MockitoAnnotations.initMocks(this);
    stubConfiguration = Configuration.builder()
      .apiKey(STUB_API_KEY)
      .customerId(STUB_CUSTOMER_ID)
      .url(getWiremockUrl())
      .build();

    clearDataFromTable(vertx, HOLDINGS_STATUS_TABLE);
    HoldingsStatusUtil.insertStatusNotStarted(vertx);

    clearDataFromTable(vertx, HOLDINGS_STATUS_AUDIT_TABLE);
    clearDataFromTable(vertx, TRANSACTION_ID_TABLE);

    clearDataFromTable(vertx, KB_CREDENTIALS_TABLE_NAME);
    setupDefaultKBConfiguration(getWiremockUrl(), vertx);
  }

  @After
  public void tearDown() {
    if (interceptor != null) {
      vertx.eventBus().removeOutboundInterceptor(interceptor);
    }
  }

  @Test
  public void shouldSaveHoldings(TestContext context) throws IOException, URISyntaxException {
    runPostHoldingsWithMocks(context);

    final List<HoldingInfoInDB> holdingsList = HoldingsTestUtil.getHoldings(vertx);
    assertThat(holdingsList.size(), Matchers.notNullValue());
  }

  @Test
  public void shouldSaveHoldingsWhenPreviousTransactionExpired(TestContext context) throws IOException, URISyntaxException {
    TransactionIdTestUtil.addTransactionId(vertx, PREVIOUS_TRANSACTION_ID);
    runPostHoldingsWithMocks(context);

    final List<HoldingInfoInDB> holdingsList = HoldingsTestUtil.getHoldings(vertx);
    assertThat(holdingsList.size(), Matchers.notNullValue());
  }

  @Test
  public void shouldUpdateHoldingsWithDeltas(TestContext context) throws IOException, URISyntaxException {
    addHolding(vertx, Json.decodeValue(readFile("responses/kb-ebsco/holdings/custom-holding.json"),
        HoldingInfoInDB.class), Instant.now());
    addHolding(vertx, Json.decodeValue(readFile("responses/kb-ebsco/holdings/custom-holding2.json"),
        HoldingInfoInDB.class), Instant.now());
    TransactionIdTestUtil.addTransactionId(vertx, PREVIOUS_TRANSACTION_ID);

    HoldingsDownloadTransaction previousTransaction = HoldingsDownloadTransaction.builder().creationDate(Instant.now()
        .minus(500, ChronoUnit.HOURS).toString()).transactionId(PREVIOUS_TRANSACTION_ID).build();
    mockTransactionList(Collections.singletonList(previousTransaction));
    mockPostHoldings();
    mockGet(new EqualToPattern(getStatusEndpoint(PREVIOUS_TRANSACTION_ID)),
        "responses/rmapi/holdings/status/get-transaction-status-completed.json");
    mockGet(new EqualToPattern(getStatusEndpoint(TRANSACTION_ID)),
        "responses/rmapi/holdings/status/get-transaction-status-completed.json");
    mockPostDeltaReport();
    mockGet(new EqualToPattern(getDeltaReportStatusEndpoint()),
        "responses/rmapi/holdings/status/get-delta-report-status-completed.json");
    mockGet(new EqualToPattern(getDeltaEndpoint()), "responses/rmapi/holdings/delta/get-delta.json");

    Async async = context.async();
    handleStatusChange(COMPLETED, holdingsStatusRepository, o -> async.complete());

    postWithStatus(LOAD_HOLDINGS_ENDPOINT, "", SC_NO_CONTENT, STUB_TOKEN_HEADER);

    async.await(TIMEOUT);

    Map<String, HoldingInfoInDB> holdings = HoldingsTestUtil.getHoldings(vertx).stream().collect(Collectors.toMap(
        this::getHoldingsId, Function.identity()));

    assertFalse(holdings.containsKey(DELETED_HOLDING_ID));

    HoldingInfoInDB updatedHolding = holdings.get(UPDATED_HOLDING_ID);
    assertEquals("Test Title Updated", updatedHolding.getPublicationTitle());
    assertEquals("Test one Press Updated", updatedHolding.getPublisherName());
    assertEquals("Book", updatedHolding.getResourceType());

    HoldingInfoInDB addedHolding = holdings.get(ADDED_HOLDING_ID);
    assertEquals("Added test title", addedHolding.getPublicationTitle());
    assertEquals("Added test publisher", addedHolding.getPublisherName());
    assertEquals("Book", addedHolding.getResourceType());
  }

  @Test
  public void shouldRetryCreationOfSnapshotWhenItFails(TestContext context) throws IOException, URISyntaxException {
    mockEmptyTransactionList();

    stubFor(
      post(new UrlPathPattern(new EqualToPattern(HOLDINGS_POST_HOLDINGS_ENDPOINT), false))
        .willReturn(new ResponseDefinitionBuilder()
          .withBody(Json.encode(createTransactionId()))
          .withStatus(202)));
    ResponseDefinitionBuilder failedResponse = new ResponseDefinitionBuilder().withStatus(500);
    ResponseDefinitionBuilder successfulResponse = new ResponseDefinitionBuilder()
      .withBody(readFile("responses/rmapi/holdings/status/get-transaction-status-completed.json"));
    mockResponseList(new UrlPathPattern(new EqualToPattern(getStatusEndpoint()), false),
      failedResponse,
      successfulResponse,
      successfulResponse);

    Async async = context.async();
    interceptor = interceptAndStop(HOLDINGS_SERVICE_ADDRESS, SNAPSHOT_CREATED_ACTION, message -> async.complete());
    vertx.eventBus().addOutboundInterceptor(interceptor);

    postWithStatus(LOAD_HOLDINGS_ENDPOINT, "", SC_NO_CONTENT, STUB_TOKEN_HEADER);

    async.await(TIMEOUT);
    assertTrue(async.isSucceeded());
  }

  @Test
  public void shouldStopRetryingAfterMultipleFailures(TestContext context) throws IOException, URISyntaxException {
    mockEmptyTransactionList();
    mockGet(new EqualToPattern(getStatusEndpoint()), "responses/rmapi/holdings/status/get-transaction-status-completed.json");

    UrlPathPattern urlPattern = new UrlPathPattern(new EqualToPattern(HOLDINGS_POST_HOLDINGS_ENDPOINT), false);
    stubFor(
      post(urlPattern)
        .willReturn(new ResponseDefinitionBuilder()
          .withStatus(500)));

    Async async = context.async(TEST_SNAPSHOT_RETRY_COUNT);
    interceptor = interceptAndContinue(HOLDINGS_SERVICE_ADDRESS, SNAPSHOT_FAILED_ACTION, o -> async.countDown());
    vertx.eventBus().addOutboundInterceptor(interceptor);

    postWithStatus(LOAD_HOLDINGS_ENDPOINT, "", SC_NO_CONTENT, STUB_TOKEN_HEADER);

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
    mockEmptyTransactionList();
    mockGet(new EqualToPattern(getStatusEndpoint()), "responses/rmapi/holdings/status/get-transaction-status-completed.json");

    stubFor(
      post(new UrlPathPattern(new EqualToPattern(HOLDINGS_POST_HOLDINGS_ENDPOINT), false))
        .willReturn(new ResponseDefinitionBuilder()
          .withBody(Json.encode(createTransactionId()))
          .withStatus(202)));

    ResponseDefinitionBuilder successfulResponse = new ResponseDefinitionBuilder()
      .withBody(readFile("responses/rmapi/holdings/holdings/get-holdings.json"))
      .withStatus(200);
    ResponseDefinitionBuilder failedResponse = new ResponseDefinitionBuilder().withStatus(500);
    mockResponseList(new UrlPathPattern(new EqualToPattern(getHoldingsEndpoint()), false),
      successfulResponse,
      failedResponse,
      failedResponse,
      successfulResponse
    );

    int firstTryPages = 1;
    int secondTryPages = 2;
    Async async = context.async(firstTryPages + secondTryPages);
    interceptor = interceptAndStop(HOLDINGS_SERVICE_ADDRESS, SAVE_HOLDINGS_ACTION, message -> async.countDown());
    vertx.eventBus().addOutboundInterceptor(interceptor);

    postWithStatus(LOAD_HOLDINGS_ENDPOINT, "", SC_NO_CONTENT, STUB_TOKEN_HEADER);

    async.await(TIMEOUT);
    assertTrue(async.isSucceeded());
  }

  @Test
  public void shouldSendSaveHoldingsEventForEachLoadedPage(TestContext context) throws IOException, URISyntaxException {
    mockEmptyTransactionList();
    mockGet(new EqualToPattern(getStatusEndpoint()), "responses/rmapi/holdings/status/get-transaction-status-completed.json");
    mockGet(new RegexPattern(getHoldingsEndpoint()), "responses/rmapi/holdings/holdings/get-holdings.json");

    List<HoldingsMessage> messages = new ArrayList<>();
    Async async = context.async(EXPECTED_LOADED_PAGES);
    interceptor = interceptAndStop(HOLDINGS_SERVICE_ADDRESS, SAVE_HOLDINGS_ACTION,
      message -> {
        messages.add(((JsonObject) message.body()).getJsonObject("holdings").mapTo(HoldingsMessage.class));
        async.countDown();
      });
    vertx.eventBus().addOutboundInterceptor(interceptor);

    LoadServiceFacade proxy = LoadServiceFacade.createProxy(vertx, LOAD_FACADE_ADDRESS);
    proxy.loadHoldings(new LoadHoldingsMessage(stubConfiguration, STUB_TENANT, 5001, 2, TRANSACTION_ID, null));

    async.await(TIMEOUT);
    assertEquals(2, messages.size());
    assertEquals(STUB_HOLDINGS_TITLE, messages.get(0).getHoldingList().get(0).getPublicationTitle());
  }

  @Test
  public void shouldRetryLoadingPageWhenPageFails(TestContext context) throws IOException, URISyntaxException {
    mockEmptyTransactionList();
    Async async = context.async();
    handleStatusChange(COMPLETED, holdingsStatusRepository, o -> async.complete());
    mockGet(new EqualToPattern(getStatusEndpoint()),
      "responses/rmapi/holdings/status/get-transaction-status-completed-one-page.json");

    mockPostHoldings();
    mockResponseList(new UrlPathPattern(new EqualToPattern(getHoldingsEndpoint()), false),
      new ResponseDefinitionBuilder().withStatus(SC_INTERNAL_SERVER_ERROR),
      new ResponseDefinitionBuilder()
        .withBody(readFile("responses/rmapi/holdings/holdings/get-holdings.json"))
    );
    postWithStatus(LOAD_HOLDINGS_ENDPOINT, "", SC_NO_CONTENT, STUB_TOKEN_HEADER);
    async.await(TIMEOUT);
    assertTrue(async.isSucceeded());
  }

  private void runPostHoldingsWithMocks(TestContext context) throws IOException, URISyntaxException {
    Async async = context.async();
    handleStatusChange(COMPLETED, holdingsStatusRepository, o -> async.complete());

    mockEmptyTransactionList();
    mockGet(new EqualToPattern(getStatusEndpoint()), "responses/rmapi/holdings/status/get-transaction-status-completed.json");
    mockPostHoldings();
    mockGet(new RegexPattern(getHoldingsEndpoint()), "responses/rmapi/holdings/holdings/get-holdings.json");

    postWithStatus(LOAD_HOLDINGS_ENDPOINT, "", SC_NO_CONTENT, STUB_TOKEN_HEADER);

    async.await(TIMEOUT);
  }

  private void mockEmptyTransactionList() {
    mockTransactionList(Collections.emptyList());
  }

  private void mockTransactionList(List<HoldingsDownloadTransaction> transactionIds) {
    HoldingsTransactionIdsList emptyTransactionList = HoldingsTransactionIdsList.builder().holdingsDownloadTransactionIds(transactionIds).build();
    mockGetWithBody(new EqualToPattern(HOLDINGS_TRANSACTIONS_ENDPOINT), Json.encode(emptyTransactionList));
  }

  private String getHoldingsEndpoint() {
    return String.format(HOLDINGS_GET_ENDPOINT, TRANSACTION_ID);
  }

  private String getDeltaEndpoint() {
    return String.format(DELTA_GET_ENDPOINT, DELTA_ID);
  }

  private void mockPostHoldings() {
    StringValuePattern urlPattern = new EqualToPattern(HOLDINGS_POST_HOLDINGS_ENDPOINT);
    stubFor(post(new UrlPathPattern(urlPattern, false))
      .willReturn(new ResponseDefinitionBuilder()
        .withBody(Json.encode(createTransactionId()))
        .withStatus(202)));
  }

  private void mockPostDeltaReport() {
    StringValuePattern urlPattern = new EqualToPattern(HOLDINGS_POST_DELTA_REPORT_ENDPOINT);
    stubFor(post(new UrlPathPattern(urlPattern, false))
      .willReturn(new ResponseDefinitionBuilder()
        .withBody(DELTA_ID)
        .withStatus(202)));
  }

  private TransactionId createTransactionId() {
    return TransactionId.builder().transactionId(TRANSACTION_ID).build();
  }

  private String getStatusEndpoint() {
    return getStatusEndpoint(TRANSACTION_ID);
  }

  private String getStatusEndpoint(String id) {
    return String.format(HOLDINGS_STATUS_ENDPOINT, id);
  }

  private String getDeltaReportStatusEndpoint() {
    return String.format(DELTA_STATUS_ENDPOINT, DELTA_ID);
  }
  private String getHoldingsId(HoldingInfoInDB holding) {
    return holding.getVendorId() + "-" + holding.getPackageId() + "-" + holding.getTitleId();
  }
}
