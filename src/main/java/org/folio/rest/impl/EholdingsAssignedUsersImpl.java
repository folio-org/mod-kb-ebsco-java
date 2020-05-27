package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;

import java.util.Map;

import javax.ws.rs.core.Response;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import org.springframework.beans.factory.annotation.Autowired;

import org.folio.rest.annotations.Validate;
import org.folio.rest.aspect.HandleValidationErrors;
import org.folio.rest.jaxrs.model.AssignedUserPostRequest;
import org.folio.rest.jaxrs.model.AssignedUserPutRequest;
import org.folio.rest.jaxrs.resource.EholdingsKbCredentialsIdUsers;
import org.folio.rest.util.ErrorHandler;
import org.folio.service.assignedusers.AssignedUsersService;
import org.folio.spring.SpringContextUtil;

public class EholdingsAssignedUsersImpl implements EholdingsKbCredentialsIdUsers {

  @Autowired
  private AssignedUsersService assignedUsersService;
  @Autowired
  private ErrorHandler errorHandler;

  public EholdingsAssignedUsersImpl() {
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
  }

  @Override
  @Validate
  @HandleValidationErrors
  public void getEholdingsKbCredentialsUsersById(String id, Map<String, String> okapiHeaders,
                                                 Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    assignedUsersService.findByCredentialsId(id, okapiHeaders)
      .thenAccept(assignedUserCollection -> asyncResultHandler.handle(succeededFuture(
        GetEholdingsKbCredentialsUsersByIdResponse.respond200WithApplicationVndApiJson(assignedUserCollection))))
      .exceptionally(errorHandler.handle(asyncResultHandler));
  }

  @Override
  @Validate
  @HandleValidationErrors
  public void postEholdingsKbCredentialsUsersById(String id, String contentType, AssignedUserPostRequest entity,
                                                  Map<String, String> okapiHeaders,
                                                  Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    assignedUsersService.save(entity, okapiHeaders)
      .thenAccept(assignedUser -> asyncResultHandler.handle(succeededFuture(
        PostEholdingsKbCredentialsUsersByIdResponse.respond201WithApplicationVndApiJson(assignedUser))))
      .exceptionally(errorHandler.handle(asyncResultHandler));
  }

  @Override
  @Validate
  @HandleValidationErrors
  public void putEholdingsKbCredentialsUsersByIdAndUserId(String id, String userId, AssignedUserPutRequest entity,
                                                          Map<String, String> okapiHeaders,
                                                          Handler<AsyncResult<Response>> asyncResultHandler,
                                                          Context vertxContext) {
    assignedUsersService.update(id, userId, entity, okapiHeaders)
      .thenAccept(o -> asyncResultHandler.handle(succeededFuture(
        PutEholdingsKbCredentialsUsersByIdAndUserIdResponse.respond204())))
      .exceptionally(errorHandler.handle(asyncResultHandler));
  }

  @Override
  @Validate
  @HandleValidationErrors
  public void deleteEholdingsKbCredentialsUsersByIdAndUserId(String id, String userId, Map<String, String> okapiHeaders,
                                                             Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    assignedUsersService.delete(id, userId, okapiHeaders)
      .thenAccept(kbCredentials -> asyncResultHandler.handle(succeededFuture(
        DeleteEholdingsKbCredentialsUsersByIdAndUserIdResponse.respond204())))
      .exceptionally(errorHandler.handle(asyncResultHandler));
  }

}
