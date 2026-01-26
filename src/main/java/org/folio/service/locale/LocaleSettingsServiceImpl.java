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
import lombok.extern.log4j.Log4j2;
import org.folio.holdingsiq.model.OkapiData;
import org.folio.rest.tools.utils.VertxUtils;
import org.jspecify.annotations.NonNull;

@Log4j2
public class LocaleSettingsServiceImpl implements LocaleSettingsService {

  public static final String LOCALE_ENDPOINT_PATH = "/locale";

  public CompletableFuture<LocaleSettings> retrieveSettings(OkapiData okapiData) {
    log.debug("Retrieving locale settings");

    return retrieveLocaleSettings(okapiData)
      .thenApply(this::mapToLocaleSettings)
      .exceptionally(throwable -> {
        log.warn("Locale settings retrieval failed, falling back to default", throwable);
        return Optional.of(getDefaultLocaleSettings());
      })
      .thenApply(localeSettings -> {
        if (localeSettings.isEmpty()) {
          log.warn("Locale settings response couldn't be mapped, falling back to default");
          return getDefaultLocaleSettings();
        }
        log.debug("Locale settings retrieved successfully");
        return localeSettings.get();
      });
  }

  private CompletableFuture<JsonObject> retrieveLocaleSettings(OkapiData okapiData) {
    CompletableFuture<JsonObject> future = new CompletableFuture<>();
    WebClient webClient = null;

    try {
      log.debug("Sending request to GET {}", LOCALE_ENDPOINT_PATH);
      webClient = prepareConfigurationsClient();
      var request = prepareRequest(okapiData, webClient);

      WebClient finalWebClient = webClient;
      request.send()
        .onSuccess(handleSuccess(future, finalWebClient))
        .onFailure(handleFailure(future, finalWebClient));
    } catch (Exception e) {
      log.warn("Request to GET {} couldn't be executed", LOCALE_ENDPOINT_PATH, e);
      if (webClient != null) {
        webClient.close();
      }
      future.completeExceptionally(e);
    }
    return future;
  }

  private Handler<Throwable> handleFailure(CompletableFuture<JsonObject> future, WebClient webClient) {
    return t -> {
      log.warn("Request to GET {} failed", LOCALE_ENDPOINT_PATH, t);
      webClient.close();
      future.completeExceptionally(t);
    };
  }

  private HttpRequest<Buffer> prepareRequest(OkapiData okapiData, WebClient webClient) {
    var request = webClient.requestAbs(HttpMethod.GET, okapiData.getOkapiUrl() + LOCALE_ENDPOINT_PATH);
    okapiData.getHeaders().forEach(request::putHeader);
    request.putHeader("Accept", "application/json,text/plain");
    return request;
  }

  private Handler<HttpResponse<Buffer>> handleSuccess(CompletableFuture<JsonObject> future, WebClient webClient) {
    return response -> {
      try {
        if (isSuccessfulResponse(response)) {
          future.complete(response.body().toJsonObject());
        }
      } catch (Exception e) {
        log.warn("Failed to process response from GET {}", LOCALE_ENDPOINT_PATH, e);
        future.completeExceptionally(e);
      } finally {
        webClient.close();
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

    log.debug("Request to GET {} succeeded [status={}]", LOCALE_ENDPOINT_PATH, status);
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
