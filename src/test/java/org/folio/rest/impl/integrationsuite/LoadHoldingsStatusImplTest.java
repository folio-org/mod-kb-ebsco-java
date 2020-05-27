package org.folio.rest.impl.integrationsuite;

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

import static org.folio.repository.kbcredentials.KbCredentialsTableConstants.KB_CREDENTIALS_TABLE_NAME;
import static org.folio.rest.impl.ProxiesTestData.STUB_CREDENTILS_ID;
import static org.folio.rest.impl.RmApiConstants.RMAPI_HOLDINGS_STATUS_URL;
import static org.folio.rest.impl.RmApiConstants.RMAPI_POST_HOLDINGS_URL;
import static org.folio.rest.impl.integrationsuite.DefaultLoadHoldingsImplTest.HOLDINGS_LOAD_BY_ID_URL;
import static org.folio.rest.impl.integrationsuite.DefaultLoadHoldingsImplTest.handleStatusChange;
import static org.folio.rest.jaxrs.model.LoadStatusNameEnum.COMPLETED;
import static org.folio.rest.jaxrs.model.LoadStatusNameEnum.FAILED;
import static org.folio.service.holdings.HoldingConstants.CREATE_SNAPSHOT_ACTION;
import static org.folio.service.holdings.HoldingConstants.HOLDINGS_SERVICE_ADDRESS;
import static org.folio.service.holdings.HoldingConstants.LOAD_FACADE_ADDRESS;
import static org.folio.service.holdings.HoldingConstants.SNAPSHOT_CREATED_ACTION;
import static org.folio.service.holdings.HoldingsServiceImpl.POSTGRES_TIMESTAMP_FORMATTER;
import static org.folio.test.util.TestUtil.mockGet;
import static org.folio.test.util.TestUtil.mockResponseList;
import static org.folio.test.util.TestUtil.readFile;
import static org.folio.util.HoldingsRetryStatusTestUtil.insertRetryStatus;
import static org.folio.util.HoldingsStatusUtil.insertStatusNotStarted;
import static org.folio.util.KBTestUtil.clearDataFromTable;
import static org.folio.util.KBTestUtil.interceptAndContinue;
import static org.folio.util.KBTestUtil.interceptAndStop;
import static org.folio.util.KbCredentialsTestUtil.STUB_CREDENTIALS_NAME;
import static org.folio.util.KbCredentialsTestUtil.STUB_TOKEN_HEADER;
import static org.folio.util.KbCredentialsTestUtil.insertKbCredentials;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import com.github.tomakehurst.wiremock.matching.RegexPattern;
import com.github.tomakehurst.wiremock.matching.UrlPathPattern;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.DeliveryContext;
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
import org.folio.rest.impl.WireMockTestBase;
import org.folio.rest.jaxrs.model.HoldingsLoadingStatus;
import org.folio.service.holdings.HoldingsService;
import org.folio.service.holdings.message.LoadHoldingsMessage;

@RunWith(VertxUnitRunner.class)
public class LoadHoldingsStatusImplTest extends WireMockTestBase {

