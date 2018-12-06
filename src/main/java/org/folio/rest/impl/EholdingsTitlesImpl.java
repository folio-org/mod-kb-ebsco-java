package org.folio.rest.impl;

import javax.validation.ValidationException;
import javax.ws.rs.core.Response;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

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
import org.folio.rest.converter.TitleConverter;
import org.folio.rest.jaxrs.model.TitlePostRequest;
import org.folio.rest.jaxrs.resource.EholdingsTitles;
import org.folio.rest.model.FilterQuery;
import org.folio.rest.model.OkapiData;
import org.folio.rest.model.PackageId;
import org.folio.rest.model.Sort;
import org.folio.rest.parser.PackageParser;
import org.folio.rest.util.ErrorHandler;
import org.folio.rest.util.ErrorUtil;
import org.folio.rest.validator.HeaderValidator;
import org.folio.rest.validator.TitleParametersValidator;
import org.folio.rest.validator.TitlesPostBodyValidator;
import org.folio.rmapi.RMAPIService;
import org.folio.rmapi.exception.RMAPIResourceNotFoundException;

import org.folio.rmapi.model.TitlePost;

public class EholdingsTitlesImpl implements EholdingsTitles {

  private static final String GET_TITLES_ERROR_MESSAGE = "Failed to retrieve titles";
  private static final String GET_TITLE_NOT_FOUND_MESSAGE = "Title not found";
  private static final String GET_TITLES_BY_ID_ERROR_MESSAGE = "Failed to retrieve title by title id";
  private static final String POST_TITLES_ERROR_MESSAGE = "Failed to create title";

  private final Logger logger = LoggerFactory.getLogger(EholdingsTitlesImpl.class);

  private RMAPIConfigurationService configurationService;
  private HeaderValidator headerValidator;
  private TitleConverter converter;
  private TitleParametersValidator parametersValidator;
  private PackageParser packageParser;

  private TitlesPostBodyValidator titlesPostBodyValidator;
  public EholdingsTitlesImpl() {
    this(
      new RMAPIConfigurationServiceCache(
        new RMAPIConfigurationServiceImpl(new ConfigurationClientProvider())),
      new HeaderValidator(),
      new TitleParametersValidator(),
      new TitlesPostBodyValidator(),
      new TitleConverter(),
      new org.folio.rest.parser.PackageParser());
  }

  public EholdingsTitlesImpl(RMAPIConfigurationService configurationService,
                             HeaderValidator headerValidator,
                             TitleParametersValidator parametersValidator,
                             TitlesPostBodyValidator titlesPostBodyValidator,
                             TitleConverter converter,
                             PackageParser packageParser) {
    this.configurationService = configurationService;
    this.headerValidator = headerValidator;
    this.converter = converter;
    this.parametersValidator = parametersValidator;
    this.titlesPostBodyValidator = titlesPostBodyValidator;
    this.packageParser = packageParser;
  }

  @Override
  @Validate
  @HandleValidationErrors
  public void getEholdingsTitles(String filterSelected, String filterType, String filterName, String filterIsxn, String filterSubject,
                                 String filterPublisher, String sort, int page, int count, Map<String, String> okapiHeaders,
                                 Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    headerValidator.validate(okapiHeaders);

    FilterQuery fq = FilterQuery.builder()
      .selected(filterSelected).type(filterType)
      .name(filterName).isxn(filterIsxn).subject(filterSubject)
      .publisher(filterPublisher).build();

    parametersValidator.validate(fq, sort);

    Sort nameSort = Sort.valueOf(sort.toUpperCase());

    CompletableFuture.completedFuture(null)
      .thenCompose(o -> configurationService.retrieveConfiguration(new OkapiData(okapiHeaders)))
      .thenCompose(rmapiConfiguration -> {
        RMAPIService rmapiService = new RMAPIService(rmapiConfiguration.getCustomerId(), rmapiConfiguration.getAPIKey(),
          rmapiConfiguration.getUrl(), vertxContext.owner());
        return rmapiService.retrieveTitles(fq, nameSort, page, count);
      })
      .thenAccept(titles ->
        asyncResultHandler.handle(Future.succeededFuture(GetEholdingsTitlesResponse
          .respond200WithApplicationVndApiJson(converter.convert(titles)))))
      .exceptionally(e -> {
        logger.error(GET_TITLES_ERROR_MESSAGE, e);
        new ErrorHandler()
          .addRmApiMapper()
          .addDefaultMapper()
          .handle(asyncResultHandler, e);
        return null;
      });
  }

  @Override
  @HandleValidationErrors
  public void postEholdingsTitles(String contentType, TitlePostRequest entity, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {

    headerValidator.validate(okapiHeaders);
    titlesPostBodyValidator.validate(entity);

    TitlePost titlePost = converter.convertToPost(entity);
    PackageId packageId = packageParser.parsePackageId(entity.getIncluded().get(0).getAttributes().getPackageId());

    MutableObject<RMAPIService> service = new MutableObject<>();
    CompletableFuture.completedFuture(null)
      .thenCompose(o -> configurationService.retrieveConfiguration(new OkapiData(okapiHeaders)))
      .thenAccept(rmapiConfiguration ->
        service.setValue(new RMAPIService(rmapiConfiguration.getCustomerId(),
          rmapiConfiguration.getAPIKey(), rmapiConfiguration.getUrl(), vertxContext.owner())))
      .thenCompose(o  ->  service.getValue().postTitle(titlePost, packageId))
      .thenAccept(title ->
        asyncResultHandler.handle(Future.succeededFuture(PostEholdingsTitlesResponse
          .respond200WithApplicationVndApiJson(converter.convertFromRMAPITitle(title, "")))))
      .exceptionally(e -> {
        logger.error(POST_TITLES_ERROR_MESSAGE, e);
        new ErrorHandler()
          .addRmApiMapper()
          .addDefaultMapper()
          .handle(asyncResultHandler, e);
        return null;
      });
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
          .respond200WithApplicationVndApiJson(converter.convertFromRMAPITitle(title, include)))))
      .exceptionally(e -> {
        logger.error(GET_TITLES_BY_ID_ERROR_MESSAGE);
        new ErrorHandler()
          .add(RMAPIResourceNotFoundException.class, exception ->
            GetEholdingsTitlesByTitleIdResponse
              .respond404WithApplicationVndApiJson(ErrorUtil.createError(GET_TITLE_NOT_FOUND_MESSAGE))
          )
          .addRmApiMapper()
          .addDefaultMapper()
          .handle(asyncResultHandler, e);
        return null;
      });
  }
}
