package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;

import java.util.Map;

import javax.ws.rs.core.Response;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import org.springframework.beans.factory.annotation.Autowired;

import org.folio.rest.jaxrs.model.UCCredentials;
import org.folio.rest.jaxrs.resource.EholdingsUcCredentials;
import org.folio.rest.util.ErrorHandler;
import org.folio.service.uc.UCAuthService;
import org.folio.spring.SpringContextUtil;

public class UsageConsolidationCredentialsApi implements EholdingsUcCredentials {

  @Autowired
  private UCAuthService authService;
  @Autowired
  private ErrorHandler errorHandler;

  public UsageConsolidationCredentialsApi() {
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
  }

  @Override
  public void getEholdingsUcCredentials(Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
                                        Context vertxContext) {
    authService.checkCredentialsPresence(okapiHeaders)
      .thenAccept(ucCredentialsPresence -> asyncResultHandler.handle(succeededFuture(
        GetEholdingsUcCredentialsResponse.respond200WithApplicationVndApiJson(ucCredentialsPresence))))
      .exceptionally(errorHandler.handle(asyncResultHandler));
  }

  @Override
  public void putEholdingsUcCredentials(UCCredentials entity, Map<String, String> okapiHeaders,
                                        Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    authService.updateCredentials(entity, okapiHeaders)
      .thenAccept(ucCredentialsPresence -> asyncResultHandler.handle(succeededFuture(
        PutEholdingsUcCredentialsResponse.respond204())))
      .exceptionally(errorHandler.handle(asyncResultHandler));
  }

  @Override
  public void getEholdingsUcCredentialsClientId(Map<String, String> okapiHeaders,
                                                Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    authService.getClientId(okapiHeaders)
      .thenAccept(ucCredentialsClientId -> asyncResultHandler.handle(succeededFuture(
        GetEholdingsUcCredentialsClientIdResponse.respond200WithTextPlain(ucCredentialsClientId))))
      .exceptionally(errorHandler.handle(asyncResultHandler));
  }
}
