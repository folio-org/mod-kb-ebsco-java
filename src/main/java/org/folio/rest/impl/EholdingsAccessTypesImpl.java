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
import org.folio.rest.jaxrs.model.AccessTypePostRequest;
import org.folio.rest.jaxrs.model.AccessTypePutRequest;
import org.folio.rest.jaxrs.resource.EholdingsAccessTypes;
import org.folio.rest.jaxrs.resource.EholdingsKbCredentialsIdAccessTypes;
import org.folio.rest.util.ErrorHandler;
import org.folio.service.accesstypes.AccessTypesService;
import org.folio.spring.SpringContextUtil;
import org.springframework.beans.factory.annotation.Autowired;

@SuppressWarnings("java:S6813")
public class EholdingsAccessTypesImpl implements EholdingsAccessTypes, EholdingsKbCredentialsIdAccessTypes {

  @Autowired
  private AccessTypesService accessTypesService;
  @Autowired
  private ErrorHandler errorHandler;

  public EholdingsAccessTypesImpl() {
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
  }

  @Override
  @Validate
  @HandleValidationErrors
  public void getEholdingsAccessTypes(Map<String, String> okapiHeaders,
                                      Handler<AsyncResult<Response>> asyncResultHandler,
                                      Context vertxContext) {
    accessTypesService.findByUser(okapiHeaders)
      .thenAccept(accessTypeCollection -> asyncResultHandler.handle(succeededFuture(
        GetEholdingsAccessTypesResponse.respond200WithApplicationVndApiJson(accessTypeCollection))))
      .exceptionally(handleException(asyncResultHandler));
  }

  @Override
  @Validate
  @HandleValidationErrors
  public void getEholdingsAccessTypesById(String id, Map<String, String> okapiHeaders,
                                          Handler<AsyncResult<Response>> asyncResultHandler,
                                          Context vertxContext) {
    accessTypesService.findByUserAndId(id, okapiHeaders)
      .thenAccept(accessType -> asyncResultHandler.handle(succeededFuture(
        GetEholdingsAccessTypesByIdResponse.respond200WithApplicationVndApiJson(accessType))))
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
  public void getEholdingsKbCredentialsAccessTypesByIdAndAccessTypeId(String credentialsId, String accessTypeId,
                                                                      Map<String, String> okapiHeaders,
                                                                      Handler<AsyncResult<Response>> asyncResultHandler,
                                                                      Context vertxContext) {
    accessTypesService.findByCredentialsAndAccessTypeId(credentialsId, accessTypeId, true, okapiHeaders)
      .thenAccept(accessType -> asyncResultHandler.handle(succeededFuture(
        GetEholdingsKbCredentialsAccessTypesByIdAndAccessTypeIdResponse.respond200WithApplicationVndApiJson(
          accessType))))
      .exceptionally(handleException(asyncResultHandler));
  }

  @Override
  @Validate
  @HandleValidationErrors
  public void putEholdingsKbCredentialsAccessTypesByIdAndAccessTypeId(String credentialsId, String accessTypeId,
                                                                      AccessTypePutRequest entity,
                                                                      Map<String, String> okapiHeaders,
                                                                      Handler<AsyncResult<Response>> asyncResultHandler,
                                                                      Context vertxContext) {
    accessTypesService.update(credentialsId, accessTypeId, entity, okapiHeaders)
      .thenAccept(accessType -> asyncResultHandler.handle(succeededFuture(
        PutEholdingsKbCredentialsAccessTypesByIdAndAccessTypeIdResponse.respond204())))
      .exceptionally(handleException(asyncResultHandler));
  }

  @Override
  @Validate
  @HandleValidationErrors
  public void deleteEholdingsKbCredentialsAccessTypesByIdAndAccessTypeId(
    String credentialsId, String accessTypeId,
    Map<String, String> okapiHeaders,
    Handler<AsyncResult<Response>> asyncResultHandler,
    Context vertxContext) {
    accessTypesService.delete(credentialsId, accessTypeId, okapiHeaders)
      .thenAccept(v -> asyncResultHandler.handle(succeededFuture(
        DeleteEholdingsKbCredentialsAccessTypesByIdAndAccessTypeIdResponse.respond204())))
      .exceptionally(handleException(asyncResultHandler));
  }

  private Function<Throwable, Void> handleException(Handler<AsyncResult<Response>> asyncResultHandler) {
    return throwable -> {
      errorHandler.handle(asyncResultHandler, throwable);
      return null;
    };
  }
}
