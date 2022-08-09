package org.folio.rest.impl.integrationsuite;

import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static java.time.ZonedDateTime.parse;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.folio.repository.kbcredentials.KbCredentialsTableConstants.KB_CREDENTIALS_TABLE_NAME;
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
import static org.folio.util.HoldingsStatusUtil.saveStatusNotStarted;
import static org.folio.util.KbCredentialsTestUtil.STUB_CREDENTIALS_NAME;
import static org.folio.util.KbCredentialsTestUtil.STUB_TOKEN_HEADER;
import static org.folio.util.KbCredentialsTestUtil.saveKbCredentials;
import static org.folio.util.KbTestUtil.clearDataFromTable;
import static org.folio.util.KbTestUtil.interceptAndContinue;
import static org.folio.util.KbTestUtil.interceptAndStop;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertTrue;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import com.github.tomakehurst.wiremock.matching.RegexPattern;
import com.github.tomakehurst.wiremock.matching.UrlPathPattern;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.DeliveryContext;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.folio.repository.holdings.status.HoldingsStatusRepositoryImpl;
import org.folio.rest.impl.WireMockTestBase;
import org.folio.rest.jaxrs.model.HoldingsLoadingStatus;
import org.folio.rest.jaxrs.model.JsonapiError;
import org.folio.rest.jaxrs.model.LoadStatusData;
import org.folio.rest.jaxrs.model.LoadStatusNameDetailEnum;
import org.folio.rest.jaxrs.model.LoadStatusNameEnum;
import org.folio.service.holdings.HoldingsService;
import org.folio.service.holdings.message.LoadHoldingsMessage;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;

@RunWith(VertxUnitRunner.class)
public class LoadHoldingsStatusImplTest extends WireMockTestBase {

  private static final String HOLDINGS_LOAD_STATUS_BY_ID_URL = "/eholdings/loading/kb-credentials/%s/status";
  private static final String STUB_HOLDINGS_LOAD_STATUS_BY_ID_URL =
    String.format(HOLDINGS_LOAD_STATUS_BY_ID_URL, STUB_CREDENTIALS_ID);
  private static final int TIMEOUT = 300;
  private static final int SNAPSHOT_RETRIES = 2;

  @InjectMocks
  @Autowired
  public HoldingsService holdingsService;
  @Spy
  @Autowired
  public HoldingsStatusRepositoryImpl holdingsStatusRepository;

