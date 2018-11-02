package org.folio.rmapi;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.folio.rest.model.Sort;
import org.folio.rmapi.builder.QueriableUrlBuilder;
import org.folio.rmapi.builder.TitlesFilterableUrlBuilder;
import org.folio.rmapi.exception.RMAPIResourceNotFoundException;
import org.folio.rmapi.exception.RMAPIResultsProcessingException;
import org.folio.rmapi.exception.RMAPIServiceException;
import org.folio.rmapi.exception.RMAPIUnAuthorizedException;
import org.folio.rmapi.model.VendorById;
import org.folio.rmapi.model.Vendors;
import org.folio.rmapi.model.Titles;

import java.util.concurrent.CompletableFuture;

public class RMAPIService {

  private static final Logger LOG = LoggerFactory.getLogger(RMAPIService.class);
  private static final String HTTP_HEADER_CONTENT_TYPE = "Content-type";
  private static final String APPLICATION_JSON = "application/json";
  private static final String HTTP_HEADER_ACCEPT = "Accept";
  private static final String RMAPI_API_KEY = "X-Api-Key";

  private static final String JSON_RESPONSE_ERROR = "Error processing RMAPI Response";
  private static final String INVALID_RMAPI_RESPONSE = "Invalid RMAPI response";

  private static final String VENDOR_NAME_PARAMETER = "vendorname";

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
            new RMAPIResourceNotFoundException(String.format("Requested resource %s not found, response body =\"%s\"", query, body.toString())));
        } else if ((response.statusCode() == 401) || (response.statusCode() == 403)) {
          future.completeExceptionally(
            new RMAPIUnAuthorizedException(String.format("Unauthorized Access to %s, response body =\"%s\"", request.absoluteURI(), body.toString())));
        } else {

          future
            .completeExceptionally(new RMAPIServiceException(
              String.format("%s Code = %s Message = %s Body = %s", INVALID_RMAPI_RESPONSE, response.statusCode(),
                response.statusMessage(), body.toString()),
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

  public CompletableFuture<Vendors> retrieveProviders(String q, int page, int count, Sort sort) {
    String query = new QueriableUrlBuilder()
        .q(q)
        .page(page)
        .count(count)
        .sort(sort)
        .nameParameter(VENDOR_NAME_PARAMETER)
        .build();
    return this.getRequest(constructURL("vendors?" + query), Vendors.class);
  }

  public CompletableFuture<Titles> retrieveTitles(String filterSelected, String filterType, String filterName, String filterIsxn, String filterSubject,
                                                  String filterPublisher, Sort sort, int page, int count) {
    String path = new TitlesFilterableUrlBuilder()
      .filterSelected(filterSelected)
      .filterType(filterType)
      .filterName(filterName)
      .filterIsxn(filterIsxn)
      .filterSubject(filterSubject)
      .filterPublisher(filterPublisher)
      .sort(sort)
      .page(page)
      .count(count)
      .build();
    return this.getRequest(constructURL("titles?" + path), Titles.class);
  }

    public CompletableFuture<VendorById> retrieveProvider(String id, String include) {

    final String path = "vendors/" + id;
//    TODO: add support of include parameter when MODKBEKBJ-22 is completed

    return this.getRequest(constructURL(path), VendorById.class);
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
