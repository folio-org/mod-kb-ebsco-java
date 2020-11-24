package org.folio.client.uc;

import static org.folio.util.FutureUtils.mapVertxFuture;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

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

import org.folio.client.uc.configuration.GetPackageUCConfiguration;
import org.folio.client.uc.configuration.GetTitlePackageUCConfiguration;
import org.folio.client.uc.configuration.GetTitleUCConfiguration;
import org.folio.client.uc.configuration.UCConfiguration;
import org.folio.client.uc.model.UCCostAnalysis;
import org.folio.client.uc.model.UCPackageCostPerUse;
import org.folio.client.uc.model.UCTitleCostPerUse;
import org.folio.client.uc.model.UCTitlePackageId;

@Component
public class UCApigeeEbscoClientImpl implements UCApigeeEbscoClient {

  private static final Logger LOG = LoggerFactory.getLogger(UCApigeeEbscoClientImpl.class);

//  private static final int TIMEOUT = 50000;

  private static final String FISCAL_YEAR_PARAM = "fiscalYear";
  private static final String FISCAL_MONTH_PARAM = "fiscalMonth";
  private static final String ANALYSIS_CURRENCY_PARAM = "analysisCurrency";
  private static final String AGGREGATED_FULL_TEXT_PARAM = "aggregatedFullText";
  private static final String PUBLISHER_PLATFORM_PARAM = "publisherPlatform";
  private static final String PREVIOUS_YEAR_PARAM = "previousYear";

  private static final String VERIFY_URI = "/uc/costperuse/package/1"
    + "?" + FISCAL_YEAR_PARAM + "=2018&" + FISCAL_MONTH_PARAM + "=DEC&" + ANALYSIS_CURRENCY_PARAM + "=USD&"
    + AGGREGATED_FULL_TEXT_PARAM + "=true";

  private static final String POST_TITLES_URI = "/uc/costperuse/titles";
  private static final String GET_TITLE_URI = "/uc/costperuse/title/%s/%s";
  private static final String GET_PACKAGE_URI = "/uc/costperuse/package/%s";

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

  @Override
  public CompletableFuture<UCPackageCostPerUse> getPackageCostPerUse(String packageId,
                                                                     GetPackageUCConfiguration configuration) {
    Promise<HttpResponse<JsonObject>> promise = Promise.promise();
    constructGetRequest(constructGetPackageUri(packageId, configuration), configuration)
      .expect(ResponsePredicate.create(ResponsePredicate.SC_OK, errorConverter()))
      .as(BodyCodec.jsonObject())
      .send(promise);
    return mapVertxFuture(promise.future())
      .thenApply(HttpResponse::body)
      .thenApply(jsonObject -> jsonObject.mapTo(UCPackageCostPerUse.class));
  }

  @Override
  public CompletableFuture<Map<String, UCCostAnalysis>> getTitlePackageCostPerUse(List<UCTitlePackageId> ids,
                                                                                  GetTitlePackageUCConfiguration configuration) {
    Promise<HttpResponse<JsonObject>> promise = Promise.promise();
    constructPostRequest(constructPostTitlesUri(configuration), configuration)
      .expect(ResponsePredicate.create(ResponsePredicate.SC_OK, errorConverter()))
      .as(BodyCodec.jsonObject())
      .sendJson(ids, promise);
    return mapVertxFuture(promise.future())
      .thenApply(HttpResponse::body)
      .thenApply(this::toCostAnalysisMap);
  }

  private Map<String, UCCostAnalysis> toCostAnalysisMap(JsonObject jsonObject) {
    return jsonObject.stream()
      .collect(Collectors.toMap(Map.Entry::getKey, o -> JsonObject.mapFrom(o.getValue()).mapTo(UCCostAnalysis.class)));
  }

  private String constructGetTitleUri(String titleId, String packageId, GetTitleUCConfiguration configuration) {
    String uriPath = String.format(GET_TITLE_URI, titleId, packageId);
    String uriParams = String.format(getRequestParams(), configuration.getFiscalYear(), configuration.getFiscalMonth(),
      configuration.getAnalysisCurrency(), configuration.isAggregatedFullText());
    return uriPath + "?" + uriParams;
  }

  private String constructGetPackageUri(String packageId, GetPackageUCConfiguration configuration) {
    String uriPath = String.format(GET_PACKAGE_URI, packageId);
    String uriParams = String.format(getRequestParams(), configuration.getFiscalYear(), configuration.getFiscalMonth(),
      configuration.getAnalysisCurrency(), configuration.isAggregatedFullText());
    return uriPath + "?" + uriParams;
  }

  private String constructPostTitlesUri(GetTitlePackageUCConfiguration configuration) {
    String uriParams = String.format(postRequestParams(), configuration.getFiscalYear(), configuration.getFiscalMonth(),
      configuration.getAnalysisCurrency(), configuration.isPublisherPlatform(), configuration.isPreviousYear());
    return POST_TITLES_URI + "?" + uriParams;
  }

  private Handler<HttpContext<?>> loggingInterceptor() {
    return httpContext -> {
      if (ClientPhase.SEND_REQUEST == httpContext.phase()) {
        HttpRequestImpl<?> request = (HttpRequestImpl<?>) httpContext.request();
        HttpMethod method = request.method();
        String uri = request.uri();
        Object body = httpContext.body();
        LOG.info("Request sends to APIGEE {} {} with body {}", method, uri, body);
      } else if (ClientPhase.RECEIVE_RESPONSE == httpContext.phase()) {
        HttpRequestImpl<?> request = (HttpRequestImpl<?>) httpContext.request();
        HttpMethod method = request.method();
        String uri = request.uri();
        LOG.info("Response received from APIGEE {} {}", method, uri);
      }
      httpContext.next();
    };
  }

  private HttpRequest<Buffer> constructGetRequest(String uri, UCConfiguration configuration) {
    return configureRequest(configuration, webClient.getAbs(baseUrl + uri));
  }

  private HttpRequest<Buffer> constructPostRequest(String uri, UCConfiguration configuration) {
    return configureRequest(configuration, webClient.postAbs(baseUrl + uri));
  }

  private HttpRequest<Buffer> configureRequest(UCConfiguration configuration, HttpRequest<Buffer> httpRequest) {
    return httpRequest
//      .timeout(TIMEOUT)
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

  private String postRequestParams() {
    return String.join("&",
      param(FISCAL_YEAR_PARAM),
      param(FISCAL_MONTH_PARAM),
      param(ANALYSIS_CURRENCY_PARAM),
      param(PUBLISHER_PLATFORM_PARAM),
      param(PREVIOUS_YEAR_PARAM)
    );
  }

  private String getRequestParams() {
    return String.join("&",
      param(FISCAL_YEAR_PARAM),
      param(FISCAL_MONTH_PARAM),
      param(ANALYSIS_CURRENCY_PARAM),
      param(AGGREGATED_FULL_TEXT_PARAM)
    );
  }

  private String param(String paramName) {
    return paramName + "=%s";
  }
}
