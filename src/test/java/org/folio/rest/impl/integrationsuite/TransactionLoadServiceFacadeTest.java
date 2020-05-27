package org.folio.rest.impl.integrationsuite;

import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.junit.Assert.assertTrue;

import static org.folio.repository.kbcredentials.KbCredentialsTableConstants.KB_CREDENTIALS_TABLE_NAME;
import static org.folio.rest.impl.ProxiesTestData.STUB_CREDENTILS_ID;
import static org.folio.rest.impl.RmApiConstants.RMAPI_POST_TRANSACTIONS_HOLDINGS_URL;
import static org.folio.rest.impl.RmApiConstants.RMAPI_TRANSACTIONS_URL;
import static org.folio.rest.impl.RmApiConstants.RMAPI_TRANSACTION_STATUS_URL;
import static org.folio.service.holdings.AbstractLoadServiceFacade.HOLDINGS_STATUS_TIME_FORMATTER;
import static org.folio.service.holdings.HoldingConstants.HOLDINGS_SERVICE_ADDRESS;
import static org.folio.service.holdings.HoldingConstants.SNAPSHOT_CREATED_ACTION;
import static org.folio.test.util.TestUtil.STUB_TENANT;
import static org.folio.test.util.TestUtil.mockGetWithBody;
import static org.folio.test.util.TestUtil.mockResponseList;
import static org.folio.test.util.TestUtil.readFile;
import static org.folio.test.util.TestUtil.readJsonFile;
import static org.folio.util.HoldingsRetryStatusTestUtil.insertRetryStatus;
import static org.folio.util.HoldingsStatusUtil.insertStatusNotStarted;
import static org.folio.util.KBTestUtil.clearDataFromTable;
import static org.folio.util.KBTestUtil.interceptAndStop;
import static org.folio.util.KbCredentialsTestUtil.STUB_CREDENTIALS_NAME;
import static org.folio.util.KbCredentialsTestUtil.insertKbCredentials;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import com.github.tomakehurst.wiremock.matching.UrlPathPattern;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.DeliveryContext;
import io.vertx.core.json.Json;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;

import org.folio.holdingsiq.model.Configuration;
import org.folio.holdingsiq.model.HoldingsDownloadTransaction;
import org.folio.holdingsiq.model.HoldingsLoadTransactionStatus;
import org.folio.holdingsiq.model.HoldingsTransactionIdsList;
import org.folio.holdingsiq.model.TransactionId;
import org.folio.rest.impl.WireMockTestBase;
import org.folio.service.holdings.TransactionLoadServiceFacade;
import org.folio.service.holdings.message.ConfigurationMessage;
import org.folio.service.holdings.message.LoadHoldingsMessage;

@RunWith(VertxUnitRunner.class)
public class TransactionLoadServiceFacadeTest extends WireMockTestBase {

  private static final String TRANSACTION_ID = "84113ab0-da4b-4a1f-a004-a9d686e54811";
  private static final int TIMEOUT = 60000;

  @Autowired
  TransactionLoadServiceFacade loadServiceFacade;

  private Configuration stubConfiguration;

  private Handler<DeliveryContext<LoadHoldingsMessage>> interceptor;

