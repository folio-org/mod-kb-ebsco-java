package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;
import static javax.ws.rs.core.Response.status;

import java.util.Map;

import javax.ws.rs.core.Response;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

import org.folio.rest.aspect.HandleValidationErrors;
import org.folio.rest.jaxrs.model.AccessTypeCollectionItem;
import org.folio.rest.jaxrs.resource.EholdingsAccessTypes;
import org.folio.spring.SpringContextUtil;

public class EholdingsAccessTypesImpl implements EholdingsAccessTypes {

  public EholdingsAccessTypesImpl() {
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
  }

  @Override
  @HandleValidationErrors
  public void getEholdingsAccessTypes(Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
                                      Context vertxContext) {
    asyncResultHandler.handle(succeededFuture(status(Response.Status.NOT_IMPLEMENTED).build()));
  }

  @Override
  @HandleValidationErrors
  public void postEholdingsAccessTypes(String contentType, AccessTypeCollectionItem entity, Map<String, String> okapiHeaders,
                                       Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {

    asyncResultHandler.handle(succeededFuture(status(Response.Status.NOT_IMPLEMENTED).build()));
  }

  @Override
  @HandleValidationErrors
  public void getEholdingsAccessTypesById(String id, Map<String, String> okapiHeaders,
                                          Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    asyncResultHandler.handle(succeededFuture(status(Response.Status.NOT_IMPLEMENTED).build()));
  }

  @Override
  @HandleValidationErrors
  public void putEholdingsAccessTypesById(String id, String contentType, AccessTypeCollectionItem entity,
                                          Map<String, String> okapiHeaders,
                                          Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    asyncResultHandler.handle(succeededFuture(status(Response.Status.NOT_IMPLEMENTED).build()));
  }

  @Override
  @HandleValidationErrors
  public void deleteEholdingsAccessTypesById(String id, Map<String, String> okapiHeaders,
                                             Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    asyncResultHandler.handle(succeededFuture(status(Response.Status.NOT_IMPLEMENTED).build()));
  }
}
