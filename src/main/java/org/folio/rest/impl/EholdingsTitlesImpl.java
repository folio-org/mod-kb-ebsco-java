package org.folio.rest.impl;

import static java.util.concurrent.CompletableFuture.completedFuture;

import static org.folio.db.RowSetUtils.toUUID;
import static org.folio.rest.util.IdParser.parsePackageId;
import static org.folio.rest.util.IdParser.parseResourceId;
import static org.folio.rest.util.IdParser.parseTitleId;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import javax.ws.rs.core.Response;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;

import org.folio.common.ListUtils;
import org.folio.holdingsiq.model.CustomerResources;
import org.folio.holdingsiq.model.PackageId;
import org.folio.holdingsiq.model.ResourcePut;
import org.folio.holdingsiq.model.TitlePost;
import org.folio.holdingsiq.model.Titles;
import org.folio.holdingsiq.service.exception.ResourceNotFoundException;
import org.folio.repository.RecordKey;
import org.folio.repository.RecordType;
import org.folio.repository.tag.TagRepository;
import org.folio.repository.titles.DbTitle;
import org.folio.repository.titles.TitlesRepository;
import org.folio.rest.annotations.Validate;
import org.folio.rest.aspect.HandleValidationErrors;
import org.folio.rest.converter.titles.TitlePutRequestConverter;
import org.folio.rest.jaxrs.model.Tags;
import org.folio.rest.jaxrs.model.Title;
import org.folio.rest.jaxrs.model.TitleCollection;
import org.folio.rest.jaxrs.model.TitlePostRequest;
import org.folio.rest.jaxrs.model.TitlePutRequest;
import org.folio.rest.jaxrs.resource.EholdingsTitles;
import org.folio.rest.model.filter.Filter;
import org.folio.rest.util.ErrorUtil;
import org.folio.rest.util.IdParser;
import org.folio.rest.util.template.RMAPITemplateContext;
import org.folio.rest.util.template.RMAPITemplateFactory;
import org.folio.rest.validator.TitleCommonRequestAttributesValidator;
import org.folio.rest.validator.TitlesPostBodyValidator;
import org.folio.rmapi.result.TitleCollectionResult;
import org.folio.rmapi.result.TitleResult;
import org.folio.service.loader.FilteredEntitiesLoader;
import org.folio.service.loader.RelatedEntitiesLoader;
import org.folio.spring.SpringContextUtil;

public class EholdingsTitlesImpl implements EholdingsTitles {

  private static final String GET_TITLE_NOT_FOUND_MESSAGE = "Title not found";
  private static final String INCLUDE_RESOURCES_VALUE = "resources";

  @Autowired
  private Converter<TitlePostRequest, TitlePost> titlePostRequestConverter;
  @Autowired
  private TitlePutRequestConverter titlePutRequestConverter;
  @Autowired
  private TitlesPostBodyValidator titlesPostBodyValidator;
  @Autowired
  private TitleCommonRequestAttributesValidator titleCommonRequestAttributesValidator;
  @Autowired
  private RMAPITemplateFactory templateFactory;
  @Autowired
  private TagRepository tagRepository;
  @Autowired
  private TitlesRepository titlesRepository;
  @Autowired
  private RelatedEntitiesLoader relatedEntitiesLoader;
  @Autowired
  private FilteredEntitiesLoader filteredEntitiesLoader;

  public EholdingsTitlesImpl() {
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
  }

  @Override
  @Validate
  @HandleValidationErrors
  public void getEholdingsTitles(List<String> filterTags, List<String> filterAccessType, String filterSelected,
                                 String filterType, String filterName, String filterIsxn, String filterSubject,
                                 String filterPublisher, String sort, int page, int count, String include,
                                 Map<String, String> okapiHeaders,
                                 Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    Filter filter = Filter.builder()
      .recordType(RecordType.TITLE)
      .filterTags(filterTags)
      .filterAccessType(filterAccessType)
      .filterSelected(filterSelected)
      .filterType(filterType)
      .filterName(filterName)
      .filterIsxn(filterIsxn)
      .filterSubject(filterSubject)
      .filterPublisher(filterPublisher)
      .sort(sort)
      .page(page)
      .count(count)
      .build();

    templateFactory.createTemplate(okapiHeaders, asyncResultHandler)
      .requestAction(context -> fetchTitlesByFilter(filter, context)
        .thenApply(titles -> toTitleCollectionResult(titles, shouldIncludeResources(include))))
      .executeWithResult(TitleCollection.class);
  }

  @Override
  @HandleValidationErrors
  public void postEholdingsTitles(String contentType, TitlePostRequest entity, Map<String, String> okapiHeaders,
                                  Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    titlesPostBodyValidator.validate(entity);

    TitlePost titlePost = titlePostRequestConverter.convert(entity);
    PackageId packageId = parsePackageId(entity.getIncluded().get(0).getAttributes().getPackageId());

    templateFactory.createTemplate(okapiHeaders, asyncResultHandler)
      .requestAction(context ->
        context.getTitlesService().postTitle(titlePost, packageId)
          .thenCompose(title -> completedFuture(toTitleResult(title, false)))
          .thenCompose(titleResult ->
            updateTags(titleResult, context, entity.getData().getAttributes().getTags()))
      )
      .executeWithResult(Title.class);
  }

