package org.folio.rest.impl;

import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.validation.ValidationException;
import javax.ws.rs.core.Response;

import org.apache.http.HttpStatus;
import org.folio.config.RMAPIConfigurationServiceCache;
import org.folio.config.RMAPIConfigurationServiceImpl;
import org.folio.config.api.RMAPIConfigurationService;
import org.folio.http.ConfigurationClientProvider;
import org.folio.rest.aspect.HandleValidationErrors;
import org.folio.rest.converter.ResourcesConverter;
import org.folio.rest.exception.InputValidationException;
import org.folio.rest.jaxrs.model.ResourcePostRequest;
import org.folio.rest.jaxrs.model.ResourcePutRequest;
import org.folio.rest.jaxrs.resource.EholdingsResources;
import org.folio.rest.model.OkapiData;
import org.folio.rest.model.ResourceId;
import org.folio.rest.util.ErrorUtil;
import org.folio.rest.validator.HeaderValidator;
import org.folio.rmapi.RMAPIService;
import org.folio.rmapi.exception.RMAPIResourceNotFoundException;
import org.folio.rmapi.exception.RMAPIServiceException;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;


public class EholdingsResourcesImpl implements EholdingsResources{
  
  private static final String RESOURCE_ID_REGEX = "([^-]+)-([^-]+)-([^-]+)";
  private static final Pattern RESOURCE_ID_PATTERN = Pattern.compile(RESOURCE_ID_REGEX);
  private static final String RESOURCE_ID_INVALID_ERROR = "Resource id is invalid";
  private static final String RESOURCE_NOT_FOUND_MESSAGE = "Resource not found";
  private static final String CONTENT_TYPE_HEADER = "Content-Type";
  private static final String CONTENT_TYPE_VALUE = "application/vnd.api+json";
  
  private final Logger logger = LoggerFactory.getLogger(EholdingsResourcesImpl.class);
  
  private RMAPIConfigurationService configurationService;
  private HeaderValidator headerValidator;
  private ResourcesConverter converter;
  
  public EholdingsResourcesImpl() {
    this(
      new RMAPIConfigurationServiceCache(
        new RMAPIConfigurationServiceImpl(new ConfigurationClientProvider())),
      new HeaderValidator(),
      new ResourcesConverter());
  }
  
  public EholdingsResourcesImpl(RMAPIConfigurationService configurationService,
      HeaderValidator headerValidator,
      ResourcesConverter converter) {
    this.configurationService = configurationService;
    this.headerValidator = headerValidator;
    this.converter = converter;
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
    ResourceId parsedResourceId = parseResourceId(resourceId);
    headerValidator.validate(okapiHeaders);
    
    CompletableFuture.completedFuture(null)
      .thenCompose(o -> configurationService.retrieveConfiguration(new OkapiData(okapiHeaders)))
      .thenCompose(rmapiConfiguration -> {
        RMAPIService rmapiService = new RMAPIService(rmapiConfiguration.getCustomerId(), rmapiConfiguration.getAPIKey(),
          rmapiConfiguration.getUrl(), vertxContext.owner());
        return rmapiService.retrieveResource(parsedResourceId);
    })
    .thenAccept(title ->
      asyncResultHandler.handle(Future.succeededFuture(GetEholdingsResourcesByResourceIdResponse
        .respond200WithApplicationVndApiJson(converter.convertFromRMAPIResource(title)))))
    .exceptionally(e -> {
      logger.error(INTERNAL_SERVER_ERROR, e);
      if (e.getCause() instanceof RMAPIResourceNotFoundException) {
        asyncResultHandler.handle(Future.succeededFuture(GetEholdingsResourcesByResourceIdResponse
          .respond404WithApplicationVndApiJson(ErrorUtil.createError(RESOURCE_NOT_FOUND_MESSAGE))));
      } else if (e.getCause() instanceof RMAPIServiceException) {
          RMAPIServiceException rmApiException = (RMAPIServiceException) e.getCause();
          asyncResultHandler.handle(Future.succeededFuture(
              Response.status(rmApiException.getRMAPICode()).header(CONTENT_TYPE_HEADER, CONTENT_TYPE_VALUE)
                  .entity(ErrorUtil.createErrorFromRMAPIResponse(rmApiException)).build()));
      } else if (e.getCause() instanceof InputValidationException) {
        InputValidationException inputValidationException = (InputValidationException) e.getCause();
        asyncResultHandler.handle(Future.succeededFuture(
            GetEholdingsResourcesByResourceIdResponse.respond400WithApplicationVndApiJson(
            ErrorUtil.createError(inputValidationException.getMessage(), inputValidationException.getMessageDetail()))));
      } else {
        asyncResultHandler.handle(Future.succeededFuture(GetEholdingsResourcesByResourceIdResponse
          .status(HttpStatus.SC_INTERNAL_SERVER_ERROR)
          .header(CONTENT_TYPE_HEADER, CONTENT_TYPE_VALUE)
          .entity(ErrorUtil.createError(e.getCause().getMessage()))
          .build()));
      }
      return null;
    });
  }

  private ResourceId parseResourceId(String resourceIdString) {
    try {
      long providerId;
      long packageId;
      long titleId;
      
      Matcher matcher = RESOURCE_ID_PATTERN.matcher(resourceIdString);
      
      if(matcher.find() && matcher.hitEnd()) {
        providerId = Long.parseLong(matcher.group(1));
        packageId = Long.parseLong(matcher.group(2));
        titleId = Long.parseLong(matcher.group(3));
      } else {
        throw new ValidationException(RESOURCE_ID_INVALID_ERROR );
      }
      
      return new ResourceId(providerId, packageId, titleId);
    } catch (NumberFormatException e) {
      throw new ValidationException(RESOURCE_ID_INVALID_ERROR);
    }
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
