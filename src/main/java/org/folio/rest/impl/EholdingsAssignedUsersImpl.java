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
import org.folio.rest.validator.AssignedUsersBodyValidator;
import org.folio.service.assignedusers.AssignedUsersService;
import org.folio.spring.SpringContextUtil;

public class EholdingsAssignedUsersImpl implements EholdingsKbCredentialsIdUsers {

  @Autowired
  private AssignedUsersBodyValidator assignedUsersBodyValidator;
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
  public void getEholdingsKbCredentialsUsersById(String credentialsId, Map<String, String> okapiHeaders,
                                                 Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    assignedUsersService.findByCredentialsId(credentialsId, okapiHeaders)
      .thenAccept(assignedUserCollection -> asyncResultHandler.handle(succeededFuture(
        GetEholdingsKbCredentialsUsersByIdResponse.respond200WithApplicationVndApiJson(assignedUserCollection))))
      .exceptionally(errorHandler.handle(asyncResultHandler));
  }

  @Override
  @Validate
  @HandleValidationErrors
  public void postEholdingsKbCredentialsUsersById(String credentialsId, String contentType, AssignedUserPostRequest entity,
                                                  Map<String, String> okapiHeaders,
                                                  Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    assignedUsersBodyValidator.validate(entity.getData());
    assignedUsersService.save(entity, okapiHeaders)
      .thenAccept(assignedUser -> asyncResultHandler.handle(succeededFuture(
        PostEholdingsKbCredentialsUsersByIdResponse.respond201WithApplicationVndApiJson(assignedUser))))
      .exceptionally(errorHandler.handle(asyncResultHandler));
  }

  @Override
  @Validate
  @HandleValidationErrors
  public void putEholdingsKbCredentialsUsersByIdAndUserId(String credentialsId, String userId, AssignedUserPutRequest entity,
                                                          Map<String, String> okapiHeaders,
                                                          Handler<AsyncResult<Response>> asyncResultHandler,
                                                          Context vertxContext) {
    assignedUsersBodyValidator.validate(entity.getData());
    assignedUsersService.update(credentialsId, userId, entity, okapiHeaders)
      .thenAccept(o -> asyncResultHandler.handle(succeededFuture(
        PutEholdingsKbCredentialsUsersByIdAndUserIdResponse.respond204())))
      .exceptionally(errorHandler.handle(asyncResultHandler));
  }

  @Override
  @Validate
  @HandleValidationErrors
  public void deleteEholdingsKbCredentialsUsersByIdAndUserId(String credentialsId, String userId,
                                                             Map<String, String> okapiHeaders,
                                                             Handler<AsyncResult<Response>> asyncResultHandler,
                                                             Context vertxContext) {
    assignedUsersService.delete(credentialsId, userId, okapiHeaders)
      .thenAccept(kbCredentials -> asyncResultHandler.handle(succeededFuture(
        DeleteEholdingsKbCredentialsUsersByIdAndUserIdResponse.respond204())))
      .exceptionally(errorHandler.handle(asyncResultHandler));
  }

}