  @Override
  @HandleValidationErrors
  public void getEholdingsTitlesByTitleId(String titleId, String include, Map<String, String> okapiHeaders,
                                          Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    long titleIdLong = parseTitleId(titleId);

    templateFactory.createTemplate(okapiHeaders, asyncResultHandler)
      .requestAction(context ->
        context.getTitlesService().retrieveTitle(titleIdLong)
          .thenCompose(title -> completedFuture(toTitleResult(title, shouldIncludeResources(include))))
          .thenCompose(result -> loadTags(result, context))
      )
      .addErrorMapper(ResourceNotFoundException.class, exception ->
        GetEholdingsTitlesByTitleIdResponse
          .respond404WithApplicationVndApiJson(ErrorUtil.createError(GET_TITLE_NOT_FOUND_MESSAGE))
      )
      .executeWithResult(Title.class);
  }

  @Override
  @HandleValidationErrors
  public void putEholdingsTitlesByTitleId(String titleId, String contentType, TitlePutRequest entity,
                                          Map<String, String> okapiHeaders,
                                          Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    titleCommonRequestAttributesValidator.validate(entity.getData().getAttributes());

    Long parsedTitleId = parseTitleId(titleId);
    templateFactory.createTemplate(okapiHeaders, asyncResultHandler)
      .requestAction(context ->
        context.getTitlesService().retrieveTitle(parsedTitleId)
          .thenCompose(title -> {
            if (BooleanUtils.isNotTrue(title.getIsTitleCustom())) {
              return completedFuture(null);
            }
            CustomerResources resource = title.getCustomerResourcesList().get(0);
            ResourcePut resourcePutRequest =
              titlePutRequestConverter.convertToRMAPICustomResourcePutRequest(entity, resource);
            String resourceId = resource.getVendorId() + "-" + resource.getPackageId() + "-" + resource.getTitleId();
            return context.getResourcesService().updateResource(parseResourceId(resourceId), resourcePutRequest);
          })
          .thenCompose(o -> context.getTitlesService().retrieveTitle(parsedTitleId))
          .thenCompose(title ->
            updateTags(toTitleResult(title, false), context, entity.getData().getAttributes().getTags()))
      )
      .executeWithResult(Title.class);
  }

  private CompletableFuture<Titles> fetchTitlesByFilter(Filter filter, RMAPITemplateContext context) {
    if (filter.isTagsFilter()) {
      return filteredEntitiesLoader.fetchTitlesByTagFilter(filter.createTagFilter(), context);
    } else if (filter.isAccessTypeFilter()) {
      return filteredEntitiesLoader.fetchTitlesByAccessTypeFilter(filter.createAccessTypeFilter(), context);
    } else {
      return context.getTitlesService()
        .retrieveTitles(filter.createFilterQuery(), filter.getSort(), filter.getPage(), filter.getCount());
    }
  }

  private TitleCollectionResult toTitleCollectionResult(Titles titles, boolean includeResource) {
    return TitleCollectionResult.builder()
      .titleResults(ListUtils.mapItems(titles.getTitleList(), title -> toTitleResult(title, includeResource)))
      .totalResults(titles.getTotalResults())
      .build();
  }

  private TitleResult toTitleResult(org.folio.holdingsiq.model.Title title, boolean includeResource) {
    return new TitleResult(title, includeResource);
  }

  private boolean shouldIncludeResources(String include) {
    return INCLUDE_RESOURCES_VALUE.equalsIgnoreCase(include);
  }

  private CompletableFuture<TitleResult> loadTags(TitleResult result,
                                                  RMAPITemplateContext context) {
    RecordKey recordKey = RecordKey.builder()
      .recordType(RecordType.TITLE)
      .recordId(String.valueOf(result.getTitle().getTitleId()))
      .build();
    if (result.isIncludeResource()) {
      List<String> resourceIds = result.getTitle()
        .getCustomerResourcesList()
        .stream()
        .map(IdParser::getResourceId)
        .collect(Collectors.toList());
      return tagRepository.findByRecordByIds(context.getOkapiData().getTenant(), resourceIds, RecordType.RESOURCE)
        .thenApply(tags -> {
          result.setResourceTagList(tags);
          return result;
        })
        .thenCompose(titleResult -> relatedEntitiesLoader.loadTags(titleResult, recordKey, context)
          .thenApply(aVoid -> titleResult)
        );
    } else {
      return relatedEntitiesLoader.loadTags(result, recordKey, context).thenApply(aVoid -> result);
    }
  }

  private CompletableFuture<TitleResult> updateTags(TitleResult result, RMAPITemplateContext context, Tags tags) {
    if (Objects.isNull(tags)) {
      return completedFuture(result);
    } else {
      String tenant = context.getOkapiData().getTenant();
      UUID credentialsId = toUUID(context.getCredentialsId());

      return updateStoredTitles(createDbTitle(result, credentialsId), tags, tenant)
        .thenCompose(o -> tagRepository.updateRecordTags(tenant, String.valueOf(
          result.getTitle().getTitleId()), RecordType.TITLE, tags.getTagList()))
        .thenApply(updated -> {
          result.setTags(new Tags().withTagList(tags.getTagList()));
          return result;
        });
    }
  }

  private DbTitle createDbTitle(TitleResult result, UUID credentialsId) {
    return DbTitle.builder()
      .id(result.getTitle().getTitleId().longValue())
      .name(result.getTitle().getTitleName())
      .credentialsId(credentialsId)
      .title(result.getTitle())
      .build();
  }

  private CompletableFuture<Void> updateStoredTitles(DbTitle dbTitle, Tags tags, String tenant) {
    if (!tags.getTagList().isEmpty()) {
      return titlesRepository.save(dbTitle, tenant);
    }
    return titlesRepository.delete(dbTitle.getId(), dbTitle.getCredentialsId(), tenant);
  }
}
