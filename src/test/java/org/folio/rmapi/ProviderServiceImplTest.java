package org.folio.rmapi;

import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.folio.test.util.TestUtil.STUB_TENANT;
import static org.folio.test.util.TestUtil.mockGet;

import com.github.tomakehurst.wiremock.common.Slf4jNotifier;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.matching.RegexPattern;
import com.github.tomakehurst.wiremock.matching.UrlPattern;
import io.vertx.core.Vertx;
import java.io.IOException;
import java.net.URISyntaxException;
import org.folio.cache.VertxCache;
import org.folio.holdingsiq.model.Configuration;
import org.junit.Rule;
import org.junit.Test;

public class ProviderServiceImplTest {
  protected static final String STUB_CUSTOMER_ID = "TEST_CUSTOMER_ID";
  private static final int STUB_VENDOR_ID = 111111;
  private static final String VENDOR_STUB_FILE = "responses/rmapi/vendors/get-vendor-by-id-response.json";

  @Rule
  public WireMockRule userMockServer = new WireMockRule(
    WireMockConfiguration.wireMockConfig()
      .dynamicPort()
      .notifier(new Slf4jNotifier(true)));

  @Test
  public void shouldReturnCachedProviderOnSecondRequest() throws IOException, URISyntaxException {
    RegexPattern getVendorPattern =
      new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors/" + STUB_VENDOR_ID);

    Configuration configuration = Configuration.builder()
      .url("http://127.0.0.1:" + userMockServer.port())
      .customerId(STUB_CUSTOMER_ID)
      .apiKey("API KEY")
      .build();
    ProvidersServiceImpl service = new ProvidersServiceImpl(configuration, Vertx.vertx(), STUB_TENANT, null,
      new VertxCache<>(Vertx.vertx(), 60, "vendorCache"));

    mockGet(getVendorPattern, VENDOR_STUB_FILE);
    service.retrieveProvider(STUB_VENDOR_ID, null, true).join();
    service.retrieveProvider(STUB_VENDOR_ID, null, true).join();

    verify(1, getRequestedFor(new UrlPattern(getVendorPattern, true)));
  }
}
