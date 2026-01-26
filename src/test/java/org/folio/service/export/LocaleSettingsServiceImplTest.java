package org.folio.service.export;

import static org.folio.test.util.TestUtil.STUB_TENANT;
import static org.folio.test.util.TestUtil.STUB_TOKEN;

import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import java.util.HashMap;
import java.util.Map;
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
    var async = context.async();
    mockSuccessfulLocaleResponse();

    var future = localeSettingsService.retrieveSettings(okapiParams);

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
    var async = context.async();
    var configFileName = "responses/configuration/locale-unexpected.json";
    mockSuccessfulLocaleResponse(configFileName);

    var future = localeSettingsService.retrieveSettings(okapiParams);

    future.thenCompose(result -> {
      assertLocaleSettingsRecoveredToDefault(context, result);
      async.complete();
      return null;
    }).exceptionally(exception -> {
      context.assertNull(exception);
      async.complete();
      return null;
    });
  }

  @Test
  public void shouldReturnDefaultSettingsWhenConfigurationFailed(TestContext context) {
    var async = context.async();
    mockFailedLocaleResponse();
    var future = localeSettingsService.retrieveSettings(okapiParams);

    future.thenCompose(result -> {
      assertLocaleSettingsRecoveredToDefault(context, result);
      async.complete();
      return null;
    }).exceptionally(exception -> {
      failTest(context);
      async.complete();
      return null;
    });
  }

  @Test
  public void shouldReturnDefaultSettingsWhenResponseIsInvalidJson(TestContext context) {
    var async = context.async();
    mockLocaleResponseWithInvalidJson();
    var future = localeSettingsService.retrieveSettings(okapiParams);

    future.thenCompose(result -> {
      assertLocaleSettingsRecoveredToDefault(context, result);
      async.complete();
      return null;
    }).exceptionally(exception -> {
      failTest(context);
      async.complete();
      return null;
    });
  }

  @Test
  public void shouldReturnDefaultSettingsWhenResponseIsEmpty(TestContext context) {
    var async = context.async();
    mockLocaleResponseWithEmptyBody();
    var future = localeSettingsService.retrieveSettings(okapiParams);

    future.thenCompose(result -> {
      assertLocaleSettingsRecoveredToDefault(context, result);
      async.complete();
      return null;
    }).exceptionally(exception -> {
      failTest(context);
      async.complete();
      return null;
    });
  }

  @Test
  public void shouldReturnDefaultSettingsWhenServerError(TestContext context) {
    var async = context.async();
    mockLocaleResponseWithServerError();
    var future = localeSettingsService.retrieveSettings(okapiParams);

    future.thenCompose(result -> {
      assertLocaleSettingsRecoveredToDefault(context, result);
      async.complete();
      return null;
    }).exceptionally(exception -> {
      failTest(context);
      async.complete();
      return null;
    });
  }

  @Test
  public void shouldReturnDefaultSettingsWhenNetworkFailure(TestContext context) {
    var async = context.async();
    mockLocaleResponseWithNetworkError();
    var future = localeSettingsService.retrieveSettings(okapiParams);

    future.thenCompose(result -> {
      assertLocaleSettingsRecoveredToDefault(context, result);
      async.complete();
      return null;
    }).exceptionally(exception -> {
      failTest(context);
      async.complete();
      return null;
    });
  }

  private void failTest(TestContext context) {
    context.fail("Should not complete exceptionally, but return default settings");
  }

  private void assertLocaleSettingsRecoveredToDefault(TestContext context, LocaleSettings result) {
    context.assertEquals("USD", result.getCurrency());
    context.assertEquals("en-US", result.getLocale());
    context.assertEquals("UTC", result.getTimezone());
  }
}
