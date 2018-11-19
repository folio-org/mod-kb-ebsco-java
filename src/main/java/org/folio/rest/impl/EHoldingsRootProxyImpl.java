package org.folio.rest.impl;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import javax.ws.rs.core.Response;

import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.http.HttpStatus;
import org.folio.config.RMAPIConfigurationServiceCache;
import org.folio.config.RMAPIConfigurationServiceImpl;
import org.folio.config.api.RMAPIConfigurationService;
import org.folio.http.ConfigurationClientProvider;
import org.folio.rest.aspect.HandleValidationErrors;
import org.folio.rest.converter.RootProxyConverter;
import org.folio.rest.jaxrs.model.RootProxyPutRequest;
import org.folio.rest.jaxrs.resource.EholdingsRootProxy;
import org.folio.rest.model.OkapiData;
import org.folio.rest.util.ErrorUtil;
import org.folio.rest.validator.HeaderValidator;
import org.folio.rest.validator.RootProxyPutBodyValidator;
import org.folio.rmapi.RMAPIService;
import org.folio.rmapi.exception.RMAPIServiceException;
import org.folio.rmapi.exception.RMAPIUnAuthorizedException;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class EHoldingsRootProxyImpl implements EholdingsRootProxy {
  
  private RMAPIConfigurationService configurationService;
  private HeaderValidator headerValidator;
  private RootProxyConverter converter;
  private RootProxyPutBodyValidator bodyValidator;
  
  private static final String CONTENT_TYPE_HEADER = "Content-Type";
  private static final String CONTENT_TYPE_VALUE = "application/vnd.api+json";
  private static final String PUT_ROOT_PROXY_ERROR_MESSAGE = "Failed to update root proxy";
  
  private final Logger logger = LoggerFactory.getLogger(EHoldingsRootProxyImpl.class);
  
  public EHoldingsRootProxyImpl() {
    this(
      new RMAPIConfigurationServiceCache(
        new RMAPIConfigurationServiceImpl(new ConfigurationClientProvider())),
      new HeaderValidator(),
      new RootProxyConverter(),
      new RootProxyPutBodyValidator());
  }

  public EHoldingsRootProxyImpl(RMAPIConfigurationService configurationService,
                                HeaderValidator headerValidator,
                                RootProxyConverter converter,
                                RootProxyPutBodyValidator bodyValidator) {
    this.configurationService = configurationService;
    this.headerValidator = headerValidator;
    this.converter = converter;
    this.bodyValidator = bodyValidator;
  }

  @Override
  @HandleValidationErrors
  public void getEholdingsRootProxy(Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    
    headerValidator.validate(okapiHeaders);
    CompletableFuture.completedFuture(null)
      .thenCompose(o -> configurationService.retrieveConfiguration(new OkapiData(okapiHeaders)))
      .thenCompose(rmapiConfiguration -> {
        RMAPIService rmapiService = new RMAPIService(rmapiConfiguration.getCustomerId(), rmapiConfiguration.getAPIKey(),
          rmapiConfiguration.getUrl(), vertxContext.owner());
        return rmapiService.retrieveRootProxyCustomLabels();
      })
      .thenAccept(rootProxyCustomLabels ->
        asyncResultHandler.handle(Future.succeededFuture(GetEholdingsRootProxyResponse
          .respond200WithApplicationVndApiJson(converter.convertRootProxy(rootProxyCustomLabels)))))
      .exceptionally(e -> {
        if(e.getCause() instanceof RMAPIUnAuthorizedException){
          RMAPIUnAuthorizedException rmApiException = (RMAPIUnAuthorizedException)e.getCause();
          asyncResultHandler.handle(Future.succeededFuture(GetEholdingsRootProxyResponse
            .status(HttpStatus.SC_FORBIDDEN)
            .header(CONTENT_TYPE_HEADER, CONTENT_TYPE_VALUE)
            .entity(ErrorUtil.createError(rmApiException.getMessage()))
            .build()));
        }
        else {
          asyncResultHandler.handle(Future.succeededFuture(GetEholdingsRootProxyResponse
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
  public void putEholdingsRootProxy(String contentType, RootProxyPutRequest entity, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    headerValidator.validate(okapiHeaders);
    bodyValidator.validate(entity);
    
    MutableObject<RMAPIService> service = new MutableObject<>();
    CompletableFuture.completedFuture(null)
      .thenCompose(o -> configurationService.retrieveConfiguration(new OkapiData(okapiHeaders)))
      .thenAccept(rmapiConfiguration -> 
        service.setValue(new RMAPIService(rmapiConfiguration.getCustomerId(), rmapiConfiguration.getAPIKey(),
          rmapiConfiguration.getUrl(), vertxContext.owner())))
      .thenCompose(o -> service.getValue().retrieveRootProxyCustomLabels())
      .thenCompose(rootProxyCustomLabels -> service.getValue().updateRootProxyCustomLabels(entity, rootProxyCustomLabels))
      .thenAccept(rootProxy ->
        asyncResultHandler.handle(Future.succeededFuture(PutEholdingsRootProxyResponse
          .respond200WithApplicationVndApiJson(converter.convertRootProxy(rootProxy)))))
      .exceptionally(e -> {
        logger.error(PUT_ROOT_PROXY_ERROR_MESSAGE, e);
        if(e.getCause() instanceof RMAPIUnAuthorizedException){
          RMAPIUnAuthorizedException rmApiException = (RMAPIUnAuthorizedException)e.getCause();
          asyncResultHandler.handle(Future.succeededFuture(PutEholdingsRootProxyResponse
            .status(HttpStatus.SC_FORBIDDEN)
            .header(CONTENT_TYPE_HEADER, CONTENT_TYPE_VALUE)
            .entity(ErrorUtil.createError(rmApiException.getMessage()))
            .build()));
        } else if (e.getCause() instanceof RMAPIServiceException) {
          RMAPIServiceException rmApiException = (RMAPIServiceException)e.getCause();
          asyncResultHandler.handle(Future.succeededFuture(PutEholdingsRootProxyResponse
            .status(rmApiException.getRMAPICode())
            .header(CONTENT_TYPE_HEADER, CONTENT_TYPE_VALUE)
            .entity(ErrorUtil.createErrorFromRMAPIResponse(rmApiException))
            .build()));
        } else {
          asyncResultHandler.handle(Future.succeededFuture(GetEholdingsRootProxyResponse
            .status(HttpStatus.SC_INTERNAL_SERVER_ERROR)
            .header(CONTENT_TYPE_HEADER, CONTENT_TYPE_VALUE)
            .entity(ErrorUtil.createError(e.getCause().getMessage()))
            .build()));
        }
        return null;
      });
  }
}
