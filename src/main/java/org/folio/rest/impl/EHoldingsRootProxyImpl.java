package org.folio.rest.impl;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import javax.ws.rs.core.Response;

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
import org.folio.rmapi.RMAPIService;
import org.folio.rmapi.exception.RMAPIUnAuthorizedException;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;

public class EHoldingsRootProxyImpl implements EholdingsRootProxy {
  
  private RMAPIConfigurationService configurationService;
  private HeaderValidator headerValidator;
  private RootProxyConverter converter;
  
  private static final String CONTENT_TYPE_HEADER = "Content-Type";
  private static final String CONTENT_TYPE_VALUE = "application/vnd.api+json";
  
  public EHoldingsRootProxyImpl() {
    this(
      new RMAPIConfigurationServiceCache(
        new RMAPIConfigurationServiceImpl(new ConfigurationClientProvider())),
      new HeaderValidator(),
      new RootProxyConverter());
  }

  public EHoldingsRootProxyImpl(RMAPIConfigurationService configurationService,
                                HeaderValidator headerValidator,
                                RootProxyConverter converter) {
    this.configurationService = configurationService;
    this.headerValidator = headerValidator;
    this.converter = converter;
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
        return rmapiService.retrieveRootProxy();
      })
      .thenAccept(rootProxy ->
        asyncResultHandler.handle(Future.succeededFuture(GetEholdingsRootProxyResponse
          .respond200WithApplicationVndApiJson(converter.convertRootProxy(rootProxy)))))
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
  public void putEholdingsRootProxy(String contentType, RootProxyPutRequest entity, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    asyncResultHandler.handle(Future.succeededFuture(GetEholdingsRootProxyResponse.status(Response.Status.NOT_IMPLEMENTED).build()));
  }
}
