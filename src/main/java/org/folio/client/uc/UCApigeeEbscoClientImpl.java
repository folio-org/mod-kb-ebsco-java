package org.folio.client.uc;

import java.util.concurrent.CompletableFuture;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.client.impl.HttpContext;
import io.vertx.ext.web.client.impl.HttpRequestImpl;
import io.vertx.ext.web.client.impl.WebClientInternal;
import io.vertx.ext.web.client.predicate.ResponsePredicate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class UCApigeeEbscoClientImpl implements UCApigeeEbscoClient {

  private static final Logger LOG = LoggerFactory.getLogger(UCApigeeEbscoClientImpl.class);

  private static final int TIMEOUT = 20000;
  private static final String VERIFY_URI = "/uc/costperuse/package/1"
    + "?fiscalYear=2018&fiscalMonth=DEC&analysisCurrency=USD&aggregatedFullText=true";

  private final String baseUrl;
  private final WebClient webClient;

  public UCApigeeEbscoClientImpl(@Value("${kb.ebsco.uc.auth.url}") String baseUrl, Vertx vertx) {
    this.baseUrl = baseUrl;
    WebClientOptions options = new WebClientOptions();
    options.setLogActivity(true);
    this.webClient = WebClient.create(vertx, options);
    ((WebClientInternal) webClient).addInterceptor(loggingInterceptor());
  }

  @Override
  public CompletableFuture<Boolean> verifyCredentials(UCConfiguration configuration) {
    CompletableFuture<Boolean> result = new CompletableFuture<>();
    constructGetRequest(VERIFY_URI, configuration)
      .send(event -> result.complete(event.succeeded()));
    return result;
  }

  private Handler<HttpContext<?>> loggingInterceptor() {
    return httpContext -> {
      HttpRequestImpl<?> request = (HttpRequestImpl<?>) httpContext.request();
      HttpMethod method = request.method();
      String uri = request.uri();
      Object body = httpContext.body();
      LOG.info("Request APIGEE {} {} with body {}", method, uri, body);
      httpContext.next();
    };
  }

  private HttpRequest<Buffer> constructGetRequest(String uri, UCConfiguration configuration) {
    return webClient
      .getAbs(baseUrl + uri)
      .timeout(TIMEOUT)
      .bearerTokenAuthentication(configuration.getAccessToken())
      .putHeader("custkey", configuration.getCustomerKey())
      .expect(ResponsePredicate.SC_OK);
  }
}
