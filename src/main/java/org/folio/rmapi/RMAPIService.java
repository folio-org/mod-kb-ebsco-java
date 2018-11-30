package org.folio.rmapi;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.mutable.MutableObject;
import org.folio.rest.jaxrs.model.RootProxyPutRequest;
import org.folio.rest.model.FilterQuery;
import org.folio.rest.model.PackageId;
import org.folio.rest.model.ResourceId;
import org.folio.rest.model.Sort;
import org.folio.rmapi.builder.PackagesFilterableUrlBuilder;
import org.folio.rmapi.builder.QueriableUrlBuilder;
import org.folio.rmapi.builder.TitlesFilterableUrlBuilder;
import org.folio.rmapi.exception.RMAPIResourceNotFoundException;
import org.folio.rmapi.exception.RMAPIResultsProcessingException;
import org.folio.rmapi.exception.RMAPIServiceException;
import org.folio.rmapi.exception.RMAPIUnAuthorizedException;
import org.folio.rmapi.model.CustomLabel;
import org.folio.rmapi.model.PackageByIdData;
import org.folio.rmapi.model.PackagePut;
import org.folio.rmapi.model.PackageCreated;
import org.folio.rmapi.model.PackagePost;
import org.folio.rmapi.model.PackageSelectedPayload;
import org.folio.rmapi.model.Packages;
import org.folio.rmapi.model.Proxies;
import org.folio.rmapi.model.Proxy;
import org.folio.rmapi.model.RootProxyCustomLabels;
import org.folio.rmapi.model.Title;
import org.folio.rmapi.model.Titles;
import org.folio.rmapi.model.Vendor;
import org.folio.rmapi.model.VendorById;
import org.folio.rmapi.model.VendorPut;
import org.folio.rmapi.model.Vendors;
import org.folio.rmapi.result.ResourceResult;
import org.folio.rmapi.result.VendorResult;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

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

  private static final String VENDOR_LOWER_STRING = "vendor";
  private static final String PROVIDER_LOWER_STRING = "provider";
  private static final String VENDOR_UPPER_STRING = "Vendor";
  private static final String PROVIDER_UPPER_STRING = "Provider";
  private static final String INCLUDE_PACKAGES_VALUE = "packages";

  private static final String INCLUDE_PROVIDER_VALUE = "provider";
  private static final String INCLUDE_PACKAGE_VALUE = "package";

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

    String msgBody = mapVendorToProvider(body.toString());

    if (response.statusCode() == 404) {
      future.completeExceptionally(new RMAPIResourceNotFoundException(
        String.format("Requested resource %s not found", query), response.statusCode(), response.statusMessage(), msgBody, query));
    } else if ((response.statusCode() == 401) || (response.statusCode() == 403)) {
      future.completeExceptionally(new RMAPIUnAuthorizedException(
        String.format("Unauthorized Access to %s", query), response.statusCode(), response.statusMessage(), msgBody, query));
    } else {

      future.completeExceptionally(new RMAPIServiceException(
        String.format("%s Code = %s Message = %s Body = %s", INVALID_RMAPI_RESPONSE, response.statusCode(),
          response.statusMessage(), body.toString()),
        response.statusCode(), response.statusMessage(), msgBody, query));
    }
  }

  private String mapVendorToProvider(String msgBody) {
    return msgBody.replace(VENDOR_LOWER_STRING, PROVIDER_LOWER_STRING).replace(VENDOR_UPPER_STRING, PROVIDER_UPPER_STRING);
  }

  private <T> CompletableFuture<T> getRequest(String query, Class<T> clazz) {

    CompletableFuture<T> future = new CompletableFuture<>();

    HttpClient httpClient = vertx.createHttpClient();

    final HttpClientRequest request = httpClient.getAbs(query);

    addRequestHeaders(request);

    LOG.info("RMAPI Service GET absolute URL is:" + request.absoluteURI());

    executeRequest(query, clazz, future, httpClient, request);

    request.end();

    return future;

  }

  private <T> CompletableFuture<Void> putRequest(String query, T putData) {

    CompletableFuture<Void> future = new CompletableFuture<>();

    HttpClient httpClient = vertx.createHttpClient();

    final HttpClientRequest request = httpClient.putAbs(query);

    addRequestHeaders(request);

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

  private <T, P> CompletableFuture<T> postRequest(String query, P postData, Class<T> clazz){

    CompletableFuture<T> future = new CompletableFuture<>();

    HttpClient httpClient = vertx.createHttpClient();

    final HttpClientRequest request = httpClient.postAbs(query);

    addRequestHeaders(request);

    LOG.info("RMAPI Service POST absolute URL is:" + request.absoluteURI());

    executeRequest(query, clazz, future, httpClient, request);

    String encodedBody = Json.encodePrettily(postData);
    LOG.info("RMAPI Service POST body is:" + encodedBody);
    request.end(encodedBody);

      return future;
  }

  private <T> void executeRequest(String query, Class<T> clazz, CompletableFuture<T> future,
    HttpClient httpClient, HttpClientRequest request) {
    request.handler(response -> response.bodyHandler(body -> {
      httpClient.close();
      if (response.statusCode() == 200) {
          try {
            final JsonObject instanceJSON = new JsonObject(body.toString());
            T results = instanceJSON.mapTo(clazz);
            future.complete(results);
          } catch (Exception e) {
            LOG.error(
              String.format("%s - Response = [%s] Target Type = [%s] Cause: [%s]",
                JSON_RESPONSE_ERROR, body.toString(), clazz, e.getMessage()));
            future.completeExceptionally(
              new RMAPIResultsProcessingException(String.format("%s for query = %s", JSON_RESPONSE_ERROR, query), e));
          }
        } else {

          handleRMAPIError(response, query, body, future);
        }
      })).exceptionHandler(future::completeExceptionally);
  }

  private void addRequestHeaders(HttpClientRequest request) {
    request.headers().add(HTTP_HEADER_ACCEPT, APPLICATION_JSON);
    request.headers().add(HTTP_HEADER_CONTENT_TYPE, APPLICATION_JSON);
    request.headers().add(RMAPI_API_KEY, apiKey);
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

  public CompletableFuture<Titles> retrieveTitles(FilterQuery filterQuery, Sort sort, int page, int count) {
    String path = new TitlesFilterableUrlBuilder()
      .filter(filterQuery)
      .sort(sort)
      .page(page)
      .count(count)
      .build();
    return this.getRequest(constructURL(TITLES_PATH + "?" + path), Titles.class);
  }

  public CompletableFuture<Packages> retrievePackages(Long providerId) {
    return retrievePackages(null, null, providerId, null, 1, 25, Sort.NAME);
  }

  public CompletableFuture<Packages> retrievePackages(
    String filterSelected, String filterType, Long providerId, String q, int page, int count,
    Sort sort) {
    String path = new PackagesFilterableUrlBuilder()
      .filterSelected(filterSelected)
      .filterType(filterType)
      .q(q)
      .page(page)
      .count(count)
      .sort(sort)
      .build();

    String packagesPath = providerId == null ? PACKAGES_PATH + "?" : VENDORS_PATH+ '/' + providerId + '/' + PACKAGES_PATH + "?";

    return this.getRequest(constructURL(packagesPath + path), Packages.class);
  }

  public CompletableFuture<Vendors> getVendors(boolean filterCustom){
    CompletableFuture<Vendors> vendorsList = CompletableFuture.completedFuture(Vendors.builder().build());
    if (filterCustom) {
      return retrieveProviders(customerId, 1, 25, Sort.RELEVANCE);
    }
    return vendorsList;
  }

  public Long getFirstProviderElement(Vendors vendors) {
    List<Vendor> vendorList = vendors.getVendorList();
    return (CollectionUtils.isEmpty(vendorList)) ? null : vendorList.get(0).getVendorId();
  }

  public CompletableFuture<VendorResult> retrieveProvider(long id, String include) {
    final String path = VENDORS_PATH + '/' + id;

    CompletableFuture<VendorById> vendorFuture;
    CompletableFuture<Packages> packagesFuture;

    vendorFuture = this.getRequest(constructURL(path), VendorById.class);
    if (INCLUDE_PACKAGES_VALUE.equalsIgnoreCase(include)) {
      packagesFuture = retrievePackages(id);
    } else {
      packagesFuture = CompletableFuture.completedFuture(null);
    }
    return CompletableFuture.allOf(vendorFuture, packagesFuture)
      .thenCompose(o ->
        CompletableFuture.completedFuture(new VendorResult(vendorFuture.join(), packagesFuture.join())));
  }

  public CompletableFuture<VendorById> updateProvider(long id, VendorPut rmapiVendor) {
    final String path = VENDORS_PATH + '/' + id;

    return this.putRequest(constructURL(path), rmapiVendor)
      .thenCompose(vend -> this.retrieveProvider(id, ""))
      .thenCompose(vendorResult -> CompletableFuture.completedFuture(vendorResult.getVendor()));
  }

  public CompletableFuture<PackageByIdData> retrievePackage(PackageId packageId) {
    final String path = VENDORS_PATH + '/' + packageId.getProviderIdPart() + '/' + PACKAGES_PATH + '/' + packageId.getPackageIdPart();
    return this.getRequest(constructURL(path), PackageByIdData.class);
  }

  public CompletionStage<Void> updatePackage(PackageId packageId, PackagePut packagePut) {
    final String path = VENDORS_PATH + '/' + packageId.getProviderIdPart() + '/' + PACKAGES_PATH + '/' + packageId.getPackageIdPart();

    return this.putRequest(constructURL(path), packagePut);
  }

  public CompletableFuture<Void> deletePackage(PackageId packageId) {
    final String path = VENDORS_PATH + '/' + packageId.getProviderIdPart() + '/' + PACKAGES_PATH + '/' + packageId.getPackageIdPart();
    return this.putRequest(constructURL(path), new PackageSelectedPayload(false));
  }

  public CompletableFuture<RootProxyCustomLabels> retrieveRootProxyCustomLabels() {
    return this.getRequest(constructURL(""), RootProxyCustomLabels.class);
  }

  public CompletableFuture<Proxies> retrieveProxies() {
    return getRequest(constructURL("proxies"), Proxies.class);
  }

  public CompletableFuture<RootProxyCustomLabels> updateRootProxyCustomLabels(RootProxyPutRequest rootProxyPutRequest,
                                                                              RootProxyCustomLabels rootProxyCustomLabels) {
    final String path = "";

    Proxy.ProxyBuilder pb = Proxy.builder();
    pb.id(rootProxyPutRequest.getData().getAttributes().getProxyTypeId());

    RootProxyCustomLabels.RootProxyCustomLabelsBuilder clb = rootProxyCustomLabels.toBuilder().proxy(pb.build());
    /* In RM API - custom labels and root proxy are updated using the same PUT endpoint.
     * We are GETting the object containing both, updating the root proxy with the new one and making a PUT request to RM API.
     * One gotcha here is that we have to prune custom labels in PUT request to not include any that have displayLabel = '' since RM API
     * gives a 400 Bad Request if we send them along as part of the update. Hence, the step below.
     */
    List<CustomLabel> filteredCustomLabelList = rootProxyCustomLabels.getLabelList().stream()
      .filter(item -> !item.getDisplayLabel().isEmpty())
      .collect((Collectors.toList()));

    clb.labelList(filteredCustomLabelList);

    return this.putRequest(constructURL(path), clb.build())
      .thenCompose(updatedRootProxy -> this.retrieveRootProxyCustomLabels());
  }

  public CompletableFuture<Title> retrieveTitle(long id) {
    final String path = TITLES_PATH + '/' + id;
    return this.getRequest(constructURL(path), Title.class);
  }

  public CompletableFuture<ResourceResult> retrieveResource(ResourceId resourceId, List<String> includes) {
    CompletableFuture<Title> titleFuture;
    CompletableFuture<PackageByIdData> packageFuture;
    CompletableFuture<VendorResult> vendorFuture;

    final String path = VENDORS_PATH + '/' + resourceId.getProviderIdPart() + '/' + PACKAGES_PATH + '/' + resourceId.getPackageIdPart() + '/' + TITLES_PATH + '/' + resourceId.getTitleIdPart();
    titleFuture = this.getRequest(constructURL(path), Title.class);
    if (includes.contains(INCLUDE_PROVIDER_VALUE)) {
      vendorFuture = retrieveProvider(resourceId.getProviderIdPart(), "");
    } else {
      vendorFuture = CompletableFuture.completedFuture(new VendorResult(null, null));
    }
    if (includes.contains(INCLUDE_PACKAGE_VALUE)) {
      packageFuture = retrievePackage(new PackageId(resourceId.getProviderIdPart(), resourceId.getPackageIdPart()));
    } else {
      packageFuture = CompletableFuture.completedFuture(null);
    }


    return CompletableFuture.allOf(titleFuture, vendorFuture, packageFuture)
      .thenCompose(o ->
        CompletableFuture.completedFuture(new ResourceResult(titleFuture.join(), vendorFuture.join().getVendor(), packageFuture.join())));
  }

  public CompletableFuture<PackageByIdData> postPackage(PackagePost entity) {

    MutableObject providerId = new MutableObject();
     return this
      .retrieveProviders(customerId, 1, 25, Sort.RELEVANCE)
      .thenCompose(
        vendors -> CompletableFuture.completedFuture(this.getFirstProviderElement(vendors)))
      .thenCompose(vendorId -> {
        providerId.setValue(vendorId);
        return this.postPackage(entity, vendorId);
      })
       .thenCompose(packageCreated -> retrievePackage(new PackageId((Long) providerId.getValue(), packageCreated.getPackageId())));

  }

  public CompletableFuture<PackageCreated> postPackage(PackagePost entity, Long id) {
    String path = VENDORS_PATH + '/' + id + '/' + PACKAGES_PATH;
    return this.postRequest(constructURL(path), entity, PackageCreated.class);
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
