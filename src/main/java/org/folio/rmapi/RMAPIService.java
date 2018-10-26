package org.folio.rmapi;

import com.google.common.base.Strings;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.folio.rmapi.exception.RMAPIResourceNotFoundException;
import org.folio.rmapi.exception.RMAPIResultsProcessingException;
import org.folio.rmapi.exception.RMAPIServiceException;
import org.folio.rmapi.exception.RMAPIUnAuthorizedException;
import org.folio.rmapi.model.Vendors;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class RMAPIService {

  private static final Logger LOG = LoggerFactory.getLogger(RMAPIService.class);
  private static final String HTTP_HEADER_CONTENT_TYPE = "Content-type";
  private static final String APPLICATION_JSON = "application/json";
  private static final String HTTP_HEADER_ACCEPT = "Accept";
  private static final String RMAPI_API_KEY = "X-Api-Key";

  private static final String JSON_RESPONSE_ERROR = "Error processing RMAPI Response";
  private static final String INVALID_RMAPI_RESPONSE = "Invalid RMAPI response";
  private static final String VENDOR_NAME_PARAMETER = "VendorName";
  private static final String RELEVANCE_PARAMETER = "Relevance";

  private String customerId;
  private String apiKey;
  private String baseURI;

  private Vertx vertx;

  public RMAPIService(String customerId, String apiKey, String baseURI, Vertx vertx) {
    this.customerId = customerId;
    this.apiKey = apiKey;
    this.baseURI = baseURI;
    this.vertx = vertx;
  }

  private <T> CompletableFuture<T> getRequest(String query, Class<T> clazz) {

    CompletableFuture<T> future = new CompletableFuture<>();

    HttpClient httpClient = vertx.createHttpClient();

    final HttpClientRequest request = httpClient.getAbs(query);

    request.headers().add(HTTP_HEADER_ACCEPT, APPLICATION_JSON);
    request.headers().add(HTTP_HEADER_CONTENT_TYPE, APPLICATION_JSON);
    request.headers().add(RMAPI_API_KEY, apiKey);

    LOG.info("RMAPI Service absolute URL is" + request.absoluteURI());

    request.handler(response -> response.bodyHandler(body -> {
      httpClient.close();
      if (response.statusCode() == 200) {
        try {
          final JsonObject instanceJSON = new JsonObject(body.toString());
          T results = instanceJSON.mapTo(clazz);
          future.complete(results);
        } catch (Exception e) {
          LOG.error(
            String.format("%s - Response = [%s] Target Type = [%s]", JSON_RESPONSE_ERROR, body.toString(), clazz));
          future.completeExceptionally(
            new RMAPIResultsProcessingException(String.format("%s for query = %s", JSON_RESPONSE_ERROR, query), e));
        }
      } else {
        LOG.error(String.format("%s status code = [%s] status message = [%s] query = [%s] body = [%s]",
          INVALID_RMAPI_RESPONSE, response.statusCode(), response.statusMessage(), query, body.toString()));

        if (response.statusCode() == 404) {
          future.completeExceptionally(
            new RMAPIResourceNotFoundException(String.format("Requested resource %s not found", query)));
        } else if ((response.statusCode() == 401) || (response.statusCode() == 403)) {
          future.completeExceptionally(
            new RMAPIUnAuthorizedException(String.format("Unauthorized Access to %s", request.absoluteURI())));
        } else {

          future
            .completeExceptionally(new RMAPIServiceException(
              String.format("%s Code = %s Message = %s", INVALID_RMAPI_RESPONSE, response.statusCode(),
                response.statusMessage()),
              response.statusCode(), response.statusMessage(), body.toString(), query));
        }
      }

    }))
      .exceptionHandler(future::completeExceptionally);

    request.end();

    return future;

  }

  public CompletableFuture<Object> verifyCredentials() {
    return this.getRequest(constructURL("vendors?search=zz12&offset=1&orderby=vendorname&count=1"), Object.class);
  }

  public CompletableFuture<Vendors> retrieveProviders(String q, int page, int count, String sort) {
    List<String> parameters = new ArrayList<>();
    if (!Strings.isNullOrEmpty(q)) {
      String encodedQuery;
      try {
        encodedQuery = URLEncoder.encode(q, "UTF-8");
      } catch (UnsupportedEncodingException e) {
        throw new IllegalStateException("failed to encode query using UTF-8");
      }
      parameters.add("search=" + encodedQuery);
    }
    parameters.add("offset=" + page);
    parameters.add("count=" + count);
    parameters.add("orderby=" + determineSortValue(sort, q));

    return this.getRequest(constructURL("vendors?" + String.join("&", parameters)), Vendors.class);
  }

  private String determineSortValue(String sort, String query) {
    if(sort == null){
      return query == null ? VENDOR_NAME_PARAMETER : RELEVANCE_PARAMETER;
    }
    if(sort.equalsIgnoreCase("relevance")){
      return RELEVANCE_PARAMETER;
    }else if(sort.equalsIgnoreCase("name")){
      return VENDOR_NAME_PARAMETER;
    }
    throw new IllegalArgumentException("Invalid value for sort - " + sort);
  }

  /**
   * Constructs full rmapi path
   *
   * @param path path appended to the end of url
   */
  private String constructURL(String path) {
    String fullPath = String.format("%s/rm/rmaccounts/%s/%s", baseURI, customerId, path);

    LOG.info("constructurl - path=" + fullPath);
    return fullPath;
  }
}
