package org.folio.rest.impl;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import javax.ws.rs.core.Response;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;

import org.folio.holdingsiq.model.CustomerResources;
import org.folio.holdingsiq.model.FilterQuery;
import org.folio.holdingsiq.model.PackageId;
import org.folio.holdingsiq.model.ResourcePut;
import org.folio.holdingsiq.model.Sort;
import org.folio.holdingsiq.model.TitlePost;
import org.folio.holdingsiq.service.exception.ResourceNotFoundException;
import org.folio.holdingsiq.service.validator.TitleParametersValidator;
import org.folio.rest.annotations.Validate;
import org.folio.rest.aspect.HandleValidationErrors;
import org.folio.rest.converter.titles.TitlePutRequestConverter;
import org.folio.rest.jaxrs.model.Tags;
import org.folio.rest.jaxrs.model.Title;
import org.folio.rest.jaxrs.model.TitleCollection;
import org.folio.rest.jaxrs.model.TitlePostRequest;
import org.folio.rest.jaxrs.model.TitlePutRequest;
import org.folio.rest.jaxrs.resource.EholdingsTitles;
import org.folio.rest.parser.IdParser;
import org.folio.rest.util.ErrorUtil;
import org.folio.rest.util.RestConstants;
import org.folio.rest.util.template.RMAPITemplateFactory;
import org.folio.rest.validator.TitlesPostAttributesValidator;
import org.folio.rest.validator.TitlesPostBodyValidator;
import org.folio.rmapi.result.TitleResult;
import org.folio.spring.SpringContextUtil;
import org.folio.tag.RecordType;
import org.folio.tag.repository.TagRepository;

public class EholdingsTitlesImpl implements EholdingsTitles {
  private static final String GET_TITLE_NOT_FOUND_MESSAGE = "Title not found";
  private static final String INCLUDE_RESOURCES_VALUE = "resources";

  @Autowired
  private Converter<TitlePostRequest, TitlePost> titlePostRequestConverter;
  @Autowired
  private TitlePutRequestConverter titlePutRequestConverter;
  @Autowired
  private TitleParametersValidator parametersValidator;
  @Autowired
  private IdParser idParser;
  @Autowired
  private TitlesPostBodyValidator titlesPostBodyValidator;
  @Autowired
  private TitlesPostAttributesValidator titlesPostAttributesValidator;
  @Autowired
  private RMAPITemplateFactory templateFactory;
  @Autowired
  private TagRepository tagRepository;

  public EholdingsTitlesImpl() {
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
  }

  @Override
  @Validate
  @HandleValidationErrors
  public void getEholdingsTitles(String filterSelected, String filterType, String filterName, String filterIsxn, String filterSubject,
                                 String filterPublisher, String sort, int page, int count, Map<String, String> okapiHeaders,
                                 Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    FilterQuery fq = FilterQuery.builder()
      .selected(RestConstants.FILTER_SELECTED_MAPPING.get(filterSelected))
      .type(filterType).name(filterName).isxn(filterIsxn).subject(filterSubject)
      .publisher(filterPublisher).build();

    parametersValidator.validate(fq, sort);

    Sort nameSort = Sort.valueOf(sort.toUpperCase());

    templateFactory.createTemplate(okapiHeaders, asyncResultHandler)
      .requestAction(context ->
        context.getTitlesService().retrieveTitles(fq, nameSort, page, count)
      )
      .executeWithResult(TitleCollection.class);
  }

  @Override
  @HandleValidationErrors
  public void postEholdingsTitles(String contentType, TitlePostRequest entity, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    titlesPostBodyValidator.validate(entity);

    TitlePost titlePost = titlePostRequestConverter.convert(entity);
    PackageId packageId = idParser.parsePackageId(entity.getIncluded().get(0).getAttributes().getPackageId());

    templateFactory.createTemplate(okapiHeaders, asyncResultHandler)
      .requestAction(context ->
        context.getTitlesService().postTitle(titlePost, packageId)
          .thenCompose(title -> CompletableFuture.completedFuture(new TitleResult(title, false)))
          .thenCompose(titleResult ->
            updateTags(titleResult, context.getOkapiData().getTenant(), entity.getData().getAttributes().getTags()))
      )
      .executeWithResult(Title.class);
  }

  @Override
  @HandleValidationErrors
  public void putEholdingsTitlesByTitleId(String titleId, String contentType, TitlePutRequest entity, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    titlesPostAttributesValidator.validate(entity.getData().getAttributes());

    Long parsedTitleId = idParser.parseTitleId(titleId);
    templateFactory.createTemplate(okapiHeaders, asyncResultHandler)
      .requestAction(context ->
        context.getTitlesService().retrieveTitle(parsedTitleId)
          .thenCompose(title -> {
            if(!title.getIsTitleCustom()){
              return CompletableFuture.completedFuture(null);
            }
            CustomerResources resource = title.getCustomerResourcesList().get(0);
            ResourcePut resourcePutRequest =
              titlePutRequestConverter.convertToRMAPICustomResourcePutRequest(entity, resource);
            String resourceId = resource.getVendorId() + "-" + resource.getPackageId() + "-" + resource.getTitleId();
            return context.getResourcesService().updateResource(idParser.parseResourceId(resourceId), resourcePutRequest);
          })
          .thenCompose(o -> context.getTitlesService().retrieveTitle(parsedTitleId))
          .thenCompose(title ->
            updateTags(new TitleResult(title, false),
              context.getOkapiData().getTenant(),
              entity.getData().getAttributes().getTags()))
      )
      .executeWithResult(Title.class);
  }

  @Override
  @HandleValidationErrors
  public void getEholdingsTitlesByTitleId(String titleId, String include, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    long titleIdLong = idParser.parseTitleId(titleId);
    boolean includeResource = INCLUDE_RESOURCES_VALUE.equalsIgnoreCase(include);

    templateFactory.createTemplate(okapiHeaders, asyncResultHandler)
      .requestAction(context ->
        context.getTitlesService().retrieveTitle(titleIdLong)
          .thenCompose(title -> CompletableFuture.completedFuture(new TitleResult(title, includeResource)))
          .thenCompose(result ->
            loadTags(result, context.getOkapiData().getTenant())
          )
      )
      .addErrorMapper(ResourceNotFoundException.class, exception ->
        GetEholdingsTitlesByTitleIdResponse
          .respond404WithApplicationVndApiJson(ErrorUtil.createError(GET_TITLE_NOT_FOUND_MESSAGE))
      )
      .executeWithResult(Title.class);
  }

  private CompletableFuture<TitleResult> loadTags(TitleResult result, String tenant) {
    return tagRepository.getTags(tenant, String.valueOf(result.getTitle().getTitleId()), RecordType.TITLE)
      .thenApply(tag -> {
        result.setTags(tag);
        return result;
      });
  }

  private CompletableFuture<TitleResult> updateTags(TitleResult result, String tenant, Tags tags) {
    if (tags == null){
      return CompletableFuture.completedFuture(result);
    }else {
      return tagRepository.updateTags(tenant, String.valueOf(result.getTitle().getTitleId()), RecordType.TITLE, tags.getTagList())
        .thenApply(updated -> {
          result.setTags(new Tags().withTagList(tags.getTagList()));
          return result;
        });
    }
  }
}
