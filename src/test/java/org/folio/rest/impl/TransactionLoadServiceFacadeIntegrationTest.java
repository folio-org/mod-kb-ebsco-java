package org.folio.rest.impl;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static org.folio.HttpStatus.SC_ACCEPTED;
import static org.folio.repository.kbcredentials.KbCredentialsTableConstants.KB_CREDENTIALS_TABLE_NAME;
import static org.folio.service.holdings.AbstractLoadServiceFacade.HOLDINGS_STATUS_TIME_FORMATTER;
import static org.folio.service.holdings.HoldingConstants.HOLDINGS_SERVICE_ADDRESS;
import static org.folio.service.holdings.HoldingConstants.SNAPSHOT_CREATED_ACTION;
import static org.folio.util.HoldingsRetryStatusTestUtil.insertRetryStatus;
import static org.folio.util.HoldingsStatusUtil.saveStatusNotStarted;
import static org.folio.util.KbCredentialsTestUtil.saveKbCredentials;
import static org.folio.util.TestUtil.STUB_TENANT;
import static org.folio.util.TestUtil.clearDataFromTable;
import static org.folio.util.TestUtil.interceptAndStop;
import static org.folio.util.TestUtil.readFile;
import static org.folio.util.TestUtil.readJsonFile;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.vertx.core.Handler;
import io.vertx.core.eventbus.DeliveryContext;
import io.vertx.core.json.Json;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.folio.holdingsiq.model.Configuration;
import org.folio.holdingsiq.model.HoldingsLoadTransactionStatus;
import org.folio.holdingsiq.model.HoldingsTransactionIdsList;
import org.folio.holdingsiq.model.TransactionId;
import org.folio.service.holdings.TransactionLoadServiceFacade;
import org.folio.service.holdings.message.ConfigurationMessage;
import org.folio.service.holdings.message.LoadHoldingsMessage;
import org.folio.util.IntegrationTestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class TransactionLoadServiceFacadeIntegrationTest extends IntegrationTestBase {

  private static final String TRANSACTION_ID = "84113ab0-da4b-4a1f-a004-a9d686e54811";
  private static final int TIMEOUT = 60000;

  // RM API responses
  private static final String RMAPI_RESPONSE_TRANSACTION_STATUS_COMPLETED =
    "responses/rmapi/holdings/status/get-transaction-status-completed.json";
  private static final String RMAPI_RESPONSE_TRANSACTION_LIST_IN_PROGRESS =
    "responses/rmapi/holdings/status/get-transaction-list-in-progress.json";
  private static final String RMAPI_RESPONSE_TRANSACTION_LIST =
    "responses/rmapi/holdings/status/get-transaction-list.json";

  @Autowired
  private TransactionLoadServiceFacade loadServiceFacade;

  private Configuration stubConfiguration;
  private Handler<DeliveryContext<LoadHoldingsMessage>> interceptor;

  @BeforeEach
  void setUp() {
    stubConfiguration = Configuration.builder()
      .apiKey(STUB_API_KEY)
      .customerId(STUB_CUSTOMER_ID)
      .url(getWiremockUrl())
      .build();
    setupDefaultLoadKbConfiguration();
  }

  @AfterEach
  void tearDown() {
    if (interceptor != null) {
      vertx.eventBus().removeOutboundInterceptor(interceptor);
    }
    clearDataFromTable(vertx, KB_CREDENTIALS_TABLE_NAME);
  }

  @Test
  void shouldCreateSnapshotOnInitialStatusNone() throws Exception {
    mockEmptyTransactionList();
    mockGet(equalTo(transactionStatusRmApi(TRANSACTION_ID)), readFile(RMAPI_RESPONSE_TRANSACTION_STATUS_COMPLETED));
    mockPost(equalTo(postTransactionsHoldingsRmApi()), Json.encode(createTransactionId()), SC_ACCEPTED);

    var latch = new CountDownLatch(1);
    interceptor = interceptAndStop(HOLDINGS_SERVICE_ADDRESS, SNAPSHOT_CREATED_ACTION, message -> latch.countDown());
    vertx.eventBus().addOutboundInterceptor(interceptor);

    loadServiceFacade.createSnapshot(new ConfigurationMessage(stubConfiguration, STUB_CREDENTIALS_ID, STUB_TENANT));

    assertTrue(latch.await(TIMEOUT, TimeUnit.MILLISECONDS));
  }

  @Test
  void shouldCreateSnapshotUsingExistingTransactionIfItIsInProgress() throws Exception {
    mockGet(equalTo(transactionsRmApi()), readFile(RMAPI_RESPONSE_TRANSACTION_LIST_IN_PROGRESS));
    mockGet(equalTo(transactionStatusRmApi(TRANSACTION_ID)), readFile(RMAPI_RESPONSE_TRANSACTION_STATUS_COMPLETED));
    mockPost(equalTo(postTransactionsHoldingsRmApi()), Json.encode(createTransactionId()), SC_ACCEPTED);

    var latch = new CountDownLatch(1);
    interceptor = interceptAndStop(HOLDINGS_SERVICE_ADDRESS, SNAPSHOT_CREATED_ACTION, message -> latch.countDown());
    vertx.eventBus().addOutboundInterceptor(interceptor);

    loadServiceFacade.createSnapshot(new ConfigurationMessage(stubConfiguration, STUB_CREDENTIALS_ID, STUB_TENANT));

    assertTrue(latch.await(TIMEOUT, TimeUnit.MILLISECONDS));
  }

  @Test
  void shouldNotCreateSnapshotIfItWasRecentlyCreated() throws Exception {
    var now = HOLDINGS_STATUS_TIME_FORMATTER.format(LocalDateTime.now(ZoneOffset.UTC));
    var idList = readJsonFile(RMAPI_RESPONSE_TRANSACTION_LIST, HoldingsTransactionIdsList.class);
    var firstTransaction = idList.getHoldingsDownloadTransactionIds().getFirst();
    idList.getHoldingsDownloadTransactionIds().set(0, firstTransaction.toBuilder().creationDate(now).build());
    var status = readJsonFile(RMAPI_RESPONSE_TRANSACTION_STATUS_COMPLETED, HoldingsLoadTransactionStatus.class)
      .toBuilder().creationDate(now).build();
    mockGet(equalTo(transactionsRmApi()), Json.encode(idList));
    mockGet(equalTo(transactionStatusRmApi(TRANSACTION_ID)), Json.encode(status));
    mockPost(equalTo(postTransactionsHoldingsRmApi()), Json.encode(createTransactionId()), SC_ACCEPTED);

    var latch = new CountDownLatch(1);
    interceptor = interceptAndStop(HOLDINGS_SERVICE_ADDRESS, SNAPSHOT_CREATED_ACTION, message -> latch.countDown());
    vertx.eventBus().addOutboundInterceptor(interceptor);

    loadServiceFacade.createSnapshot(new ConfigurationMessage(stubConfiguration, STUB_CREDENTIALS_ID, STUB_TENANT));

    latch.await(TIMEOUT, TimeUnit.MILLISECONDS);

    verifyPost(equalTo(postTransactionsHoldingsRmApi()), 0);
  }

  private void mockEmptyTransactionList() {
    var transactionList = HoldingsTransactionIdsList.builder()
      .holdingsDownloadTransactionIds(Collections.emptyList()).build();
    mockGet(equalTo(transactionsRmApi()), Json.encode(transactionList));
  }

  private TransactionId createTransactionId() {
    return TransactionId.builder().transactionId(TRANSACTION_ID).build();
  }

  private void setupDefaultLoadKbConfiguration() {
    saveKbCredentials(STUB_CREDENTIALS_ID, getWiremockUrl(), vertx);
    saveStatusNotStarted(STUB_CREDENTIALS_ID, vertx);
    insertRetryStatus(STUB_CREDENTIALS_ID, vertx);
  }
}
