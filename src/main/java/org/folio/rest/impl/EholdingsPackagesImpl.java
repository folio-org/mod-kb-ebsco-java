package org.folio.rest.impl;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import javax.ws.rs.core.Response;

import org.folio.config.cache.VendorIdCacheKey;
import org.folio.config.cache.VertxCache;
import org.folio.rest.annotations.Validate;
import org.folio.rest.aspect.HandleValidationErrors;
import org.folio.rest.converter.packages.PackageRequestConverter;
import org.folio.rest.exception.InputValidationException;
import org.folio.rest.jaxrs.model.Package;
import org.folio.rest.jaxrs.model.PackageCollection;
import org.folio.rest.jaxrs.model.PackagePostRequest;
import org.folio.rest.jaxrs.model.PackagePutRequest;
import org.folio.rest.jaxrs.model.ResourceCollection;
import org.folio.rest.jaxrs.resource.EholdingsPackages;
import org.folio.rest.model.FilterQuery;
import org.folio.rest.model.PackageId;
import org.folio.rest.model.Sort;
import org.folio.rest.parser.IdParser;
import org.folio.rest.util.ErrorUtil;
import org.folio.rest.util.template.RMAPITemplateContext;
import org.folio.rest.util.template.RMAPITemplateFactory;
import org.folio.rest.validator.CustomPackagePutBodyValidator;
import org.folio.rest.validator.PackageParametersValidator;
import org.folio.rest.validator.PackagePutBodyValidator;
import org.folio.rest.validator.PackagesPostBodyValidator;
import org.folio.rest.validator.TitleParametersValidator;
import org.folio.rmapi.exception.RMAPIResourceNotFoundException;
import org.folio.rmapi.exception.RMAPIServiceException;
import org.folio.rmapi.model.PackagePost;
import org.folio.rmapi.model.PackagePut;
import org.folio.spring.SpringContextUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.converter.Converter;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

public class EholdingsPackagesImpl implements EholdingsPackages {

  private static final String PACKAGE_NOT_FOUND_MESSAGE = "Package not found";

  private static final String INVALID_PACKAGE_TITLE = "Package cannot be deleted";
  private static final String INVALID_PACKAGE_DETAILS = "Invalid package";

  @Autowired
  private PackageRequestConverter converter;
  @Autowired
  private Converter<PackagePostRequest, PackagePost> packagePostRequestConverter;
  @Autowired
  private PackageParametersValidator packageParametersValidator;
  @Autowired
  private PackagePutBodyValidator packagePutBodyValidator;
  @Autowired
  private CustomPackagePutBodyValidator customPackagePutBodyValidator;
  @Autowired
  private PackagesPostBodyValidator packagesPostBodyValidator;
  @Autowired
  private TitleParametersValidator titleParametersValidator;
  @Autowired
  private IdParser idParser;
  @Autowired
  private RMAPITemplateFactory templateFactory;
  @Autowired
  @Qualifier("vendorIdCache")
  private VertxCache<VendorIdCacheKey, Long> vendorIdCache;

  public EholdingsPackagesImpl() {
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
  }

  @Override
  @Validate
  @HandleValidationErrors
  public void getEholdingsPackages(String filterCustom, String q, String filterSelected,
                                   String filterType, String sort, int page, int count, Map<String, String> okapiHeaders,
                                   Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    packageParametersValidator.validate(filterCustom, filterSelected, filterType, sort, q);

    boolean isFilterCustom = Boolean.parseBoolean(filterCustom);
    Sort nameSort = Sort.valueOf(sort.toUpperCase());

    templateFactory.createTemplate(okapiHeaders, asyncResultHandler)
      .requestAction(context ->
      {
        if (isFilterCustom) {
          return getVendorId(context)
            .thenCompose(vendorId ->
              context.getService().retrievePackages(filterSelected, filterType, vendorId, q, page, count, nameSort));
        } else {
          return context.getService().retrievePackages(filterSelected, filterType, null, q, page, count, nameSort);
        }
      })
      .addErrorMapper(RMAPIServiceException.class,
        exception ->
          GetEholdingsPackagesResponse.respond400WithApplicationVndApiJson(
            ErrorUtil.createErrorFromRMAPIResponse(exception)))
      .executeWithResult(PackageCollection.class);
  }

  @Override
  @HandleValidationErrors
  public void postEholdingsPackages(String contentType, PackagePostRequest entity, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    packagesPostBodyValidator.validate(entity);

    PackagePost packagePost = packagePostRequestConverter.convert(entity);

    templateFactory.createTemplate(okapiHeaders, asyncResultHandler)
      .requestAction(context ->
        getVendorId(context)
          .thenCompose(id -> context.getService().postPackage(packagePost, id))
      )
      .addErrorMapper(RMAPIServiceException.class,
        exception ->
          PostEholdingsPackagesResponse.respond400WithApplicationVndApiJson(
            ErrorUtil.createErrorFromRMAPIResponse(exception)))
      .executeWithResult(Package.class);
  }

