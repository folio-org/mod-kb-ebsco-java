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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;

import static org.folio.repository.kbcredentials.KbCredentialsTableConstants.KB_CREDENTIALS_TABLE_NAME;
import static org.folio.rest.impl.ProxiesTestData.STUB_CREDENTILS_ID;
import static org.folio.rest.impl.RmApiConstants.RMAPI_DELTAS_URL;
import static org.folio.rest.impl.RmApiConstants.RMAPI_DELTA_BY_ID_URL;
import static org.folio.rest.impl.RmApiConstants.RMAPI_DELTA_STATUS_URL;
import static org.folio.rest.impl.RmApiConstants.RMAPI_POST_TRANSACTIONS_HOLDINGS_URL;
import static org.folio.rest.impl.RmApiConstants.RMAPI_TRANSACTIONS_URL;
import static org.folio.rest.impl.RmApiConstants.RMAPI_TRANSACTION_BY_ID_URL;
import static org.folio.rest.impl.RmApiConstants.RMAPI_TRANSACTION_STATUS_URL;
import static org.folio.rest.impl.integrationsuite.DefaultLoadHoldingsImplTest.HOLDINGS_LOAD_BY_ID_URL;
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
import static org.folio.util.HoldingsRetryStatusTestUtil.insertRetryStatus;
import static org.folio.util.HoldingsStatusUtil.saveStatusNotStarted;
import static org.folio.util.KBTestUtil.clearDataFromTable;
import static org.folio.util.KBTestUtil.interceptAndContinue;
import static org.folio.util.KBTestUtil.interceptAndStop;
import static org.folio.util.KbCredentialsTestUtil.STUB_CREDENTIALS_NAME;
import static org.folio.util.KbCredentialsTestUtil.STUB_TOKEN_HEADER;
import static org.folio.util.KbCredentialsTestUtil.saveKbCredentials;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
import org.folio.repository.holdings.DbHoldingInfo;
import org.folio.repository.holdings.status.HoldingsStatusRepositoryImpl;
import org.folio.repository.holdings.status.retry.RetryStatusRepository;
import org.folio.rest.jaxrs.model.LoadStatusNameEnum;
import org.folio.service.holdings.HoldingsService;
import org.folio.service.holdings.LoadServiceFacade;
import org.folio.service.holdings.message.HoldingsMessage;
import org.folio.service.holdings.message.LoadHoldingsMessage;
import org.folio.util.HoldingsTestUtil;
import org.folio.util.TransactionIdTestUtil;

@RunWith(VertxUnitRunner.class)
public class TransactionLoadHoldingsImplTest extends WireMockTestBase {

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
  private static final String RMAPI_RESPONSE_TRANSACTION_STATUS_COMPLETED =
    "responses/rmapi/holdings/status/get-transaction-status-completed.json";
  private static final String RMAPI_RESPONSE_HOLDINGS = "responses/rmapi/holdings/holdings/get-holdings.json";

  @InjectMocks
  @Autowired
  HoldingsService holdingsService;
  @Spy
  @Autowired
  HoldingsStatusRepositoryImpl holdingsStatusRepository;
  @Autowired
  RetryStatusRepository retryStatusRepository;
  private Configuration configuration;
  private Handler<DeliveryContext<LoadHoldingsMessage>> interceptor;

  @BeforeClass
  public static void setUpClass(TestContext context) {
    System.setProperty("holdings.load.implementation.qualifier", "TransactionLoadServiceFacade");
    WireMockTestBase.setUpClass(context);
  }

  @AfterClass
  public static void tearDownPropertiesAfterClass() {
    System.clearProperty("holdings.load.implementation.qualifier");
  }

  public static void handleStatusChange(LoadStatusNameEnum status, HoldingsStatusRepositoryImpl repositorySpy,
                                        Consumer<Void> handler) {
    doAnswer(invocationOnMock -> {
      @SuppressWarnings("unchecked")
      CompletableFuture<Void> future = (CompletableFuture<Void>) invocationOnMock.callRealMethod();
      return future.thenAccept(handler);
    }).when(repositorySpy).update(
      argThat(argument -> argument.getData().getAttributes().getStatus().getName() == status), any(UUID.class), anyString());
  }

