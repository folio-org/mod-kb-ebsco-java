package org.folio.service.locale;

import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import java.util.Currency;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.tools.utils.VertxUtils;
import org.jetbrains.annotations.NotNull;
import org.joda.time.DateTimeZone;

import org.folio.holdingsiq.model.OkapiData;
import org.folio.rest.client.ConfigurationsClient;
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
      var configurationsClient = prepareConfigurationsClient(okapiData, tenantId);
      LOG.info("Send GET request to mod-configuration {}", QUERY);
      Promise<HttpResponse<Buffer>> promise = Promise.promise();
      configurationsClient.getConfigurationsEntries(QUERY, 0, 100, null, null, promise);

      promise.future()
        .onSuccess(event -> {
          try {
            if (isSuccessfulResponse(event)) {
              future.complete(event.body().toJsonObject());
            }
          } catch (IllegalStateException e) {
            future.completeExceptionally(e);
          }
        })
        .onFailure(future::completeExceptionally);

    } catch (Exception e) {
      LOG.error("Request to mod-configuration failed:", e);
      future.completeExceptionally(e);
    }
    return future;
  }

  @NotNull
  private ConfigurationsClient prepareConfigurationsClient(OkapiData okapiData, String tenantId) {
    var options = new WebClientOptions();
    options.setLogActivity(true);
    options.setKeepAlive(true);
    options.setConnectTimeout(2000);
    options.setIdleTimeout(5000);
    var webClient = WebClient.create(VertxUtils.getVertxFromContextOrNew(), options);
    return new ConfigurationsClient(okapiData.getOkapiUrl(), tenantId, okapiData.getApiToken(), webClient);
  }

  private boolean isSuccessfulResponse(HttpResponse<Buffer> response) {
    if (!isSuccess(response.statusCode())) {
      String errorMessage = String.format(
        "Request to mod-configuration failed: error code - %s response body - %s", response.statusCode(),
        response.bodyAsString());
      LOG.error(errorMessage);
      throw new IllegalStateException(errorMessage);
    }
    return true;
  }

  private static boolean isSuccess(int statusCode){
    return statusCode >= 200 && statusCode < 300;
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
