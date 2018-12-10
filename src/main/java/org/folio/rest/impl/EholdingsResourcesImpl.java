package org.folio.rest.impl;

import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import javax.ws.rs.core.Response;

import org.folio.config.api.RMAPIConfigurationService;
import org.folio.rest.aspect.HandleValidationErrors;
import org.folio.rest.converter.ResourcesConverter;
import org.folio.rest.jaxrs.model.ResourcePostRequest;
import org.folio.rest.jaxrs.model.ResourcePutRequest;
import org.folio.rest.jaxrs.resource.EholdingsResources;
import org.folio.rest.model.OkapiData;
import org.folio.rest.model.ResourceId;
import org.folio.rest.parser.IdParser;
import org.folio.rest.util.ErrorHandler;
import org.folio.rest.util.ErrorUtil;
import org.folio.rest.validator.HeaderValidator;
import org.folio.rmapi.RMAPIService;
import org.folio.rmapi.exception.RMAPIResourceNotFoundException;
import org.folio.spring.SpringContextUtil;
import org.springframework.beans.factory.annotation.Autowired;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;


public class EholdingsResourcesImpl implements EholdingsResources{
  private static final String RESOURCE_NOT_FOUND_MESSAGE = "Resource not found";

  private final Logger logger = LoggerFactory.getLogger(EholdingsResourcesImpl.class);

  @Autowired
  private RMAPIConfigurationService configurationService;
  @Autowired
  private HeaderValidator headerValidator;
  @Autowired
  private ResourcesConverter converter;
  @Autowired
  private IdParser idParser;

  @SuppressWarnings("squid:S1172")
  public EholdingsResourcesImpl(Vertx vertx, String tenantId) {
    SpringContextUtil.autowireDependencies(this, vertx.getOrCreateContext());
  }

  @Override
  public void postEholdingsResources(String contentType, ResourcePostRequest entity, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    asyncResultHandler.handle(Future.succeededFuture(PostEholdingsResourcesResponse.status(Response.Status.NOT_IMPLEMENTED).build()));

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
}
