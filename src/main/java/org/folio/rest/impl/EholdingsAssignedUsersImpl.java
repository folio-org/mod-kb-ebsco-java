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
import org.folio.rest.jaxrs.resource.EholdingsKbCredentialsId;
import org.folio.rest.util.ErrorHandler;
import org.folio.service.assignedusers.AssignedUsersService;
import org.folio.spring.SpringContextUtil;

public class EholdingsAssignedUsersImpl implements EholdingsKbCredentialsId {

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
      .exceptionally(handleException(asyncResultHandler));;
  }

  private Function<Throwable, Void> handleException(Handler<AsyncResult<Response>> asyncResultHandler) {
    return throwable -> {
      errorHandler.handle(asyncResultHandler, throwable);
      return null;
    };
  }
}
