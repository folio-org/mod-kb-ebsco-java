package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;

import java.util.Map;

import javax.ws.rs.core.Response;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import org.folio.rest.jaxrs.resource.EholdingsKbCredentialsIdUc;
import org.folio.rest.util.ErrorHandler;
import org.folio.service.uc.UCSettingsService;
import org.folio.spring.SpringContextUtil;

public class EholdingsKbCredentialsUCImpl implements EholdingsKbCredentialsIdUc {

  @Autowired
  @Qualifier("securedUCSettingsService")
  private UCSettingsService settingsService;
  @Autowired
  private ErrorHandler errorHandler;

  public EholdingsKbCredentialsUCImpl() {
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
  }

  @Override
  public void getEholdingsKbCredentialsUcById(String credentialsId, Map<String, String> okapiHeaders,
                                              Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    settingsService.fetchByCredentialsId(credentialsId, okapiHeaders)
      .thenAccept(ucSettings -> asyncResultHandler.handle(succeededFuture(
        GetEholdingsKbCredentialsUcByIdResponse.respond200WithApplicationVndApiJson(ucSettings))))
      .exceptionally(errorHandler.handle(asyncResultHandler));
  }
}
