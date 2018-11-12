package org.folio.rest.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.apache.http.HttpStatus;
import org.folio.config.RMAPIConfigurationServiceCache;
import org.folio.config.RMAPIConfigurationServiceImpl;
import org.folio.config.api.RMAPIConfigurationService;
import org.folio.http.ConfigurationClientProvider;
import org.folio.rest.annotations.Validate;
import org.folio.rest.aspect.HandleValidationErrors;
import org.folio.rest.converter.VendorConverter;
import org.folio.rest.jaxrs.model.ProviderPutRequest;
import org.folio.rest.jaxrs.resource.EholdingsProviders;
import org.folio.rest.model.OkapiData;
import org.folio.rest.model.Sort;
import org.folio.rest.util.ErrorUtil;
import org.folio.rest.validator.HeaderValidator;
import org.folio.rmapi.RMAPIService;
import org.folio.rmapi.exception.RMAPIResourceNotFoundException;
import org.folio.rmapi.exception.RMAPIServiceException;

import javax.validation.ValidationException;
import javax.ws.rs.core.Response;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class EholdingsProvidersImpl implements EholdingsProviders {

  private static final String INTERNAL_SERVER_ERROR = "Internal server error";
  private static final String GET_PROVIDER_NOT_FOUND_MESSAGE = "Provider not found";

  private final Logger logger = LoggerFactory.getLogger(EholdingsConfigurationImpl.class);

  private RMAPIConfigurationService configurationService;
  private HeaderValidator headerValidator;
  private VendorConverter converter;
  private static final String CONTENT_TYPE_HEADER = "Content-Type";
  private static final String CONTENT_TYPE_VALUE = "application/vnd.api+json";

  public EholdingsProvidersImpl() {
    this(
      new RMAPIConfigurationServiceCache(
        new RMAPIConfigurationServiceImpl(new ConfigurationClientProvider())),
      new HeaderValidator(),
      new VendorConverter());
  }

  public EholdingsProvidersImpl(RMAPIConfigurationService configurationService,
                                HeaderValidator headerValidator,
                                VendorConverter converter) {
    this.configurationService = configurationService;
    this.headerValidator = headerValidator;
    this.converter = converter;
  }

  @Override
  @Validate
  @HandleValidationErrors
  public void getEholdingsProviders(String q, String sort, int page, int count, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    headerValidator.validate(okapiHeaders);
    if(!Sort.contains(sort.toUpperCase())){
      throw new ValidationException("Invalid sort parameter");
    }
    CompletableFuture.completedFuture(null)
    .thenCompose(o -> configurationService.retrieveConfiguration(new OkapiData(okapiHeaders)))
    .thenCompose(rmapiConfiguration -> {
      RMAPIService rmapiService = new RMAPIService(rmapiConfiguration.getCustomerId(), rmapiConfiguration.getAPIKey(),
        rmapiConfiguration.getUrl(), vertxContext.owner());
      return rmapiService.retrieveProviders(q, page, count, Sort.valueOf(sort.toUpperCase()));
    })
    .thenAccept(vendors ->
      asyncResultHandler.handle(Future.succeededFuture(GetEholdingsProvidersResponse
       .respond200WithApplicationVndApiJson(converter.convert(vendors)))))
    .exceptionally(e -> {
      if(e.getCause() instanceof RMAPIServiceException){
        RMAPIServiceException rmApiException = (RMAPIServiceException)e.getCause();
        asyncResultHandler.handle(Future.succeededFuture(GetEholdingsProvidersResponse
          .status(rmApiException.getRMAPICode())
          .header(CONTENT_TYPE_HEADER, CONTENT_TYPE_VALUE)
          .entity(ErrorUtil.createErrorFromRMAPIResponse(rmApiException))
          .build()));
      }
      else {
        asyncResultHandler.handle(Future.succeededFuture(GetEholdingsProvidersResponse
          .status(HttpStatus.SC_INTERNAL_SERVER_ERROR)
          .header(CONTENT_TYPE_HEADER, CONTENT_TYPE_VALUE)
          .entity(ErrorUtil.createError(e.getCause().getMessage()))
          .build()));
      }
      return null;
    });
  }

  @Override
  @HandleValidationErrors
  public void getEholdingsProvidersByProviderId(String providerId, String include, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    long providerIdLong;
    try {
      providerIdLong = Long.parseLong(providerId);
    } catch (NumberFormatException e) {
      throw new ValidationException("Provider id is invalid - " + providerId, e);
    }

    headerValidator.validate(okapiHeaders);
    CompletableFuture.completedFuture(null)
      .thenCompose(o -> configurationService.retrieveConfiguration(new OkapiData(okapiHeaders)))
      .thenCompose(rmapiConfiguration -> {
        RMAPIService rmapiService = new RMAPIService(rmapiConfiguration.getCustomerId(), rmapiConfiguration.getAPIKey(),
          rmapiConfiguration.getUrl(), vertxContext.owner());
        return rmapiService.retrieveProvider(providerIdLong, include);
      })
      .thenAccept(vendor ->
        asyncResultHandler.handle(Future.succeededFuture(GetEholdingsProvidersByProviderIdResponse
          .respond200WithApplicationVndApiJson(converter.convertToVendor(vendor)))))
      .exceptionally(e -> {
        if (e.getCause() instanceof RMAPIResourceNotFoundException) {
          asyncResultHandler.handle(Future.succeededFuture(GetEholdingsProvidersByProviderIdResponse
            .respond404WithApplicationVndApiJson(ErrorUtil.createError(GET_PROVIDER_NOT_FOUND_MESSAGE))));
        } else {
          logger.error(INTERNAL_SERVER_ERROR, e);
          asyncResultHandler.handle(Future.succeededFuture(GetEholdingsProvidersByProviderIdResponse
            .status(HttpStatus.SC_INTERNAL_SERVER_ERROR)
            .header(CONTENT_TYPE_HEADER, CONTENT_TYPE_VALUE)
            .entity(ErrorUtil.createError(e.getCause().getMessage()))
            .build()));
        }
        return null;
      });
  }

  @Override
  @HandleValidationErrors
  public void putEholdingsProvidersByProviderId(String providerId, String contentType, ProviderPutRequest entity, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    asyncResultHandler.handle(Future.succeededFuture(GetEholdingsProvidersResponse.status(Response.Status.NOT_IMPLEMENTED).build()));
  }

  @Override
  @HandleValidationErrors
  public void getEholdingsProvidersPackages(String q, String filterSelected, String filterType, String sort, int page, int count, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    asyncResultHandler.handle(Future.succeededFuture(GetEholdingsProvidersResponse.status(Response.Status.NOT_IMPLEMENTED).build()));
  }
}
