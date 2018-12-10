package org.folio.rest.impl;

import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.ws.rs.core.Response;

import org.apache.commons.lang3.mutable.MutableObject;
import org.folio.config.RMAPIConfigurationServiceCache;
import org.folio.config.RMAPIConfigurationServiceImpl;
import org.folio.config.api.RMAPIConfigurationService;
import org.folio.http.ConfigurationClientProvider;
import org.folio.rest.aspect.HandleValidationErrors;
import org.folio.rest.converter.ResourcesConverter;
import org.folio.rest.exception.InputValidationException;
import org.folio.rest.jaxrs.model.ResourcePostDataAttributes;
import org.folio.rest.jaxrs.model.ResourcePostRequest;
import org.folio.rest.jaxrs.model.ResourcePutRequest;
import org.folio.rest.jaxrs.resource.EholdingsResources;
import org.folio.rest.model.FilterQuery;
import org.folio.rest.model.OkapiData;
import org.folio.rest.model.PackageId;
import org.folio.rest.model.ResourceId;
import org.folio.rest.model.Sort;
import org.folio.rest.parser.IdParser;
import org.folio.rest.util.ErrorHandler;
import org.folio.rest.util.ErrorUtil;
import org.folio.rest.validator.HeaderValidator;
import org.folio.rest.validator.ResourcePostValidator;
import org.folio.rmapi.RMAPIService;
import org.folio.rmapi.exception.RMAPIResourceNotFoundException;
import org.folio.rmapi.model.PackageByIdData;
import org.folio.rmapi.model.ResourceSelectedPayload;
import org.folio.rmapi.model.Title;
import org.folio.rmapi.result.ObjectsForPostResourceResult;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;


public class EholdingsResourcesImpl implements EholdingsResources{
  private static final String RESOURCE_NOT_FOUND_MESSAGE = "Resource not found";
  private static final int MAX_TITLE_COUNT = 100;

  private final Logger logger = LoggerFactory.getLogger(EholdingsResourcesImpl.class);

  private RMAPIConfigurationService configurationService;
  private HeaderValidator headerValidator;
  private ResourcesConverter converter;
  private IdParser idParser;
  private ResourcePostValidator postValidator;

  public EholdingsResourcesImpl() {
    this(
      new RMAPIConfigurationServiceCache(
        new RMAPIConfigurationServiceImpl(new ConfigurationClientProvider())),
      new HeaderValidator(),
      new ResourcePostValidator(),
      new ResourcesConverter(),
      new IdParser());
  }

  public EholdingsResourcesImpl(RMAPIConfigurationService configurationService,
      HeaderValidator headerValidator,
      ResourcePostValidator postValidator,
      ResourcesConverter converter,
      IdParser idParser) {
    this.configurationService = configurationService;
    this.headerValidator = headerValidator;
    this.postValidator = postValidator;
    this.converter = converter;
    this.idParser = idParser;
}

  @Override
  @HandleValidationErrors
  public void postEholdingsResources(String contentType, ResourcePostRequest entity, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    headerValidator.validate(okapiHeaders);
    postValidator.validate(entity);

    ResourcePostDataAttributes attributes = entity.getData().getAttributes();

    long titleId = idParser.parseTitleId(attributes.getTitleId());
    PackageId packageId = idParser.parsePackageId(attributes.getPackageId());

    MutableObject<RMAPIService> rmapiService = new MutableObject<>();
    CompletableFuture.completedFuture(null)
      .thenCompose(o -> configurationService.retrieveConfiguration(new OkapiData(okapiHeaders)))
      .thenCompose(rmapiConfiguration -> {
        rmapiService.setValue(new RMAPIService(rmapiConfiguration.getCustomerId(), rmapiConfiguration.getAPIKey(),
          rmapiConfiguration.getUrl(), vertxContext.owner()));
        return getObjectsForPostResource(titleId, packageId, rmapiService.getValue());
      })
      .thenCompose(result -> {
        Title title = result.getTitle();
        postValidator.validateRelatedObjects(result.getPackageData(), title, result.getTitles());
        ResourceSelectedPayload postRequest =
          new ResourceSelectedPayload(true, title.getTitleName(), title.getPubType(), attributes.getUrl());
        ResourceId resourceId = new ResourceId(packageId.getProviderIdPart(), packageId.getPackageIdPart(), titleId);
        return rmapiService.getValue().postResource(postRequest, resourceId);
      })
      .thenAccept(resource ->
        asyncResultHandler.handle(Future.succeededFuture(GetEholdingsResourcesByResourceIdResponse
          .respond200WithApplicationVndApiJson(converter.convertFromRMAPIResource(resource.getTitle(), resource.getVendor(), resource.getPackageData(),
            false).get(0)))))
      .exceptionally(e -> {
        logger.error(INTERNAL_SERVER_ERROR, e);
        new ErrorHandler()
          .add(InputValidationException.class, exception ->
            PostEholdingsResourcesResponse.respond422WithApplicationVndApiJson(
              ErrorUtil.createError(exception.getMessage(), exception.getMessageDetail())))
          .addRmApiMapper()
          .addDefaultMapper()
          .handle(asyncResultHandler, e);
        return null;
      });
  }

