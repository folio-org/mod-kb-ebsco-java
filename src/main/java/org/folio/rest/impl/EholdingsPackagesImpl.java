package org.folio.rest.impl;

import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.validation.ValidationException;
import javax.ws.rs.core.Response;
import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.http.HttpStatus;
import org.folio.config.RMAPIConfigurationServiceCache;
import org.folio.config.RMAPIConfigurationServiceImpl;
import org.folio.config.api.RMAPIConfigurationService;
import org.folio.http.ConfigurationClientProvider;
import org.folio.rest.annotations.Validate;
import org.folio.rest.aspect.HandleValidationErrors;
import org.folio.rest.converter.PackagesConverter;
import org.folio.rest.exception.InputValidationException;
import org.folio.rest.jaxrs.model.PackagePostRequest;
import org.folio.rest.jaxrs.model.PackagePutRequest;
import org.folio.rest.jaxrs.resource.EholdingsPackages;
import org.folio.rest.model.OkapiData;
import org.folio.rest.model.PackageId;
import org.folio.rest.model.Sort;
import org.folio.rest.util.ErrorUtil;
import org.folio.rest.validator.HeaderValidator;
import org.folio.rest.validator.PackageParametersValidator;
import org.folio.rmapi.RMAPIService;
import org.folio.rmapi.exception.RMAPIServiceException;

public class EholdingsPackagesImpl implements EholdingsPackages {

  private static final String PACKAGE_ID_REGEX = "(\\d+)-(\\d+)";
  private static final Pattern PACKAGE_ID_PATTERN = Pattern.compile(PACKAGE_ID_REGEX);
  private static final String GET_PACKAGES_ERROR_MESSAGE = "Failed to retrieve packages";
  public static final String PACKAGE_ID_INVALID_ERROR = "Package and provider id are required";

  private static final String INVALID_PACKAGE_TITLE = "Invalid package";
  private static final String INVALID_PACKAGE_DETAILS = "Package cannot be deleted";

  private static final String CONTENT_TYPE_HEADER = "Content-Type";
  private static final String CONTENT_TYPE_VALUE = "application/vnd.api+json";

  private final Logger logger = LoggerFactory.getLogger(EholdingsPackagesImpl.class);


  private RMAPIConfigurationService configurationService;
  private PackagesConverter converter;
  private HeaderValidator headerValidator;
  private PackageParametersValidator packageParametersValidator;


  public EholdingsPackagesImpl() {
    this(
      new RMAPIConfigurationServiceCache(
        new RMAPIConfigurationServiceImpl(new ConfigurationClientProvider())),
      new HeaderValidator(),
      new PackageParametersValidator(),
      new PackagesConverter());
  }

  public EholdingsPackagesImpl(RMAPIConfigurationService configurationService,
                               HeaderValidator headerValidator,
                               PackageParametersValidator packageParametersValidator,
                               PackagesConverter converter) {
    this.configurationService = configurationService;
    this.headerValidator = headerValidator;
    this.packageParametersValidator = packageParametersValidator;
    this.converter = converter;
  }

