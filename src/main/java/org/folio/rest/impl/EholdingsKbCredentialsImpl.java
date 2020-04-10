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

import org.folio.rest.annotations.Validate;
import org.folio.rest.aspect.HandleValidationErrors;
import org.folio.rest.jaxrs.model.KbCredentialsPostRequest;
import org.folio.rest.jaxrs.model.KbCredentialsPutRequest;
import org.folio.rest.jaxrs.resource.EholdingsKbCredentials;
import org.folio.rest.util.ErrorHandler;
import org.folio.service.kbcredentials.KbCredentialsService;
import org.folio.spring.SpringContextUtil;

public class EholdingsKbCredentialsImpl implements EholdingsKbCredentials {

  @Autowired
  private KbCredentialsService credentialsService;

  @Autowired
  private ErrorHandler errorHandler;

  public EholdingsKbCredentialsImpl() {
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
  }

  @Override
  @Validate
  @HandleValidationErrors
  public void getEholdingsKbCredentials(Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
                                        Context vertxContext) {
    credentialsService.findAll(okapiHeaders)
      .thenAccept(kbCredentialsCollection -> asyncResultHandler.handle(succeededFuture(
        GetEholdingsKbCredentialsResponse.respond200WithApplicationVndApiJson(kbCredentialsCollection))))
      .exceptionally(handleException(asyncResultHandler));

  }

  @Override
  @Validate
  @HandleValidationErrors
  public void postEholdingsKbCredentials(String contentType, KbCredentialsPostRequest entity,
                                         Map<String, String> okapiHeaders,
                                         Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    credentialsService.save(entity, okapiHeaders)
      .thenAccept(kbCredentials -> asyncResultHandler.handle(succeededFuture(
        PostEholdingsKbCredentialsResponse.respond201WithApplicationVndApiJson(kbCredentials))))
      .exceptionally(handleException(asyncResultHandler));
  }

  @Override
  @Validate
  @HandleValidationErrors
  public void getEholdingsKbCredentialsById(String id, Map<String, String> okapiHeaders,
                                            Handler<AsyncResult<Response>> asyncResultHandler,
                                            Context vertxContext) {
    credentialsService.findById(id, okapiHeaders)
      .thenAccept(kbCredentials -> asyncResultHandler.handle(succeededFuture(
        GetEholdingsKbCredentialsByIdResponse.respond200WithApplicationVndApiJson(kbCredentials))))
      .exceptionally(handleException(asyncResultHandler));
  }

  @Override
  @Validate
  @HandleValidationErrors
  public void putEholdingsKbCredentialsById(String id, String contentType, KbCredentialsPutRequest entity,
                                            Map<String, String> okapiHeaders,
                                            Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    credentialsService.update(id, entity, okapiHeaders)
      .thenAccept(kbCredentials -> asyncResultHandler.handle(succeededFuture(
        PutEholdingsKbCredentialsByIdResponse.respond204())))
      .exceptionally(handleException(asyncResultHandler));
  }

  @Override
  @Validate
  @HandleValidationErrors
  public void deleteEholdingsKbCredentialsById(String id, Map<String, String> okapiHeaders,
                                               Handler<AsyncResult<Response>> asyncResultHandler,
                                               Context vertxContext) {
    credentialsService.delete(id, okapiHeaders)
      .thenAccept(kbCredentials -> asyncResultHandler.handle(succeededFuture(
        DeleteEholdingsKbCredentialsByIdResponse.respond204())))
      .exceptionally(handleException(asyncResultHandler));
  }

  @Override
  @Validate
  @HandleValidationErrors
  public void getEholdingsKbCredentialsCustomLabelsById(String id, Map<String, String> okapiHeaders,
                                                        Handler<AsyncResult<Response>> asyncResultHandler,
                                                        Context vertxContext) {
    credentialsService.fetchCustomLabels(id, okapiHeaders)
      .thenAccept(customLabelsCollection -> asyncResultHandler.handle(succeededFuture(
        GetEholdingsKbCredentialsCustomLabelsByIdResponse.respond200WithApplicationVndApiJson(customLabelsCollection))))
      .exceptionally(handleException(asyncResultHandler));
  }

  private Function<Throwable, Void> handleException(Handler<AsyncResult<Response>> asyncResultHandler) {
    return throwable -> {
      errorHandler.handle(asyncResultHandler, throwable);
      return null;
    };
  }

}
