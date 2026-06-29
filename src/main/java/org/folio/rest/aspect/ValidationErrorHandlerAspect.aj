package org.folio.rest.aspect;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import jakarta.validation.ValidationException;
import javax.ws.rs.core.Response;
import org.aspectj.lang.annotation.SuppressAjWarnings;
import org.folio.holdingsiq.service.exception.RequestValidationException;
import org.folio.rest.exception.InputValidationException;
import org.folio.rest.exception.QueryParamsValidationException;
import org.folio.rest.util.ErrorUtil;

public aspect ValidationErrorHandlerAspect {
  pointcut validatedMethodCall(
    Handler<AsyncResult<Response>> asyncResultHandler): execution(@HandleValidationErrors * *(.., Handler, *)) && args(.., asyncResultHandler, *);

  @SuppressAjWarnings({"adviceDidNotMatch"})
  void around(Handler<AsyncResult<Response>> asyncResultHandler): validatedMethodCall(asyncResultHandler) {
    try {
      proceed(asyncResultHandler);
    } catch (ValidationException | RequestValidationException e) {
      asyncResultHandler.handle(Future.succeededFuture(Response.status(400)
        .header("Content-Type", "application/vnd.api+json")
        .entity(ErrorUtil.createError(e.getMessage()))
        .build()));
      return;
    } catch (QueryParamsValidationException e) {
      asyncResultHandler.handle(Future.succeededFuture(Response.status(400)
        .header("Content-Type", "application/vnd.api+json")
        .entity(ErrorUtil.createErrors(e.getMessageDetail()))
        .build()));
    } catch (InputValidationException e) {
      asyncResultHandler.handle(Future.succeededFuture(Response.status(422)
        .header("Content-Type", "application/vnd.api+json")
        .entity(ErrorUtil.createError(e.getMessage(), e.getMessageDetail()))
        .build()));
      return;
    }
  }
}
