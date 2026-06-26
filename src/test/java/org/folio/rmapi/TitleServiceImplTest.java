package org.folio.rmapi;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static org.folio.util.TestUtil.STUB_TENANT;
import static org.folio.util.TestUtil.readFile;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.tomakehurst.wiremock.matching.UrlPattern;
import io.vertx.core.Vertx;
import java.util.ArrayList;
import java.util.List;
import org.folio.cache.VertxCache;
import org.folio.holdingsiq.model.Configuration;
import org.folio.holdingsiq.model.CustomerResources;
import org.folio.holdingsiq.model.Title;
import org.folio.rmapi.cache.TitleCacheKey;
import org.folio.util.WireMockTestBase;
import org.junit.jupiter.api.Test;

class TitleServiceImplTest extends WireMockTestBase {

  protected static final String STUB_CUSTOMER_ID = "TEST_CUSTOMER_ID";

  private static final int TITLE_ID = 123456;
  private static final String TITLE_STUB_FILE = "responses/rmapi/titles/get-title-by-id-response.json";

  private final Configuration configuration = getStubConfiguration();

  @Test
  void shouldReturnCachedTitleOnSecondRequest() {
    var getTitlePattern = equalTo(titlesRmApi(TITLE_ID));

    var service = new TitlesServiceImpl(configuration, Vertx.vertx(), STUB_TENANT,
      new VertxCache<>(Vertx.vertx(), 60, "titleCache"));

    mockGet(getTitlePattern, readFile(TITLE_STUB_FILE));
    service.retrieveTitle(TITLE_ID, true).join();
    service.retrieveTitle(TITLE_ID, true).join();

    wm.verify(1, getRequestedFor(new UrlPattern(getTitlePattern, true)));
  }

  @Test
  void shouldNotUseCache() {
    var getTitlePattern = equalTo(titlesRmApi(TITLE_ID));

    var service = new TitlesServiceImpl(configuration, Vertx.vertx(), STUB_TENANT,
      new VertxCache<>(Vertx.vertx(), 60, "titleCache"));

    mockGet(getTitlePattern, readFile(TITLE_STUB_FILE));
    service.retrieveTitle(TITLE_ID, false).join();

    wm.verify(1, getRequestedFor(new UrlPattern(getTitlePattern, true)));
  }

  @Test
  void shouldUpdateCachedTitle() {
    VertxCache<TitleCacheKey, Title> titleCache = mock();
    var service = new TitlesServiceImpl(configuration, Vertx.vertx(), STUB_TENANT, titleCache);

    when(titleCache.getValue(any(TitleCacheKey.class))).thenReturn(buildTitleWithCustomerResource(1));

    var title = buildTitleWithCustomerResource(2);
    service.updateCache(title);

    assertEquals(2, title.getCustomerResourcesList().size());
  }

  private Title buildTitleWithCustomerResource(int packageId) {
    var customerResource = CustomerResources.builder()
      .packageId(packageId)
      .build();

    return Title.builder()
      .titleId(TITLE_ID)
      .customerResourcesList(new ArrayList<>(List.of(customerResource)))
      .build();
  }
}
