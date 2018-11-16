package org.folio.rmapi;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import org.folio.rest.jaxrs.model.RootProxyPutRequest;
import org.folio.rest.model.PackageId;
import org.folio.rest.model.Sort;
import org.folio.rmapi.builder.QueriableUrlBuilder;
import org.folio.rmapi.builder.TitlesFilterableUrlBuilder;
import org.folio.rmapi.exception.RMAPIResourceNotFoundException;
import org.folio.rmapi.exception.RMAPIResultsProcessingException;
import org.folio.rmapi.exception.RMAPIServiceException;
import org.folio.rmapi.exception.RMAPIUnAuthorizedException;
import org.folio.rmapi.model.PackageData;
import org.folio.rmapi.model.PackageSelectedPayload;
import org.folio.rmapi.model.Title;
import org.folio.rmapi.model.Titles;
import org.folio.rmapi.model.VendorById;
import org.folio.rmapi.model.VendorPut;
import org.folio.rmapi.model.Vendors;
import org.folio.rmapi.model.RootProxyCustomLabels;

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

  private static final String VENDORS_PATH = "vendors";
  private static final String PACKAGES_PATH = "packages";
  private static final String TITLES_PATH = "titles";

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

  private <T> void handleRMAPIError(HttpClientResponse response, String query, Buffer body,
      CompletableFuture<T> future) {

    LOG.error(String.format("%s status code = [%s] status message = [%s] query = [%s] body = [%s]",
      INVALID_RMAPI_RESPONSE, response.statusCode(), response.statusMessage(), query, body.toString()));

    if (response.statusCode() == 404) {
      future.completeExceptionally(new RMAPIResourceNotFoundException(
        String.format("Requested resource %s not found", query), response.statusCode(), response.statusMessage(), body.toString(), query));
    } else if ((response.statusCode() == 401) || (response.statusCode() == 403)) {
      future.completeExceptionally(new RMAPIUnAuthorizedException(
        String.format("Unauthorized Access to %s", query), response.statusCode(), response.statusMessage(), body.toString(), query));
    } else {

      future.completeExceptionally(new RMAPIServiceException(
        String.format("%s Code = %s Message = %s Body = %s", INVALID_RMAPI_RESPONSE, response.statusCode(),
          response.statusMessage(), body.toString()),
        response.statusCode(), response.statusMessage(), body.toString(), query));
    }
  }

  private <T> CompletableFuture<T> getRequest(String query, Class<T> clazz) {

    CompletableFuture<T> future = new CompletableFuture<>();

    HttpClient httpClient = vertx.createHttpClient();

    final HttpClientRequest request = httpClient.getAbs(query);

    request.headers().add(HTTP_HEADER_ACCEPT, APPLICATION_JSON);
    request.headers().add(HTTP_HEADER_CONTENT_TYPE, APPLICATION_JSON);
    request.headers().add(RMAPI_API_KEY, apiKey);

    LOG.info("RMAPI Service GET absolute URL is:" + request.absoluteURI());

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

        handleRMAPIError(response, query, body, future);
      }
    })).exceptionHandler(future::completeExceptionally);

    request.end();

    return future;

  }

  private <T> CompletableFuture<Void> putRequest(String query, T putData) {

    CompletableFuture<Void> future = new CompletableFuture<>();

    HttpClient httpClient = vertx.createHttpClient();

    final HttpClientRequest request = httpClient.putAbs(query);

    request.headers().add(HTTP_HEADER_ACCEPT, APPLICATION_JSON);
    request.headers().add(HTTP_HEADER_CONTENT_TYPE, APPLICATION_JSON);
    request.headers().add(RMAPI_API_KEY, apiKey);

    LOG.info("RMAPI Service PUT absolute URL is:" + request.absoluteURI());

    request.handler(response -> response.bodyHandler(body -> {
      httpClient.close();
      if (response.statusCode() == 204) {
        future.complete(null);
      } else {
        handleRMAPIError(response, query, body, future);
      }

    })).exceptionHandler(future::completeExceptionally);

    String encodedBody = Json.encodePrettily(putData);
    LOG.info("RMAPI Service PUT body is:" + encodedBody);
    request.end(encodedBody);

    return future;

  }

  public CompletableFuture<Object> verifyCredentials() {
    return this.getRequest(constructURL(VENDORS_PATH + "?search=zz12&offset=1&orderby=vendorname&count=1"), Object.class);
  }

  public CompletableFuture<Vendors> retrieveProviders(String q, int page, int count, Sort sort) {
    String query = new QueriableUrlBuilder()
        .q(q)
        .page(page)
        .count(count)
        .sort(sort)
        .nameParameter(VENDOR_NAME_PARAMETER)
        .build();
    return this.getRequest(constructURL(VENDORS_PATH + "?" + query), Vendors.class);
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
    return this.getRequest(constructURL(TITLES_PATH + "?" + path), Titles.class);
  }

  public CompletableFuture<VendorById> retrieveProvider(long id, String include) {

    final String path = VENDORS_PATH + '/' + id;
    // TODO: add support of include parameter when MODKBEKBJ-22 is completed

    return this.getRequest(constructURL(path), VendorById.class);
  }

  public CompletableFuture<VendorById> updateProvider(long id, VendorPut rmapiVendor) {

    final String path = VENDORS_PATH + '/' + id;

    return this.putRequest(constructURL(path), rmapiVendor).thenCompose(vend -> this.retrieveProvider(id, ""));

  }

  public CompletableFuture<PackageData> retrievePackage(PackageId packageId) {
    final String path = VENDORS_PATH + '/' + packageId.getProviderIdPart() + '/' + PACKAGES_PATH + '/' + packageId.getPackageIdPart();
    return this.getRequest(constructURL(path), PackageData.class);
  }

  public CompletableFuture<Void> deletePackage(PackageId packageId) {
    final String path = VENDORS_PATH + '/' + packageId.getProviderIdPart() + '/' + PACKAGES_PATH + '/' + packageId.getPackageIdPart();
    return this.putRequest(constructURL(path),new PackageSelectedPayload(false));
  }

  public CompletableFuture<RootProxyCustomLabels> retrieveRootProxyCustomLabels() {
    final String path = "";

    return this.getRequest(constructURL(path), RootProxyCustomLabels.class);
  }
  
  public CompletableFuture<RootProxyCustomLabels> updateRootProxyCustomLabels(RootProxyPutRequest rootProxyPutRequest, RootProxyCustomLabels rootProxyCustomLabels) {
    final String path = "";
    
    org.folio.rmapi.model.Proxy proxyRMAPI = new org.folio.rmapi.model.Proxy();   
    proxyRMAPI.setId(rootProxyPutRequest.getData().getAttributes().getProxyTypeId());
    rootProxyCustomLabels.setProxy(proxyRMAPI);
    return this.putRequest(constructURL(path), rootProxyCustomLabels).thenCompose(updatedRootProxy -> this.retrieveRootProxyCustomLabels());
  }

  public CompletableFuture<Title> retrieveTitle(long id) {

    final String path = TITLES_PATH + '/' + id;
    return this.getRequest(constructURL(path), Title.class);
  }

  /**
   * Constructs full rmapi path
   *
   * @param path
   *          path appended to the end of url
   */
  private String constructURL(String path) {
    String fullPath = String.format("%s/rm/rmaccounts/%s/%s", baseURI, customerId, path);

    LOG.info("constructurl - path=" + fullPath);
    return fullPath;
  }
}