  @Before
  public void setUp() throws Exception {
    super.setUp();
    MockitoAnnotations.initMocks(this);
    configuration = Configuration.builder()
      .apiKey(STUB_API_KEY)
      .customerId(STUB_CUSTOMER_ID)
      .url(getWiremockUrl())
      .build();
    setupDefaultLoadKBConfiguration();
  }

  @After
  public void tearDown() {
    if (interceptor != null) {
      vertx.eventBus().removeOutboundInterceptor(interceptor);
    }
    tearDownHoldingsData();
  }

  @Test
  public void shouldSaveHoldings(TestContext context) throws IOException, URISyntaxException {

    runPostHoldingsWithMocks(context);

    final List<DbHoldingInfo> holdingsList = HoldingsTestUtil.getHoldings(vertx);
    assertThat(holdingsList.size(), Matchers.notNullValue());
  }

  @Test
  public void shouldSaveHoldingsWhenPreviousTransactionExpired(TestContext context) throws IOException, URISyntaxException {

    TransactionIdTestUtil.addTransactionId(STUB_CREDENTILS_ID, PREVIOUS_TRANSACTION_ID, vertx);
    runPostHoldingsWithMocks(context);

    final List<DbHoldingInfo> holdingsList = HoldingsTestUtil.getHoldings(vertx);
    assertThat(holdingsList.size(), Matchers.notNullValue());
  }

  @Test
  public void shouldUpdateHoldingsWithDeltas(TestContext context) throws IOException, URISyntaxException {
    HoldingsTestUtil.saveHolding(STUB_CREDENTILS_ID,
      Json.decodeValue(readFile("responses/kb-ebsco/holdings/custom-holding.json"), DbHoldingInfo.class),
      OffsetDateTime.now(), vertx);
    HoldingsTestUtil.saveHolding(STUB_CREDENTILS_ID,
      Json.decodeValue(readFile("responses/kb-ebsco/holdings/custom-holding2.json"), DbHoldingInfo.class),
      OffsetDateTime.now(), vertx);
    TransactionIdTestUtil.addTransactionId(STUB_CREDENTILS_ID, PREVIOUS_TRANSACTION_ID, vertx);

    HoldingsDownloadTransaction previousTransaction = HoldingsDownloadTransaction.builder()
      .creationDate(OffsetDateTime.now().minus(500, ChronoUnit.HOURS).toString())
      .transactionId(PREVIOUS_TRANSACTION_ID)
      .build();
    mockTransactionList(Collections.singletonList(previousTransaction));
    mockPostHoldings();
    mockGet(new EqualToPattern(getStatusEndpoint(PREVIOUS_TRANSACTION_ID)), RMAPI_RESPONSE_TRANSACTION_STATUS_COMPLETED);
    mockGet(new EqualToPattern(getStatusEndpoint(TRANSACTION_ID)), RMAPI_RESPONSE_TRANSACTION_STATUS_COMPLETED);
    mockPostDeltaReport();
    mockGet(new EqualToPattern(getDeltaReportStatusEndpoint()),
      "responses/rmapi/holdings/status/get-delta-report-status-completed.json");
    mockGet(new EqualToPattern(getDeltaEndpoint()), "responses/rmapi/holdings/delta/get-delta.json");

    Async async = context.async();
    handleStatusChange(COMPLETED, holdingsStatusRepository, o -> async.complete());

    postWithStatus(HOLDINGS_LOAD_BY_ID_URL, "", SC_NO_CONTENT, STUB_TOKEN_HEADER);

    async.await(TIMEOUT);

    Map<String, DbHoldingInfo> holdings = HoldingsTestUtil.getHoldings(vertx)
      .stream()
      .collect(Collectors.toMap(this::getHoldingsId, Function.identity()));

    assertFalse(holdings.containsKey(DELETED_HOLDING_ID));

    DbHoldingInfo updatedHolding = holdings.get(UPDATED_HOLDING_ID);
    assertEquals("Test Title Updated", updatedHolding.getPublicationTitle());
    assertEquals("Test one Press Updated", updatedHolding.getPublisherName());
    assertEquals("Book", updatedHolding.getResourceType());

    DbHoldingInfo addedHolding = holdings.get(ADDED_HOLDING_ID);
    assertEquals("Added test title", addedHolding.getPublicationTitle());
    assertEquals("Added test publisher", addedHolding.getPublisherName());
    assertEquals("Book", addedHolding.getResourceType());
  }

