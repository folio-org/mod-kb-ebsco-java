package org.folio.service.packages;

import static org.folio.util.TestUtil.result;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.vertx.core.Vertx;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.folio.cache.VertxCache;
import org.folio.config.cache.VendorIdCacheKey;
import org.folio.holdingsiq.model.Configuration;
import org.folio.holdingsiq.model.RequestContext;
import org.folio.okapi.common.XOkapiHeaders;
import org.folio.rest.util.template.RmApiTemplateContext;
import org.folio.rmapi.ProvidersServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CustomProviderIdServiceTest {

  @Mock
  private ProvidersServiceImpl providersService;

  private CustomProviderIdService customProviderIdService;
  private RmApiTemplateContext context;

  @BeforeEach
  void setUp() {
    var vendorIdCache = new VertxCache<VendorIdCacheKey, Integer>(Vertx.vertx(), 60, "vendorIdCacheTest");
    customProviderIdService = new CustomProviderIdService(vendorIdCache);

    var requestContext = new RequestContext(Map.of(
      XOkapiHeaders.TENANT, "fs",
      XOkapiHeaders.URL, "http://localhost:8080"));
    var configuration = Configuration.builder().customerId("customerId").apiKey("apiKey").build();

    context = RmApiTemplateContext.builder()
      .requestContext(requestContext)
      .configuration(configuration)
      .providersService(providersService)
      .build();
  }

  @Test
  void shouldFetchVendorIdWhenNotCached() {
    when(providersService.getVendorId()).thenReturn(CompletableFuture.completedFuture(111));

    var result = result(customProviderIdService.getCustomProviderId(context));

    assertEquals(111, result);
    verify(providersService).getVendorId();
  }

  @Test
  void shouldReturnCachedVendorIdWithoutCallingRmApiAgain() {
    when(providersService.getVendorId()).thenReturn(CompletableFuture.completedFuture(111));

    var firstResult = result(customProviderIdService.getCustomProviderId(context));
    var secondResult = result(customProviderIdService.getCustomProviderId(context));

    assertEquals(111, firstResult);
    assertEquals(111, secondResult);
    verify(providersService, times(1)).getVendorId();
  }

  @Test
  void shouldNotShareCacheAcrossDifferentTenants() {
    when(providersService.getVendorId()).thenReturn(CompletableFuture.completedFuture(111));

    result(customProviderIdService.getCustomProviderId(context));

    var otherTenantContext = context.toBuilder()
      .requestContext(new RequestContext(Map.of(
        XOkapiHeaders.TENANT, "other",
        XOkapiHeaders.URL, "http://localhost:8080")))
      .build();
    result(customProviderIdService.getCustomProviderId(otherTenantContext));

    verify(providersService, times(2)).getVendorId();
  }
}
