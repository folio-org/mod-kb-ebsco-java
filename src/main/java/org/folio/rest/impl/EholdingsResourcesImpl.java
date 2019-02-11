package org.folio.rest.impl;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.ws.rs.core.Response;

import org.folio.rest.aspect.HandleValidationErrors;
import org.folio.rest.converter.resources.ResourceRequestConverter;
import org.folio.rest.exception.InputValidationException;
import org.folio.rest.jaxrs.model.Resource;
import org.folio.rest.jaxrs.model.ResourcePostDataAttributes;
import org.folio.rest.jaxrs.model.ResourcePostRequest;
import org.folio.rest.jaxrs.model.ResourcePutRequest;
import org.folio.rest.jaxrs.resource.EholdingsResources;
import org.folio.rest.model.FilterQuery;
import org.folio.rest.model.PackageId;
import org.folio.rest.model.ResourceId;
import org.folio.rest.model.Sort;
import org.folio.rest.parser.IdParser;
import org.folio.rest.util.ErrorUtil;
import org.folio.rest.util.template.RMAPITemplateFactory;
import org.folio.rest.validator.ResourcePostValidator;
import org.folio.rest.validator.ResourcePutBodyValidator;
import org.folio.rmapi.RMAPIService;
import org.folio.rmapi.exception.RMAPIResourceNotFoundException;
import org.folio.rmapi.model.CustomerResources;
import org.folio.rmapi.model.PackageByIdData;
import org.folio.rmapi.model.ResourcePut;
import org.folio.rmapi.model.ResourceSelectedPayload;
import org.folio.rmapi.model.Title;
import org.folio.rmapi.result.ObjectsForPostResourceResult;
import org.folio.rmapi.result.ResourceResult;
import org.folio.spring.SpringContextUtil;
import org.folio.tag.RecordType;
import org.folio.tag.repository.TagRepository;
import org.springframework.beans.factory.annotation.Autowired;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;


public class EholdingsResourcesImpl implements EholdingsResources {
  private static final String RESOURCE_NOT_FOUND_MESSAGE = "Resource not found";
  private static final int MAX_TITLE_COUNT = 100;
  private static final String RESOURCE_CANNOT_BE_DELETED_TITLE = "Resource cannot be deleted";
  private static final String RESOURCE_CANNOT_BE_DELETED_DETAIL = "Resource is not in a custom package";

  @Autowired
  private ResourceRequestConverter converter;
  @Autowired
  private IdParser idParser;
  @Autowired
  private ResourcePostValidator postValidator;
  @Autowired
  private ResourcePutBodyValidator resourcePutBodyValidator;
  @Autowired
  private RMAPITemplateFactory templateFactory;
  @Autowired
  private TagRepository tagRepository;

  public EholdingsResourcesImpl() {
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
  }

  @Override
  @HandleValidationErrors
  public void postEholdingsResources(String contentType, ResourcePostRequest entity, Map<String, String> okapiHeaders,
                                     Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    postValidator.validate(entity);

    ResourcePostDataAttributes attributes = entity.getData().getAttributes();

    long titleId = idParser.parseTitleId(attributes.getTitleId());
    PackageId packageId = idParser.parsePackageId(attributes.getPackageId());

    templateFactory.createTemplate(okapiHeaders, asyncResultHandler)
      .requestAction(context -> (CompletableFuture<?>) getObjectsForPostResource(titleId, packageId, context.getService())
        .thenCompose(result -> {
          Title title = result.getTitle();
          postValidator.validateRelatedObjects(result.getPackageData(), title, result.getTitles());
          ResourceSelectedPayload postRequest =
            new ResourceSelectedPayload(true, title.getTitleName(), title.getPubType(), attributes.getUrl());
          ResourceId resourceId = new ResourceId(packageId.getProviderIdPart(), packageId.getPackageIdPart(), titleId);
          return context.getService().postResource(postRequest, resourceId);
        })
      )
      .addErrorMapper(InputValidationException.class, exception ->
        PostEholdingsResourcesResponse.respond422WithApplicationVndApiJson(
          ErrorUtil.createError(exception.getMessage(), exception.getMessageDetail())))
      .executeWithResult(Resource.class);
  }