  @Test
  public void shouldRetryCreationOfSnapshotWhenItFails(TestContext context) throws IOException, URISyntaxException {
    mockEmptyTransactionList();

    stubFor(
      post(new UrlPathPattern(new EqualToPattern(RMAPI_POST_TRANSACTIONS_HOLDINGS_URL), false))
        .willReturn(new ResponseDefinitionBuilder()
          .withBody(Json.encode(createTransactionId()))
          .withStatus(202)));
    ResponseDefinitionBuilder failedResponse = new ResponseDefinitionBuilder().withStatus(500);
    ResponseDefinitionBuilder successfulResponse = new ResponseDefinitionBuilder()
      .withBody(readFile(RMAPI_RESPONSE_TRANSACTION_STATUS_COMPLETED));
    mockResponseList(new UrlPathPattern(new EqualToPattern(getStatusEndpoint()), false),
      failedResponse,
      successfulResponse,
      successfulResponse);

    Async async = context.async();
    interceptor = interceptAndStop(HOLDINGS_SERVICE_ADDRESS, SNAPSHOT_CREATED_ACTION, message -> async.complete());
    vertx.eventBus().addOutboundInterceptor(interceptor);

    postWithStatus(HOLDINGS_LOAD_BY_ID_URL, "", SC_NO_CONTENT, STUB_TOKEN_HEADER);

    async.await(TIMEOUT);
    assertTrue(async.isSucceeded());
  }

  @Test
  public void shouldStopRetryingAfterMultipleFailures(TestContext context) throws IOException, URISyntaxException {
    mockEmptyTransactionList();
    mockGet(new EqualToPattern(getStatusEndpoint()), RMAPI_RESPONSE_TRANSACTION_STATUS_COMPLETED);

    UrlPathPattern urlPattern = new UrlPathPattern(new EqualToPattern(RMAPI_POST_TRANSACTIONS_HOLDINGS_URL), false);
    stubFor(
      post(urlPattern)
        .willReturn(new ResponseDefinitionBuilder()
          .withStatus(500)));

    Async async = context.async(TEST_SNAPSHOT_RETRY_COUNT);
    interceptor = interceptAndContinue(HOLDINGS_SERVICE_ADDRESS, SNAPSHOT_FAILED_ACTION, o -> async.countDown());
    vertx.eventBus().addOutboundInterceptor(interceptor);

    postWithStatus(HOLDINGS_LOAD_BY_ID_URL, "", SC_NO_CONTENT, STUB_TOKEN_HEADER);

    async.await(TIMEOUT);

    Async retryStatusAsync = context.async();
    retryStatusRepository.findByCredentialsId(UUID.fromString(STUB_CREDENTILS_ID), STUB_TENANT)
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
  public void shouldRetryLoadingHoldingsFromStartWhenPageFailsToLoad(TestContext context)
    throws IOException, URISyntaxException {
    mockEmptyTransactionList();
    mockGet(new EqualToPattern(getStatusEndpoint()), RMAPI_RESPONSE_TRANSACTION_STATUS_COMPLETED);

    stubFor(
      post(new UrlPathPattern(new EqualToPattern(RMAPI_POST_TRANSACTIONS_HOLDINGS_URL), false))
        .willReturn(new ResponseDefinitionBuilder()
          .withBody(Json.encode(createTransactionId()))
          .withStatus(202)));

    ResponseDefinitionBuilder successfulResponse = new ResponseDefinitionBuilder()
      .withBody(readFile(RMAPI_RESPONSE_HOLDINGS))
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

    postWithStatus(HOLDINGS_LOAD_BY_ID_URL, "", SC_NO_CONTENT, STUB_TOKEN_HEADER);

    async.await(TIMEOUT);
    assertTrue(async.isSucceeded());
  }

  @Test
  public void shouldSendSaveHoldingsEventForEachLoadedPage(TestContext context) throws IOException, URISyntaxException {
    mockEmptyTransactionList();
    mockGet(new EqualToPattern(getStatusEndpoint()), RMAPI_RESPONSE_TRANSACTION_STATUS_COMPLETED);
    mockGet(new RegexPattern(getHoldingsEndpoint()), RMAPI_RESPONSE_HOLDINGS);

    List<HoldingsMessage> messages = new ArrayList<>();
    Async async = context.async(EXPECTED_LOADED_PAGES);
    interceptor = interceptAndStop(HOLDINGS_SERVICE_ADDRESS, SAVE_HOLDINGS_ACTION,
      message -> {
        messages.add(((JsonObject) message.body()).getJsonObject("holdings").mapTo(HoldingsMessage.class));
        async.countDown();
      });
    vertx.eventBus().addOutboundInterceptor(interceptor);

    LoadServiceFacade proxy = LoadServiceFacade.createProxy(vertx, LOAD_FACADE_ADDRESS);
    LoadHoldingsMessage message = new LoadHoldingsMessage(this.configuration, STUB_CREDENTILS_ID,
      STUB_TENANT, 5001, 2, TRANSACTION_ID, null);
    proxy.loadHoldings(message);

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
      new ResponseDefinitionBuilder().withBody(readFile(RMAPI_RESPONSE_HOLDINGS))
    );
    postWithStatus(HOLDINGS_LOAD_BY_ID_URL, "", SC_NO_CONTENT, STUB_TOKEN_HEADER);
    async.await(TIMEOUT);
    assertTrue(async.isSucceeded());
  }