  @Override
  @HandleValidationErrors
  public void getEholdingsPackagesByPackageId(String packageId, String include, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PackageId parsedPackageId = idParser.parsePackageId(packageId);
    List<String> includedObjects = include != null ? Arrays.asList(include.split(",")) : Collections.emptyList();

    templateFactory.createTemplate(okapiHeaders, asyncResultHandler)
      .requestAction((context ->
        context.getService().retrievePackage(parsedPackageId, includedObjects)
      ))
      .executeWithResult(Package.class);
  }

  @Override
  @HandleValidationErrors
  public void putEholdingsPackagesByPackageId(String packageId, String contentType, PackagePutRequest entity, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PackageId parsedPackageId = idParser.parsePackageId(packageId);

    templateFactory.createTemplate(okapiHeaders, asyncResultHandler)
      .requestAction(context ->
        context.getService().retrievePackage(parsedPackageId)
          .thenCompose(packageData -> {
            PackagePut packagePutBody;
            if (packageData.getIsCustom()) {
              customPackagePutBodyValidator.validate(entity);
              packagePutBody = converter.convertToRMAPICustomPackagePutRequest(entity);
            } else {
              packagePutBodyValidator.validate(entity);
              packagePutBody = converter.convertToRMAPIPackagePutRequest(entity);
            }
            return context.getService().updatePackage(parsedPackageId, packagePutBody);
          })
          .thenCompose(o -> context.getService().retrievePackage(parsedPackageId)))
      .addErrorMapper(InputValidationException.class, exception ->
        EholdingsPackages.PutEholdingsPackagesByPackageIdResponse.respond422WithApplicationVndApiJson(
          ErrorUtil.createError(exception.getMessage(), exception.getMessageDetail())))
      .executeWithResult(Package.class);
  }

  @Override
  @HandleValidationErrors
  public void deleteEholdingsPackagesByPackageId(String packageId, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PackageId parsedPackageId = idParser.parsePackageId(packageId);
    templateFactory.createTemplate(okapiHeaders, asyncResultHandler)
      .requestAction(context ->
        context.getService().retrievePackage(parsedPackageId)
          .thenCompose(packageData -> {
            if (!packageData.getIsCustom()) {
              throw new InputValidationException(INVALID_PACKAGE_TITLE, INVALID_PACKAGE_DETAILS);
            }
            return context.getService().deletePackage(parsedPackageId);
          }))
      .execute();
  }

  @Override
  @Validate
  @HandleValidationErrors
  public void getEholdingsPackagesResourcesByPackageId(String packageId, String sort, String filterSelected, String filterType, String filterName, String filterIsxn, String filterSubject, String filterPublisher, int page, int count, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PackageId parsedPackageId = idParser.parsePackageId(packageId);

    FilterQuery fq = FilterQuery.builder()
      .selected(filterSelected).type(filterType)
      .name(filterName).isxn(filterIsxn).subject(filterSubject)
      .publisher(filterPublisher).build();

    titleParametersValidator.validate(fq, sort, true);

    Sort nameSort = Sort.valueOf(sort.toUpperCase());

    templateFactory.createTemplate(okapiHeaders, asyncResultHandler)
      .requestAction(context ->
        context.getService().retrieveTitles(parsedPackageId.getProviderIdPart(), parsedPackageId.getPackageIdPart(), fq, nameSort, page, count)
      )
      .addErrorMapper(RMAPIResourceNotFoundException.class, exception ->
        GetEholdingsPackagesResourcesByPackageIdResponse.respond404WithApplicationVndApiJson(
          ErrorUtil.createError(PACKAGE_NOT_FOUND_MESSAGE)))
      .executeWithResult(ResourceCollection.class);
  }

  private CompletableFuture<Long> getVendorId(RMAPITemplateContext context) {
    VendorIdCacheKey cacheKey = VendorIdCacheKey.builder()
      .tenant(context.getOkapiData().getTenant())
      .rmapiConfiguration(context.getConfiguration())
      .build();
    Long cachedId = vendorIdCache.getValue(cacheKey);
    if(cachedId != null) {
      return CompletableFuture.completedFuture(cachedId);
    }
    else{
      return context.getService().getVendorId()
        .thenCompose(id -> {
          vendorIdCache.putValue(cacheKey, id);
          return CompletableFuture.completedFuture(id);
        });
    }
  }
}
