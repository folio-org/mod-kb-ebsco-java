package org.folio.rest.impl.integrationsuite;

import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.folio.repository.kbcredentials.KbCredentialsTableConstants.KB_CREDENTIALS_TABLE_NAME;
import static org.folio.rest.impl.RmApiConstants.RMAPI_HOLDINGS_STATUS_URL;
import static org.folio.rest.impl.RmApiConstants.RMAPI_POST_HOLDINGS_URL;
import static org.folio.service.holdings.AbstractLoadServiceFacade.HOLDINGS_STATUS_TIME_FORMATTER;
import static org.folio.service.holdings.HoldingConstants.HOLDINGS_SERVICE_ADDRESS;
import static org.folio.service.holdings.HoldingConstants.SNAPSHOT_CREATED_ACTION;
import static org.folio.test.util.TestUtil.STUB_TENANT;
import static org.folio.test.util.TestUtil.mockGetWithBody;
import static org.folio.test.util.TestUtil.mockResponseList;
import static org.folio.test.util.TestUtil.readFile;
import static org.folio.test.util.TestUtil.readJsonFile;
import static org.folio.util.HoldingsRetryStatusTestUtil.insertRetryStatus;
import static org.folio.util.HoldingsStatusUtil.saveStatusNotStarted;
import static org.folio.util.KbCredentialsTestUtil.saveKbCredentials;
import static org.folio.util.KbTestUtil.clearDataFromTable;
import static org.folio.util.KbTestUtil.interceptAndStop;
import static org.junit.Assert.assertTrue;

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
import java.io.IOException;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.folio.holdingsiq.model.Configuration;
import org.folio.holdingsiq.model.HoldingsLoadStatus;
import org.folio.rest.impl.WireMockTestBase;
import org.folio.service.holdings.DefaultLoadServiceFacade;
import org.folio.service.holdings.message.ConfigurationMessage;
import org.folio.service.holdings.message.LoadHoldingsMessage;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;

@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@RunWith(VertxUnitRunner.class)
public class DefaultLoadServiceFacadeTest extends WireMockTestBase {

  private static final int TIMEOUT = 60000;

  @Autowired
  private DefaultLoadServiceFacade loadServiceFacade;
  private Configuration configuration;
  private Handler<DeliveryContext<LoadHoldingsMessage>> interceptor;

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    configuration = Configuration.builder()
      .apiKey(STUB_API_KEY)
      .customerId(STUB_CUSTOMER_ID)
      .url(getWiremockUrl())
      .build();
    setupDefaultLoadKbConfiguration();
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

    mockResponseList(new UrlPathPattern(new EqualToPattern(RMAPI_HOLDINGS_STATUS_URL), false),
      new ResponseDefinitionBuilder().withBody(readFile("responses/rmapi/holdings/status/get-status-none.json")),
      new ResponseDefinitionBuilder().withBody(readFile("responses/rmapi/holdings/status/get-status-completed.json"))
    );
    mockPostHoldings();

    interceptor = interceptAndStop(HOLDINGS_SERVICE_ADDRESS, SNAPSHOT_CREATED_ACTION,
      message -> async.complete());
    vertx.eventBus().addOutboundInterceptor(interceptor);

    loadServiceFacade.createSnapshot(new ConfigurationMessage(configuration, STUB_CREDENTIALS_ID, STUB_TENANT));

    async.await(TIMEOUT);
    assertTrue(async.isSucceeded());
  }

  @Test
  public void shouldNotCreateSnapshotIfItWasRecentlyCreated(TestContext context)
    throws IOException, URISyntaxException {
    Async async = context.async();

    String now = HOLDINGS_STATUS_TIME_FORMATTER.format(LocalDateTime.now(ZoneOffset.UTC));
    HoldingsLoadStatus status =
      readJsonFile("responses/rmapi/holdings/status/get-status-completed.json", HoldingsLoadStatus.class)
        .toBuilder().created(now).build();
    mockGetWithBody(new EqualToPattern(RMAPI_HOLDINGS_STATUS_URL), Json.encode(status));
    mockPostHoldings();
    interceptor = interceptAndStop(HOLDINGS_SERVICE_ADDRESS, SNAPSHOT_CREATED_ACTION,
      message -> async.complete());
    vertx.eventBus().addOutboundInterceptor(interceptor);

    loadServiceFacade.createSnapshot(new ConfigurationMessage(configuration, STUB_CREDENTIALS_ID, STUB_TENANT));

    async.await(TIMEOUT);

    WireMock.verify(0, postRequestedFor(new UrlPathPattern(new EqualToPattern(RMAPI_POST_HOLDINGS_URL), false)));
  }

  private void setupDefaultLoadKbConfiguration() {
    saveKbCredentials(STUB_CREDENTIALS_ID, getWiremockUrl(), vertx);
    saveStatusNotStarted(STUB_CREDENTIALS_ID, vertx);
    insertRetryStatus(STUB_CREDENTIALS_ID, vertx);
  }

  private void mockPostHoldings() {
    stubFor(post(new UrlPathPattern(new EqualToPattern(RMAPI_POST_HOLDINGS_URL), false))
      .willReturn(new ResponseDefinitionBuilder()
        .withBody("")
        .withStatus(202)));
  }

  private void tearDownHoldingsData() {
    clearDataFromTable(vertx, KB_CREDENTIALS_TABLE_NAME);
  }
}