  public void setupDefaultLoadKBConfiguration() {
    saveKbCredentials(STUB_CREDENTILS_ID, getWiremockUrl(), STUB_CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);
    saveStatusNotStarted(STUB_CREDENTILS_ID, vertx);
    insertRetryStatus(STUB_CREDENTILS_ID, vertx);
  }

  private void runPostHoldingsWithMocks(TestContext context) throws IOException, URISyntaxException {
    Async async = context.async();
    handleStatusChange(COMPLETED, holdingsStatusRepository, o -> async.complete());

    mockEmptyTransactionList();
    mockGet(new EqualToPattern(getStatusEndpoint()), RMAPI_RESPONSE_TRANSACTION_STATUS_COMPLETED);
    mockPostHoldings();
    mockGet(new RegexPattern(getHoldingsEndpoint()), RMAPI_RESPONSE_HOLDINGS);

    postWithStatus(HOLDINGS_LOAD_BY_ID_URL, "", SC_NO_CONTENT, STUB_TOKEN_HEADER);

    async.await(TIMEOUT);
  }

  private void mockEmptyTransactionList() {
    mockTransactionList(Collections.emptyList());
  }

  private void mockTransactionList(List<HoldingsDownloadTransaction> transactionIds) {
    HoldingsTransactionIdsList emptyTransactionList =
      HoldingsTransactionIdsList.builder().holdingsDownloadTransactionIds(transactionIds).build();
    mockGetWithBody(new EqualToPattern(RMAPI_TRANSACTIONS_URL), Json.encode(emptyTransactionList));
  }

  private String getHoldingsEndpoint() {
    return String.format(RMAPI_TRANSACTION_BY_ID_URL, TRANSACTION_ID);
  }

  private String getDeltaEndpoint() {
    return String.format(RMAPI_DELTA_BY_ID_URL, DELTA_ID);
  }

  private void mockPostHoldings() {
    StringValuePattern urlPattern = new EqualToPattern(RMAPI_POST_TRANSACTIONS_HOLDINGS_URL);
    stubFor(post(new UrlPathPattern(urlPattern, false))
      .willReturn(new ResponseDefinitionBuilder()
        .withBody(Json.encode(createTransactionId()))
        .withStatus(202)));
  }

  private void mockPostDeltaReport() {
    StringValuePattern urlPattern = new EqualToPattern(RMAPI_DELTAS_URL);
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
    return String.format(RMAPI_TRANSACTION_STATUS_URL, id);
  }

  private String getDeltaReportStatusEndpoint() {
    return String.format(RMAPI_DELTA_STATUS_URL, DELTA_ID);
  }

  private String getHoldingsId(DbHoldingInfo holding) {
    return holding.getVendorId() + "-" + holding.getPackageId() + "-" + holding.getTitleId();
  }

  private void tearDownHoldingsData() {
    clearDataFromTable(vertx, KB_CREDENTIALS_TABLE_NAME);
  }
}
