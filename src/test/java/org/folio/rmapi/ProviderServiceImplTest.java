package org.folio.rmapi;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static org.folio.util.TestUtil.STUB_TENANT;
import static org.folio.util.TestUtil.readFile;

import com.github.tomakehurst.wiremock.matching.UrlPattern;
import io.vertx.core.Vertx;
import org.folio.cache.VertxCache;
import org.folio.util.WireMockTestBase;
import org.junit.jupiter.api.Test;

class ProviderServiceImplTest extends WireMockTestBase {

  private static final int VENDOR_ID = 111111;
  private static final String VENDOR_STUB_FILE = "responses/rmapi/vendors/get-vendor-by-id-response.json";

  @Test
  void shouldReturnCachedProviderOnSecondRequest() {
    var getVendorPattern = equalTo(vendorsRmApi(VENDOR_ID));

    var configuration = getStubConfiguration();
    var service = new ProvidersServiceImpl(configuration, Vertx.vertx(), STUB_TENANT, null,
      new VertxCache<>(Vertx.vertx(), 60, "vendorCache"));

    mockGet(getVendorPattern, readFile(VENDOR_STUB_FILE));
    service.retrieveProvider(VENDOR_ID, null, true).join();
    service.retrieveProvider(VENDOR_ID, null, true).join();

    wm.verify(1, getRequestedFor(new UrlPattern(getVendorPattern, false)));
  }
}
