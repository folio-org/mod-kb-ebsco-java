package org.folio.rest.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import javax.ws.rs.core.Response;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;

import org.folio.holdingsiq.model.CustomerResources;
import org.folio.holdingsiq.model.FilterQuery;
import org.folio.holdingsiq.model.PackageId;
import org.folio.holdingsiq.model.ResourcePut;
import org.folio.holdingsiq.model.Sort;
import org.folio.holdingsiq.model.TitlePost;
import org.folio.holdingsiq.model.Titles;
import org.folio.holdingsiq.service.exception.ResourceNotFoundException;
import org.folio.holdingsiq.service.validator.TitleParametersValidator;
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
import org.folio.rest.parser.IdParser;
import org.folio.rest.tools.utils.TenantTool;
import org.folio.rest.util.ErrorUtil;
import org.folio.rest.util.RestConstants;
import org.folio.rest.util.template.RMAPITemplate;
import org.folio.rest.util.template.RMAPITemplateContext;
import org.folio.rest.util.template.RMAPITemplateFactory;
import org.folio.rest.validator.TitleCommonRequestAttributesValidator;
import org.folio.rest.validator.TitlesPostBodyValidator;
import org.folio.rmapi.result.TitleResult;
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
  private TitleParametersValidator parametersValidator;
  @Autowired
  private IdParser idParser;
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

  public EholdingsTitlesImpl() {
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
  }

  @Override
  @Validate
  @HandleValidationErrors
  public void getEholdingsTitles(String filterTags, String filterSelected, String filterType, String filterName, String filterIsxn, String filterSubject,
                                 String filterPublisher, String sort, int page, int count, Map<String, String> okapiHeaders,
                                 Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    RMAPITemplate template = templateFactory.createTemplate(okapiHeaders, asyncResultHandler);
    if(!StringUtils.isEmpty(filterTags)){
      List<String> tags = Arrays.asList(filterTags.split("\\s*,\\s*"));
      template.requestAction(context -> getResourcesByTags(tags, page, count, context));
    }
    else {
      FilterQuery fq = FilterQuery.builder()
        .selected(RestConstants.FILTER_SELECTED_MAPPING.get(filterSelected))
        .type(filterType).name(filterName).isxn(filterIsxn).subject(filterSubject)
        .publisher(filterPublisher).build();

      parametersValidator.validate(fq, sort);

      Sort nameSort = Sort.valueOf(sort.toUpperCase());

      template
        .requestAction(context ->
          context.getTitlesService().retrieveTitles(fq, nameSort, page, count)
        );
    }
    template.executeWithResult(TitleCollection.class);
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
    titleCommonRequestAttributesValidator.validate(entity.getData().getAttributes());

    Long parsedTitleId = idParser.parseTitleId(titleId);
    templateFactory.createTemplate(okapiHeaders, asyncResultHandler)
      .requestAction(context ->
        context.getTitlesService().retrieveTitle(parsedTitleId)
          .thenCompose(title -> {
            if(BooleanUtils.isNotTrue(title.getIsTitleCustom())){
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
          .thenCompose(result -> loadTags(result, okapiHeaders))
      )
      .addErrorMapper(ResourceNotFoundException.class, exception ->
        GetEholdingsTitlesByTitleIdResponse
          .respond404WithApplicationVndApiJson(ErrorUtil.createError(GET_TITLE_NOT_FOUND_MESSAGE))
      )
      .executeWithResult(Title.class);
  }

  private CompletableFuture<Titles> getResourcesByTags(List<String> tags, int page, int count, RMAPITemplateContext context) {
    MutableObject<Integer> totalResults = new MutableObject<>();
    MutableObject<List<DbTitle>> mutableDbTitles = new MutableObject<>();

    String tenant = context.getOkapiData().getTenant();

    return titlesRepository.countTitlesByResourceTags(tags, tenant)
      .thenCompose(resultsCount -> {
        totalResults.setValue(resultsCount);
        return titlesRepository.getTitlesByResourceTags(tags, page, count, tenant);
      })
      .thenCompose(dbTitles -> {
        mutableDbTitles.setValue(dbTitles);
        List<Long> missingTitleIds = dbTitles.stream()
          .filter(title -> Objects.isNull(title.getTitle()))
          .map(DbTitle::getId)
          .collect(Collectors.toList());
        return context.getTitlesService().retrieveTitles(missingTitleIds);
      })
      .thenApply(titles ->
        titles.toBuilder()
          .titleList(combineTitles(mutableDbTitles.getValue(), titles.getTitleList()))
          .totalResults(totalResults.getValue())
          .build()
      );
  }

  private List<org.folio.holdingsiq.model.Title> combineTitles(List<DbTitle> dbTitles, List<org.folio.holdingsiq.model.Title> titleList) {
    List<org.folio.holdingsiq.model.Title> resultList = new ArrayList<>(titleList);
    resultList.addAll(
      dbTitles.stream()
        .filter(title -> Objects.nonNull(title.getTitle()))
        .map(DbTitle::getTitle)
        .collect(Collectors.toList())
    );
    resultList.sort(Comparator.comparing(org.folio.holdingsiq.model.Title::getTitleName));

    return resultList;
  }

  private CompletableFuture<TitleResult> loadTags(TitleResult result,
                                                  Map<String, String> okapiHeaders) {
    RecordKey recordKey = RecordKey.builder()
      .recordType(RecordType.TITLE)
      .recordId(String.valueOf(result.getTitle().getTitleId()))
      .build();
    if (result.isIncludeResource()) {
      List<String> resourceIds = result.getTitle()
        .getCustomerResourcesList()
        .stream()
        .map(this::buildResourceId)
        .collect(Collectors.toList());
      return tagRepository.findByRecordByIds(TenantTool.tenantId(okapiHeaders), resourceIds, RecordType.RESOURCE)
        .thenApply(tags -> {
          result.setResourceTagList(tags);
          return result;
        })
        .thenCompose(titleResult -> relatedEntitiesLoader.loadTags(titleResult, recordKey, okapiHeaders)
          .thenApply(aVoid -> titleResult)
        );
    } else {
      return relatedEntitiesLoader.loadTags(result, recordKey, okapiHeaders).thenApply(aVoid -> result);
    }
  }

  private String buildResourceId(CustomerResources customerResources) {
    return customerResources.getVendorId() + "-" + customerResources.getPackageId() + "-"
      + customerResources.getTitleId();
  }

  private CompletableFuture<TitleResult> updateTags(TitleResult result, String tenant, Tags tags) {
    if (Objects.isNull(tags)) {
      return CompletableFuture.completedFuture(result);
    } else {
      return updateStoredTitles(result, tags, tenant)
        .thenCompose(o -> tagRepository.updateRecordTags(tenant, String.valueOf(
          result.getTitle().getTitleId()), RecordType.TITLE, tags.getTagList()))
        .thenApply(updated -> {
          result.setTags(new Tags().withTagList(tags.getTagList()));
          return result;
        });
    }
  }

  private CompletableFuture<Void> updateStoredTitles(TitleResult result, Tags tags, String tenant) {

    if (!tags.getTagList().isEmpty()) {
      return titlesRepository.saveTitle(result.getTitle(), tenant);
    }
    return titlesRepository.deleteTitle(String.valueOf(result.getTitle().getTitleId()), tenant);
  }
}
