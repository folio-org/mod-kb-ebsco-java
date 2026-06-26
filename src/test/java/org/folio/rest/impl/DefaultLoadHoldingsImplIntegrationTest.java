package org.folio.rest.impl;

import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.folio.repository.holdings.status.HoldingsLoadingStatusFactory.getStatusCompleted;
import static org.folio.repository.holdings.status.HoldingsLoadingStatusFactory.getStatusLoadingHoldings;
import static org.folio.repository.kbcredentials.KbCredentialsTableConstants.KB_CREDENTIALS_TABLE_NAME;
import static org.folio.rest.util.DateTimeUtil.POSTGRES_TIMESTAMP_FORMATTER;
import static org.folio.rest.util.DateTimeUtil.POSTGRES_TIMESTAMP_OLD_FORMATTER;
import static org.folio.service.holdings.HoldingConstants.CREATE_SNAPSHOT_ACTION;
import static org.folio.service.holdings.HoldingConstants.HOLDINGS_SERVICE_ADDRESS;
import static org.folio.service.holdings.HoldingConstants.LOAD_FACADE_ADDRESS;
import static org.folio.service.holdings.HoldingConstants.SAVE_HOLDINGS_ACTION;
import static org.folio.service.holdings.HoldingConstants.SNAPSHOT_CREATED_ACTION;
import static org.folio.service.holdings.HoldingConstants.SNAPSHOT_FAILED_ACTION;
import static org.folio.util.HoldingsRetryStatusTestUtil.insertRetryStatus;
import static org.folio.util.HoldingsStatusAuditTestUtil.saveStatusAudit;
import static org.folio.util.HoldingsStatusUtil.PROCESS_ID;
import static org.folio.util.HoldingsStatusUtil.saveStatus;
import static org.folio.util.HoldingsStatusUtil.saveStatusNotStarted;
import static org.folio.util.KbCredentialsTestUtil.saveKbCredentials;
import static org.folio.util.TestUtil.STUB_TENANT;
import static org.folio.util.TestUtil.clearDataFromTable;
import static org.folio.util.TestUtil.interceptAndContinue;
import static org.folio.util.TestUtil.interceptAndStop;
import static org.folio.util.TestUtil.readFile;
import static org.folio.util.TestUtil.result;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.UrlPathPattern;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.DeliveryContext;
import io.vertx.core.json.JsonObject;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import lombok.SneakyThrows;
import org.folio.holdingsiq.model.Configuration;
import org.folio.repository.holdings.status.HoldingsStatusRepositoryImpl;
import org.folio.repository.holdings.status.retry.RetryStatusRepository;
import org.folio.rest.jaxrs.model.LoadStatusAttributes;
import org.folio.rest.jaxrs.model.LoadStatusNameDetailEnum;
import org.folio.rest.jaxrs.model.LoadStatusNameEnum;
import org.folio.service.holdings.HoldingsService;
import org.folio.service.holdings.LoadServiceFacade;
import org.folio.service.holdings.message.HoldingsMessage;
import org.folio.service.holdings.message.LoadHoldingsMessage;
import org.folio.util.HoldingsStatusAuditTestUtil;
import org.folio.util.HoldingsStatusUtil;
import org.folio.util.HoldingsTestUtil;
import org.folio.util.IntegrationTestBase;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

class DefaultLoadHoldingsImplIntegrationTest extends IntegrationTestBase {

  public static final String HOLDINGS_LOAD_URL = "/eholdings/loading/kb-credentials";
  public static final String HOLDINGS_LOAD_BY_ID_URL = HOLDINGS_LOAD_URL + "/" + STUB_CREDENTIALS_ID;

  // RM API responses
  private static final String RMAPI_RESPONSE_HOLDINGS_STATUS_COMPLETED =
    "responses/rmapi/holdings/status/get-status-completed.json";
  private static final String RMAPI_RESPONSE_HOLDINGS_STATUS_COMPLETED_ONE_PAGE =
    "responses/rmapi/holdings/status/get-status-completed-one-page.json";
  private static final String RMAPI_RESPONSE_HOLDINGS =
    "responses/rmapi/holdings/holdings/get-holdings.json";

