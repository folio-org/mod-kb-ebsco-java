package org.folio.rest.util;

import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static org.folio.rest.util.ExceptionMappers.error400InputValidationMapper;
import static org.folio.rest.util.ExceptionMappers.error403UnAuthorizedMapper;
import static org.folio.rest.util.ExceptionMappers.error422InputValidationMapper;
import static org.folio.rest.util.ExceptionMappers.error500ThrowableMapper;
import static org.folio.rest.util.ExceptionMappers.errorServiceResponseMapper;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import javax.ws.rs.core.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.holdingsiq.service.exception.ServiceResponseException;
import org.folio.holdingsiq.service.exception.UnAuthorizedException;
import org.folio.rest.exception.InputValidationException;

/**
 * Utility class for mapping exceptions to response that is passed to io.vertx.core.Handler
 * ErrorHandler instance can be configured with error mappers for each Exception type by using add* methods,
 * When {@link org.folio.rest.util.ErrorHandler#handle(io.vertx.core.Handler, java.lang.Throwable)} method is called,
 * first mapper that matches exception type is used to construct javax.ws.rs.core.Response
 * and pass it to io.vertx.core.Handler. Error mappers are checked in the order they were registered
 */
public class ErrorHandler {

  private static final Logger LOGGER = LogManager.getLogger(ErrorHandler.class);

  private static final Function<Throwable, Response> DEFAULT_MAPPER = error500ThrowableMapper();

  private final Map<Class<? extends Throwable>, Function<? extends Throwable, Response>> errorMappers =
    new LinkedHashMap<>();

  /**
   * Register error mapper for exceptionClass.
   *
   * @param exceptionClass class of exception that this mapper will handle
   * @param errorMapper    function that converts exception to javax.ws.rs.core.Response
   * @return this
   */
  public <T extends Throwable> ErrorHandler add(Class<T> exceptionClass, Function<T, Response> errorMapper) {
    errorMappers.putIfAbsent(exceptionClass, errorMapper);
    return this;
  }

  /**
   * Register error mapper for InputValidationException.
   *
   * @return this
   */
  public ErrorHandler addInputValidation400Mapper() {
    add(InputValidationException.class, error400InputValidationMapper());
    return this;
  }

  public ErrorHandler addInputValidation422Mapper() {
    add(InputValidationException.class, error422InputValidationMapper());
    return this;
  }

  /**
   * Register error mapping for RMAPIServiceException and RMAPIUnAuthorizedException.
   *
   * @return this
   */
  public ErrorHandler addRmApiMapping() {
    add(UnAuthorizedException.class, error403UnAuthorizedMapper());
    add(ServiceResponseException.class, errorServiceResponseMapper());
    return this;
  }

  /**
   * Use registered error mappers to create response from exception and pass it to asyncResultHandler.
   */
  public void handle(Handler<AsyncResult<Response>> asyncResultHandler, Throwable e) {
    Optional<Function<? extends Throwable, Response>> optionalErrorMapper = errorMappers.entrySet()
      .stream()
      .filter(entry -> entry.getKey().isInstance(e.getCause()))
      .findFirst()
      .map(Map.Entry::getValue);

    if (optionalErrorMapper.isPresent()) {
      //    Type of "e" parameter is guaranteed to match type of found mapper, because of isInstance check,
      //    so type-safety here
      @SuppressWarnings("unchecked")
      Function<Throwable, Response> errorMapper = (Function<Throwable, Response>) optionalErrorMapper.get();
      asyncResultHandler.handle(Future.succeededFuture(errorMapper.apply(e.getCause())));
    } else {
      LOGGER.error(INTERNAL_SERVER_ERROR.getReasonPhrase(), e.getCause());
      asyncResultHandler.handle(Future.succeededFuture(DEFAULT_MAPPER.apply(e.getCause())));
    }
  }

  public Function<Throwable, Void> handle(Handler<AsyncResult<Response>> asyncResultHandler) {
    return throwable -> {
      handle(asyncResultHandler, throwable);
      return null;
    };
  }
}


