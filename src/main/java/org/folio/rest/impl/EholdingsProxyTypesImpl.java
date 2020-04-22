package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;

import java.util.Map;
import java.util.function.Function;

import javax.ws.rs.core.Response;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import org.springframework.beans.factory.annotation.Autowired;

import org.folio.rest.aspect.HandleValidationErrors;
import org.folio.rest.jaxrs.resource.EholdingsKbCredentialsIdProxyTypes;
import org.folio.rest.jaxrs.resource.EholdingsProxyTypes;
import org.folio.rest.util.ErrorHandler;
import org.folio.service.proxytypes.ProxyTypesService;
import org.folio.spring.SpringContextUtil;

public class EholdingsProxyTypesImpl implements EholdingsProxyTypes, EholdingsKbCredentialsIdProxyTypes {

  @Autowired
  private ProxyTypesService proxyTypesService;
  @Autowired
  private ErrorHandler errorHandler;

  public EholdingsProxyTypesImpl() {
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
  }

  @Override
  @HandleValidationErrors
  public void getEholdingsProxyTypes(Map<String, String> okapiHeaders,
                                     Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {

    proxyTypesService.findByUser(okapiHeaders)
      .thenAccept(proxyTypes -> asyncResultHandler.handle(succeededFuture(
        GetEholdingsProxyTypesResponse.respond200WithApplicationVndApiJson(proxyTypes))))
      .exceptionally(handleException(asyncResultHandler));
  }

  @Override
  public void getEholdingsKbCredentialsProxyTypesById(String id, Map<String, String> okapiHeaders,
                                                      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {

    proxyTypesService.findByCredentialsId(id,okapiHeaders)
      .thenAccept(proxyTypes -> asyncResultHandler.handle(succeededFuture(
        GetEholdingsKbCredentialsProxyTypesByIdResponse.respond200WithApplicationVndApiJson(proxyTypes))))
      .exceptionally(handleException(asyncResultHandler));
  }

  private Function<Throwable, Void> handleException(Handler<AsyncResult<Response>> asyncResultHandler) {
    return throwable -> {
      errorHandler.handle(asyncResultHandler, throwable);
      return null;
    };
  }
}
