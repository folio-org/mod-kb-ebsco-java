package org.folio.rmapi;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static org.folio.util.TestUtil.STUB_TENANT;
import static org.folio.util.TestUtil.readFile;

import com.github.tomakehurst.wiremock.matching.UrlPattern;
import io.vertx.core.Vertx;
import java.util.Collections;
import org.folio.cache.VertxCache;
import org.folio.holdingsiq.model.PackageId;
import org.folio.util.WireMockTestBase;
import org.junit.jupiter.api.Test;

class PackageServiceImplTest extends WireMockTestBase {

  private static final int PACKAGE_ID = 3964;
  private static final int VENDOR_ID = 111111;
  private static final String CUSTOM_PACKAGE_STUB_FILE =
    "responses/rmapi/packages/get-custom-package-by-id-response.json";

  @Test
  void shouldReturnCachedPackage() {
    var getPackagePattern = equalTo(packageRmApi(PACKAGE_ID));

    var configuration = getStubConfiguration();
    var service = new PackageServiceImpl(configuration, Vertx.vertx(), STUB_TENANT, null, null,
      new VertxCache<>(Vertx.vertx(), 60, "packageCache"), null);

    mockGet(getPackagePattern, readFile(CUSTOM_PACKAGE_STUB_FILE));

    var packageId = new PackageId(VENDOR_ID, PACKAGE_ID);
    service.retrievePackage(packageId, Collections.emptyList(), true).join();
    service.retrievePackage(packageId, Collections.emptyList(), true).join();

    wm.verify(1, getRequestedFor(new UrlPattern(getPackagePattern, false)));
  }
}
