package org.folio.client.uc;

import static org.folio.util.FutureUtils.mapVertxFuture;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.client.predicate.ResponsePredicate;
import io.vertx.ext.web.codec.BodyCodec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import org.folio.service.uc.UcAuthenticationException;

@Component
public class UCAuthEbscoClientImpl implements UCAuthEbscoClient {

  private static final Logger LOG = LoggerFactory.getLogger(UCAuthEbscoClientImpl.class);

  private static final int PORT = 443;
  private static final int TIMEOUT = 20000;
  private static final String REQUEST_URI = "/oauth-proxy/token";

  private HttpRequest<JsonObject> tokenRequest;

  public UCAuthEbscoClientImpl(@Value("${kb.ebsco.uc.auth.host:apis.ebsco.com}") String host, Vertx vertx) {
    WebClientOptions options = new WebClientOptions();
    options.setLogActivity(true);
    WebClient webClient = WebClient.create(vertx, options);
    tokenRequest = webClient
      .post(PORT, host, REQUEST_URI)
      .ssl(true)
      .timeout(TIMEOUT)
      .expect(ResponsePredicate.SC_OK)
      .as(BodyCodec.jsonObject());
  }

  @Override
  public CompletableFuture<UCAuthToken> requestToken(String clientId, String clientSecret) {
    Promise<HttpResponse<JsonObject>> promise = Promise.promise();
    LOG.info("Request UC Token");
    tokenRequest.sendForm(createRequestBody(clientId, clientSecret), promise);
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
      return Future.failedFuture(new UcAuthenticationException(throwable.getMessage()));};
  }
}
