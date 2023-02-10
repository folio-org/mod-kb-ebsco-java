package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;
import static java.util.concurrent.CompletableFuture.completedFuture;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import javax.ws.rs.core.Response;
import javax.xml.bind.ValidationException;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.repository.tag.DbTag;
import org.folio.repository.tag.TagRepository;
import org.folio.rest.annotations.Validate;
import org.folio.rest.aspect.HandleValidationErrors;
import org.folio.rest.jaxrs.model.TagCollection;
import org.folio.rest.jaxrs.model.TagUniqueCollection;
import org.folio.rest.jaxrs.resource.EholdingsTags;
import org.folio.rest.tools.utils.TenantTool;
import org.folio.rest.util.ErrorHandler;
import org.folio.rest.util.ErrorUtil;
import org.folio.rest.validator.RectypeParameterValidator;
import org.folio.spring.SpringContextUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;

public class EholdingsTagsImpl implements EholdingsTags {

  private final Logger log = LogManager.getLogger(EholdingsTagsImpl.class);

  @Autowired
  private RectypeParameterValidator recordTypeValidator;
  @Autowired
  private TagRepository tagRepository;
  @Autowired
  private Converter<List<String>, Set<org.folio.repository.RecordType>> recordTypesConverter;
  @Autowired
  private Converter<List<DbTag>, TagCollection> tagsConverter;
  @Autowired
  private Converter<List<String>, TagUniqueCollection> uniqueTagsConverter;

  public EholdingsTagsImpl() {
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
  }

  @Validate
  @HandleValidationErrors
  @Override
  public void getEholdingsTags(List<String> filterRectypes, Map<String, String> okapiHeaders,
                               Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    validateRecordTypes(filterRectypes);

    completedFuture(null)
      .thenCompose(o -> findTags(filterRectypes, TenantTool.tenantId(okapiHeaders)))
      .thenAccept(tags -> successfulGetTags(tags, asyncResultHandler))
      .exceptionally(throwable -> failedGetTags(throwable, asyncResultHandler,
        e -> GetEholdingsTagsResponse.respond400WithApplicationVndApiJson(ErrorUtil.createError(e.getMessage()))));
  }

  @Validate
  @HandleValidationErrors
  @Override
  public void getEholdingsTagsSummary(List<String> filterRectype, Map<String, String> okapiHeaders,
                                      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    validateRecordTypes(filterRectype);

    completedFuture(null)
      .thenCompose(o -> findUniqueTags(filterRectype, TenantTool.tenantId(okapiHeaders)))
      .thenAccept(tags -> asyncResultHandler.handle(succeededFuture(
        GetEholdingsTagsSummaryResponse.respond200WithApplicationVndApiJson(
          uniqueTagsConverter.convert(tags)))))
      .exceptionally(throwable -> failedGetTags(throwable, asyncResultHandler,
        e -> GetEholdingsTagsSummaryResponse.respond400WithApplicationVndApiJson(
          ErrorUtil.createError(e.getMessage()))));
  }

  private void validateRecordTypes(List<String> filterRectypes) {
    if (CollectionUtils.isNotEmpty(filterRectypes)) {
      filterRectypes.forEach(recordTypeValidator::validate);
    }
  }

  private CompletableFuture<List<String>> findUniqueTags(List<String> filterRectypes, String tenantId) {
    log.info("Retrieving tags: tenantId = {}, recordTypes = {}", tenantId, filterRectypes);

    if (CollectionUtils.isEmpty(filterRectypes)) {
      return tagRepository.findDistinctRecordTags(tenantId);
    } else {
      return tagRepository.findDistinctByRecordTypes(tenantId, recordTypesConverter.convert(filterRectypes));
    }
  }

  private CompletableFuture<List<DbTag>> findTags(List<String> filterRectypes, String tenantId) {
    log.info("Retrieving tags: tenantId = {}, recordTypes = {}", tenantId, filterRectypes);

    if (CollectionUtils.isEmpty(filterRectypes)) {
      return tagRepository.findAll(tenantId);
    } else {
      return tagRepository.findByRecordTypes(tenantId, recordTypesConverter.convert(filterRectypes));
    }
  }

  private void successfulGetTags(List<DbTag> tags, Handler<AsyncResult<Response>> handler) {
    handler.handle(succeededFuture(GetEholdingsTagsResponse.respond200WithApplicationVndApiJson(
      tagsConverter.convert(tags))));
  }

  private Void failedGetTags(Throwable th, Handler<AsyncResult<Response>> handler,
                             Function<ValidationException, Response> exceptionHandler) {
    log.warn("Tag retrieval failed: " + th.getMessage(), th);

    ErrorHandler errHandler = new ErrorHandler()
      .add(ValidationException.class, exceptionHandler)
      .addInputValidation400Mapper();

    errHandler.handle(handler, th);

    return null;
  }

}