  static final String HOLDINGS_LOAD_STATUS_OLD_URL = "loadHoldings/status";
  private static final int TIMEOUT = 300;
  private static final int SNAPSHOT_RETRIES = 2;
  private List<Handler<DeliveryContext<LoadHoldingsMessage>>> interceptors = new ArrayList<>();

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
    setupDefaultLoadKBConfiguration();
  }

  @After
  public void tearDown() {
    interceptors.forEach(interceptor ->
      vertx.eventBus().removeOutboundInterceptor(interceptor));

    tearDownHoldingsData();
  }

  @Test
  public void shouldReturnStatusNotStarted() {

    final HoldingsLoadingStatus status = getWithOk(HOLDINGS_LOAD_STATUS_OLD_URL, STUB_TOKEN_HEADER).body()
      .as(HoldingsLoadingStatus.class);

    assertThat(status.getData().getAttributes().getStatus().getName().value(), equalToIgnoringWhiteSpace("Not Started"));
  }

  @Test
  public void shouldReturnStatusPopulatingStagingArea(TestContext context) throws IOException, URISyntaxException {

    mockResponseList(
      new UrlPathPattern(new EqualToPattern(RMAPI_HOLDINGS_STATUS_URL), false),
      new ResponseDefinitionBuilder()
        .withBody(readFile("responses/rmapi/holdings/status/get-status-in-progress.json"))
        .withStatus(200),
      new ResponseDefinitionBuilder()
        .withBody(readFile("responses/rmapi/holdings/status/get-status-completed.json"))
        .withStatus(200)
    );

    Async startedAsync = context.async();
    Handler<DeliveryContext<LoadHoldingsMessage>> interceptor = interceptAndContinue(LOAD_FACADE_ADDRESS, CREATE_SNAPSHOT_ACTION, message -> startedAsync.complete());
    vertx.eventBus().addOutboundInterceptor(interceptor);
    interceptors.add(interceptor);

    Async finishedAsync = context.async();
    interceptor = interceptAndStop(HOLDINGS_SERVICE_ADDRESS, SNAPSHOT_CREATED_ACTION, message -> finishedAsync.complete());
    vertx.eventBus().addOutboundInterceptor(interceptor);
    interceptors.add(interceptor);

    postWithStatus(HOLDINGS_LOAD_BY_ID_URL, "", SC_NO_CONTENT, STUB_TOKEN_HEADER);

    startedAsync.await(TIMEOUT);
    final HoldingsLoadingStatus status = getWithOk(HOLDINGS_LOAD_STATUS_OLD_URL, STUB_TOKEN_HEADER).body()
      .as(HoldingsLoadingStatus.class);
    assertThat(status.getData().getAttributes().getStatus().getDetail().value(), equalToIgnoringWhiteSpace("Populating staging area"));

    finishedAsync.await(TIMEOUT);
  }

  @Test
  public void shouldReturnStatusCompleted(TestContext context) throws IOException, URISyntaxException {

    mockGet(new EqualToPattern(RMAPI_HOLDINGS_STATUS_URL), "responses/rmapi/holdings/status/get-status-completed-one-page.json");

    stubFor(post(new UrlPathPattern(new EqualToPattern(RMAPI_POST_HOLDINGS_URL), false))
      .willReturn(new ResponseDefinitionBuilder()
        .withBody("")
        .withStatus(202)));

    mockGet(new RegexPattern(RMAPI_POST_HOLDINGS_URL), "responses/rmapi/holdings/holdings/get-holdings.json");

    Async async = context.async();
    handleStatusChange(COMPLETED, holdingsStatusRepository, o -> async.complete());

    postWithStatus(HOLDINGS_LOAD_BY_ID_URL, "", SC_NO_CONTENT, STUB_TOKEN_HEADER);
    async.await(TIMEOUT);
    final HoldingsLoadingStatus status = getWithOk(HOLDINGS_LOAD_STATUS_OLD_URL, STUB_TOKEN_HEADER).body()
      .as(HoldingsLoadingStatus.class);

    assertThat(status.getData().getType(), equalTo("status"));
    assertThat(status.getData().getAttributes().getTotalCount(), equalTo(2));
    assertThat(status.getData().getAttributes().getStatus().getName().value(), equalToIgnoringCase("Completed"));

    assertTrue(parse(status.getData().getAttributes().getStarted(), POSTGRES_TIMESTAMP_FORMATTER)
      .isBefore(parse(status.getData().getAttributes().getFinished(), POSTGRES_TIMESTAMP_FORMATTER)));
  }

  @Test
  public void shouldReturnErrorWhenRMAPIReturnsError(TestContext context) {

    mockGet(new EqualToPattern(RMAPI_HOLDINGS_STATUS_URL), SC_INTERNAL_SERVER_ERROR);

    Async finishedAsync = context.async(SNAPSHOT_RETRIES);
    handleStatusChange(FAILED, holdingsStatusRepository, o -> finishedAsync.countDown());
    postWithStatus(HOLDINGS_LOAD_BY_ID_URL, "", SC_NO_CONTENT, STUB_TOKEN_HEADER);
    finishedAsync.await(TIMEOUT);

    final HoldingsLoadingStatus status = getWithOk(HOLDINGS_LOAD_STATUS_OLD_URL, STUB_TOKEN_HEADER).body()
      .as(HoldingsLoadingStatus.class);
    assertThat(status.getData().getAttributes().getStatus().getName().value(), equalToIgnoringCase("Failed"));
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
