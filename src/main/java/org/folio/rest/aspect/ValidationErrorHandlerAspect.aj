package org.folio.rest.aspect;

import org.folio.holdingsiq.service.exception.RequestValidationException;
import org.folio.rest.annotations.Validate;
import org.folio.rest.exception.InputValidationException;
import org.folio.rest.util.ErrorUtil;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Future;

import org.aspectj.lang.annotation.SuppressAjWarnings;

import javax.ws.rs.core.Response;
import jakarta.validation.ValidationException;

public aspect ValidationErrorHandlerAspect {
  pointcut validatedMethodCall(Handler<AsyncResult<Response>> asyncResultHandler) : execution(@HandleValidationErrors * *(.., Handler, *)) && args(.., asyncResultHandler, *);

  @SuppressAjWarnings({"adviceDidNotMatch"})
  void around(Handler asyncResultHandler) : validatedMethodCall(asyncResultHandler) {
    try{
      proceed(asyncResultHandler);
    }
    catch (ValidationException | RequestValidationException e){
            asyncResultHandler.handle(Future.succeededFuture(Response.status(400)
            .header("Content-Type", "application/vnd.api+json")
            .entity(ErrorUtil.createError(e.getMessage()))
            .build()));
            return;
    }
    catch (InputValidationException e){
      asyncResultHandler.handle(Future.succeededFuture(Response.status(422)
      .header("Content-Type", "application/vnd.api+json")
      .entity(ErrorUtil.createError(e.getMessage(), e.getMessageDetail()))
      .build()));
      return;
    }
  }
}
