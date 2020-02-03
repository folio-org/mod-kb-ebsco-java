package org.folio.rest.exception;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.apache.http.protocol.HTTP.CONTENT_TYPE;

import static org.folio.rest.util.RestConstants.JSON_API_TYPE;

import java.util.function.Function;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;

import org.folio.db.exc.AuthorizationException;
import org.folio.db.exc.ConstraintViolationException;
import org.folio.rest.util.ErrorUtil;

public class AccessTypesExceptionHandlers {


  public AccessTypesExceptionHandlers() {
  }
  /**
   * Register error mapper for BadRequestException
   * @return this
   */
  public static Function<BadRequestException, Response> error400Mapper() {
    return exception ->
      Response.status(SC_BAD_REQUEST)
        .header(CONTENT_TYPE, JSON_API_TYPE)
        .entity(ErrorUtil.createError(exception.getMessage()))
        .build();
  }

  public static Function<NotFoundException, Response> error404Mapper() {
    return exception ->
      Response.status(SC_NOT_FOUND)
        .header(CONTENT_TYPE, JSON_API_TYPE)
        .entity(ErrorUtil.createError(exception.getMessage()))
        .build();
  }
  public static Function<NotAuthorizedException, Response> error401NotAuthorizedMapper() {
    return exception ->
      Response.status(SC_UNAUTHORIZED)
        .header(CONTENT_TYPE, JSON_API_TYPE)
        .entity(ErrorUtil.createError(exception.getMessage()))
        .build();
  }

  public static Function<AuthorizationException, Response> error401AuthorizationExcMapper() {
    return exception ->
      Response.status(SC_UNAUTHORIZED)
        .header(CONTENT_TYPE, JSON_API_TYPE)
        .entity(ErrorUtil.createError(exception.getMessage()))
        .build();
  }

  public static Function<ConstraintViolationException, Response> errorDataBaseMapper() {
    return exception ->
      Response.status(SC_BAD_REQUEST)
        .header(CONTENT_TYPE, JSON_API_TYPE)
        .entity(ErrorUtil.createError(exception.getMessage(), exception.getDetailedMessage()))
        .build();
  }
}
