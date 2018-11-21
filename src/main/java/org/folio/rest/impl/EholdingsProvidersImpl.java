package org.folio.rest.impl;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import javax.validation.ValidationException;
import javax.ws.rs.core.Response;

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
import org.folio.rest.util.ErrorHandler;
import org.folio.rest.util.ErrorUtil;
import org.folio.rest.validator.HeaderValidator;
import org.folio.rest.validator.ProviderPutBodyValidator;
import org.folio.rmapi.RMAPIService;
import org.folio.rmapi.exception.RMAPIResourceNotFoundException;
import org.folio.rmapi.exception.RMAPIServiceException;
import org.folio.rmapi.model.VendorPut;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class EholdingsProvidersImpl implements EholdingsProviders {

  private static final String GET_PROVIDER_NOT_FOUND_MESSAGE = "Provider not found";
  private static final String PUT_PROVIDER_ERROR_MESSAGE = "Failed to update provider";


  private final Logger logger = LoggerFactory.getLogger(EholdingsConfigurationImpl.class);

  private RMAPIConfigurationService configurationService;
  private HeaderValidator headerValidator;
  private VendorConverter converter;
  private ProviderPutBodyValidator bodyValidator;
  private static final String CONTENT_TYPE_HEADER = "Content-Type";
  private static final String CONTENT_TYPE_VALUE = "application/vnd.api+json";

  public EholdingsProvidersImpl() {
    this(
      new RMAPIConfigurationServiceCache(
        new RMAPIConfigurationServiceImpl(new ConfigurationClientProvider())),
      new HeaderValidator(),
      new VendorConverter(),
      new ProviderPutBodyValidator());
  }

  public EholdingsProvidersImpl(RMAPIConfigurationService configurationService,
                                HeaderValidator headerValidator,
                                VendorConverter converter,
                                ProviderPutBodyValidator bodyValidator) {
    this.configurationService = configurationService;
    this.headerValidator = headerValidator;
    this.converter = converter;
    this.bodyValidator = bodyValidator;
  }

  @Override
  @Validate
  @HandleValidationErrors
  public void getEholdingsProviders(String q, String sort, int page, int count, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    headerValidator.validate(okapiHeaders);
    if(!Sort.contains(sort.toUpperCase())){
      throw new ValidationException("Invalid sort parameter");
    }
    if("".equals(q)){
      throw new ValidationException("Search parameter cannot be empty");
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
      .thenAccept(
        vendorResult ->
          asyncResultHandler.handle(Future.succeededFuture(GetEholdingsProvidersByProviderIdResponse
            .respond200WithApplicationVndApiJson(converter.convertToProvider(vendorResult.getVendor(), vendorResult.getPackages()))))
      )
      .exceptionally(e -> {
        new ErrorHandler()
          .add(RMAPIResourceNotFoundException.class, exception ->
            GetEholdingsProvidersByProviderIdResponse.respond404WithApplicationVndApiJson(
              ErrorUtil.createError(GET_PROVIDER_NOT_FOUND_MESSAGE)))
        .addDefaultMapper()
        .handle(asyncResultHandler, e);
        return null;
      });
  }

  @Override
  @HandleValidationErrors
  public void putEholdingsProvidersByProviderId(String providerId, String contentType, ProviderPutRequest entity,
      Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    long providerIdLong;
    try {
      providerIdLong = Long.parseLong(providerId);
    } catch (NumberFormatException e) {
      throw new ValidationException("Provider id is invalid - " + providerId, e);
    }
    headerValidator.validate(okapiHeaders);
    bodyValidator.validate(entity);

    VendorPut rmapiVendor = converter.convertToVendor(entity);

    CompletableFuture.completedFuture(null)
        .thenCompose(o -> configurationService.retrieveConfiguration(new OkapiData(okapiHeaders)))
        .thenCompose(rmapiConfiguration -> {
          RMAPIService rmapiService = new RMAPIService(rmapiConfiguration.getCustomerId(),
              rmapiConfiguration.getAPIKey(), rmapiConfiguration.getUrl(), vertxContext.owner());
          return rmapiService.updateProvider(providerIdLong, rmapiVendor);
        })
        .thenAccept(vendor -> asyncResultHandler.handle(Future.succeededFuture(PutEholdingsProvidersByProviderIdResponse
            .respond200WithApplicationVndApiJson(converter.convertToProvider(vendor)))))
        .exceptionally(e -> {
          logger.error(PUT_PROVIDER_ERROR_MESSAGE, e);
          new ErrorHandler()
            .addRmApiMapper()
            .addDefaultMapper()
            .handle(asyncResultHandler, e);
          return null;
        });
  }


  @Override
  @HandleValidationErrors
  public void getEholdingsProvidersPackages(String q, String filterSelected, String filterType, String sort, int page, int count, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    asyncResultHandler.handle(Future.succeededFuture(GetEholdingsProvidersResponse.status(Response.Status.NOT_IMPLEMENTED).build()));
  }
}
