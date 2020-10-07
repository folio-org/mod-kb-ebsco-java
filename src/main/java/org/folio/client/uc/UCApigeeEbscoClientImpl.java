package org.folio.client.uc;

import static org.folio.util.FutureUtils.mapVertxFuture;

import java.util.concurrent.CompletableFuture;

import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.client.impl.ClientPhase;
import io.vertx.ext.web.client.impl.HttpContext;
import io.vertx.ext.web.client.impl.HttpRequestImpl;
import io.vertx.ext.web.client.impl.WebClientInternal;
import io.vertx.ext.web.client.predicate.ErrorConverter;
import io.vertx.ext.web.client.predicate.ResponsePredicate;
import io.vertx.ext.web.codec.BodyCodec;
import org.apache.http.HttpHeaders;
import org.apache.http.entity.ContentType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import org.folio.client.uc.configuration.GetTitleUCConfiguration;
import org.folio.client.uc.configuration.UCConfiguration;
import org.folio.client.uc.model.UCTitleCostPerUse;

@Component
public class UCApigeeEbscoClientImpl implements UCApigeeEbscoClient {

  private static final Logger LOG = LoggerFactory.getLogger(UCApigeeEbscoClientImpl.class);

  private static final int TIMEOUT = 20000;
  private static final String VERIFY_URI = "/uc/costperuse/package/1"
    + "?fiscalYear=2018&fiscalMonth=DEC&analysisCurrency=USD&aggregatedFullText=true";

  private static final String GET_TITLE_URI = "/uc/costperuse/title/%s/%s";
  private static final String GET_TITLE_URI_PARAMS =
    "fiscalYear=%s&fiscalMonth=%s&analysisCurrency=%s&aggregatedFullText=%s";

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
      .expect(ResponsePredicate.SC_OK)
      .send(event -> result.complete(event.succeeded()));
    return result;
  }

  @Override
  public CompletableFuture<UCTitleCostPerUse> getTitleCostPerUse(String titleId, String packageId,
                                                                 GetTitleUCConfiguration configuration) {
    Promise<HttpResponse<JsonObject>> promise = Promise.promise();
    constructGetRequest(constructGetTitleUri(titleId, packageId, configuration), configuration)
      .expect(ResponsePredicate.create(ResponsePredicate.SC_OK, errorConverter()))
      .as(BodyCodec.jsonObject())
      .send(promise);
    return mapVertxFuture(promise.future())
      .thenApply(HttpResponse::body)
      .thenApply(jsonObject -> jsonObject.mapTo(UCTitleCostPerUse.class));
  }

  private String constructGetTitleUri(String titleId, String packageId, GetTitleUCConfiguration configuration) {
    String uriPath = String.format(GET_TITLE_URI, titleId, packageId);
    String uriParams = String.format(GET_TITLE_URI_PARAMS, configuration.getFiscalYear(), configuration.getFiscalMonth(),
      configuration.getAnalysisCurrency(), configuration.isAggregatedFullText());
    return uriPath + "?" + uriParams;
  }

  private Handler<HttpContext<?>> loggingInterceptor() {
    return httpContext -> {
      if (httpContext.phase() == ClientPhase.SEND_REQUEST) {
        HttpRequestImpl<?> request = (HttpRequestImpl<?>) httpContext.request();
        HttpMethod method = request.method();
        String uri = request.uri();
        Object body = httpContext.body();
        LOG.info("Request APIGEE {} {} with body {}", method, uri, body);
      }
      httpContext.next();
    };
  }

  private HttpRequest<Buffer> constructGetRequest(String uri, UCConfiguration configuration) {
    return webClient
      .getAbs(baseUrl + uri)
      .timeout(TIMEOUT)
      .bearerTokenAuthentication(configuration.getAccessToken())
      .putHeader("custkey", configuration.getCustomerKey());
  }

  private ErrorConverter errorConverter() {
    return ErrorConverter.createFullBody(result -> {
      HttpResponse<Buffer> response = result.response();
      if (ContentType.APPLICATION_JSON.getMimeType().equals(response.getHeader(HttpHeaders.CONTENT_TYPE))) {
        JsonObject body = response.bodyAsJsonObject();
        return new UCFailedRequestException(response.statusCode(), body);
      }

      return new UCFailedRequestException(response.statusCode(), response.bodyAsString());
    });
  }
}
