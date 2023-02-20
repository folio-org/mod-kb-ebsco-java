package org.folio.client.uc;

import static org.folio.util.FutureUtils.mapVertxFuture;

import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.client.predicate.ResponsePredicate;
import io.vertx.ext.web.codec.BodyCodec;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import lombok.extern.log4j.Log4j2;
import org.folio.client.uc.model.UcAuthToken;
import org.folio.service.uc.UcAuthenticationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Log4j2
@Component
public class UcAuthEbscoClientImpl implements UcAuthEbscoClient {

  private static final int TIMEOUT = 20000;
  private static final String REQUEST_URI = "/oauth-proxy/token";

  private final String baseUrl;
  private final WebClient webClient;

  public UcAuthEbscoClientImpl(@Value("${kb.ebsco.uc.auth.url}") String baseUrl, Vertx vertx) {
    this.baseUrl = baseUrl;
    WebClientOptions options = new WebClientOptions();
    options.setLogActivity(true);
    this.webClient = WebClient.create(vertx, options);
  }

  @Override
  public CompletableFuture<UcAuthToken> requestToken(String clientId, String clientSecret) {
    Promise<HttpResponse<JsonObject>> promise = Promise.promise();
    log.info("Request UC Token");
    webClient
      .postAbs(baseUrl + REQUEST_URI)
      .timeout(TIMEOUT)
      .expect(ResponsePredicate.SC_OK)
      .as(BodyCodec.jsonObject())
      .sendForm(createRequestBody(clientId, clientSecret), promise);
    return mapVertxFuture(promise.future().recover(mapException()))
      .thenApply(HttpResponse::body)
      .thenApply(jsonObject -> jsonObject.mapTo(UcAuthToken.class));
  }

  private MultiMap createRequestBody(String clientId, String clientSecret) {
    MultiMap requestBody = MultiMap.caseInsensitiveMultiMap();
    requestBody.set("grant_type", "client_credentials");
    requestBody.set("client_id", clientId);
    requestBody.set("client_secret", clientSecret);
    return requestBody;
  }

  private Function<Throwable, Future<HttpResponse<JsonObject>>> mapException() {
    return throwable -> {
      log.warn("Request UC Token failed", throwable);
      return Future.failedFuture(new UcAuthenticationException(throwable.getMessage()));
    };
  }
}