  @Override
  @HandleValidationErrors
  public void getEholdingsResourcesByResourceId(String resourceId, String include, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    ResourceId parsedResourceId = idParser.parseResourceId(resourceId);
    headerValidator.validate(okapiHeaders);

    List<String> includedObjects = include != null ? Arrays.asList(include.split(",")) : Collections.emptyList();
    boolean includeTitle = includedObjects.contains("title");

    CompletableFuture.completedFuture(null)
      .thenCompose(o -> configurationService.retrieveConfiguration(new OkapiData(okapiHeaders)))
      .thenCompose(rmapiConfiguration -> {
        RMAPIService rmapiService = new RMAPIService(rmapiConfiguration.getCustomerId(), rmapiConfiguration.getAPIKey(),
          rmapiConfiguration.getUrl(), vertxContext.owner());
        return rmapiService.retrieveResource(parsedResourceId, includedObjects);
    })
    .thenAccept(title ->
      asyncResultHandler.handle(Future.succeededFuture(GetEholdingsResourcesByResourceIdResponse
        .respond200WithApplicationVndApiJson(converter.convertFromRMAPIResource(title.getTitle(), title.getVendor(), title.getPackageData(), includeTitle).get(0)))))
    .exceptionally(e -> {
      logger.error(INTERNAL_SERVER_ERROR, e);
      new ErrorHandler()
        .add(RMAPIResourceNotFoundException.class, exception ->
        GetEholdingsResourcesByResourceIdResponse.respond404WithApplicationVndApiJson(
            ErrorUtil.createError(RESOURCE_NOT_FOUND_MESSAGE)))
        .addRmApiMapper()
        .addInputValidationMapper()
        .addDefaultMapper()
        .handle(asyncResultHandler, e);
      return null;
    });
  }

  @Override
  public void putEholdingsResourcesByResourceId(String resourceId, String contentType, ResourcePutRequest entity,
      Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    asyncResultHandler.handle(Future.succeededFuture(PutEholdingsResourcesByResourceIdResponse.status(Response.Status.NOT_IMPLEMENTED).build()));
  }

  @Override
  public void deleteEholdingsResourcesByResourceId(String resourceId, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    asyncResultHandler.handle(Future.succeededFuture(DeleteEholdingsResourcesByResourceIdResponse.status(Response.Status.NOT_IMPLEMENTED).build()));
  }

  private CompletionStage<ObjectsForPostResourceResult> getObjectsForPostResource(Long titleId, PackageId packageId, RMAPIService rmapiService) {
    CompletableFuture<Title> titleFuture = rmapiService.retrieveTitle(titleId);
    CompletableFuture<PackageByIdData> packageFuture = rmapiService.retrievePackage(packageId);
    return CompletableFuture.allOf(titleFuture, packageFuture)
      .thenCompose(o -> {
        FilterQuery filterByName = FilterQuery.builder()
          .name(titleFuture.join().getTitleName())
          .build();
        return rmapiService.retrieveTitles(packageId.getProviderIdPart(), packageId.getPackageIdPart(),
          filterByName, Sort.RELEVANCE, 1, MAX_TITLE_COUNT);
      })
      .thenCompose(titles -> CompletableFuture.completedFuture(
        new ObjectsForPostResourceResult(titleFuture.join(), packageFuture.join(), titles)));
  }
}
