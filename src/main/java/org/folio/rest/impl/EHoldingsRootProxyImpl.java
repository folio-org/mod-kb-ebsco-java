package org.folio.rest.impl;

import static org.folio.http.HttpConsts.CONTENT_TYPE_HEADER;
import static org.folio.http.HttpConsts.JSON_API_TYPE;

import java.util.Map;

import javax.ws.rs.core.Response;

import org.apache.http.HttpStatus;
import org.folio.rest.aspect.HandleValidationErrors;
import org.folio.rest.jaxrs.model.RootProxy;
import org.folio.rest.jaxrs.model.RootProxyPutRequest;
import org.folio.rest.jaxrs.resource.EholdingsRootProxy;
import org.folio.rest.util.ErrorUtil;
import org.folio.rest.util.template.RMAPITemplateFactory;
import org.folio.rest.validator.RootProxyPutBodyValidator;
import org.folio.rmapi.exception.RMAPIUnAuthorizedException;
import org.folio.spring.SpringContextUtil;
import org.springframework.beans.factory.annotation.Autowired;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

public class EHoldingsRootProxyImpl implements EholdingsRootProxy {

  @Autowired
  private RootProxyPutBodyValidator bodyValidator;
  @Autowired
  private RMAPITemplateFactory templateFactory;

  public EHoldingsRootProxyImpl() {
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
  }

  @Override
  @HandleValidationErrors
  public void getEholdingsRootProxy(Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    templateFactory.createTemplate(okapiHeaders, asyncResultHandler)
      .requestAction((rmapiService, okapiData) ->
        rmapiService.retrieveRootProxyCustomLabels()
      )
      .addErrorMapper(RMAPIUnAuthorizedException.class, rmApiException ->
        GetEholdingsRootProxyResponse
          .status(HttpStatus.SC_FORBIDDEN)
          .header(CONTENT_TYPE_HEADER, JSON_API_TYPE)
          .entity(ErrorUtil.createError(rmApiException.getMessage()))
          .build())
      .executeWithResult(RootProxy.class);
  }

  @Override
  @HandleValidationErrors
  public void putEholdingsRootProxy(String contentType, RootProxyPutRequest entity, Map<String, String> okapiHeaders,
                                    Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    bodyValidator.validate(entity);
    templateFactory.createTemplate(okapiHeaders, asyncResultHandler)
      .requestAction((rmapiService, okapiData) ->
        rmapiService.retrieveRootProxyCustomLabels()
          .thenCompose(rootProxyCustomLabels -> rmapiService.updateRootProxyCustomLabels(entity, rootProxyCustomLabels))
      )
      .addErrorMapper(RMAPIUnAuthorizedException.class, rmApiException ->
        PutEholdingsRootProxyResponse
          .status(HttpStatus.SC_FORBIDDEN)
          .header(CONTENT_TYPE_HEADER, JSON_API_TYPE)
          .entity(ErrorUtil.createError(rmApiException.getMessage()))
          .build()
      )
      .executeWithResult(RootProxy.class);
  }
}
