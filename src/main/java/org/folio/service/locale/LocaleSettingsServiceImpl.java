package org.folio.service.locale;

import java.util.Currency;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.joda.time.DateTimeZone;

import org.folio.holdingsiq.model.OkapiData;
import org.folio.rest.client.ConfigurationsClient;
import org.folio.rest.tools.client.Response;
import org.folio.rest.tools.utils.TenantTool;

public class LocaleSettingsServiceImpl implements LocaleSettingsService {

  private static final Logger LOG = LogManager.getLogger(LocaleSettingsServiceImpl.class);
  private static final String QUERY = "module=ORG and configName=localeSettings";

  public CompletableFuture<LocaleSettings> retrieveSettings(OkapiData okapiData) {
    CompletableFuture<LocaleSettings> future = new CompletableFuture<>();
    retrieveLocationSettings(okapiData)
      .thenApply(this::mapToLocaleSettings)
      .whenComplete(recovery(future));
    return future;
  }

  @NotNull
  private BiConsumer<Optional<LocaleSettings>, Throwable> recovery(CompletableFuture<LocaleSettings> future) {
    return (localeSettings, throwable) -> {
      if (throwable != null || localeSettings.isEmpty()) {
        LOG.info("Default Locale settings will be used to proceed");
        future.complete(getDefaultLocaleSettings());
      } else {
        future.complete(localeSettings.get());
      }
    };
  }

  private CompletableFuture<JsonObject> retrieveLocationSettings(OkapiData okapiData) {
    final String tenantId = TenantTool.calculateTenantId(okapiData.getTenant());
    CompletableFuture<JsonObject> future = new CompletableFuture<>();
    try {

      ConfigurationsClient configurationsClient =
        new ConfigurationsClient(okapiData.getOkapiHost(), okapiData.getOkapiPort(), tenantId, okapiData.getApiToken());
      LOG.info("Send GET request to mod-configuration {}", QUERY);
      configurationsClient.getEntries(QUERY, 0, 100, null, null, response ->
        response.bodyHandler(body -> {
          if (isSuccessfulResponse(response, body, future)) {
            future.complete(body.toJsonObject());
          }
        }));
    } catch (Throwable throwable) {
      LOG.error("Request to mod-configuration failed:", throwable);
      future.completeExceptionally(throwable);
    }
    return future;
  }

  private boolean isSuccessfulResponse(HttpClientResponse response, Buffer responseBody, CompletableFuture<?> future) {
    if (!Response.isSuccess(response.statusCode())) {
      String errorMessage = String.format(
        "Request to mod-configuration failed: error code - %s response body - %s", response.statusCode(), responseBody);
      LOG.error(errorMessage);
      future.completeExceptionally(new IllegalStateException(errorMessage));
      return false;
    }
    return true;
  }

  private Optional<LocaleSettings> mapToLocaleSettings(JsonObject config) {
    return config.getJsonArray("configs")
      .stream()
      .filter(JsonObject.class::isInstance)
      .map(JsonObject.class::cast)
      .findFirst()
      .map(entry -> mapLocale(new JsonObject(entry.getString("value"))));
  }

  private LocaleSettings mapLocale(JsonObject jsonObject) {
    return LocaleSettings.builder()
      .locale(jsonObject.getString("locale"))
      .timezone(jsonObject.getString("timezone"))
      .currency(jsonObject.getString("currency"))
      .build();
  }

  private LocaleSettings getDefaultLocaleSettings() {
    return LocaleSettings.builder()
      .locale(Locale.US.toLanguageTag())
      .timezone(DateTimeZone.UTC.getID())
      .currency(Currency.getInstance(Locale.US).getCurrencyCode())
      .build();
  }
}
