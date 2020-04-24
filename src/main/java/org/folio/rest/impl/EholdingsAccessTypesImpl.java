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
import org.springframework.beans.factory.annotation.Qualifier;

import org.folio.rest.annotations.Validate;
import org.folio.rest.aspect.HandleValidationErrors;
import org.folio.rest.jaxrs.model.AccessType;
import org.folio.rest.jaxrs.model.AccessTypePostRequest;
import org.folio.rest.jaxrs.resource.EholdingsAccessTypes;
import org.folio.rest.jaxrs.resource.EholdingsKbCredentialsIdAccessTypes;
import org.folio.rest.util.ErrorHandler;
import org.folio.service.accesstypes.AccessTypesService;
import org.folio.spring.SpringContextUtil;

public class EholdingsAccessTypesImpl implements EholdingsAccessTypes, EholdingsKbCredentialsIdAccessTypes {

  @Autowired
  @Qualifier("newAccessTypesService")
  private AccessTypesService accessTypesService;
  @Autowired
  @Qualifier("oldAccessTypesService")
  private AccessTypesService oldAccessTypesService;
  @Autowired
  private ErrorHandler errorHandler;

  public EholdingsAccessTypesImpl() {
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
  }

  @Override
  @Validate
  @HandleValidationErrors
  public void getEholdingsAccessTypes(Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
                                      Context vertxContext) {
    accessTypesService.findByUser(okapiHeaders)
      .thenAccept(accessTypeCollection -> asyncResultHandler.handle(succeededFuture(
        GetEholdingsAccessTypesResponse.respond200WithApplicationVndApiJson(accessTypeCollection))))
      .exceptionally(handleException(asyncResultHandler));
  }

  @Override
  @Validate
  @HandleValidationErrors
  public void getEholdingsKbCredentialsAccessTypesById(String credentialsId, Map<String, String> okapiHeaders,
                                                       Handler<AsyncResult<Response>> asyncResultHandler,
                                                       Context vertxContext) {
    accessTypesService.findByCredentialsId(credentialsId, okapiHeaders)
      .thenAccept(accessTypeCollection -> asyncResultHandler.handle(succeededFuture(
        GetEholdingsAccessTypesResponse.respond200WithApplicationVndApiJson(accessTypeCollection))))
      .exceptionally(handleException(asyncResultHandler));
  }

  @Override
  @Validate
  @HandleValidationErrors
  public void postEholdingsKbCredentialsAccessTypesById(String credentialsId, AccessTypePostRequest entity,
                                                        Map<String, String> okapiHeaders,
                                                        Handler<AsyncResult<Response>> asyncResultHandler,
                                                        Context vertxContext) {
    accessTypesService.save(credentialsId, entity, okapiHeaders)
      .thenAccept(accessType -> asyncResultHandler.handle(succeededFuture(
        PostEholdingsKbCredentialsAccessTypesByIdResponse.respond201WithApplicationVndApiJson(accessType))))
      .exceptionally(handleException(asyncResultHandler));
  }

  @Override
  @Validate
  @HandleValidationErrors
  public void getEholdingsAccessTypesById(String id, Map<String, String> okapiHeaders,
                                          Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    oldAccessTypesService.findById(id, okapiHeaders)
      .thenAccept(accessType -> asyncResultHandler.handle(succeededFuture(
        GetEholdingsAccessTypesByIdResponse.respond200WithApplicationVndApiJson(accessType))))
      .exceptionally(handleException(asyncResultHandler));
  }

  @Override
  @Validate
  @HandleValidationErrors
  public void putEholdingsAccessTypesById(String id, String contentType, AccessType entity,
                                          Map<String, String> okapiHeaders,
                                          Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    oldAccessTypesService.update(id, entity, okapiHeaders)
      .thenAccept(accessType -> asyncResultHandler.handle(succeededFuture(
        PutEholdingsAccessTypesByIdResponse.respond204())))
      .exceptionally(handleException(asyncResultHandler));
  }

  @Override
  @Validate
  @HandleValidationErrors
  public void deleteEholdingsAccessTypesById(String id, Map<String, String> okapiHeaders,
                                             Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    oldAccessTypesService.deleteById(id, okapiHeaders)
      .thenAccept(aVoid -> asyncResultHandler.handle(succeededFuture(DeleteEholdingsAccessTypesByIdResponse.respond204())))
      .exceptionally(handleException(asyncResultHandler));
  }

  private Function<Throwable, Void> handleException(Handler<AsyncResult<Response>> asyncResultHandler) {
    return throwable -> {
      errorHandler.handle(asyncResultHandler, throwable);
      return null;
    };
  }
}
