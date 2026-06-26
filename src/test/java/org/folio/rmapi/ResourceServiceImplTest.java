package org.folio.rmapi;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static org.folio.util.TestUtil.STUB_TENANT;
import static org.folio.util.TestUtil.readFile;

import com.github.tomakehurst.wiremock.matching.UrlPattern;
import io.vertx.core.Vertx;
import java.util.Collections;
import org.folio.cache.VertxCache;
import org.folio.holdingsiq.model.ResourceId;
import org.folio.util.WireMockTestBase;
import org.junit.jupiter.api.Test;

class ResourceServiceImplTest extends WireMockTestBase {

  private static final int PACKAGE_ID = 3964;
  private static final int VENDOR_ID = 111111;
  private static final int TITLE_ID = 985846;

  private static final String CUSTOM_RESOURCE_STUB_FILE =
    "responses/rmapi/resources/get-resource-by-id-success-response.json";

  @Test
  void shouldReturnCachedResource() {
    var getResourcePattern = equalTo(resourcesRmApi(VENDOR_ID, PACKAGE_ID, TITLE_ID));

    var configuration = getStubConfiguration();
    var service = new ResourcesServiceImpl(configuration, Vertx.vertx(), STUB_TENANT, null, null,
      new VertxCache<>(Vertx.vertx(), 60, "resourceCache"));

    mockGet(getResourcePattern, readFile(CUSTOM_RESOURCE_STUB_FILE));

    var resourceId = new ResourceId(VENDOR_ID, PACKAGE_ID, TITLE_ID);
    service.retrieveResource(resourceId, Collections.emptyList(), true).join();
    service.retrieveResource(resourceId, Collections.emptyList(), true).join();

    wm.verify(1, getRequestedFor(new UrlPattern(getResourcePattern, true)));
  }
}
