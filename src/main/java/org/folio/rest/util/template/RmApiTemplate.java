package org.folio.rest.util.template;

import static org.folio.rest.util.RestConstants.JSON_API_TYPE;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import javax.ws.rs.core.Response;
import org.apache.http.HttpStatus;
import org.apache.http.protocol.HTTP;
import org.folio.holdingsiq.model.OkapiData;
import org.folio.rest.util.ErrorHandler;
import org.folio.rest.validator.HeaderValidator;
import org.folio.service.kbcredentials.UserKbCredentialsService;
import org.springframework.core.convert.ConversionService;

/**
 * Provides a common template for asynchronous interaction with Holdings services,
 * RMAPITemplate executes following step:
 * 1) Creates and configures Holdings services
 * 2) Calls requestAction with one of Holding services as a parameter
 * 3) Automatically converts return value of requestAction to the required response
 * 4) Optionally handles exception with custom error mappers or with default list of error mappers
 * Automatic conversion of result requires ConversionService to contain appropriate Converter implementation
 * RMAPITemplate requires following parameters:
 * 1) okapiHeaders to retrieve correct RMAPIConfiguration
 * 2) asyncResultHandler that will be called on success or failure
 * 3) requestAction function that defines main interaction with Holdings services
 * 4) optional error mappers.
 */
public class RmApiTemplate {

  private final UserKbCredentialsService userKbCredentialsService;
  private final ConversionService conversionService;
  private final HeaderValidator headerValidator;
  private final RmApiTemplateContextBuilder contextBuilder;

  private final Map<String, String> okapiHeaders;
  private final Handler<AsyncResult<Response>> asyncResultHandler;

  private Function<RmApiTemplateContext, CompletableFuture<?>> requestAction;

  private ErrorHandler errorHandler = new ErrorHandler();

  public RmApiTemplate(UserKbCredentialsService userKbCredentialsService, ConversionService conversionService,
                       HeaderValidator headerValidator, RmApiTemplateContextBuilder contextBuilder,
                       Map<String, String> okapiHeaders,
                       Handler<AsyncResult<Response>> asyncResultHandler) {
    this.userKbCredentialsService = userKbCredentialsService;
    this.conversionService = conversionService;
    this.headerValidator = headerValidator;
    this.okapiHeaders = okapiHeaders;
    this.asyncResultHandler = asyncResultHandler;
    this.contextBuilder = contextBuilder;
  }

  /**
   * Return value of this function will be converted to response.
   *
   * @param requestAction Defines function that will be executed after Holdings services are configured
   * @return this
   */
  public RmApiTemplate requestAction(Function<RmApiTemplateContext, CompletableFuture<?>> requestAction) {
    this.requestAction = requestAction;
    return this;
  }

  /**
   * Register external errorHandler.
   */
  public RmApiTemplate withErrorHandler(ErrorHandler errorHandler) {
    this.errorHandler = errorHandler;
    return this;
  }

  /**
   * Register error mapper for exceptionClass.
   *
   * @param exceptionClass class of exception that this mapper will handle
   * @param errorMapper    function that converts exception to javax.ws.rs.core.Response
   * @return this
   */
  public <T extends Throwable> RmApiTemplate addErrorMapper(Class<T> exceptionClass,
                                                            Function<T, Response> errorMapper) {
    errorHandler.add(exceptionClass, errorMapper);
    return this;
  }

  /**
   * Runs main template method asynchronously, converts return value into responseClass, and returns it
   * in response body with status code 200.
   */
  public <T> void executeWithResult(Class<T> responseClass) {
    executeInternal(
      result ->
        Response
          .status(HttpStatus.SC_OK)
          .header(HTTP.CONTENT_TYPE, JSON_API_TYPE)
          .entity(conversionService.convert(result, responseClass))
          .build()
    );
  }

  /**
   * Runs main template method asynchronously and returns 204 "NO CONTENT" response.
   */
  public void execute() {
    executeInternal(
      result -> Response
        .status(HttpStatus.SC_NO_CONTENT)
        .build()
    );
  }

  public CompletableFuture<RmApiTemplateContext> getRmapiTemplateContext() {
    headerValidator.validate(okapiHeaders);
    return CompletableFuture.completedFuture(null)
      .thenCompose(o -> {
        OkapiData okapiData = new OkapiData(okapiHeaders);
        contextBuilder.okapiData(okapiData);
        return userKbCredentialsService.findByUser(okapiHeaders);
      })
      .thenAccept(contextBuilder::kbCredentials)
      .thenApply(unused -> contextBuilder.build());
  }

  private void executeInternal(Function<Object, Response> successHandler) {
    getRmapiTemplateContext()
      .thenCompose(rmapiTemplateContext -> requestAction.apply(rmapiTemplateContext))
      .thenAccept(result -> asyncResultHandler.handle(Future.succeededFuture(successHandler.apply(result))))
      .exceptionally(e -> {
        errorHandler
          .addInputValidation400Mapper()
          .addRmApiMapping()
          .handle(asyncResultHandler, e);
        return null;
      });
  }
}
