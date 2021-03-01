package org.folio.client.uc;

import static org.folio.util.FutureUtils.mapVertxFuture;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import org.folio.client.uc.model.UCAuthToken;
import org.folio.service.uc.UcAuthenticationException;

@Component
public class UCAuthEbscoClientImpl implements UCAuthEbscoClient {

  private static final Logger LOG = LogManager.getLogger(UCAuthEbscoClientImpl.class);

  private static final int TIMEOUT = 20000;
  private static final String REQUEST_URI = "/oauth-proxy/token";

  private final String baseUrl;
  private final WebClient webClient;

  public UCAuthEbscoClientImpl(@Value("${kb.ebsco.uc.auth.url}") String baseUrl, Vertx vertx) {
    this.baseUrl = baseUrl;
    WebClientOptions options = new WebClientOptions();
    options.setLogActivity(true);
    this.webClient = WebClient.create(vertx, options);
  }

  @Override
  public CompletableFuture<UCAuthToken> requestToken(String clientId, String clientSecret) {
    Promise<HttpResponse<JsonObject>> promise = Promise.promise();
    LOG.info("Request UC Token");
    webClient
      .postAbs(baseUrl + REQUEST_URI)
      .timeout(TIMEOUT)
      .expect(ResponsePredicate.SC_OK)
      .as(BodyCodec.jsonObject())
      .sendForm(createRequestBody(clientId, clientSecret), promise);
    return mapVertxFuture(promise.future().recover(mapException()))
      .thenApply(HttpResponse::body)
      .thenApply(jsonObject -> jsonObject.mapTo(UCAuthToken.class));
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
      LOG.error("Request UC Token failed", throwable);
      return Future.failedFuture(new UcAuthenticationException(throwable.getMessage()));
    };
  }
}
