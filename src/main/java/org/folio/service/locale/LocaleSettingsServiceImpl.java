package org.folio.service.locale;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import java.util.Currency;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import lombok.extern.log4j.Log4j2;
import org.folio.holdingsiq.model.OkapiData;
import org.folio.rest.tools.utils.VertxUtils;
import org.jspecify.annotations.NonNull;

@Log4j2
public class LocaleSettingsServiceImpl implements LocaleSettingsService {

  private static final String LOCALE_ENDPOINT_PATH = "/locale";

  public CompletableFuture<LocaleSettings> retrieveSettings(OkapiData okapiData) {
    CompletableFuture<LocaleSettings> future = new CompletableFuture<>();

    log.debug("Retrieving locale settings");

    retrieveLocaleSettings(okapiData)
      .thenApply(this::mapToLocaleSettings)
      .whenComplete(recovery(future));
    return future;
  }

  @NonNull
  private BiConsumer<Optional<LocaleSettings>, Throwable> recovery(CompletableFuture<LocaleSettings> future) {
    return (localeSettings, throwable) -> {
      if (throwable != null) {
        log.warn("Locale settings retrieval failed, falling back to default", throwable);
        future.complete(getDefaultLocaleSettings());
        return;
      }

      if (localeSettings.isEmpty()) {
        log.warn("Locale settings response couldn't be mapped, falling back to default");
        future.complete(getDefaultLocaleSettings());
        return;
      }

      log.debug("Locale settings retrieved successfully");
      future.complete(localeSettings.get());
    };
  }

  private CompletableFuture<JsonObject> retrieveLocaleSettings(OkapiData okapiData) {
    CompletableFuture<JsonObject> future = new CompletableFuture<>();

    try {
      if (log.isDebugEnabled()) {
        log.debug("Sending request to GET {}", LOCALE_ENDPOINT_PATH);
      }
      var webClient = prepareConfigurationsClient();
      var request = prepareRequest(okapiData, webClient);

      request.send()
        .onSuccess(handleSuccess(future))
        .onFailure(handleFailure(future));
    } catch (Exception e) {
      log.warn("Request to GET {} couldn't be executed", LOCALE_ENDPOINT_PATH, e);
      future.completeExceptionally(e);
    }
    return future;
  }

  private Handler<Throwable> handleFailure(CompletableFuture<JsonObject> future) {
    return t -> {
      log.warn("Request to GET {} failed [tookMs={}]", LOCALE_ENDPOINT_PATH, t);
      future.completeExceptionally(t);
    };
  }

  private HttpRequest<Buffer> prepareRequest(OkapiData okapiData, WebClient webClient) {
    var request = webClient.requestAbs(HttpMethod.GET, okapiData.getOkapiUrl() + LOCALE_ENDPOINT_PATH);
    okapiData.getHeaders().forEach(request::putHeader);
    request.putHeader("Accept", "application/json,text/plain");
    return request;
  }

  private Handler<HttpResponse<Buffer>> handleSuccess(CompletableFuture<JsonObject> future) {
    return response -> {
      try {
        if (isSuccessfulResponse(response)) {
          future.complete(response.body().toJsonObject());
        }
      } catch (IllegalStateException e) {
        future.completeExceptionally(e);
      }
    };
  }

  @NonNull
  private WebClient prepareConfigurationsClient() {
    var options = new WebClientOptions();
    options.setLogActivity(true);
    options.setKeepAlive(true);
    options.setConnectTimeout(2000);
    options.setIdleTimeout(5000);
    return WebClient.create(VertxUtils.getVertxFromContextOrNew(), options);
  }

  private boolean isSuccessfulResponse(HttpResponse<Buffer> response) {
    int status = response.statusCode();

    if (!isSuccess(status)) {
      var errorMessage = String.format("Request to GET %s failed: status=%s", LOCALE_ENDPOINT_PATH, status);
      log.warn(errorMessage);
      throw new IllegalStateException(errorMessage);
    }

    if (log.isDebugEnabled()) {
      log.debug("Request to GET {} succeeded [status={}]", LOCALE_ENDPOINT_PATH, status);
    }
    return true;
  }

  private static boolean isSuccess(int statusCode) {
    return statusCode >= 200 && statusCode < 300;
  }

  private Optional<LocaleSettings> mapToLocaleSettings(JsonObject config) {
    return config == null ? Optional.empty() : Optional.of(config.mapTo(LocaleSettings.class));
  }

  private LocaleSettings getDefaultLocaleSettings() {
    return LocaleSettings.builder()
      .locale(Locale.US.toLanguageTag())
      .timezone("UTC")
      .currency(Currency.getInstance(Locale.US).getCurrencyCode())
      .build();
  }
}
