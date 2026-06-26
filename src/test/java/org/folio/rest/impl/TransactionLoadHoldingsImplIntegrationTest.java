package org.folio.rest.impl;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.folio.repository.kbcredentials.KbCredentialsTableConstants.KB_CREDENTIALS_TABLE_NAME;
import static org.folio.rest.impl.DefaultLoadHoldingsImplIntegrationTest.HOLDINGS_LOAD_BY_ID_URL;
import static org.folio.rest.impl.DefaultLoadHoldingsImplIntegrationTest.assertLatch;
import static org.folio.service.holdings.HoldingConstants.HOLDINGS_SERVICE_ADDRESS;
import static org.folio.service.holdings.HoldingConstants.LOAD_FACADE_ADDRESS;
import static org.folio.service.holdings.HoldingConstants.SAVE_HOLDINGS_ACTION;
import static org.folio.service.holdings.HoldingConstants.SNAPSHOT_CREATED_ACTION;
import static org.folio.service.holdings.HoldingConstants.SNAPSHOT_FAILED_ACTION;
import static org.folio.util.HoldingsRetryStatusTestUtil.insertRetryStatus;
import static org.folio.util.HoldingsStatusUtil.saveStatusNotStarted;
import static org.folio.util.KbCredentialsTestUtil.saveKbCredentials;
import static org.folio.util.TestUtil.STUB_TENANT;
import static org.folio.util.TestUtil.clearDataFromTable;
import static org.folio.util.TestUtil.interceptAndContinue;
import static org.folio.util.TestUtil.interceptAndStop;
import static org.folio.util.TestUtil.readFile;
import static org.folio.util.TestUtil.result;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.spy;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.DeliveryContext;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import org.folio.holdingsiq.model.Configuration;
import org.folio.holdingsiq.model.HoldingsDownloadTransaction;
import org.folio.holdingsiq.model.HoldingsTransactionIdsList;
import org.folio.holdingsiq.model.TransactionId;
import org.folio.repository.holdings.DbHoldingInfo;
import org.folio.repository.holdings.status.HoldingsStatusRepositoryImpl;
import org.folio.repository.holdings.status.retry.RetryStatusRepository;
import org.folio.service.holdings.HoldingsService;
import org.folio.service.holdings.LoadServiceFacade;
import org.folio.service.holdings.message.HoldingsMessage;
import org.folio.service.holdings.message.LoadHoldingsMessage;
import org.folio.util.HoldingsTestUtil;
import org.folio.util.IntegrationTestBase;
import org.folio.util.TransactionIdTestUtil;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith({TransactionLoadHoldingsImplIntegrationTest.SystemPropertyExtension.class})
class TransactionLoadHoldingsImplIntegrationTest extends IntegrationTestBase {

  private static final String PREVIOUS_TRANSACTION_ID = "abcd3ab0-da4b-4a1f-a004-a9d323e54cde";
  private static final String TRANSACTION_ID = "84113ab0-da4b-4a1f-a004-a9d686e54811";
  private static final String DELTA_ID = "7e3537a0-3f30-4ef8-9470-dd0a87ac1066";
  private static final String DELETED_HOLDING_ID = "123356-3157070-19412030";
  private static final String UPDATED_HOLDING_ID = "123357-3157072-19412032";
  private static final String ADDED_HOLDING_ID = "36-7191-2435667";
  private static final int EXPECTED_LOADED_PAGES = 2;
  private static final int TEST_SNAPSHOT_RETRY_COUNT = 2;
  private static final String STUB_HOLDINGS_TITLE = "java-test-one";

  // RM API responses
  private static final String RMAPI_RESPONSE_TRANSACTION_STATUS_COMPLETED =
    "responses/rmapi/holdings/status/get-transaction-status-completed.json";
  private static final String RMAPI_RESPONSE_TRANSACTION_STATUS_COMPLETED_ONE_PAGE =
    "responses/rmapi/holdings/status/get-transaction-status-completed-one-page.json";
  private static final String RMAPI_RESPONSE_DELTA_REPORT_STATUS_COMPLETED =
    "responses/rmapi/holdings/status/get-delta-report-status-completed.json";
  private static final String RMAPI_RESPONSE_HOLDINGS =
    "responses/rmapi/holdings/holdings/get-holdings.json";
  private static final String RMAPI_RESPONSE_DELTA =
    "responses/rmapi/holdings/delta/get-delta.json";

