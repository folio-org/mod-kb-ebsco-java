package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;
import static java.util.concurrent.CompletableFuture.completedFuture;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import javax.ws.rs.core.Response;
import javax.xml.bind.ValidationException;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;

import org.folio.rest.annotations.Validate;
import org.folio.rest.aspect.HandleValidationErrors;
import org.folio.rest.jaxrs.model.TagCollection;
import org.folio.rest.jaxrs.resource.EholdingsTags;
import org.folio.rest.tools.utils.TenantTool;
import org.folio.rest.util.ErrorHandler;
import org.folio.rest.util.ErrorUtil;
import org.folio.rest.validator.RectypeParameterValidator;
import org.folio.spring.SpringContextUtil;
import org.folio.tag.Tag;
import org.folio.tag.repository.TagRepository;

public class EholdingsTagsImpl implements EholdingsTags {

  private final Logger log = LoggerFactory.getLogger(EholdingsTagsImpl.class);

  @Autowired
  private RectypeParameterValidator recordTypeValidator;
  @Autowired
  private TagRepository tagRepository;
  @Autowired
  private Converter<List<String>, Set<org.folio.tag.RecordType>> recordTypesConverter;
  @Autowired
  private Converter<List<Tag>, TagCollection> tagsConverter;


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
      .exceptionally(throwable -> failedGetTags(throwable, asyncResultHandler));
  }

  private void validateRecordTypes(List<String> filterRectypes) {
    if (CollectionUtils.isNotEmpty(filterRectypes)) {
      filterRectypes.forEach(recordTypeValidator::validate);
    }
  }

  private CompletableFuture<List<Tag>> findTags(List<String> filterRectypes, String tenantId) {
    log.info("Retrieving tags: tenantId = %s, recordTypes = %s", tenantId, filterRectypes);

    if (CollectionUtils.isEmpty(filterRectypes)) {
      return tagRepository.findAll(tenantId);
    } else {
      return tagRepository.findByRecordTypes(tenantId, recordTypesConverter.convert(filterRectypes));
    }
  }

  private void successfulGetTags(List<Tag> tags, Handler<AsyncResult<Response>> handler) {
    handler.handle(succeededFuture(GetEholdingsTagsResponse.respond200WithApplicationVndApiJson(
      tagsConverter.convert(tags))));
  }

  private Void failedGetTags(Throwable th, Handler<AsyncResult<Response>> handler) {
    log.error("Tag retrieval failed: " + th.getMessage(), th);

    ErrorHandler errHandler = new ErrorHandler()
        .add(ValidationException.class,
          e -> GetEholdingsTagsResponse.respond400WithApplicationVndApiJson(ErrorUtil.createError(e.getMessage())))
        .addInputValidationMapper()
        .addDefaultMapper();

    errHandler.handle(handler, th);

    return null;
  }

}
