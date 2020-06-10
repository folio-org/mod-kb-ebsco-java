package org.folio.rest.util;

import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.apache.http.HttpStatus.SC_UNPROCESSABLE_ENTITY;
import static org.apache.http.protocol.HTTP.CONTENT_TYPE;

import static org.folio.rest.util.ErrorUtil.createError;
import static org.folio.rest.util.ErrorUtil.createErrorFromRMAPIResponse;
import static org.folio.rest.util.RestConstants.JSON_API_TYPE;

import java.util.function.Function;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;

import org.folio.db.exc.AuthorizationException;
import org.folio.db.exc.ConstraintViolationException;
import org.folio.db.exc.DatabaseException;
import org.folio.holdingsiq.service.exception.ConfigurationInvalidException;
import org.folio.holdingsiq.service.exception.ServiceResponseException;
import org.folio.holdingsiq.service.exception.UnAuthorizedException;
import org.folio.rest.exception.InputValidationException;
import org.folio.service.holdings.exception.ProcessInProgressException;

public final class ExceptionMappers {

  private ExceptionMappers() {
  }

  /**
   * {@link NotFoundException} to {@link Response} error mapper
   * <pre>
   * Response.status = {@code 400}
   * Response.entity =  {@link org.folio.rest.jaxrs.model.JsonapiError}
   * Response.header.Content-Type = {@code application/vnd.api+json}
   * </pre>
   *
   * @return mapper
   */
  public static Function<NotFoundException, Response> error400NotFoundMapper() {
    return exception ->
      Response.status(SC_BAD_REQUEST)
        .header(CONTENT_TYPE, JSON_API_TYPE)
        .entity(createError(exception.getMessage()))
        .build();
  }

  /**
   * {@link BadRequestException} to {@link Response} error mapper
   * <pre>
   * Response.status = {@code 400}
   * Response.entity =  {@link org.folio.rest.jaxrs.model.JsonapiError}
   * Response.header.Content-Type = {@code application/vnd.api+json}
   * </pre>
   *
   * @return mapper
   */
  public static Function<BadRequestException, Response> error400BadRequestMapper() {
    return exception ->
      Response.status(SC_BAD_REQUEST)
        .header(CONTENT_TYPE, JSON_API_TYPE)
        .entity(createError(exception.getMessage()))
        .build();
  }

  /**
   * {@link InputValidationException} to {@link Response} error mapper
   * <pre>
   * Response.status = {@code 400}
   * Response.entity =  {@link org.folio.rest.jaxrs.model.JsonapiError}
   * Response.header.Content-Type = {@code application/vnd.api+json}
   * </pre>
   *
   * @return mapper
   */
  public static Function<InputValidationException, Response> error400InputValidationMapper() {
    return exception ->
      Response.status(SC_BAD_REQUEST)
        .header(CONTENT_TYPE, JSON_API_TYPE)
        .entity(createError(exception.getMessage(), exception.getMessageDetail()))
        .build();
  }

  /**
   * {@link ConstraintViolationException} to {@link Response} error mapper
   * <pre>
   * Response.status = {@code 400}
   * Response.entity =  {@link org.folio.rest.jaxrs.model.JsonapiError}
   * Response.header.Content-Type = {@code application/vnd.api+json}
   * </pre>
   *
   * @return mapper
   */
  public static Function<ConstraintViolationException, Response> error400ConstraintViolationMapper() {
    return exception ->
      Response.status(SC_BAD_REQUEST)
        .header(CONTENT_TYPE, JSON_API_TYPE)
        .entity(createError(exception.getMessage(), exception.getDetailedMessage()))
        .build();
  }

  /**
   * {@link DatabaseException} to {@link Response} error mapper
   * <pre>
   * Response.status = {@code 400}
   * Response.entity =  {@link org.folio.rest.jaxrs.model.JsonapiError}
   * Response.header.Content-Type = {@code application/vnd.api+json}
   * </pre>
   *
   * @return mapper
   */
  public static Function<DatabaseException, Response> error400DatabaseMapper() {
    return exception ->
      Response.status(SC_BAD_REQUEST)
        .header(CONTENT_TYPE, JSON_API_TYPE)
        .entity(createError(exception.getMessage(), exception.getSqlState()))
        .build();
  }

  /**
   * {@link NotAuthorizedException} to {@link Response} error mapper
   * <pre>
   * Response.status = {@code 401}
   * Response.entity =  {@link org.folio.rest.jaxrs.model.JsonapiError}
   * Response.header.Content-Type = {@code application/vnd.api+json}
   * </pre>
   *
   * @return mapper
   */
  public static Function<NotAuthorizedException, Response> error401NotAuthorizedMapper() {
    return exception ->
      Response.status(SC_UNAUTHORIZED)
        .header(CONTENT_TYPE, JSON_API_TYPE)
        .entity(createError(exception.getMessage()))
        .build();
  }

