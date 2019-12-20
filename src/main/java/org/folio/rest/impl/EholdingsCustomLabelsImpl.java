package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;
import static java.lang.String.format;
import static javax.ws.rs.core.Response.status;

import static org.folio.rest.util.ErrorUtil.createError;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import javax.validation.ValidationException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import org.springframework.beans.factory.annotation.Autowired;

import org.folio.holdingsiq.model.CustomLabel;
import org.folio.holdingsiq.model.RootProxyCustomLabels;
import org.folio.rest.aspect.HandleValidationErrors;
import org.folio.rest.converter.labels.CustomLabelPutRequestToRmApiConverter;
import org.folio.rest.jaxrs.model.CustomLabelPutRequest;
import org.folio.rest.jaxrs.model.CustomLabelsCollection;
import org.folio.rest.jaxrs.resource.EholdingsCustomLabels;
import org.folio.rest.util.template.RMAPITemplateContext;
import org.folio.rest.util.template.RMAPITemplateFactory;
import org.folio.rest.validator.CustomLabelsPutBodyValidator;
import org.folio.spring.SpringContextUtil;

public class EholdingsCustomLabelsImpl implements EholdingsCustomLabels {

  private static final String CUSTOM_LABEL_NOT_FOUND_TITLE = "Label not found";
  private static final String CUSTOM_LABEL_NOT_FOUND_MESSAGE = "Label with id: '%s' does not exist";
  private static final String CUSTOM_LABEL_BAD_REQUEST_MESSAGE = "Invalid format for Custom Label id: '%s'";

  @Autowired
  private RMAPITemplateFactory templateFactory;
  @Autowired
  private CustomLabelPutRequestToRmApiConverter putRequestConverter;
  @Autowired
  private CustomLabelsPutBodyValidator putBodyValidator;

  public EholdingsCustomLabelsImpl() {
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
  }

  @Override
  @HandleValidationErrors
  public void getEholdingsCustomLabels(Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
                                       Context vertxContext) {
    templateFactory.createTemplate(okapiHeaders, asyncResultHandler)
      .requestAction(context -> context.getHoldingsService().retrieveRootProxyCustomLabels())
      .executeWithResult(CustomLabelsCollection.class);
  }

  @Override
  @HandleValidationErrors
  public void getEholdingsCustomLabelsById(String id, Map<String, String> okapiHeaders,
                                           Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    final int fieldId = parseCustomLabelId(id);
    templateFactory.createTemplate(okapiHeaders, asyncResultHandler)
      .requestAction(context -> context.getHoldingsService().retrieveRootProxyCustomLabels()
        .thenApply(rootProxyCustomLabels -> getCustomLabelById(fieldId, rootProxyCustomLabels)))
      .addErrorMapper(NotFoundException.class, exception ->
        GetEholdingsCustomLabelsByIdResponse.respond404WithApplicationVndApiJson(
          createError(CUSTOM_LABEL_NOT_FOUND_TITLE, format(CUSTOM_LABEL_NOT_FOUND_MESSAGE, fieldId))))
      .executeWithResult(org.folio.rest.jaxrs.model.CustomLabel.class);
  }

  private int parseCustomLabelId(String id) {
    try {
      return Integer.parseInt(id);
    } catch (NumberFormatException ex) {
      throw new ValidationException(format(CUSTOM_LABEL_BAD_REQUEST_MESSAGE, id));
    }
  }

  private CustomLabel getCustomLabelById(int fieldId, RootProxyCustomLabels rootProxyCustomLabels) {
    final Integer index = findIndexById(rootProxyCustomLabels.getLabelList(), fieldId);
    if (fieldId <= 0 || Objects.isNull(index)){
      throw new NotFoundException();
    }

    return rootProxyCustomLabels.getLabelList().get(index);
  }

  @Override
  @HandleValidationErrors
  public void putEholdingsCustomLabelsById(String id, String contentType, CustomLabelPutRequest entity,
                                           Map<String, String> okapiHeaders,
                                           Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    final int fieldId = parseCustomLabelId(id);
    putBodyValidator.validate(entity, fieldId);
    templateFactory.createTemplate(okapiHeaders, asyncResultHandler)
      .requestAction(context -> updateCustomLabel(context, putRequestConverter.convert(entity))
        .thenApply(o -> entity))
      .executeWithResult(org.folio.rest.jaxrs.model.CustomLabel.class);
  }

  private CompletableFuture<RootProxyCustomLabels> updateCustomLabel(RMAPITemplateContext context, CustomLabel customLabel) {
    return context.getHoldingsService().retrieveRootProxyCustomLabels()
      .thenCompose(rootProxyCustomLabels -> updateCustomLabelsCollection(customLabel, rootProxyCustomLabels, context));
  }

  private CompletableFuture<RootProxyCustomLabels> updateCustomLabelsCollection(CustomLabel entity,
                                                                                RootProxyCustomLabels labels,
                                                                                RMAPITemplateContext context) {
    updateCustomLabelsCollection(entity, labels);
    return context.getHoldingsService().updateRootProxyCustomLabels(labels);
  }

  private void updateCustomLabelsCollection(CustomLabel entity, RootProxyCustomLabels labels) {
    final List<CustomLabel> labelList = labels.getLabelList();
    final Integer index = findIndexById(labelList, entity.getId());

    if(Objects.nonNull(index)){
      labelList.set(index, entity);
    } else {
      labelList.add(entity);
    }
  }

  private Integer findIndexById(List<CustomLabel> labelList, int id){

    for (int index = 0; index < labelList.size(); index++) {
      final Integer customLabelId = labelList.get(index).getId();
      if (customLabelId.equals(id)){
        return index;
      }
    }
    return null;
  }

  @Override
  @HandleValidationErrors
  public void deleteEholdingsCustomLabelsById(String id, Map<String, String> okapiHeaders,
                                              Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    asyncResultHandler.handle(succeededFuture(status(Status.NOT_IMPLEMENTED).build()));
  }

}
