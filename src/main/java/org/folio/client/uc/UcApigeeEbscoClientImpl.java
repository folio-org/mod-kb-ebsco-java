package org.folio.client.uc;

import static org.folio.util.FutureUtils.mapVertxFuture;

import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
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
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import lombok.extern.log4j.Log4j2;
import org.apache.http.HttpHeaders;
import org.apache.http.entity.ContentType;
import org.folio.client.uc.configuration.GetPackageUcConfiguration;
import org.folio.client.uc.configuration.GetTitlePackageUcConfiguration;
import org.folio.client.uc.configuration.GetTitleUcConfiguration;
import org.folio.client.uc.configuration.UcConfiguration;
import org.folio.client.uc.model.UcCostAnalysis;
import org.folio.client.uc.model.UcMetricType;
import org.folio.client.uc.model.UcPackageCostPerUse;
import org.folio.client.uc.model.UcTitleCostPerUse;
import org.folio.client.uc.model.UcTitlePackageId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Log4j2
@Component
public class UcApigeeEbscoClientImpl implements UcApigeeEbscoClient {

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
  private static final String GET_METRIC_URI = "/uc/usageanalysis/analysismetrictype";

  private final String baseUrl;
  private final WebClient webClient;

  public UcApigeeEbscoClientImpl(@Value("${kb.ebsco.uc.auth.url}") String baseUrl, Vertx vertx) {
    this.baseUrl = baseUrl;
    WebClientOptions options = new WebClientOptions();
    options.setLogActivity(true);
    this.webClient = WebClient.create(vertx, options);
    ((WebClientInternal) webClient).addInterceptor(loggingInterceptor());
  }

  @Override
  public CompletableFuture<Boolean> verifyCredentials(UcConfiguration configuration) {
    CompletableFuture<Boolean> result = new CompletableFuture<>();
    constructGetRequest(VERIFY_URI, configuration)
      .expect(ResponsePredicate.SC_OK)
      .send(event -> result.complete(event.succeeded()));
    return result;
  }

  @Override
  public CompletableFuture<UcTitleCostPerUse> getTitleCostPerUse(String titleId, String packageId,
                                                                 GetTitleUcConfiguration configuration) {
    Promise<HttpResponse<JsonObject>> promise = Promise.promise();
    constructGetRequest(constructGetTitleUri(titleId, packageId, configuration), configuration)
      .expect(ResponsePredicate.create(ResponsePredicate.SC_OK, errorConverter()))
      .as(BodyCodec.jsonObject())
      .send(promise);
    return mapVertxFuture(promise.future())
      .thenApply(HttpResponse::body)
      .thenApply(jsonObject -> jsonObject.mapTo(UcTitleCostPerUse.class));
  }

  @Override
  public CompletableFuture<UcPackageCostPerUse> getPackageCostPerUse(String packageId,
                                                                     GetPackageUcConfiguration configuration) {
    Promise<HttpResponse<JsonObject>> promise = Promise.promise();
    constructGetRequest(constructGetPackageUri(packageId, configuration), configuration)
      .expect(ResponsePredicate.create(ResponsePredicate.SC_OK, errorConverter()))
      .as(BodyCodec.jsonObject())
      .send(promise);
    return mapVertxFuture(promise.future())
      .thenApply(HttpResponse::body)
      .thenApply(jsonObject -> jsonObject.mapTo(UcPackageCostPerUse.class));
  }

  @Override
  public CompletableFuture<Map<String, UcCostAnalysis>> getTitlePackageCostPerUse(
    List<UcTitlePackageId> ids,
    GetTitlePackageUcConfiguration configuration) {
    Promise<HttpResponse<JsonObject>> promise = Promise.promise();
    constructPostRequest(constructPostTitlesUri(configuration), configuration)
      .expect(ResponsePredicate.create(ResponsePredicate.SC_OK, errorConverter()))
      .as(BodyCodec.jsonObject())
      .sendJson(ids, promise);
    return mapVertxFuture(promise.future())
      .thenApply(HttpResponse::body)
      .thenApply(this::toCostAnalysisMap);
  }

