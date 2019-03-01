package org.folio.rest.impl;

import java.util.Map;

import javax.ws.rs.core.Response;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import org.springframework.beans.factory.annotation.Autowired;

import org.folio.rest.aspect.HandleValidationErrors;
import org.folio.rest.jaxrs.model.ProxyTypes;
import org.folio.rest.jaxrs.resource.EholdingsProxyTypes;
import org.folio.rest.util.template.RMAPITemplateFactory;
import org.folio.spring.SpringContextUtil;

public class EholdingsProxyTypesImpl implements EholdingsProxyTypes {
  @Autowired
  private RMAPITemplateFactory rmapiTemplateFactory;

  public EholdingsProxyTypesImpl() {
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
  }

  @Override
  @HandleValidationErrors
  public void getEholdingsProxyTypes(Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    rmapiTemplateFactory.createTemplate(okapiHeaders, asyncResultHandler)
      .requestAction(context ->
        context.getHoldingsService().retrieveProxies()
      )
      .executeWithResult(ProxyTypes.class);
  }
}