  @Before
  public void setUp() throws Exception {
    super.setUp();
    stubConfiguration = Configuration.builder()
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
  public void shouldCreateSnapshotOnInitialStatusNone(TestContext context) throws IOException, URISyntaxException {

    Async async = context.async();
    mockEmptyTransactionList();

    mockResponseList(new UrlPathPattern(new EqualToPattern(getStatusEndpoint(TRANSACTION_ID)),false),
      new ResponseDefinitionBuilder().withBody(readFile("responses/rmapi/holdings/status/get-transaction-status-completed.json"))
    );
    mockPostHoldings();

    interceptor = interceptAndStop(HOLDINGS_SERVICE_ADDRESS, SNAPSHOT_CREATED_ACTION,
      message -> async.complete());
    vertx.eventBus().addOutboundInterceptor(interceptor);

    loadServiceFacade.createSnapshot(new ConfigurationMessage(stubConfiguration, STUB_CREDENTILS_ID, STUB_TENANT));

    async.await(TIMEOUT);
    assertTrue(async.isSucceeded());
  }

  @Test
  public void shouldCreateSnapshotUsingExistingTransactionIfItIsInProgress(TestContext context) throws IOException, URISyntaxException {

    Async async = context.async();
    mockGetWithBody(new EqualToPattern(RMAPI_TRANSACTIONS_URL), readFile("responses/rmapi/holdings/status/get-transaction-list-in-progress.json"));

    mockResponseList(new UrlPathPattern(new EqualToPattern(getStatusEndpoint(TRANSACTION_ID)),false),
      new ResponseDefinitionBuilder().withBody(readFile("responses/rmapi/holdings/status/get-transaction-status-completed.json"))
    );
    mockPostHoldings();

    interceptor = interceptAndStop(HOLDINGS_SERVICE_ADDRESS, SNAPSHOT_CREATED_ACTION,
      message -> async.complete());
    vertx.eventBus().addOutboundInterceptor(interceptor);

    loadServiceFacade.createSnapshot(new ConfigurationMessage(stubConfiguration, STUB_CREDENTILS_ID, STUB_TENANT));

    async.await(TIMEOUT);
    assertTrue(async.isSucceeded());
  }

  @Test
  public void shouldNotCreateSnapshotIfItWasRecentlyCreated(TestContext context) throws IOException, URISyntaxException {
    Async async = context.async();

    String now = HOLDINGS_STATUS_TIME_FORMATTER.format(LocalDateTime.now(ZoneOffset.UTC));
    HoldingsTransactionIdsList idList = readJsonFile("responses/rmapi/holdings/status/get-transaction-list.json", HoldingsTransactionIdsList.class);
    HoldingsDownloadTransaction firstTransaction = idList.getHoldingsDownloadTransactionIds().get(0);
    idList.getHoldingsDownloadTransactionIds().set(0, firstTransaction.toBuilder().creationDate(now).build());
    HoldingsLoadTransactionStatus status = readJsonFile("responses/rmapi/holdings/status/get-transaction-status-completed.json", HoldingsLoadTransactionStatus.class)
      .toBuilder().creationDate(now).build();
    mockGetWithBody(new EqualToPattern(RMAPI_TRANSACTIONS_URL), Json.encode(idList));
    mockGetWithBody(new EqualToPattern(getStatusEndpoint(TRANSACTION_ID)), Json.encode(status));
    mockPostHoldings();
    interceptor = interceptAndStop(HOLDINGS_SERVICE_ADDRESS, SNAPSHOT_CREATED_ACTION,
      message -> async.complete());
    vertx.eventBus().addOutboundInterceptor(interceptor);

    loadServiceFacade.createSnapshot(new ConfigurationMessage(stubConfiguration, STUB_CREDENTILS_ID, STUB_TENANT));

    async.await(TIMEOUT);

    WireMock.verify(0, postRequestedFor(new UrlPathPattern(new EqualToPattern(RMAPI_POST_TRANSACTIONS_HOLDINGS_URL), false)));
  }

  private void mockPostHoldings() {
    stubFor(post(new UrlPathPattern(new EqualToPattern(RMAPI_POST_TRANSACTIONS_HOLDINGS_URL), false))
      .willReturn(new ResponseDefinitionBuilder()
        .withBody(Json.encode(createTransactionId(TRANSACTION_ID)))
        .withStatus(202)));
  }

  private void mockEmptyTransactionList() {
    HoldingsTransactionIdsList emptyTransactionList = HoldingsTransactionIdsList.builder().holdingsDownloadTransactionIds(Collections.emptyList()).build();
    mockGetWithBody(new EqualToPattern(RMAPI_TRANSACTIONS_URL), Json.encode(emptyTransactionList));
  }

  private String getStatusEndpoint(String transactionId) {
    return String.format(RMAPI_TRANSACTION_STATUS_URL, transactionId);
  }

  private TransactionId createTransactionId(String transactionId) {
    return TransactionId.builder().transactionId(transactionId).build();
  }

  public void setupDefaultLoadKBConfiguration() {
    insertKbCredentials(STUB_CREDENTILS_ID, getWiremockUrl(), STUB_CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);
    insertStatusNotStarted(STUB_CREDENTILS_ID, vertx);
    insertRetryStatus(STUB_CREDENTILS_ID, vertx);
  }

  private void tearDownHoldingsData() {
    clearDataFromTable(vertx, KB_CREDENTIALS_TABLE_NAME);
  }
}
