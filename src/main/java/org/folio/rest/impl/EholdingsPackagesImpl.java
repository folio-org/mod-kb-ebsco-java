package org.folio.rest.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.apache.commons.lang3.mutable.MutableObject;
import org.folio.config.RMAPIConfigurationServiceCache;
import org.folio.config.RMAPIConfigurationServiceImpl;
import org.folio.config.api.RMAPIConfigurationService;
import org.folio.http.ConfigurationClientProvider;
import org.folio.rest.annotations.Validate;
import org.folio.rest.aspect.HandleValidationErrors;
import org.folio.rest.converter.PackagesConverter;
import org.folio.rest.converter.ResourcesConverter;
import org.folio.rest.exception.InputValidationException;
import org.folio.rest.jaxrs.model.PackagePostRequest;
import org.folio.rest.jaxrs.model.PackagePutRequest;
import org.folio.rest.jaxrs.resource.EholdingsPackages;
import org.folio.rest.model.FilterQuery;
import org.folio.rest.model.OkapiData;
import org.folio.rest.model.PackageId;
import org.folio.rest.model.Sort;
import org.folio.rest.util.ErrorHandler;
import org.folio.rest.util.ErrorUtil;
import org.folio.rest.validator.*;
import org.folio.rmapi.RMAPIService;
import org.folio.rmapi.exception.RMAPIResourceNotFoundException;
import org.folio.rmapi.exception.RMAPIServiceException;
import org.folio.rmapi.model.PackagePost;
import org.folio.rmapi.model.PackagePut;

import javax.validation.ValidationException;
import javax.ws.rs.core.Response;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;

public class EholdingsPackagesImpl implements EholdingsPackages {

  private static final String PACKAGE_ID_REGEX = "([^-]+)-([^-]+)";
  private static final Pattern PACKAGE_ID_PATTERN = Pattern.compile(PACKAGE_ID_REGEX);
  private static final String GET_PACKAGES_ERROR_MESSAGE = "Failed to retrieve packages";
  private static final String PUT_PACKAGE_ERROR_MESSAGE = "Failed to update package";
  private static final String POST_PACKAGES_ERROR_MESSAGE = "Failed to create packages";
  public static final String PACKAGE_ID_MISSING_ERROR = "Package and provider id are required";
  public static final String PACKAGE_ID_INVALID_ERROR = "Package or provider id are invalid";
  private static final String GET_PACKAGE_RESOURCES_ERROR_MESSAGE = "Failed to retrieve package resources";
  private static final String PACKAGE_NOT_FOUND_MESSAGE = "Package not found";

  private static final String INVALID_PACKAGE_TITLE = "Package cannot be deleted";
  private static final String INVALID_PACKAGE_DETAILS = "Invalid package";

  private final Logger logger = LoggerFactory.getLogger(EholdingsPackagesImpl.class);


  private RMAPIConfigurationService configurationService;
  private PackagesConverter converter;
  private HeaderValidator headerValidator;
  private PackageParametersValidator packageParametersValidator;
  private PackagePutBodyValidator packagePutBodyValidator;
  private CustomPackagePutBodyValidator customPackagePutBodyValidator;
  private PackagesPostBodyValidator packagesPostBodyValidator;
  private TitleParametersValidator titleParametersValidator;
  private ResourcesConverter resourceConverter;


  public EholdingsPackagesImpl() {
    this(
      new RMAPIConfigurationServiceCache(
        new RMAPIConfigurationServiceImpl(new ConfigurationClientProvider())),
      new HeaderValidator(),
      new PackageParametersValidator(),
      new PackagePutBodyValidator(),
      new CustomPackagePutBodyValidator(),
      new PackagesPostBodyValidator(),
      new PackagesConverter(),
      new TitleParametersValidator(),
      new ResourcesConverter());
  }

  public EholdingsPackagesImpl(RMAPIConfigurationService configurationService,
                               HeaderValidator headerValidator,
                               PackageParametersValidator packageParametersValidator,
                               PackagePutBodyValidator packagePutBodyValidator,
                               CustomPackagePutBodyValidator customPackagePutBodyValidator,
                               PackagesPostBodyValidator packagesPostBodyValidator,
                               PackagesConverter converter,
                               TitleParametersValidator titleParametersValidator,
                               ResourcesConverter resourceConverter) {
    this.configurationService = configurationService;
    this.headerValidator = headerValidator;
    this.packageParametersValidator = packageParametersValidator;
    this.packagesPostBodyValidator = packagesPostBodyValidator;
    this.converter = converter;
    this.packagePutBodyValidator = packagePutBodyValidator;
    this.customPackagePutBodyValidator = customPackagePutBodyValidator;
    this.titleParametersValidator = titleParametersValidator;
    this.resourceConverter = resourceConverter;
  }