  private final List<Handler<DeliveryContext<LoadHoldingsMessage>>> interceptors = new ArrayList<>();

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    MockitoAnnotations.openMocks(this).close();
  }

  @After
  public void tearDown() {
    interceptors.forEach(interceptor -> vertx.eventBus().removeOutboundInterceptor(interceptor));

    tearDownHoldingsData();
  }

  @Test
  public void shouldReturnStatusNotStarted() {
    setupDefaultLoadKbConfiguration();
    final HoldingsLoadingStatus status =
      getWithOk(STUB_HOLDINGS_LOAD_STATUS_BY_ID_URL, STUB_TOKEN_HEADER).body().as(HoldingsLoadingStatus.class);
    assertThat(status.getData().getAttributes().getStatus().getName(), equalTo(LoadStatusNameEnum.NOT_STARTED));
  }

  @Test
  public void shouldReturnStatusPopulatingStagingArea(TestContext context) throws IOException, URISyntaxException {
    setupDefaultLoadKbConfiguration();
    mockResponseList(new UrlPathPattern(new EqualToPattern(RMAPI_HOLDINGS_STATUS_URL), false),
      new ResponseDefinitionBuilder().withBody(readFile("responses/rmapi/holdings/status/get-status-in-progress.json"))
        .withStatus(200),
      new ResponseDefinitionBuilder().withBody(readFile("responses/rmapi/holdings/status/get-status-completed.json"))
        .withStatus(200));

    Async startedAsync = context.async();
    Handler<DeliveryContext<LoadHoldingsMessage>> interceptor =
      interceptAndContinue(LOAD_FACADE_ADDRESS, CREATE_SNAPSHOT_ACTION, message -> startedAsync.complete());
    vertx.eventBus().addOutboundInterceptor(interceptor);
    interceptors.add(interceptor);

    Async finishedAsync = context.async();
    interceptor =
      interceptAndStop(HOLDINGS_SERVICE_ADDRESS, SNAPSHOT_CREATED_ACTION, message -> finishedAsync.complete());
    vertx.eventBus().addOutboundInterceptor(interceptor);
    interceptors.add(interceptor);

    postWithStatus(HOLDINGS_LOAD_BY_ID_URL, "", SC_NO_CONTENT, STUB_TOKEN_HEADER);

    startedAsync.await(TIMEOUT);

    final HoldingsLoadingStatus status =
      getWithOk(STUB_HOLDINGS_LOAD_STATUS_BY_ID_URL, STUB_TOKEN_HEADER).body().as(HoldingsLoadingStatus.class);
    assertThat(status.getData().getAttributes().getStatus().getDetail(),
      equalTo(LoadStatusNameDetailEnum.POPULATING_STAGING_AREA));

    finishedAsync.await(TIMEOUT);
  }

  @Test
  public void shouldReturnStatusCompleted(TestContext context) throws IOException, URISyntaxException {
    setupDefaultLoadKbConfiguration();
    mockGet(new EqualToPattern(RMAPI_HOLDINGS_STATUS_URL),
      "responses/rmapi/holdings/status/get-status-completed-one-page.json");

    stubFor(post(new UrlPathPattern(new EqualToPattern(RMAPI_POST_HOLDINGS_URL), false)).willReturn(
      new ResponseDefinitionBuilder().withBody("").withStatus(202)));

    mockGet(new RegexPattern(RMAPI_POST_HOLDINGS_URL), "responses/rmapi/holdings/holdings/get-holdings.json");

    Async async = context.async();
    handleStatusChange(COMPLETED, holdingsStatusRepository, o -> async.complete());

    postWithStatus(HOLDINGS_LOAD_BY_ID_URL, "", SC_NO_CONTENT, STUB_TOKEN_HEADER);
    async.await(TIMEOUT);
    final HoldingsLoadingStatus status =
      getWithOk(STUB_HOLDINGS_LOAD_STATUS_BY_ID_URL, STUB_TOKEN_HEADER).body().as(HoldingsLoadingStatus.class);

    assertThat(status.getData().getType(), equalTo(LoadStatusData.Type.STATUS));
    assertThat(status.getData().getAttributes().getTotalCount(), equalTo(2));
    assertThat(status.getData().getAttributes().getStatus().getName(), equalTo(COMPLETED));

    assertTrue(parse(status.getData().getAttributes().getStarted(), POSTGRES_TIMESTAMP_FORMATTER).isBefore(
      parse(status.getData().getAttributes().getFinished(), POSTGRES_TIMESTAMP_FORMATTER)));
  }

  @Test
  public void shouldReturnErrorWhenRmApiReturnsError(TestContext context) {
    setupDefaultLoadKbConfiguration();
    mockGet(new EqualToPattern(RMAPI_HOLDINGS_STATUS_URL), SC_INTERNAL_SERVER_ERROR);

    Async finishedAsync = context.async(SNAPSHOT_RETRIES);
    handleStatusChange(FAILED, holdingsStatusRepository, o -> finishedAsync.countDown());
    postWithStatus(HOLDINGS_LOAD_BY_ID_URL, "", SC_NO_CONTENT, STUB_TOKEN_HEADER);
    finishedAsync.await(TIMEOUT);

    final HoldingsLoadingStatus status =
      getWithOk(STUB_HOLDINGS_LOAD_STATUS_BY_ID_URL, STUB_TOKEN_HEADER).body().as(HoldingsLoadingStatus.class);
    assertThat(status.getData().getAttributes().getStatus().getName(), equalTo(FAILED));
  }

  @Test
  public void shouldReturn404WhenNoKbCredentialsFound() {
    final String url = String.format(HOLDINGS_LOAD_STATUS_BY_ID_URL, UUID.randomUUID());
    final JsonapiError error = getWithStatus(url, SC_NOT_FOUND, JOHN_TOKEN_HEADER).as(JsonapiError.class);
    assertThat(error.getErrors().get(0).getTitle(), containsString("not exist"));
  }

  @Test
  public void shouldReturn401WhenNoHeader() {
    final String url = String.format(HOLDINGS_LOAD_STATUS_BY_ID_URL, UUID.randomUUID());
    final JsonapiError error = getWithStatus(url, SC_UNAUTHORIZED).as(JsonapiError.class);
    assertThat(error.getErrors().get(0).getTitle(), containsString("Invalid token"));
  }

  public void setupDefaultLoadKbConfiguration() {
    saveKbCredentials(STUB_CREDENTIALS_ID, getWiremockUrl(), STUB_CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID,
      vertx);
    saveStatusNotStarted(STUB_CREDENTIALS_ID, vertx);
    insertRetryStatus(STUB_CREDENTIALS_ID, vertx);
  }

  private void tearDownHoldingsData() {
    clearDataFromTable(vertx, KB_CREDENTIALS_TABLE_NAME);
  }
}