  private static final int TIMEOUT = 180000;
  private static final int EXPECTED_LOADED_PAGES = 2;
  private static final int TEST_SNAPSHOT_RETRY_COUNT = 2;
  private static final String STUB_HOLDINGS_TITLE = "java-test-one";

  @Autowired
  HoldingsService holdingsService;
  @Autowired
  HoldingsStatusRepositoryImpl holdingsStatusRepository;
  @Autowired
  RetryStatusRepository retryStatusRepository;
  private Configuration stubConfiguration;
  private Handler<DeliveryContext<LoadHoldingsMessage>> interceptor;

  @BeforeEach
  void setUp() {
    stubConfiguration = Configuration.builder()
      .apiKey(STUB_API_KEY)
      .customerId(STUB_CUSTOMER_ID)
      .url(getWiremockUrl())
      .build();
    holdingsStatusRepository = spy(holdingsStatusRepository);
    ReflectionTestUtils.setField(holdingsService, "holdingsStatusRepository", holdingsStatusRepository);
  }

  @AfterEach
  void tearDown() {
    if (interceptor != null) {
      vertx.eventBus().removeOutboundInterceptor(interceptor);
    }
    tearDownHoldingsData();
  }

  @Test
  void shouldSaveHoldings() {
    setupDefaultLoadKbConfiguration();
    runPostHoldingsWithMocks();

    var holdingsList = HoldingsTestUtil.getHoldings(vertx);
    assertFalse(holdingsList.isEmpty());
  }

  @Test
  void shouldSaveMultiHoldings() {
    setupDefaultLoadKbConfiguration();
    var latch = new CountDownLatch(1);
    handleStatusCompleted(holdingsStatusRepository, o -> latch.countDown());

    mockGet(WireMock.equalTo(holdingsStatusRmApi()), readFile(RMAPI_RESPONSE_HOLDINGS_STATUS_COMPLETED));
    mockPostHoldings();
    mockGet(WireMock.matching(postHoldingsRmApi()), readFile(RMAPI_RESPONSE_HOLDINGS));

    postWithStatus(HOLDINGS_LOAD_URL, "", SC_NO_CONTENT);

    assertLatch(latch);

    var holdingsList = HoldingsTestUtil.getHoldings(vertx);
    assertFalse(holdingsList.isEmpty());
  }

  @Test
  void shouldSaveMultiHoldingsWhenSnapshotNotCreated() {
    setupDefaultLoadKbConfiguration();
    var latch = new CountDownLatch(1);
    handleStatusCompleted(holdingsStatusRepository, o -> latch.countDown());

    var errorResponse = new ResponseDefinitionBuilder().withStatus(500);
    var emptyResponse = new ResponseDefinitionBuilder().withBody("");
    var successfulResponse = new ResponseDefinitionBuilder()
      .withBody(readFile(RMAPI_RESPONSE_HOLDINGS_STATUS_COMPLETED));

    mockResponseList(new UrlPathPattern(WireMock.equalTo(holdingsStatusRmApi()), false),
      errorResponse, emptyResponse, successfulResponse);

    mockPostHoldings();
    mockGet(WireMock.matching(postHoldingsRmApi()), readFile(RMAPI_RESPONSE_HOLDINGS));

    postWithStatus(HOLDINGS_LOAD_URL, "", SC_NO_CONTENT);

    assertLatch(latch);

    var holdingsList = HoldingsTestUtil.getHoldings(vertx);
    assertFalse(holdingsList.isEmpty());
  }

  @Test
  void shouldNotStartLoadingWhenStatusInProgress() {
    saveKbCredentials(STUB_CREDENTIALS_ID, getWiremockUrl(), vertx);
    saveStatus(STUB_CREDENTIALS_ID, getStatusLoadingHoldings(1000, 500, 10, 5), PROCESS_ID, vertx);
    interceptor = interceptAndStop(LOAD_FACADE_ADDRESS, CREATE_SNAPSHOT_ACTION, message -> { });
    vertx.eventBus().addOutboundInterceptor(interceptor);
    var statusCode = postWithStatus(HOLDINGS_LOAD_BY_ID_URL, "", SC_CONFLICT).statusCode();
    assertEquals(SC_CONFLICT, statusCode);
  }