  @Override
  @Validate
  @HandleValidationErrors
  public void getEholdingsPackages(String filterCustom, String q, String filterSelected,
                                   String filterType, String sort, int page, int count, Map<String, String> okapiHeaders,
                                   Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {

    headerValidator.validate(okapiHeaders);
    packageParametersValidator.validate(filterCustom, filterSelected, filterType, sort);
    if ("".equals(q)) {
      throw new ValidationException("Search parameter cannot be empty");
    }

    boolean isFilterCustom = Boolean.parseBoolean(filterCustom);
    Sort nameSort = Sort.valueOf(sort.toUpperCase());
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
        new ErrorHandler()
          .add(RMAPIServiceException.class,
            exception ->
              GetEholdingsPackagesResponse.respond400WithApplicationVndApiJson(
                ErrorUtil.createErrorFromRMAPIResponse(exception)))
          .addDefaultMapper()
          .handle(asyncResultHandler, e);
        return null;
      });
  }

  @Override
  @HandleValidationErrors
  public void postEholdingsPackages(String contentType, PackagePostRequest entity, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {

    headerValidator.validate(okapiHeaders);
    packagesPostBodyValidator.validate(entity);

    PackagePost packagePost = converter.convertToPackage(entity);

    MutableObject<RMAPIService> service = new MutableObject<>();
    CompletableFuture.completedFuture(null)
      .thenCompose(o -> configurationService.retrieveConfiguration(new OkapiData(okapiHeaders)))
      .thenAccept(rmapiConfiguration ->
        service.setValue(new RMAPIService(rmapiConfiguration.getCustomerId(),
          rmapiConfiguration.getAPIKey(), rmapiConfiguration.getUrl(), vertxContext.owner())))
      .thenCompose(o -> service.getValue().postPackage(packagePost))
      .thenAccept(packageCreated ->
        asyncResultHandler.handle(Future.succeededFuture(PostEholdingsPackagesResponse
          .respond200WithApplicationVndApiJson(converter.convert(packageCreated)))))
      .exceptionally(e -> {
        logger.error(POST_PACKAGES_ERROR_MESSAGE, e);
        new ErrorHandler()
          .add(RMAPIServiceException.class,
            exception ->
              PostEholdingsPackagesResponse.respond400WithApplicationVndApiJson(
                ErrorUtil.createErrorFromRMAPIResponse(exception)))
          .addDefaultMapper()
          .handle(asyncResultHandler, e);
        return null;
      });

  }

  @Override
  @HandleValidationErrors
  public void getEholdingsPackagesByPackageId(String packageId, String include, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PackageId parsedPackageId = parsePackageId(packageId);
    headerValidator.validate(okapiHeaders);
    CompletableFuture.completedFuture(null)
      .thenCompose(okapiData -> configurationService.retrieveConfiguration(new OkapiData(okapiHeaders)))
      .thenCompose(rmapiConfiguration -> {
        RMAPIService rmapiService = new RMAPIService(rmapiConfiguration.getCustomerId(), rmapiConfiguration.getAPIKey(),
          rmapiConfiguration.getUrl(), vertxContext.owner());
        return rmapiService.retrievePackage(parsedPackageId);
      })
      .thenAccept(packageData ->
        asyncResultHandler.handle(Future.succeededFuture(
          GetEholdingsPackagesByPackageIdResponse.respond200WithApplicationVndApiJson(converter.convert(packageData)))))
      .exceptionally(e -> {
        logger.error(INTERNAL_SERVER_ERROR, e);
        handleError(asyncResultHandler, e);
        return null;
      });
  }

  @Override
  @HandleValidationErrors
  public void putEholdingsPackagesByPackageId(String packageId, String contentType, PackagePutRequest entity, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    headerValidator.validate(okapiHeaders);
    PackageId parsedPackageId = parsePackageId(packageId);
    MutableObject<RMAPIService> rmapiService = new MutableObject<>();
    CompletableFuture.completedFuture(null)
      .thenCompose(o -> configurationService.retrieveConfiguration(new OkapiData(okapiHeaders)))
      .thenCompose(rmapiConfiguration -> {
        rmapiService.setValue(new RMAPIService(rmapiConfiguration.getCustomerId(),
          rmapiConfiguration.getAPIKey(), rmapiConfiguration.getUrl(), vertxContext.owner()));
        return rmapiService.getValue().retrievePackage(parsedPackageId);
      })
      .thenCompose(packageData -> {
        PackagePut packagePutBody;
        if (packageData.getIsCustom()) {
          customPackagePutBodyValidator.validate(entity);
          packagePutBody = converter.convertToRMAPICustomPackagePutRequest(entity);
        } else {
          packagePutBodyValidator.validate(entity);
          packagePutBody = converter.convertToRMAPIPackagePutRequest(entity);
        }
        return rmapiService.getValue().updatePackage(parsedPackageId, packagePutBody);
      })
      .thenCompose(o -> rmapiService.getValue().retrievePackage(parsedPackageId))
      .thenAccept(packageData ->
        asyncResultHandler.handle(Future.succeededFuture(EholdingsPackages.PutEholdingsPackagesByPackageIdResponse
          .respond200WithApplicationVndApiJson(converter.convert(packageData)))))
      .exceptionally(e -> {
        logger.error(PUT_PACKAGE_ERROR_MESSAGE, e);
        new ErrorHandler()
          .add(InputValidationException.class, exception ->
            EholdingsPackages.PutEholdingsPackagesByPackageIdResponse.respond422WithApplicationVndApiJson(
              ErrorUtil.createError(exception.getMessage(), exception.getMessageDetail())))
          .addRmApiMapper()
          .addDefaultMapper()
          .handle(asyncResultHandler, e);
        return null;
      });

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
      .thenCompose(packageData -> {
        if (!packageData.getIsCustom()) {
          throw new InputValidationException(INVALID_PACKAGE_TITLE, INVALID_PACKAGE_DETAILS);
        }
        return rmapiService.getValue().deletePackage(parsedPackageId);
      })
      .thenAccept(o -> asyncResultHandler.handle(Future.succeededFuture(
        EholdingsPackages.DeleteEholdingsPackagesByPackageIdResponse.respond204())))
      .exceptionally(e -> {
        logger.error(INTERNAL_SERVER_ERROR, e);
        handleError(asyncResultHandler, e);
        return null;
      });

  }

  @Override
  public void getEholdingsPackagesResourcesByPackageId(String packageId, String sort, String filterSelected, String filterType, String filterName, String filterIsxn, String filterSubject, String filterPublisher,  int page,   int count, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PackageId parsedPackageId = parsePackageId(packageId);
    headerValidator.validate(okapiHeaders);

    FilterQuery fq = FilterQuery.builder()
        .selected(filterSelected).type(filterType)
        .name(filterName).isxn(filterIsxn).subject(filterSubject)
        .publisher(filterPublisher).build();

    titleParametersValidator.validate(fq, sort, true);

    Sort nameSort = Sort.valueOf(sort.toUpperCase());

    CompletableFuture.completedFuture(null)
      .thenCompose(okapiData -> configurationService.retrieveConfiguration(new OkapiData(okapiHeaders)))
      .thenCompose(rmapiConfiguration -> {
        RMAPIService rmapiService = new RMAPIService(rmapiConfiguration.getCustomerId(), rmapiConfiguration.getAPIKey(),
          rmapiConfiguration.getUrl(), vertxContext.owner());
        return rmapiService.retrieveResources(parsedPackageId.getProviderIdPart(),parsedPackageId.getPackageIdPart(), fq, nameSort, page, count);
      })
      .thenAccept(resourceList ->
        asyncResultHandler.handle(Future.succeededFuture(
            GetEholdingsPackagesResourcesByPackageIdResponse.respond200WithApplicationVndApiJson(resourceConverter.convertFromRMAPIResourceList(resourceList)))))
      .exceptionally(e -> {
        logger.error(GET_PACKAGE_RESOURCES_ERROR_MESSAGE, e);
        new ErrorHandler()
          .add(RMAPIResourceNotFoundException.class, exception ->
            GetEholdingsPackagesResourcesByPackageIdResponse.respond404WithApplicationVndApiJson(
              ErrorUtil.createError(PACKAGE_NOT_FOUND_MESSAGE)))
          .add(RMAPIServiceException.class,
          exception ->
            GetEholdingsPackagesResourcesByPackageIdResponse.respond400WithApplicationVndApiJson(
              ErrorUtil.createErrorFromRMAPIResponse(exception)))
          .handle(asyncResultHandler, e);
        return null;
      });
  }

  private void handleError(Handler<AsyncResult<Response>> asyncResultHandler, Throwable e) {
    new ErrorHandler()
      .addRmApiMapper()
      .addInputValidationMapper()
      .addDefaultMapper()
      .handle(asyncResultHandler, e);
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
        throw new ValidationException(PACKAGE_ID_MISSING_ERROR);
      }

      return new PackageId(providerId, packageId);
    } catch (NumberFormatException e) {
      throw new ValidationException(PACKAGE_ID_INVALID_ERROR);
    }
  }

}