  // KB-EBSCO holdings fixtures
  private static final String KB_EBSCO_CUSTOM_HOLDING =
    "responses/kb-ebsco/holdings/custom-holding.json";
  private static final String KB_EBSCO_CUSTOM_HOLDING_2 =
    "responses/kb-ebsco/holdings/custom-holding2.json";

  @Autowired
  private HoldingsService holdingsService;
  @Autowired
  private HoldingsStatusRepositoryImpl holdingsStatusRepository;
  @Autowired
  private RetryStatusRepository retryStatusRepository;
  private Configuration configuration;
  private Handler<DeliveryContext<LoadHoldingsMessage>> interceptor;

  @BeforeEach
  void setUp() {
    configuration = Configuration.builder()
      .apiKey(STUB_API_KEY)
      .customerId(STUB_CUSTOMER_ID)
      .url(getWiremockUrl())
      .build();
    setupDefaultLoadKbConfiguration();

    holdingsStatusRepository = spy(holdingsStatusRepository);
    ReflectionTestUtils.setField(holdingsService, "holdingsStatusRepository", holdingsStatusRepository);
  }

  @AfterEach
  void tearDown() {
    if (interceptor != null) {
      vertx.eventBus().removeOutboundInterceptor(interceptor);
    }
    clearDataFromTable(vertx, KB_CREDENTIALS_TABLE_NAME);
  }

  @Test
  void shouldSaveHoldings() {
    runPostHoldingsWithMocks();

    var holdingsList = HoldingsTestUtil.getHoldings(vertx);
    assertFalse(holdingsList.isEmpty());
  }

  @Test
  void shouldSaveHoldingsWhenPreviousTransactionExpired() {
    TransactionIdTestUtil.addTransactionId(STUB_CREDENTIALS_ID, PREVIOUS_TRANSACTION_ID, vertx);
    runPostHoldingsWithMocks();

    var holdingsList = HoldingsTestUtil.getHoldings(vertx);
    assertFalse(holdingsList.isEmpty());
  }

  @Test
  @SuppressWarnings("checkstyle:MethodLength")
  void shouldUpdateHoldingsWithDeltas() {
    HoldingsTestUtil.saveHolding(STUB_CREDENTIALS_ID,
      Json.decodeValue(readFile(KB_EBSCO_CUSTOM_HOLDING), DbHoldingInfo.class),
      OffsetDateTime.now(), vertx);
    HoldingsTestUtil.saveHolding(STUB_CREDENTIALS_ID,
      Json.decodeValue(readFile(KB_EBSCO_CUSTOM_HOLDING_2), DbHoldingInfo.class),
      OffsetDateTime.now(), vertx);
    TransactionIdTestUtil.addTransactionId(STUB_CREDENTIALS_ID, PREVIOUS_TRANSACTION_ID, vertx);

    var previousTransaction = HoldingsDownloadTransaction.builder()
      .creationDate(OffsetDateTime.now().minusHours(500).toString())
      .transactionId(PREVIOUS_TRANSACTION_ID)
      .build();
    mockTransactionList(singletonList(previousTransaction));
    mockPost(equalTo(postTransactionsHoldingsRmApi()), Json.encode(createTransactionId()), 202);
    mockGet(equalTo(transactionStatusRmApi(PREVIOUS_TRANSACTION_ID)),
      readFile(RMAPI_RESPONSE_TRANSACTION_STATUS_COMPLETED));
    mockGet(equalTo(transactionStatusRmApi(TRANSACTION_ID)), readFile(RMAPI_RESPONSE_TRANSACTION_STATUS_COMPLETED));
    mockPost(equalTo(deltasRmApi()), DELTA_ID, 202);
    mockGet(equalTo(deltaStatusRmApi(DELTA_ID)), readFile(RMAPI_RESPONSE_DELTA_REPORT_STATUS_COMPLETED));
    mockGet(equalTo(deltaByIdRmApi(DELTA_ID)), readFile(RMAPI_RESPONSE_DELTA));

    var latch = new CountDownLatch(1);
    DefaultLoadHoldingsImplIntegrationTest.handleStatusCompleted(holdingsStatusRepository, o -> latch.countDown());

    postWithStatus(HOLDINGS_LOAD_BY_ID_URL, "", SC_NO_CONTENT);

    assertLatch(latch);

    var holdings = HoldingsTestUtil.getHoldings(vertx)
      .stream()
      .collect(Collectors.toMap(this::getHoldingsId, Function.identity()));

    assertFalse(holdings.containsKey(DELETED_HOLDING_ID));

    var updatedHolding = holdings.get(UPDATED_HOLDING_ID);
    assertEquals("Test Title Updated", updatedHolding.getPublicationTitle());
    assertEquals("Test one Press Updated", updatedHolding.getPublisherName());
    assertEquals("Book", updatedHolding.getResourceType());

    var addedHolding = holdings.get(ADDED_HOLDING_ID);
    assertEquals("Added test title", addedHolding.getPublicationTitle());
    assertEquals("Added test publisher", addedHolding.getPublisherName());
    assertEquals("Book", addedHolding.getResourceType());
  }

