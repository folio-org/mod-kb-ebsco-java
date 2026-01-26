package org.folio.service.export;

import static org.folio.test.util.TestUtil.STUB_TENANT;
import static org.folio.test.util.TestUtil.STUB_TOKEN;

import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.folio.holdingsiq.model.OkapiData;
import org.folio.okapi.common.XOkapiHeaders;
import org.folio.rest.impl.WireMockTestBase;
import org.folio.service.locale.LocaleSettings;
import org.folio.service.locale.LocaleSettingsService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;

@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@RunWith(VertxUnitRunner.class)
public class LocaleSettingsServiceImplTest extends WireMockTestBase {

  @Autowired
  private LocaleSettingsService localeSettingsService;

  private OkapiData okapiParams;

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    Map<String, String> headers = new HashMap<>();
    headers.put(XOkapiHeaders.TOKEN, STUB_TOKEN);
    headers.put(XOkapiHeaders.TENANT, STUB_TENANT);
    headers.put(XOkapiHeaders.URL, getWiremockUrl());
    okapiParams = new OkapiData(headers);
  }

  @Test
  public void shouldReturnValidSettings(TestContext context) {
    Async async = context.async();

    mockSuccessfulLocaleResponse();

    CompletableFuture<LocaleSettings> future = localeSettingsService.retrieveSettings(okapiParams);

    future.thenCompose(result -> {
      context.assertEquals(result.getCurrency(), "EUR");
      context.assertEquals(result.getLocale(), "en-GB");
      context.assertEquals(result.getTimezone(), "UTC");
      async.complete();
      return null;
    }).exceptionally(exception -> {
      context.assertNull(exception);
      async.complete();
      return null;
    });
  }

  @Test
  public void shouldReturnDefaultSettingsWhenResponseUnexpected(TestContext context) {
    Async async = context.async();

    String configFileName = "responses/configuration/locale-unexpected.json";
    mockSuccessfulLocaleResponse(configFileName);

    CompletableFuture<LocaleSettings> future = localeSettingsService.retrieveSettings(okapiParams);

    future.thenCompose(result -> {
      context.assertEquals(result.getCurrency(), "USD");
      context.assertEquals(result.getLocale(), "en-US");
      context.assertEquals(result.getTimezone(), "UTC");
      async.complete();
      return null;
    }).exceptionally(exception -> {
      context.assertNull(exception);
      async.complete();
      return null;
    });
  }

  @Test
  public void shouldCompleteExceptionallyWhenConfigurationFailed(TestContext context) {
    Async async = context.async();
    mockFailedLocaleResponse();
    CompletableFuture<LocaleSettings> future = localeSettingsService.retrieveSettings(okapiParams);
    future.thenCompose(result -> {
      async.complete();
      return null;
    }).exceptionally(exception -> {
      context.assertEquals(IllegalStateException.class, exception.getCause().getClass());
      async.complete();
      return null;
    });
  }
}