  @Override
  @Validate
  @HandleValidationErrors
  public void getEholdingsPackages(String filterCustom, String q, String filterSelected,
    String filterType, String sort, int page, int count, Map<String, String> okapiHeaders,
    Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {

    headerValidator.validate(okapiHeaders);
    packageParametersValidator.validate(filterCustom, filterSelected, filterType, sort);

    boolean isFilterCustom = Boolean.parseBoolean(filterCustom);
    Sort nameSort =  Sort.valueOf(sort.toUpperCase());
    MutableObject<RMAPIService> service = new MutableObject<>();
    CompletableFuture.completedFuture(null)
      .thenCompose(o -> configurationService.retrieveConfiguration(new OkapiData(okapiHeaders)))
      .thenAccept(rmapiConfiguration ->
        service.setValue(new RMAPIService(rmapiConfiguration.getCustomerId(),
          rmapiConfiguration.getAPIKey(), rmapiConfiguration.getUrl(), vertxContext.owner())))
      .thenCompose(o -> service.getValue().getVendors(isFilterCustom))
      .thenCompose(vendors ->
        service.getValue().retrievePackages(filterSelected, filterType,
          service.getValue().getFirstProviderElement(vendors), q, page, count, nameSort))
      .thenAccept(packages ->
        asyncResultHandler.handle(Future.succeededFuture(GetEholdingsPackagesResponse
          .respond200WithApplicationVndApiJson(converter.convert(packages)))))
      .exceptionally(e -> {
        logger.error(GET_PACKAGES_ERROR_MESSAGE, e);
        if (e.getCause() instanceof RMAPIServiceException) {
          RMAPIServiceException rmApiException = (RMAPIServiceException) e.getCause();
          asyncResultHandler.handle(Future.succeededFuture(
            GetEholdingsPackagesResponse.respond400WithApplicationVndApiJson(
              ErrorUtil.createErrorFromRMAPIResponse(rmApiException))));
        } else {
          asyncResultHandler.handle(Future.succeededFuture(GetEholdingsPackagesResponse
            .status(HttpStatus.SC_INTERNAL_SERVER_ERROR)
            .header(CONTENT_TYPE_HEADER, CONTENT_TYPE_VALUE)
            .entity(ErrorUtil.createError(e.getCause().getMessage()))
            .build()));
        }
        return null;
      });
  }

  @Override
  public void postEholdingsPackages(String contentType, PackagePostRequest entity, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    asyncResultHandler.handle(Future.succeededFuture(Response.status(Response.Status.NOT_IMPLEMENTED).build()));
  }

  @Override
  public void getEholdingsPackagesByPackageId(String packageId, String include, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    asyncResultHandler.handle(Future.succeededFuture(Response.status(Response.Status.NOT_IMPLEMENTED).build()));
  }

  @Override
  public void putEholdingsPackagesByPackageId(String packageId, String contentType, PackagePutRequest entity, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    asyncResultHandler.handle(Future.succeededFuture(Response.status(Response.Status.NOT_IMPLEMENTED).build()));
  }

  @Override
  @HandleValidationErrors
  public void deleteEholdingsPackagesByPackageId(String packageId, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    headerValidator.validate(okapiHeaders);
    PackageId parsedPackageId = parsePackageId(packageId);
    MutableObject<RMAPIService> rmapiService = new MutableObject<>();
    CompletableFuture.completedFuture(null)
      .thenCompose(okapiData -> configurationService.retrieveConfiguration(new OkapiData(okapiHeaders)))
      .thenCompose(rmapiConfiguration -> {
        rmapiService.setValue(new RMAPIService(rmapiConfiguration.getCustomerId(), rmapiConfiguration.getAPIKey(),
          rmapiConfiguration.getUrl(), vertxContext.owner()));
        return rmapiService.getValue().retrievePackage(parsedPackageId);
      })
      .thenCompose( packageData -> {
        if(!packageData.getCustom()){
          throw new InputValidationException(INVALID_PACKAGE_TITLE, INVALID_PACKAGE_DETAILS);
        }
        return rmapiService.getValue().deletePackage(parsedPackageId);
      })
      .thenAccept(isValid -> asyncResultHandler.handle(Future.succeededFuture(
        EholdingsPackages.DeleteEholdingsPackagesByPackageIdResponse.respond204())))
      .exceptionally(e -> {
        logger.error(INTERNAL_SERVER_ERROR, e);
        if(e.getCause() instanceof RMAPIServiceException){
          RMAPIServiceException rmApiException = (RMAPIServiceException)e.getCause();
          asyncResultHandler.handle(Future.succeededFuture(Response
            .status(rmApiException.getRMAPICode())
            .header(CONTENT_TYPE_HEADER, CONTENT_TYPE_VALUE)
            .entity(ErrorUtil.createErrorFromRMAPIResponse(rmApiException))
            .build()));
        }
        else if(e.getCause() instanceof InputValidationException){
          InputValidationException inputValidationException = (InputValidationException) e.getCause();
          asyncResultHandler.handle(Future.succeededFuture(
            EholdingsPackages.DeleteEholdingsPackagesByPackageIdResponse.respond400WithApplicationVndApiJson(
              ErrorUtil.createError(inputValidationException.getMessage(), inputValidationException.getMessageDetail()))));
        }
        else {
          asyncResultHandler.handle(Future.succeededFuture(Response
            .status(HttpStatus.SC_INTERNAL_SERVER_ERROR).header(CONTENT_TYPE_HEADER, CONTENT_TYPE_VALUE)
            .entity(ErrorUtil.createError(e.getCause().getMessage())).build()));
        }
        return null;
      });

  }

  @Override
  public void getEholdingsPackagesResourcesByPackageId(String packageId, String sort, String filterSelected, String filterType, String filterName, String filterIsxn, String filterSubject, String filterPublisher, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    asyncResultHandler.handle(Future.succeededFuture(Response.status(Response.Status.NOT_IMPLEMENTED).build()));
  }

  private PackageId parsePackageId(String packageIdString) {
    try {
      long providerId;
      long packageId;
      Matcher matcher = PACKAGE_ID_PATTERN.matcher(packageIdString);

      if (matcher.find() && matcher.hitEnd()) {
        providerId = Long.parseLong(matcher.group(1));
        packageId = Long.parseLong(matcher.group(2));
      } else {
        throw new ValidationException(PACKAGE_ID_INVALID_ERROR);
      }

      return new PackageId(providerId, packageId);
    } catch (NumberFormatException e) {
      throw new ValidationException(PACKAGE_ID_INVALID_ERROR);
    }
  }

}
