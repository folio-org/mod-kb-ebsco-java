package org.folio.service.export;

import static org.folio.util.TestUtil.STUB_TENANT;
import static org.folio.util.TestUtil.STUB_TOKEN;
import static org.folio.util.TestUtil.result;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;
import org.folio.holdingsiq.model.RequestContext;
import org.folio.okapi.common.XOkapiHeaders;
import org.folio.service.locale.LocaleSettings;
import org.folio.service.locale.LocaleSettingsService;
import org.folio.service.locale.LocaleSettingsServiceImpl;
import org.folio.util.WireMockTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LocaleSettingsServiceImplTest extends WireMockTestBase {

  private final LocaleSettingsService localeSettingsService = new LocaleSettingsServiceImpl();

  private RequestContext requestContext;

  @BeforeEach
  void setUp() {
    Map<String, String> headers = new HashMap<>();
    headers.put(XOkapiHeaders.TOKEN, STUB_TOKEN);
    headers.put(XOkapiHeaders.TENANT, STUB_TENANT);
    headers.put(XOkapiHeaders.URL, wm.baseUrl());
    requestContext = new RequestContext(headers);
  }

  @Test
  void shouldReturnValidSettings() {
    mockSuccessfulLocaleResponse();

    var result = result(localeSettingsService.retrieveSettings(requestContext));

    assertEquals("EUR", result.getCurrency());
    assertEquals("en-GB", result.getLocale());
    assertEquals("UTC", result.getTimezone());
  }

  @Test
  void shouldReturnDefaultSettingsWhenResponseUnexpected() {
    mockSuccessfulLocaleResponse("responses/configuration/locale-unexpected.json");

    var result = result(localeSettingsService.retrieveSettings(requestContext));

    assertDefaultLocaleSettings(result);
  }

  @Test
  void shouldReturnDefaultSettingsWhenConfigurationFailed() {
    mockFailedLocaleResponse();

    var result = result(localeSettingsService.retrieveSettings(requestContext));

    assertDefaultLocaleSettings(result);
  }

  @Test
  void shouldReturnDefaultSettingsWhenResponseIsInvalidJson() {
    mockLocaleResponseWithInvalidJson();

    var result = result(localeSettingsService.retrieveSettings(requestContext));

    assertDefaultLocaleSettings(result);
  }

  @Test
  void shouldReturnDefaultSettingsWhenResponseIsEmpty() {
    mockLocaleResponseWithEmptyBody();

    var result = result(localeSettingsService.retrieveSettings(requestContext));

    assertDefaultLocaleSettings(result);
  }

  @Test
  void shouldReturnDefaultSettingsWhenServerError() {
    mockLocaleResponseWithServerError();

    var result = result(localeSettingsService.retrieveSettings(requestContext));

    assertDefaultLocaleSettings(result);
  }

  @Test
  void shouldReturnDefaultSettingsWhenNetworkFailure() {
    mockLocaleResponseWithNetworkError();

    var result = result(localeSettingsService.retrieveSettings(requestContext));

    assertDefaultLocaleSettings(result);
  }

  private void assertDefaultLocaleSettings(LocaleSettings result) {
    assertEquals("USD", result.getCurrency());
    assertEquals("en-US", result.getLocale());
    assertEquals("UTC", result.getTimezone());
  }
}
