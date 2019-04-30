package org.folio.rest.util.template;

import static org.folio.rest.util.RestConstants.JSON_API_TYPE;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import javax.ws.rs.core.Response;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import org.apache.http.HttpStatus;
import org.apache.http.protocol.HTTP;
import org.springframework.core.convert.ConversionService;

import org.folio.cache.VertxCache;
import org.folio.holdingsiq.model.OkapiData;
import org.folio.holdingsiq.model.PackageByIdData;
import org.folio.holdingsiq.service.ConfigurationService;
import org.folio.holdingsiq.service.HoldingsIQService;
import org.folio.holdingsiq.service.TitlesHoldingsIQService;
import org.folio.holdingsiq.service.impl.HoldingsIQServiceImpl;
import org.folio.holdingsiq.service.impl.TitlesHoldingsIQServiceImpl;
import org.folio.rest.impl.EholdingsPackagesImpl;
import org.folio.rest.util.ErrorHandler;
import org.folio.rest.validator.HeaderValidator;
import org.folio.rmapi.PackageServiceImpl;
import org.folio.rmapi.ProvidersServiceImpl;
import org.folio.rmapi.ResourcesServiceImpl;
import org.folio.rmapi.cache.PackageCacheKey;

/**
 * Provides a common template for asynchronous interaction with ProvidersServiceImpl,
 *
 * RMAPITemplate executes following step:
 * 1) Creates and configures Holdings services
 * 2) Calls requestAction with one of Holding services as a parameter
 * 3) Automatically converts return value of requestAction to the required response
 * 4) Optionally handles exception with custom error mappers or with default list of error mappers
 *
 * Automatic conversion of result requires ConversionService to contain appropriate Converter implementation
 *
 * RMAPITemplate requires following parameters:
 * 1) okapiHeaders to retrieve correct RMAPIConfiguration
 * 2) asyncResultHandler that will be called on success or failure
 * 3) requestAction function that defines main interaction with Holdings services
 * 4) optional error mappers
 */
public class RMAPITemplate {

  private final Logger logger = LoggerFactory.getLogger(EholdingsPackagesImpl.class);

  private ConfigurationService configurationService;
  private Vertx vertx;
  private ConversionService conversionService;
  private HeaderValidator headerValidator;
  private VertxCache<PackageCacheKey, PackageByIdData> packageCache;

  private Map<String, String> okapiHeaders;
  private Handler<AsyncResult<Response>> asyncResultHandler;

  private Function<RMAPITemplateContext, CompletableFuture<?>> requestAction;

  private ErrorHandler errorHandler = new ErrorHandler();


  public RMAPITemplate(ConfigurationService configurationService, Vertx vertx, ConversionService conversionService,
                       HeaderValidator headerValidator, VertxCache<PackageCacheKey, PackageByIdData> packageCache, Map<String, String> okapiHeaders,
                       Handler<AsyncResult<Response>> asyncResultHandler) {
    this.configurationService = configurationService;
    this.vertx = vertx;
    this.conversionService = conversionService;
    this.headerValidator = headerValidator;
    this.okapiHeaders = okapiHeaders;
    this.asyncResultHandler = asyncResultHandler;
    this.packageCache = packageCache;
  }

  /**
   * @param requestAction Defines function that will be executed after ProvidersServiceImpl is configured
   *                      Return value of this function will be converted to response
   * @return this
   */
  public RMAPITemplate requestAction(Function<RMAPITemplateContext, CompletableFuture<?>> requestAction){
    this.requestAction = requestAction;
    return this;
  }

  /**
   * Register error mapper for exceptionClass
   * @param exceptionClass class of exception that this mapper will handle
   * @param errorMapper function that converts exception to javax.ws.rs.core.Response
   * @return this
   */
  public <T extends Throwable>  RMAPITemplate addErrorMapper(Class<T> exceptionClass, Function<T, Response> errorMapper){
    errorHandler.add(exceptionClass, errorMapper);
    return this;
  }

  /**
   * Runs main template method asynchronously, converts return value into responseClass, and returns it
   * in response body with status code 200
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
   * Runs main template method asynchronously and returns 204 "NO CONTENT" response
   */
  public void execute() {
    executeInternal(
      result -> Response
        .status(HttpStatus.SC_NO_CONTENT)
        .build()
    );
  }

  private void executeInternal(Function<Object, Response> successHandler) {
    headerValidator.validate(okapiHeaders);
    RMAPITemplateContext.RMAPITemplateContextBuilder contextBuilder = RMAPITemplateContext.builder();
    CompletableFuture.completedFuture(null)
      .thenCompose(o -> {
        OkapiData okapiData = new OkapiData(okapiHeaders);
        contextBuilder.okapiData(okapiData);
        return configurationService.retrieveConfiguration(okapiData);
      })
      .thenAccept(rmapiConfiguration -> {

        final HoldingsIQService holdingsService = new HoldingsIQServiceImpl(rmapiConfiguration, vertx);
        final TitlesHoldingsIQService titlesService = new TitlesHoldingsIQServiceImpl(rmapiConfiguration, vertx);
        final ProvidersServiceImpl providersService = new ProvidersServiceImpl(rmapiConfiguration, vertx, holdingsService);
        final PackageServiceImpl packagesService = new PackageServiceImpl(rmapiConfiguration,vertx, new OkapiData(okapiHeaders).getTenant(),
          providersService, titlesService, packageCache);
        final ResourcesServiceImpl resourcesService = new ResourcesServiceImpl(rmapiConfiguration, vertx, providersService, packagesService);
        contextBuilder.holdingsService(holdingsService);
        contextBuilder.providersService(providersService);
        contextBuilder.packagesService(packagesService);
        contextBuilder.resourcesService(resourcesService);
        contextBuilder.titlesService(titlesService);
        providersService.setPackagesService(packagesService);

      })
      .thenCompose(o -> requestAction.apply(contextBuilder.build()))
      .thenAccept(result -> asyncResultHandler.handle(Future.succeededFuture(successHandler.apply(result))))
      .exceptionally(e -> {
        logger.error("Internal Server Error", e);
        errorHandler
          .addInputValidationMapper()
          .addRmApiMapping()
          .addDefaultMapper()
          .handle(asyncResultHandler, e);
        return null;
      });
  }
}