  /**
   * {@link AuthorizationException} to {@link Response} error mapper
   * <pre>
   * Response.status = {@code 401}
   * Response.entity =  {@link org.folio.rest.jaxrs.model.JsonapiError}
   * Response.header.Content-Type = {@code application/vnd.api+json}
   * </pre>
   *
   * @return mapper
   */
  public static Function<AuthorizationException, Response> error401AuthorizationMapper() {
    return exception ->
      Response.status(SC_UNAUTHORIZED)
        .header(CONTENT_TYPE, JSON_API_TYPE)
        .entity(createError(exception.getMessage()))
        .build();
  }

  /**
   * {@link UnAuthorizedException} to {@link Response} error mapper
   * <pre>
   * Response.status = {@code 403}
   * Response.entity =  {@link org.folio.rest.jaxrs.model.JsonapiError}
   * Response.header.Content-Type = {@code application/vnd.api+json}
   * </pre>
   *
   * @return mapper
   */
  public static Function<UnAuthorizedException, Response> error403UnAuthorizedMapper() {
    return exception -> Response
      .status(SC_FORBIDDEN)
      .header(CONTENT_TYPE, JSON_API_TYPE)
      .entity(createErrorFromRMAPIResponse(exception))
      .build();
  }

  /**
   * {@link NotFoundException} to {@link Response} error mapper
   * <pre>
   * Response.status = {@code 404}
   * Response.entity =  {@link org.folio.rest.jaxrs.model.JsonapiError}
   * Response.header.Content-Type = {@code application/vnd.api+json}
   * </pre>
   *
   * @return mapper
   */
  public static Function<NotFoundException, Response> error404NotFoundMapper() {
    return exception ->
      Response.status(SC_NOT_FOUND)
        .header(CONTENT_TYPE, JSON_API_TYPE)
        .entity(createError(exception.getMessage()))
        .build();
  }

  /**
   * {@link ProcessInProgressException} to {@link Response} error mapper
   * <pre>
   * Response.status = {@code 409}
   * Response.entity =  {@link org.folio.rest.jaxrs.model.JsonapiError}
   * Response.header.Content-Type = {@code application/vnd.api+json}
   * </pre>
   *
   * @return mapper
   */
  public static Function<ProcessInProgressException, Response> error409ProcessInProgressMapper() {
    return exception ->
      Response.status(SC_CONFLICT)
        .header(CONTENT_TYPE, JSON_API_TYPE)
        .entity(createError(exception.getMessage()))
        .build();
  }

  /**
   * {@link InputValidationException} to {@link Response} error mapper
   * <pre>
   * Response.status = {@code 422}
   * Response.entity =  {@link org.folio.rest.jaxrs.model.JsonapiError}
   * Response.header.Content-Type = {@code application/vnd.api+json}
   * </pre>
   *
   * @return mapper
   */
  public static Function<InputValidationException, Response> error422InputValidationMapper() {
    return exception ->
      Response.status(SC_UNPROCESSABLE_ENTITY)
        .header(CONTENT_TYPE, JSON_API_TYPE)
        .entity(createError(exception.getMessage(), exception.getMessageDetail()))
        .build();
  }

  /**
   * {@link ConfigurationInvalidException} to {@link Response} error mapper
   * <pre>
   * Response.status = {@code 422}
   * Response.entity =  {@link org.folio.rest.jaxrs.model.JsonapiError}
   * Response.header.Content-Type = {@code application/vnd.api+json}
   * </pre>
   *
   * @return mapper
   */
  public static Function<ConfigurationInvalidException, Response> error422ConfigurationInvalidMapper() {
    return exception ->
      Response.status(SC_UNPROCESSABLE_ENTITY)
        .header(CONTENT_TYPE, JSON_API_TYPE)
        .entity(ErrorUtil.createError(exception.getErrors().get(0).getMessage()))
        .build();
  }

  /**
   * {@link ServiceResponseException} to {@link Response} error mapper
   * <pre>
   * Response.status = {@code ServiceResponseException.code}
   * Response.entity =  {@link org.folio.rest.jaxrs.model.JsonapiError}
   * Response.header.Content-Type = {@code application/vnd.api+json}
   * </pre>
   *
   * @return mapper
   */
  public static Function<ServiceResponseException, Response> errorServiceResponseMapper() {
    return exception ->
      Response.status(exception.getCode())
        .header(CONTENT_TYPE, JSON_API_TYPE)
        .entity(createErrorFromRMAPIResponse(exception))
        .build();
  }

  /**
   * {@link Throwable} to {@link Response} error mapper
   * <pre>
   * Response.status = {@code 500}
   * Response.entity =  {@link org.folio.rest.jaxrs.model.JsonapiError}
   * Response.header.Content-Type = {@code application/vnd.api+json}
   * </pre>
   *
   * @return mapper
   */
  public static Function<Throwable, Response> error500ThrowableMapper() {
    return exception ->
      Response.status(SC_INTERNAL_SERVER_ERROR)
        .header(CONTENT_TYPE, JSON_API_TYPE)
        .entity(createError(INTERNAL_SERVER_ERROR.getReasonPhrase()))
        .build();
  }
}
