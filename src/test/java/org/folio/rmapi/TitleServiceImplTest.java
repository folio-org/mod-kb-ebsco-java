package org.folio.rmapi;

import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.folio.test.util.TestUtil.STUB_TENANT;
import static org.folio.test.util.TestUtil.mockGet;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.tomakehurst.wiremock.common.Slf4jNotifier;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.matching.RegexPattern;
import com.github.tomakehurst.wiremock.matching.UrlPattern;
import io.vertx.core.Vertx;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import org.folio.cache.VertxCache;
import org.folio.holdingsiq.model.Configuration;
import org.folio.holdingsiq.model.CustomerResources;
import org.folio.holdingsiq.model.Title;
import org.folio.rmapi.cache.TitleCacheKey;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class TitleServiceImplTest {

  protected static final String STUB_CUSTOMER_ID = "TEST_CUSTOMER_ID";

  private static final int STUB_TITLE_ID = 123456;
  private static final String TITLE_STUB_FILE = "responses/rmapi/titles/get-title-by-id-response.json";

  @Rule
  public WireMockRule userMockServer = new WireMockRule(
    WireMockConfiguration.wireMockConfig()
      .dynamicPort()
      .notifier(new Slf4jNotifier(true)));

  private Configuration configuration;

  @Before
  public void setUp() {
    configuration = Configuration.builder()
      .url("http://127.0.0.1:" + userMockServer.port())
      .customerId(STUB_CUSTOMER_ID)
      .apiKey("API KEY")
      .build();
  }

  @Test
  public void shouldReturnCachedTitleOnSecondRequest() throws IOException, URISyntaxException {
    RegexPattern getTitlePattern = new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/titles/" + STUB_TITLE_ID);

    TitlesServiceImpl service = new TitlesServiceImpl(configuration, Vertx.vertx(), STUB_TENANT,
      new VertxCache<>(Vertx.vertx(), 60, "titleCache"));

    mockGet(getTitlePattern, TITLE_STUB_FILE);
    service.retrieveTitle(STUB_TITLE_ID, true).join();
    service.retrieveTitle(STUB_TITLE_ID, true).join();

    verify(1, getRequestedFor(new UrlPattern(getTitlePattern, true)));
  }

  @Test
  public void shouldNotUseCache() throws IOException, URISyntaxException {
    RegexPattern getTitlePattern = new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/titles/" + STUB_TITLE_ID);

    TitlesServiceImpl service = new TitlesServiceImpl(configuration, Vertx.vertx(), STUB_TENANT,
      new VertxCache<>(Vertx.vertx(), 60, "titleCache"));

    mockGet(getTitlePattern, TITLE_STUB_FILE);
    service.retrieveTitle(STUB_TITLE_ID, false).join();

    verify(1, getRequestedFor(new UrlPattern(getTitlePattern, true)));
  }

  @Test
  public void shouldUpdateCachedTitle() {
    var titleCache = mock(VertxCache.class);
    var service = new TitlesServiceImpl(configuration, Vertx.vertx(), STUB_TENANT, titleCache);

    when(titleCache.getValue(any(TitleCacheKey.class))).thenReturn(buildTitleWithCustomerResource(1));

    Title title = buildTitleWithCustomerResource(2);
    service.updateCache(title);

    assertEquals(2, title.getCustomerResourcesList().size());
  }

  private Title buildTitleWithCustomerResource(int packageId) {
    var customerResource = CustomerResources.builder()
      .packageId(packageId)
      .build();

    return Title.builder()
      .titleId(STUB_TITLE_ID)
      .customerResourcesList(new ArrayList<>(List.of(customerResource)))
      .build();
  }

}
