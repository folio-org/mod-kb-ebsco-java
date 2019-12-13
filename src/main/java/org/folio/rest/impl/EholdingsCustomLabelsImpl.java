package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;
import static java.lang.String.format;
import static javax.ws.rs.core.Response.status;

import static org.folio.rest.util.ErrorUtil.createError;

import java.util.Map;

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
import org.folio.rest.jaxrs.model.CustomLabelPutRequest;
import org.folio.rest.jaxrs.model.CustomLabelsCollection;
import org.folio.rest.jaxrs.resource.EholdingsCustomLabels;
import org.folio.rest.util.template.RMAPITemplateFactory;
import org.folio.spring.SpringContextUtil;

public class EholdingsCustomLabelsImpl implements EholdingsCustomLabels {

  private static final String CUSTOM_LABEL_NOT_FOUND_TITLE = "Label not found";
  private static final String CUSTOM_LABEL_NOT_FOUND_MESSAGE = "Label with id: '%s' does not exist";
  private static final String CUSTOM_LABEL_BAD_REQUEST_MESSAGE = "Invalid format for Custom Label id: '%s'";

  @Autowired
  private RMAPITemplateFactory templateFactory;

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
    if (fieldId <= 0 || fieldId > rootProxyCustomLabels.getLabelList().size()){
      throw new NotFoundException();
    }
    return rootProxyCustomLabels.getLabelList().get(fieldId - 1);
  }

  @Override
  @HandleValidationErrors
  public void putEholdingsCustomLabelsById(String id, String contentType, CustomLabelPutRequest entity,
                                           Map<String, String> okapiHeaders,
                                           Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    asyncResultHandler.handle(succeededFuture(status(Status.NOT_IMPLEMENTED).build()));
  }

  @Override
  @HandleValidationErrors
  public void deleteEholdingsCustomLabelsById(String id, Map<String, String> okapiHeaders,
                                              Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    asyncResultHandler.handle(succeededFuture(status(Status.NOT_IMPLEMENTED).build()));
  }

}
