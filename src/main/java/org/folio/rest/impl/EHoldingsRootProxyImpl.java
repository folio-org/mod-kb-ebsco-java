package org.folio.rest.impl;

import java.util.Map;

import javax.ws.rs.core.Response;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import org.springframework.beans.factory.annotation.Autowired;

import org.folio.rest.aspect.HandleValidationErrors;
import org.folio.rest.converter.proxy.RootProxyPutConverter;
import org.folio.rest.jaxrs.model.RootProxy;
import org.folio.rest.jaxrs.model.RootProxyPutRequest;
import org.folio.rest.jaxrs.resource.EholdingsRootProxy;
import org.folio.rest.util.template.RMAPITemplateFactory;
import org.folio.rest.validator.RootProxyPutBodyValidator;
import org.folio.spring.SpringContextUtil;

public class EHoldingsRootProxyImpl implements EholdingsRootProxy {

  @Autowired
  private RootProxyPutBodyValidator bodyValidator;
  @Autowired
  private RMAPITemplateFactory templateFactory;
  @Autowired
  private RootProxyPutConverter rootProxyPutRequestConverter;

  public EHoldingsRootProxyImpl() {
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
  }

  @Override
  @HandleValidationErrors
  public void getEholdingsRootProxy(Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    templateFactory.createTemplate(okapiHeaders, asyncResultHandler)
      .requestAction(context ->
        context.getHoldingsService().retrieveRootProxyCustomLabels()
      )
      .executeWithResult(RootProxy.class);
  }

  @Override
  @HandleValidationErrors
  public void putEholdingsRootProxy(String contentType, RootProxyPutRequest entity, Map<String, String> okapiHeaders,
                                    Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    bodyValidator.validate(entity);
    templateFactory.createTemplate(okapiHeaders, asyncResultHandler)
      .requestAction(context ->
        context.getHoldingsService().retrieveRootProxyCustomLabels()
          .thenCompose(rootProxyCustomLabels -> {
            rootProxyCustomLabels = rootProxyPutRequestConverter.convertToRootProxyCustomLabels(entity, rootProxyCustomLabels);
            return context.getHoldingsService().updateRootProxyCustomLabels(rootProxyCustomLabels);
          })
      )
      .executeWithResult(RootProxy.class);
  }
}