  @Test
  void shouldStartLoadingWithOldDateFormat() {
    saveKbCredentials(STUB_CREDENTIALS_ID, getWiremockUrl(), vertx);

    var dateTime = OffsetDateTime.now().minusDays(6);
    var status = getStatusLoadingHoldings(1000, 500, 10, 5);
    status.getData().getAttributes().setStarted(dateTime.format(POSTGRES_TIMESTAMP_OLD_FORMATTER));
    status.getData().getAttributes().setUpdated(dateTime.format(POSTGRES_TIMESTAMP_OLD_FORMATTER));
    saveStatus(STUB_CREDENTIALS_ID, status, PROCESS_ID, vertx);

    insertRetryStatus(STUB_CREDENTIALS_ID, vertx);
    runPostHoldingsWithMocks();

    var holdingsList = HoldingsTestUtil.getHoldings(vertx);
    assertFalse(holdingsList.isEmpty());
  }

  @Test
  void shouldStartLoadingWhenStatusInProgressAndStartedMoreThen5DaysBefore() {
    saveKbCredentials(STUB_CREDENTIALS_ID, getWiremockUrl(), vertx);

    var dateTime = OffsetDateTime.now().minusDays(6);
    var statusLoadingHoldings = getStatusLoadingHoldings(1000, 500, 10, 5);
    statusLoadingHoldings.getData().getAttributes().setStarted(dateTime.format(POSTGRES_TIMESTAMP_FORMATTER));
    statusLoadingHoldings.getData().getAttributes().setUpdated(dateTime.format(POSTGRES_TIMESTAMP_FORMATTER));
    saveStatus(STUB_CREDENTIALS_ID, statusLoadingHoldings, PROCESS_ID, vertx);

    insertRetryStatus(STUB_CREDENTIALS_ID, vertx);
    runPostHoldingsWithMocks();

    var holdingsList = HoldingsTestUtil.getHoldings(vertx);
    assertFalse(holdingsList.isEmpty());
  }

  @Test
  void shouldSaveStatusChangesToAuditTable() {
    setupDefaultLoadKbConfiguration();

    runPostHoldingsWithMocks();
    var attributes = HoldingsStatusAuditTestUtil.getRecords(vertx)
      .stream().map(status -> status.getData().getAttributes()).toList();

    assertThat(attributes, containsInAnyOrder(
        statusEquals(LoadStatusNameEnum.NOT_STARTED),
        statusEquals(LoadStatusNameEnum.NOT_STARTED),
        statusEquals(LoadStatusNameEnum.IN_PROGRESS, LoadStatusNameDetailEnum.POPULATING_STAGING_AREA, null),
        statusEquals(LoadStatusNameEnum.IN_PROGRESS, LoadStatusNameDetailEnum.LOADING_HOLDINGS, 0),
        statusEquals(LoadStatusNameEnum.IN_PROGRESS, LoadStatusNameDetailEnum.LOADING_HOLDINGS, 1),
        statusEquals(LoadStatusNameEnum.IN_PROGRESS, LoadStatusNameDetailEnum.LOADING_HOLDINGS, 2),
        statusEquals(LoadStatusNameEnum.COMPLETED)
      )
    );
  }

