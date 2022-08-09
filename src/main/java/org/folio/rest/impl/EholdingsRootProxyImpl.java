package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import java.util.Map;
import java.util.function.Function;
import javax.ws.rs.core.Response;
import org.folio.rest.annotations.Validate;
import org.folio.rest.aspect.HandleValidationErrors;
import org.folio.rest.jaxrs.model.RootProxyPutRequest;
import org.folio.rest.jaxrs.resource.EholdingsKbCredentialsIdRootProxy;
import org.folio.rest.jaxrs.resource.EholdingsRootProxy;
import org.folio.rest.util.ErrorHandler;
import org.folio.service.rootproxies.RootProxyService;
import org.folio.spring.SpringContextUtil;
import org.springframework.beans.factory.annotation.Autowired;

public class EholdingsRootProxyImpl implements EholdingsRootProxy, EholdingsKbCredentialsIdRootProxy {

  @Autowired
  private RootProxyService rootProxyService;
  @Autowired
  private ErrorHandler errorHandler;

  public EholdingsRootProxyImpl() {
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
  }

  @Override
  @HandleValidationErrors
  public void getEholdingsRootProxy(Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
                                    Context vertxContext) {

    rootProxyService.findByUser(okapiHeaders)
      .thenAccept(rootProxy -> asyncResultHandler.handle(succeededFuture(
        GetEholdingsRootProxyResponse.respond200WithApplicationVndApiJson(rootProxy))))
      .exceptionally(handleException(asyncResultHandler));
  }

  @Override
  @Validate
  @HandleValidationErrors
  public void getEholdingsKbCredentialsRootProxyById(String id, Map<String, String> okapiHeaders,
                                                     Handler<AsyncResult<Response>> asyncResultHandler,
                                                     Context vertxContext) {
    rootProxyService.findByCredentialsId(id, okapiHeaders)
      .thenAccept(rootProxy -> asyncResultHandler.handle(succeededFuture(
        GetEholdingsKbCredentialsRootProxyByIdResponse.respond200WithApplicationVndApiJson(rootProxy))))
      .exceptionally(handleException(asyncResultHandler));
  }

  @Override
  @Validate
  @HandleValidationErrors
  public void putEholdingsKbCredentialsRootProxyById(String id, String contentType, RootProxyPutRequest entity,
                                                     Map<String, String> okapiHeaders,
                                                     Handler<AsyncResult<Response>> asyncResultHandler,
                                                     Context vertxContext) {
    rootProxyService.updateRootProxy(id, entity, okapiHeaders)
      .thenAccept(rootProxy -> asyncResultHandler.handle(succeededFuture(
        PutEholdingsKbCredentialsRootProxyByIdResponse.respond200WithApplicationVndApiJson(rootProxy))))
      .exceptionally(handleException(asyncResultHandler));
  }

  private Function<Throwable, Void> handleException(Handler<AsyncResult<Response>> asyncResultHandler) {
    return throwable -> {
      errorHandler.handle(asyncResultHandler, throwable);
      return null;
    };
  }
}
