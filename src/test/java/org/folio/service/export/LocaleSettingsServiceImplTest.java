package org.folio.service.export;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_OK;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import static org.folio.test.util.TestUtil.STUB_TENANT;
import static org.folio.test.util.TestUtil.STUB_TOKEN;
import static org.folio.test.util.TestUtil.readFile;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.matching.RegexPattern;
import com.github.tomakehurst.wiremock.matching.UrlPathPattern;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;

import org.folio.holdingsiq.model.OkapiData;
import org.folio.holdingsiq.service.impl.ConfigurationClientProvider;
import org.folio.okapi.common.XOkapiHeaders;
import org.folio.rest.client.ConfigurationsClient;
import org.folio.rest.impl.WireMockTestBase;
import org.folio.service.locale.LocaleSettings;
import org.folio.service.locale.LocaleSettingsService;

@RunWith(VertxUnitRunner.class)
public class LocaleSettingsServiceImplTest extends WireMockTestBase {
  private ConfigurationClientProvider configurationClientProvider ;
  private ConfigurationsClient mockConfigurationsClient;
  @Autowired
  private LocaleSettingsService localeSettingsService;

  private OkapiData okapiParams;

  @Before
  public void setUp() throws Exception {
    super.setUp();
    Map<String, String> headers = new HashMap<>();
    headers.put(XOkapiHeaders.TOKEN, STUB_TOKEN);
    headers.put(XOkapiHeaders.TENANT, STUB_TENANT);
    headers.put(XOkapiHeaders.URL, getWiremockUrl());
    okapiParams = new OkapiData(headers);
    configurationClientProvider = mock(ConfigurationClientProvider.class);
    mockConfigurationsClient = mock(ConfigurationsClient.class);
    when(configurationClientProvider.createClient(anyString(), anyInt(), anyString(), anyString())).thenReturn(mockConfigurationsClient);
  }

  @Test
  public void shouldReturnValidSettings(TestContext context) throws Exception {
    Async async = context.async();

    String configFileName = "responses/configuration/locale-settings.json";
    mockSuccessfulConfigurationResponse(configFileName);

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
  public void shouldReturnDefaultSettingsWhenNoLocaleSettingsExists(TestContext context) throws IOException, URISyntaxException {
    Async async = context.async();

    String configFileName = "responses/configuration/locale-settings-empty.json";
    mockSuccessfulConfigurationResponse(configFileName);

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
    mockFailedConfigurationResponse(SC_BAD_REQUEST);
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

  private void mockSuccessfulConfigurationResponse(String configFileName) throws IOException, URISyntaxException {
    stubFor(
      get(new UrlPathPattern(new RegexPattern("/configurations/entries.*"), true))
        .willReturn(new ResponseDefinitionBuilder()
          .withStatus(SC_OK)
          .withBody(readFile(configFileName))));
  }

  private void mockFailedConfigurationResponse(int status) {
    stubFor(
      get(new UrlPathPattern(new RegexPattern("/configurations/entries.*"), true))
        .willReturn(new ResponseDefinitionBuilder()
          .withStatus(status)
          .withBody("")));
  }
}