  @Test
  void shouldRetryCreationOfSnapshotWhenItFails() {
    mockEmptyTransactionList();
    mockPost(equalTo(postTransactionsHoldingsRmApi()), Json.encode(createTransactionId()), 202);
    var failedResponse = aResponse().withStatus(500);
    var successfulResponse = aResponse().withBody(readFile(RMAPI_RESPONSE_TRANSACTION_STATUS_COMPLETED));
    mockResponseList(urlPathEqualTo(transactionStatusRmApi(TRANSACTION_ID)),
      failedResponse,
      successfulResponse,
      successfulResponse);

    var latch = new CountDownLatch(1);
    interceptor = interceptAndStop(HOLDINGS_SERVICE_ADDRESS, SNAPSHOT_CREATED_ACTION, message -> latch.countDown());
    vertx.eventBus().addOutboundInterceptor(interceptor);

    postWithStatus(HOLDINGS_LOAD_BY_ID_URL, "", SC_NO_CONTENT);

    assertLatch(latch);
  }

  @Test
  void shouldStopRetryingAfterMultipleFailures() {
    mockEmptyTransactionList();
    mockGet(equalTo(transactionStatusRmApi(TRANSACTION_ID)), readFile(RMAPI_RESPONSE_TRANSACTION_STATUS_COMPLETED));
    mockPost(equalTo(postTransactionsHoldingsRmApi()), "", 500);

    var latch = new CountDownLatch(TEST_SNAPSHOT_RETRY_COUNT);
    interceptor = interceptAndContinue(HOLDINGS_SERVICE_ADDRESS, SNAPSHOT_FAILED_ACTION, o -> latch.countDown());
    vertx.eventBus().addOutboundInterceptor(interceptor);

    postWithStatus(HOLDINGS_LOAD_BY_ID_URL, "", SC_NO_CONTENT);

    assertLatch(latch);

    var status = result(retryStatusRepository.findByCredentialsId(UUID.fromString(STUB_CREDENTIALS_ID), STUB_TENANT));
    var timerExists = vertx.cancelTimer(status.getTimerId());
    assertEquals(0, status.getRetryAttemptsLeft());
    assertFalse(timerExists);

    verifyPost(equalTo(postTransactionsHoldingsRmApi()), TEST_SNAPSHOT_RETRY_COUNT);
  }

  @Test
  void shouldRetryLoadingHoldingsFromStartWhenPageFailsToLoad() {
    mockEmptyTransactionList();
    mockGet(equalTo(transactionStatusRmApi(TRANSACTION_ID)), readFile(RMAPI_RESPONSE_TRANSACTION_STATUS_COMPLETED));
    mockPost(equalTo(postTransactionsHoldingsRmApi()), Json.encode(createTransactionId()), 202);

    var successResponse = new ResponseDefinitionBuilder()
      .withBody(readFile(RMAPI_RESPONSE_HOLDINGS))
      .withStatus(200);
    var failResponse = new ResponseDefinitionBuilder().withStatus(500);
    mockResponseList(urlPathEqualTo(transactionByIdRmApi(TRANSACTION_ID)),
      successResponse, failResponse, failResponse, successResponse);

    var firstTryPages = 1;
    var secondTryPages = 2;
    var latch = new CountDownLatch(firstTryPages + secondTryPages);
    interceptor = interceptAndStop(HOLDINGS_SERVICE_ADDRESS, SAVE_HOLDINGS_ACTION, message -> latch.countDown());
    vertx.eventBus().addOutboundInterceptor(interceptor);

    postWithStatus(HOLDINGS_LOAD_BY_ID_URL, "", SC_NO_CONTENT);

    assertLatch(latch);
  }

