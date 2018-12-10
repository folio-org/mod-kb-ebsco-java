package org.folio.rest.impl;

import static org.folio.http.HttpConsts.CONTENT_TYPE_HEADER;
import static org.folio.http.HttpConsts.JSON_API_TYPE;

import javax.ws.rs.core.Response;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.http.HttpStatus;
import org.folio.config.api.RMAPIConfigurationService;
import org.folio.rest.aspect.HandleValidationErrors;
import org.folio.rest.converter.RootProxyConverter;
import org.folio.rest.jaxrs.model.RootProxyPutRequest;
import org.folio.rest.jaxrs.resource.EholdingsRootProxy;
import org.folio.rest.model.OkapiData;
import org.folio.rest.util.ErrorHandler;
import org.folio.rest.util.ErrorUtil;
import org.folio.rest.validator.HeaderValidator;
import org.folio.rest.validator.RootProxyPutBodyValidator;
import org.folio.rmapi.RMAPIService;
import org.folio.rmapi.exception.RMAPIUnAuthorizedException;
import org.folio.spring.SpringContextUtil;
import org.springframework.beans.factory.annotation.Autowired;

public class EHoldingsRootProxyImpl implements EholdingsRootProxy {

  @Autowired
  private RMAPIConfigurationService configurationService;
  @Autowired
  private HeaderValidator headerValidator;
  @Autowired
  private RootProxyConverter converter;
  @Autowired
  private RootProxyPutBodyValidator bodyValidator;

  private static final String PUT_ROOT_PROXY_ERROR_MESSAGE = "Failed to update root proxy";

  private final Logger logger = LoggerFactory.getLogger(EHoldingsRootProxyImpl.class);

  @SuppressWarnings("squid:S1172")
  public EHoldingsRootProxyImpl(Vertx vertx, String tenantId) {
    SpringContextUtil.autowireDependencies(this, vertx.getOrCreateContext());
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
        new ErrorHandler()
          .add(RMAPIUnAuthorizedException.class, rmApiException ->
            GetEholdingsRootProxyResponse
              .status(HttpStatus.SC_FORBIDDEN)
              .header(CONTENT_TYPE_HEADER, JSON_API_TYPE)
              .entity(ErrorUtil.createError(rmApiException.getMessage()))
              .build())
          .addDefaultMapper()
          .handle(asyncResultHandler, e);
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
        new ErrorHandler()
          .add(RMAPIUnAuthorizedException.class, rmApiException ->
            PutEholdingsRootProxyResponse
              .status(HttpStatus.SC_FORBIDDEN)
              .header(CONTENT_TYPE_HEADER, JSON_API_TYPE)
              .entity(ErrorUtil.createError(rmApiException.getMessage()))
              .build()
          )
          .addRmApiMapper()
          .addDefaultMapper()
          .handle(asyncResultHandler, e);
        return null;
      });
  }
}
