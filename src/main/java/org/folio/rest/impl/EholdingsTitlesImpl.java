package org.folio.rest.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import org.apache.http.HttpStatus;
import org.folio.config.RMAPIConfigurationServiceCache;
import org.folio.config.RMAPIConfigurationServiceImpl;
import org.folio.config.api.RMAPIConfigurationService;
import org.folio.http.ConfigurationClientProvider;
import org.folio.rest.annotations.Validate;
import org.folio.rest.aspect.HandleValidationErrors;
import org.folio.rest.converter.TitleConverter;
import org.folio.rest.jaxrs.model.TitlePostRequest;
import org.folio.rest.jaxrs.resource.EholdingsTitles;
import org.folio.rest.model.OkapiData;
import org.folio.rest.model.Sort;
import org.folio.rest.util.ErrorUtil;
import org.folio.rest.validator.HeaderValidator;
import org.folio.rest.validator.TitleParametersValidator;
import org.folio.rmapi.RMAPIService;
import org.folio.rmapi.exception.RMAPIResourceNotFoundException;
import org.folio.rmapi.exception.RMAPIServiceException;

import javax.validation.ValidationException;
import javax.ws.rs.core.Response;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class EholdingsTitlesImpl implements EholdingsTitles {

  private static final String GET_TITLES_ERROR_MESSAGE = "Failed to retrieve titles";
  private static final String GET_TITLE_NOT_FOUND_MESSAGE = "Title not found";
  
  private static final String CONTENT_TYPE_HEADER = "Content-Type";
  private static final String CONTENT_TYPE_VALUE = "application/vnd.api+json";


  private final Logger logger = LoggerFactory.getLogger(EholdingsTitlesImpl.class);

  private RMAPIConfigurationService configurationService;
  private HeaderValidator headerValidator;
  private TitleConverter converter;
  private TitleParametersValidator parametersValidator;
  public EholdingsTitlesImpl() {
    this(
      new RMAPIConfigurationServiceCache(
        new RMAPIConfigurationServiceImpl(new ConfigurationClientProvider())),
      new HeaderValidator(),
      new TitleParametersValidator(),
      new TitleConverter());
  }

  public EholdingsTitlesImpl(RMAPIConfigurationService configurationService,
                             HeaderValidator headerValidator, TitleParametersValidator parametersValidator,
                             TitleConverter converter) {
    this.configurationService = configurationService;
    this.headerValidator = headerValidator;
    this.converter = converter;
    this.parametersValidator = parametersValidator;
  }

  @Override
  @Validate
  @HandleValidationErrors
  public void getEholdingsTitles(String filterSelected, String filterType, String filterName, String filterIsxn, String filterSubject,
                                 String filterPublisher, String sort, int page, int count, Map<String, String> okapiHeaders,
                                 Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    headerValidator.validate(okapiHeaders);
    parametersValidator.validate(filterSelected, filterType, filterName, filterIsxn, filterSubject, filterPublisher, sort);
    Sort nameSort = Sort.valueOf(sort.toUpperCase());
    CompletableFuture.completedFuture(null)
      .thenCompose(o -> configurationService.retrieveConfiguration(new OkapiData(okapiHeaders)))
      .thenCompose(rmapiConfiguration -> {
        RMAPIService rmapiService = new RMAPIService(rmapiConfiguration.getCustomerId(), rmapiConfiguration.getAPIKey(),
          rmapiConfiguration.getUrl(), vertxContext.owner());
        return rmapiService.retrieveTitles(filterSelected, filterType, filterName, filterIsxn, filterSubject,
          filterPublisher, nameSort, page, count);
      })
      .thenAccept(titles ->
        asyncResultHandler.handle(Future.succeededFuture(GetEholdingsTitlesResponse
          .respond200WithApplicationVndApiJson(converter.convert(titles)))))
      .exceptionally(e -> {
        logger.error(GET_TITLES_ERROR_MESSAGE, e);
        if(e.getCause() instanceof RMAPIServiceException){
          RMAPIServiceException rmApiException = (RMAPIServiceException)e.getCause();
          asyncResultHandler.handle(Future.succeededFuture(Response
            .status(rmApiException.getRMAPICode())
            .header(CONTENT_TYPE_HEADER, CONTENT_TYPE_VALUE)
            .entity(ErrorUtil.createErrorFromRMAPIResponse(rmApiException))
            .build()));
        }
        else {
          asyncResultHandler.handle(Future.succeededFuture(Response
            .status(500)
            .header(CONTENT_TYPE_HEADER, CONTENT_TYPE_VALUE)
            .entity(ErrorUtil.createError(e.getCause().getMessage()))
            .build()));
        }
        return null;
      });
  }

  @Override
  @HandleValidationErrors
  public void postEholdingsTitles(String contentType, TitlePostRequest entity, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    asyncResultHandler.handle(Future.succeededFuture(GetEholdingsTitlesResponse.status(Response.Status.NOT_IMPLEMENTED).build()));
  }

  @Override
  @HandleValidationErrors
  public void getEholdingsTitlesByTitleId(String titleId, String include, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    long titleIdLong;
    try {
      titleIdLong = Long.parseLong(titleId);
    } catch (NumberFormatException e) {
      throw new ValidationException("Title id is invalid - " + titleId, e);
    }

    headerValidator.validate(okapiHeaders);
    CompletableFuture.completedFuture(null)
      .thenCompose(o -> configurationService.retrieveConfiguration(new OkapiData(okapiHeaders)))
      .thenCompose(rmapiConfiguration -> {
        RMAPIService rmapiService = new RMAPIService(rmapiConfiguration.getCustomerId(), rmapiConfiguration.getAPIKey(),
          rmapiConfiguration.getUrl(), vertxContext.owner());
        return rmapiService.retrieveTitle(titleIdLong);
      })
      .thenAccept(title ->
        asyncResultHandler.handle(Future.succeededFuture(GetEholdingsTitlesByTitleIdResponse
          .respond200WithApplicationVndApiJson(converter.convertFromRMAPITitle(title)))))
      .exceptionally(e -> {
        if (e.getCause() instanceof RMAPIResourceNotFoundException) {
          asyncResultHandler.handle(Future.succeededFuture(GetEholdingsTitlesByTitleIdResponse
            .respond404WithApplicationVndApiJson(ErrorUtil.createError(GET_TITLE_NOT_FOUND_MESSAGE))));
        } else if (e.getCause() instanceof RMAPIServiceException) {
            RMAPIServiceException rmApiException = (RMAPIServiceException) e.getCause();
            asyncResultHandler.handle(Future.succeededFuture(
                Response.status(rmApiException.getRMAPICode()).header(CONTENT_TYPE_HEADER, CONTENT_TYPE_VALUE)
                    .entity(ErrorUtil.createErrorFromRMAPIResponse(rmApiException)).build()));
        } else {
          asyncResultHandler.handle(Future.succeededFuture(GetEholdingsTitlesByTitleIdResponse
            .status(HttpStatus.SC_INTERNAL_SERVER_ERROR)
            .header(CONTENT_TYPE_HEADER, CONTENT_TYPE_VALUE)
            .entity(ErrorUtil.createError(e.getCause().getMessage()))
            .build()));
        }
        return null;
      });
  }
}