  @Test
  void shouldSendSaveHoldingsEventForEachLoadedPage() {
    mockEmptyTransactionList();
    mockGet(equalTo(transactionStatusRmApi(TRANSACTION_ID)), readFile(RMAPI_RESPONSE_TRANSACTION_STATUS_COMPLETED));
    mockGet(matching(transactionByIdRmApi(TRANSACTION_ID)), readFile(RMAPI_RESPONSE_HOLDINGS));

    var messages = new ArrayList<HoldingsMessage>();
    var latch = new CountDownLatch(EXPECTED_LOADED_PAGES);
    interceptor = interceptAndStop(HOLDINGS_SERVICE_ADDRESS, SAVE_HOLDINGS_ACTION,
      message -> {
        messages.add(((JsonObject) message.body()).getJsonObject("holdings").mapTo(HoldingsMessage.class));
        latch.countDown();
      });
    vertx.eventBus().addOutboundInterceptor(interceptor);

    var proxy = LoadServiceFacade.createProxy(vertx, LOAD_FACADE_ADDRESS);
    var message = new LoadHoldingsMessage(this.configuration, STUB_CREDENTIALS_ID,
      STUB_TENANT, 5001, 2, TRANSACTION_ID, null);
    proxy.loadHoldings(message);

    assertLatch(latch);
    assertEquals(2, messages.size());
    assertEquals(STUB_HOLDINGS_TITLE, messages.getFirst().getHoldingList().getFirst().getPublicationTitle());
  }

  @Test
  void shouldRetryLoadingPageWhenPageFails() {
    mockEmptyTransactionList();
    var latch = new CountDownLatch(1);
    DefaultLoadHoldingsImplIntegrationTest.handleStatusCompleted(holdingsStatusRepository, o -> latch.countDown());
    mockGet(equalTo(transactionStatusRmApi(TRANSACTION_ID)),
      readFile(RMAPI_RESPONSE_TRANSACTION_STATUS_COMPLETED_ONE_PAGE));

    mockPost(equalTo(postTransactionsHoldingsRmApi()), Json.encode(createTransactionId()), 202);
    mockResponseList(urlPathEqualTo(transactionByIdRmApi(TRANSACTION_ID)),
      new ResponseDefinitionBuilder().withStatus(SC_INTERNAL_SERVER_ERROR),
      new ResponseDefinitionBuilder().withBody(readFile(RMAPI_RESPONSE_HOLDINGS))
    );
    postWithStatus(HOLDINGS_LOAD_BY_ID_URL, "", SC_NO_CONTENT);
    assertLatch(latch);
  }

  private void setupDefaultLoadKbConfiguration() {
    saveKbCredentials(STUB_CREDENTIALS_ID, getWiremockUrl(), vertx);
    saveStatusNotStarted(STUB_CREDENTIALS_ID, vertx);
    insertRetryStatus(STUB_CREDENTIALS_ID, vertx);
  }

  @SneakyThrows
  private void runPostHoldingsWithMocks() {
    var latch = new CountDownLatch(1);
    DefaultLoadHoldingsImplIntegrationTest.handleStatusCompleted(holdingsStatusRepository, o -> latch.countDown());

    mockEmptyTransactionList();
    mockGet(equalTo(transactionStatusRmApi(TRANSACTION_ID)), readFile(RMAPI_RESPONSE_TRANSACTION_STATUS_COMPLETED));
    mockPost(equalTo(postTransactionsHoldingsRmApi()), Json.encode(createTransactionId()), 202);
    mockGet(matching(transactionByIdRmApi(TRANSACTION_ID)), readFile(RMAPI_RESPONSE_HOLDINGS));

    postWithStatus(HOLDINGS_LOAD_BY_ID_URL, "", SC_NO_CONTENT);

    assertLatch(latch);
  }

  private void mockEmptyTransactionList() {
    mockTransactionList(emptyList());
  }

  private void mockTransactionList(List<HoldingsDownloadTransaction> transactionIds) {
    var transactionList =
      HoldingsTransactionIdsList.builder().holdingsDownloadTransactionIds(transactionIds).build();
    mockGet(equalTo(transactionsRmApi()), Json.encode(transactionList));
  }

  private TransactionId createTransactionId() {
    return TransactionId.builder().transactionId(TRANSACTION_ID).build();
  }

  private String getHoldingsId(DbHoldingInfo holding) {
    return holding.getVendorId() + "-" + holding.getPackageId() + "-" + holding.getTitleId();
  }

  public static class SystemPropertyExtension
    implements BeforeAllCallback, AfterAllCallback {

    @Override
    public void beforeAll(@NonNull ExtensionContext context) {
      System.setProperty("holdings.load.implementation.qualifier", "transactionLoadServiceFacade");
    }

    @Override
    public void afterAll(@NonNull ExtensionContext context) {
      System.clearProperty("holdings.load.implementation.qualifier");
    }
  }
}