  @Test
  void shouldClearOldStatusChangeRecords() {
    setupDefaultLoadKbConfiguration();
    saveStatusAudit(STUB_CREDENTIALS_ID, getStatusCompleted(1000),
      OffsetDateTime.now().minusDays(60), vertx);

    interceptor = interceptAndStop(LOAD_FACADE_ADDRESS, CREATE_SNAPSHOT_ACTION, message -> { });
    vertx.eventBus().addOutboundInterceptor(interceptor);
    postWithStatus(HOLDINGS_LOAD_BY_ID_URL, "", SC_NO_CONTENT);

    var attributes = HoldingsStatusAuditTestUtil.getRecords(vertx)
      .stream().map(status -> status.getData().getAttributes()).toList();
    assertEquals(3, attributes.size());
    assertThat(attributes, containsInAnyOrder(
      statusEquals(LoadStatusNameEnum.NOT_STARTED), //insert
      statusEquals(LoadStatusNameEnum.NOT_STARTED), //delete
      statusEquals(LoadStatusNameEnum.IN_PROGRESS, LoadStatusNameDetailEnum.POPULATING_STAGING_AREA, null)
    ));
  }

  @Test
  void shouldStartLoadingWhenStatusInProgressAndProcessTimedOut() {
    setupDefaultLoadKbConfiguration();
    var status = getStatusLoadingHoldings(1000, 500, 10, 5);

    var dateTime = OffsetDateTime.now().minusDays(10);
    status.getData().getAttributes().setUpdated(POSTGRES_TIMESTAMP_FORMATTER.format(dateTime));
    saveStatus(STUB_CREDENTIALS_ID, status, PROCESS_ID, vertx);

    interceptor = interceptAndStop(LOAD_FACADE_ADDRESS, CREATE_SNAPSHOT_ACTION, message -> { });
    vertx.eventBus().addOutboundInterceptor(interceptor);
    var statusCode = postWithStatus(HOLDINGS_LOAD_BY_ID_URL, "", SC_NO_CONTENT).statusCode();
    assertEquals(SC_NO_CONTENT, statusCode);
  }

  @Test
  void shouldRetryCreationOfSnapshotWhenItFails() {
    setupDefaultLoadKbConfiguration();

    mockPost(WireMock.equalTo(postHoldingsRmApi()), "", 202);
    var failedResponse = new ResponseDefinitionBuilder().withStatus(500);
    var successfulResponse = new ResponseDefinitionBuilder()
      .withBody(readFile(RMAPI_RESPONSE_HOLDINGS_STATUS_COMPLETED));
    mockResponseList(new UrlPathPattern(WireMock.equalTo(holdingsStatusRmApi()), false),
      failedResponse, successfulResponse, successfulResponse);

    var latch = new CountDownLatch(1);
    interceptor = interceptAndStop(HOLDINGS_SERVICE_ADDRESS, SNAPSHOT_CREATED_ACTION, message -> latch.countDown());
    vertx.eventBus().addOutboundInterceptor(interceptor);

    postWithStatus(HOLDINGS_LOAD_BY_ID_URL, "", SC_NO_CONTENT);

    assertLatch(latch);
  }

  @Test
  void shouldStopRetryingAfterMultipleFailures() {
    setupDefaultLoadKbConfiguration();

    mockGet(WireMock.equalTo(holdingsStatusRmApi()), readFile(RMAPI_RESPONSE_HOLDINGS_STATUS_COMPLETED));
    mockPost(WireMock.equalTo(postHoldingsRmApi()), "", 500);

    var latch = new CountDownLatch(TEST_SNAPSHOT_RETRY_COUNT);
    interceptor = interceptAndContinue(HOLDINGS_SERVICE_ADDRESS, SNAPSHOT_FAILED_ACTION, o -> latch.countDown());
    vertx.eventBus().addOutboundInterceptor(interceptor);

    postWithStatus(HOLDINGS_LOAD_BY_ID_URL, "", SC_NO_CONTENT);

    assertLatch(latch);

    var status =
      result(retryStatusRepository.findByCredentialsId(UUID.fromString(STUB_CREDENTIALS_ID), STUB_TENANT));
    var timerExists = vertx.cancelTimer(status.getTimerId());
    assertEquals(0, status.getRetryAttemptsLeft());
    assertFalse(timerExists);

    verifyPost(WireMock.equalTo(postHoldingsRmApi()), TEST_SNAPSHOT_RETRY_COUNT);
  }

