package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;

import java.util.Map;

import javax.ws.rs.core.Response;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import org.springframework.beans.factory.annotation.Autowired;

import org.folio.rest.aspect.HandleValidationErrors;
import org.folio.rest.jaxrs.model.AccessTypeCollectionItem;
import org.folio.rest.jaxrs.resource.EholdingsAccessTypes;
import org.folio.rest.util.ErrorHandler;
import org.folio.rest.validator.AccessTypesBodyValidator;
import org.folio.rest.validator.ValidatorUtil;
import org.folio.service.accesstypes.AccessTypesService;
import org.folio.spring.SpringContextUtil;

public class EholdingsAccessTypesImpl implements EholdingsAccessTypes {

  @Autowired
  private AccessTypesService accessTypesService;
  @Autowired
  private AccessTypesBodyValidator bodyValidator;
  @Autowired
  private ErrorHandler accessTypesExceptionHandler;

  public EholdingsAccessTypesImpl() {
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
  }

  @Override
  @HandleValidationErrors
  public void getEholdingsAccessTypes(Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
                                      Context vertxContext) {
    accessTypesService.findAll(okapiHeaders)
      .thenAccept(accessTypeCollection -> asyncResultHandler.handle(succeededFuture(
        GetEholdingsAccessTypesResponse.respond200WithApplicationVndApiJson(accessTypeCollection))))
      .exceptionally(throwable -> {
        accessTypesExceptionHandler.handle(asyncResultHandler, throwable);
        return null;
      });
  }

  @Override
  @HandleValidationErrors
  public void postEholdingsAccessTypes(String contentType, AccessTypeCollectionItem entity, Map<String, String> okapiHeaders,
                                       Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {

    bodyValidator.validate(entity, null);
    accessTypesService.save(entity, okapiHeaders)
    .thenAccept(accessType -> asyncResultHandler.handle(succeededFuture(
      PostEholdingsAccessTypesResponse.respond201WithApplicationVndApiJson(accessType))))
    .exceptionally(throwable -> {
      accessTypesExceptionHandler.handle(asyncResultHandler, throwable);
      return null;
    });
  }

  @Override
  @HandleValidationErrors
  public void getEholdingsAccessTypesById(String id, Map<String, String> okapiHeaders,
                                          Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    ValidatorUtil.checkUUIDValid(id);
    accessTypesService.findById(id, okapiHeaders)
      .thenAccept(accessTypeCollectionItem -> asyncResultHandler.handle(succeededFuture(
        GetEholdingsAccessTypesByIdResponse.respond200WithApplicationVndApiJson(accessTypeCollectionItem))))
      .exceptionally(throwable -> {
        accessTypesExceptionHandler.handle(asyncResultHandler, throwable);
        return null;
      });
  }

  @Override
  @HandleValidationErrors
  public void putEholdingsAccessTypesById(String id, String contentType, AccessTypeCollectionItem entity,
                                          Map<String, String> okapiHeaders,
                                          Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    bodyValidator.validate(entity, id);
    accessTypesService.update(id, entity, okapiHeaders)
      .thenAccept(accessType -> asyncResultHandler.handle(succeededFuture(
        PutEholdingsAccessTypesByIdResponse.respond204())))
      .exceptionally(throwable -> {
        accessTypesExceptionHandler.handle(asyncResultHandler, throwable);
        return null;
      });
  }

  @Override
  @HandleValidationErrors
  public void deleteEholdingsAccessTypesById(String id, Map<String, String> okapiHeaders,
                                             Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    ValidatorUtil.checkUUIDValid(id);
    accessTypesService.deleteById(id, okapiHeaders)
      .thenAccept(aVoid -> asyncResultHandler.handle(succeededFuture(DeleteEholdingsAccessTypesByIdResponse.respond204())))
      .exceptionally(throwable -> {
        accessTypesExceptionHandler.handle(asyncResultHandler, throwable);
        return null;
      });
  }
}