  @Override
  @HandleValidationErrors
  public void getEholdingsResourcesByResourceId(String resourceId, String include, Map<String, String> okapiHeaders,
                                                Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    ResourceId parsedResourceId = idParser.parseResourceId(resourceId);
    List<String> includedObjects = include != null ? Arrays.asList(include.split(",")) : Collections.emptyList();

    templateFactory.createTemplate(okapiHeaders, asyncResultHandler)
      .requestAction(context ->
        context.getService().retrieveResource(parsedResourceId, includedObjects)
          .thenCompose(result ->
            loadTags(result, context.getOkapiData().getTenant())
          )
      )
      .addErrorMapper(RMAPIResourceNotFoundException.class, exception ->
        GetEholdingsResourcesByResourceIdResponse.respond404WithApplicationVndApiJson(
          ErrorUtil.createError(RESOURCE_NOT_FOUND_MESSAGE)))
      .executeWithResult(Resource.class);
  }

  @Override
  @HandleValidationErrors
  public void putEholdingsResourcesByResourceId(String resourceId, String contentType, ResourcePutRequest entity,
                                                Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    ResourceId parsedResourceId = idParser.parseResourceId(resourceId);
    templateFactory.createTemplate(okapiHeaders, asyncResultHandler)
      .requestAction(context ->
        context.getService().retrieveResource(parsedResourceId, Collections.emptyList())
          .thenCompose(resourceData -> {
            ResourcePut resourcePutBody;
            boolean isTitleCustom = resourceData.getTitle().getIsTitleCustom();
            resourcePutBodyValidator.validate(entity, isTitleCustom);
            if (isTitleCustom) {
              resourcePutBody = converter.convertToRMAPICustomResourcePutRequest(entity, resourceData);
            } else {
              resourcePutBody = converter.convertToRMAPIResourcePutRequest(entity, resourceData);
            }
            return context.getService().updateResource(parsedResourceId, resourcePutBody);
          })
          .thenCompose(o -> context.getService().retrieveResource(parsedResourceId, Collections.emptyList()))
      )
      .addErrorMapper(InputValidationException.class, exception ->
        EholdingsResources.PutEholdingsResourcesByResourceIdResponse.respond422WithApplicationVndApiJson(
          ErrorUtil.createError(exception.getMessage(), exception.getMessageDetail())))
      .addErrorMapper(RMAPIResourceNotFoundException.class, exception ->
        EholdingsResources.PutEholdingsResourcesByResourceIdResponse.respond404WithApplicationVndApiJson(
          ErrorUtil.createError(RESOURCE_NOT_FOUND_MESSAGE)))
      .executeWithResult(Resource.class);
  }

  @Override
  @HandleValidationErrors
  public void deleteEholdingsResourcesByResourceId(String resourceId, Map<String, String> okapiHeaders,
                                                   Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    ResourceId parsedResourceId = idParser.parseResourceId(resourceId);

    templateFactory.createTemplate(okapiHeaders, asyncResultHandler)
      .requestAction(context ->
        context.getService().retrieveResource(parsedResourceId, Collections.emptyList())
          .thenCompose(resourceData -> {
            if (!resourceData.getTitle().getCustomerResourcesList().get(0).getIsPackageCustom()) {
              throw new InputValidationException(RESOURCE_CANNOT_BE_DELETED_TITLE, RESOURCE_CANNOT_BE_DELETED_DETAIL);
            }
            return context.getService().deleteResource(parsedResourceId);
          })
      )
      .execute();
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

  private CompletableFuture<ResourceResult> loadTags(ResourceResult result, String tenant) {
    CustomerResources resource = result.getTitle().getCustomerResourcesList().get(0);
    String resourceId = resource.getVendorId() + "-" + resource.getPackageId() + "-" + resource.getTitleId();
    return tagRepository.getTags(tenant, resourceId, RecordType.RESOURCE)
      .thenApply(tag -> {
        result.setTags(tag);
        return result;
      });
  }
}