  @Test
  void shouldRetryLoadingHoldingsFromStartWhenPageFailsToLoad() {
    setupDefaultLoadKbConfiguration();
    mockGet(WireMock.equalTo(holdingsStatusRmApi()), readFile(RMAPI_RESPONSE_HOLDINGS_STATUS_COMPLETED));

    mockPost(WireMock.equalTo(postHoldingsRmApi()), "", 202);

    var successResponse = new ResponseDefinitionBuilder()
      .withBody(readFile(RMAPI_RESPONSE_HOLDINGS))
      .withStatus(200);
    var failResponse = new ResponseDefinitionBuilder().withStatus(500);
    mockResponseList(urlPathEqualTo(postHoldingsRmApi()), successResponse, failResponse, failResponse, successResponse);

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
    mockGet(WireMock.equalTo(holdingsStatusRmApi()), readFile(RMAPI_RESPONSE_HOLDINGS_STATUS_COMPLETED));
    mockGet(WireMock.matching(postHoldingsRmApi()), readFile(RMAPI_RESPONSE_HOLDINGS));

    var messages = new ArrayList<HoldingsMessage>();
    var latch = new CountDownLatch(EXPECTED_LOADED_PAGES);
    interceptor = interceptAndStop(HOLDINGS_SERVICE_ADDRESS, SAVE_HOLDINGS_ACTION,
      message -> {
        messages.add(((JsonObject) message.body()).getJsonObject("holdings").mapTo(HoldingsMessage.class));
        latch.countDown();
      });
    vertx.eventBus().addOutboundInterceptor(interceptor);

    var proxy = LoadServiceFacade.createProxy(vertx, LOAD_FACADE_ADDRESS);
    proxy.loadHoldings(
      new LoadHoldingsMessage(stubConfiguration, STUB_CREDENTIALS_ID, STUB_TENANT, 5001, 2, null, null));

    assertLatch(latch);
    assertEquals(2, messages.size());
    assertEquals(STUB_HOLDINGS_TITLE, messages.getFirst().getHoldingList().getFirst().getPublicationTitle());
  }

  @Test
  void shouldRetryLoadingPageWhenPageFails() {
    setupDefaultLoadKbConfiguration();
    var latch = new CountDownLatch(1);
    handleStatusCompleted(holdingsStatusRepository, o -> latch.countDown());
    mockGet(WireMock.equalTo(holdingsStatusRmApi()), readFile(RMAPI_RESPONSE_HOLDINGS_STATUS_COMPLETED_ONE_PAGE));

    mockPostHoldings();
    mockResponseList(new UrlPathPattern(WireMock.equalTo(postHoldingsRmApi()), false),
      new ResponseDefinitionBuilder().withStatus(SC_INTERNAL_SERVER_ERROR),
      new ResponseDefinitionBuilder().withBody(readFile(RMAPI_RESPONSE_HOLDINGS))
    );
    postWithStatus(HOLDINGS_LOAD_BY_ID_URL, "", SC_NO_CONTENT);
    assertLatch(latch);
  }