  @Override
  public CompletableFuture<UcMetricType> getUsageMetricType(UcConfiguration configuration) {
    Promise<HttpResponse<JsonObject>> promise = Promise.promise();
    constructGetRequest(GET_METRIC_URI, configuration)
      .expect(ResponsePredicate.create(ResponsePredicate.SC_OK, errorConverter()))
      .as(BodyCodec.jsonObject())
      .send(promise);
    return mapVertxFuture(promise.future())
      .thenApply(HttpResponse::body)
      .thenApply(jsonObject -> jsonObject.mapTo(UcMetricType.class));
  }

  private Map<String, UcCostAnalysis> toCostAnalysisMap(JsonObject jsonObject) {
    return jsonObject.stream()
      .collect(Collectors.toMap(Map.Entry::getKey, o -> JsonObject.mapFrom(o.getValue()).mapTo(UcCostAnalysis.class)));
  }

  private String constructGetTitleUri(String titleId, String packageId, GetTitleUcConfiguration configuration) {
    String uriPath = String.format(GET_TITLE_URI, titleId, packageId);
    String uriParams = String.format(getRequestParams(), configuration.getFiscalYear(), configuration.getFiscalMonth(),
      configuration.getAnalysisCurrency(), configuration.isAggregatedFullText());
    return uriPath + "?" + uriParams;
  }

  private String constructGetPackageUri(String packageId, GetPackageUcConfiguration configuration) {
    String uriPath = String.format(GET_PACKAGE_URI, packageId);
    String uriParams = String.format(getRequestParams(), configuration.getFiscalYear(), configuration.getFiscalMonth(),
      configuration.getAnalysisCurrency(), configuration.isAggregatedFullText());
    return uriPath + "?" + uriParams;
  }

  private String constructPostTitlesUri(GetTitlePackageUcConfiguration configuration) {
    String uriParams = String.format(postRequestParams(), configuration.getFiscalYear(), configuration.getFiscalMonth(),
      configuration.getAnalysisCurrency(), configuration.isPublisherPlatform(), configuration.isPreviousYear());
    return POST_TITLES_URI + "?" + uriParams;
  }

  private Handler<HttpContext<?>> loggingInterceptor() {
    return httpContext -> {
      if (ClientPhase.SEND_REQUEST == httpContext.phase()) {
        HttpRequestImpl<?> request = (HttpRequestImpl<?>) httpContext.request();
        String uri = request.uri();
        Object body = httpContext.body();
        log.trace("Request sends to APIGEE: {} with body {}", uri, body);
      } else if (ClientPhase.RECEIVE_RESPONSE == httpContext.phase()) {
        HttpRequestImpl<?> request = (HttpRequestImpl<?>) httpContext.request();
        String uri = request.uri();
        log.trace("Response received from APIGEE: {}", uri);
      }
      httpContext.next();
    };
  }

  private HttpRequest<Buffer> constructGetRequest(String uri, UcConfiguration configuration) {
    return configureRequest(configuration, webClient.getAbs(baseUrl + uri));
  }

  private HttpRequest<Buffer> constructPostRequest(String uri, UcConfiguration configuration) {
    return configureRequest(configuration, webClient.postAbs(baseUrl + uri));
  }

  private HttpRequest<Buffer> configureRequest(UcConfiguration configuration, HttpRequest<Buffer> httpRequest) {
    return httpRequest
      .bearerTokenAuthentication(configuration.getAccessToken())
      .putHeader("custkey", configuration.getCustomerKey());
  }

  private ErrorConverter errorConverter() {
    return ErrorConverter.createFullBody(result -> {
      HttpResponse<Buffer> response = result.response();
      if (ContentType.APPLICATION_JSON.getMimeType().equals(response.getHeader(HttpHeaders.CONTENT_TYPE))) {
        JsonObject body = response.bodyAsJsonObject();
        return new UcFailedRequestException(response.statusCode(), body);
      }

      return new UcFailedRequestException(response.statusCode(), response.bodyAsString());
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