  @Test
  @SuppressWarnings("checkstyle:methodLength")
  void shouldRetryCreationOfSnapshotAndUpdateHoldingsStatusWhenItFails() {
    setupDefaultLoadKbConfiguration();
    var latch = new CountDownLatch(1);
    handleStatusCompleted(holdingsStatusRepository, o -> latch.countDown());
    var failResponse = new ResponseDefinitionBuilder().withStatus(500);
    var successResponse = new ResponseDefinitionBuilder()
      .withBody(readFile(RMAPI_RESPONSE_HOLDINGS_STATUS_COMPLETED_ONE_PAGE));
    mockResponseList(urlPathEqualTo(holdingsStatusRmApi()), failResponse, successResponse, successResponse);
    mockPostHoldings();
    postWithStatus(HOLDINGS_LOAD_BY_ID_URL, "", SC_NO_CONTENT);
    mockGet(WireMock.matching(postHoldingsRmApi()), readFile(RMAPI_RESPONSE_HOLDINGS));

    assertLatch(latch);
    var attributes = HoldingsStatusAuditTestUtil.getRecords(vertx)
      .stream().map(status -> status.getData().getAttributes()).toList();
    assertThat(attributes, containsInAnyOrder(
      statusEquals(LoadStatusNameEnum.NOT_STARTED),
      statusEquals(LoadStatusNameEnum.NOT_STARTED),
      statusEquals(LoadStatusNameEnum.FAILED),
      statusEquals(LoadStatusNameEnum.FAILED),
      statusEquals(LoadStatusNameEnum.IN_PROGRESS, LoadStatusNameDetailEnum.POPULATING_STAGING_AREA, null),
      statusEquals(LoadStatusNameEnum.IN_PROGRESS, LoadStatusNameDetailEnum.POPULATING_STAGING_AREA, null),
      statusEquals(LoadStatusNameEnum.IN_PROGRESS, LoadStatusNameDetailEnum.LOADING_HOLDINGS, 0),
      statusEquals(LoadStatusNameEnum.IN_PROGRESS, LoadStatusNameDetailEnum.LOADING_HOLDINGS, 1),
      statusEquals(LoadStatusNameEnum.COMPLETED)
    ));

    var holdingsLoadingStatus = HoldingsStatusUtil.getStatus(STUB_CREDENTIALS_ID, vertx);
    assertThat(holdingsLoadingStatus.getData().getAttributes(), statusEquals(LoadStatusNameEnum.COMPLETED));
  }

  static void handleStatusCompleted(HoldingsStatusRepositoryImpl repositorySpy,
                                    Consumer<Void> handler) {
    doAnswer(invocationOnMock -> {
      @SuppressWarnings("unchecked")
      CompletableFuture<Void> future = (CompletableFuture<Void>) invocationOnMock.callRealMethod();
      return future.thenAccept(handler);
    }).when(repositorySpy).update(
      argThat(argument -> argument.getData().getAttributes().getStatus().getName()
                          == LoadStatusNameEnum.COMPLETED), any(UUID.class),
      anyString());
  }

  static void assertLatch(CountDownLatch latch) {
    try {
      assertTrue(latch.await(TIMEOUT, TimeUnit.MILLISECONDS));
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  private void setupDefaultLoadKbConfiguration() {
    saveKbCredentials(STUB_CREDENTIALS_ID, getWiremockUrl(), vertx);
    saveStatusNotStarted(STUB_CREDENTIALS_ID, vertx);
    insertRetryStatus(STUB_CREDENTIALS_ID, vertx);
  }

  @SneakyThrows
  private void runPostHoldingsWithMocks() {
    var latch = new CountDownLatch(1);
    handleStatusCompleted(holdingsStatusRepository, o -> latch.countDown());

    mockGet(WireMock.equalTo(holdingsStatusRmApi()), readFile(RMAPI_RESPONSE_HOLDINGS_STATUS_COMPLETED));
    mockPostHoldings();
    mockGet(WireMock.matching(postHoldingsRmApi()), readFile(RMAPI_RESPONSE_HOLDINGS));

    postWithStatus(HOLDINGS_LOAD_BY_ID_URL, "", SC_NO_CONTENT);

    assertLatch(latch);
  }

  private void mockPostHoldings() {
    mockPost(WireMock.equalTo(postHoldingsRmApi()), "", 202);
  }

  private Matcher<LoadStatusAttributes> statusEquals(LoadStatusNameEnum status) {
    return statusEquals(status, null, null);
  }

  private Matcher<LoadStatusAttributes> statusEquals(LoadStatusNameEnum status, LoadStatusNameDetailEnum detail,
                                                     Integer importedPages) {
    return allOf(
      hasProperty("status", hasProperty("name", equalTo(status))),
      hasProperty("status", hasProperty("detail", equalTo(detail))),
      hasProperty("importedPages", equalTo(importedPages))
    );
  }

  private void tearDownHoldingsData() {
    clearDataFromTable(vertx, KB_CREDENTIALS_TABLE_NAME);
  }
}
