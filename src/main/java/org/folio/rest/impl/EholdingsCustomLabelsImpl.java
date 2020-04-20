package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;

import java.util.Map;
import java.util.function.Function;

import javax.ws.rs.core.Response;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import org.springframework.beans.factory.annotation.Autowired;

import org.folio.rest.annotations.Validate;
import org.folio.rest.aspect.HandleValidationErrors;
import org.folio.rest.jaxrs.model.CustomLabelsCollection;
import org.folio.rest.jaxrs.model.CustomLabelsPutRequest;
import org.folio.rest.jaxrs.resource.EholdingsCustomLabels;
import org.folio.rest.jaxrs.resource.EholdingsKbCredentialsIdCustomLabels;
import org.folio.rest.util.ErrorHandler;
import org.folio.rest.util.template.RMAPITemplateFactory;
import org.folio.service.customlabels.CustomLabelsService;
import org.folio.spring.SpringContextUtil;

public class EholdingsCustomLabelsImpl implements EholdingsCustomLabels, EholdingsKbCredentialsIdCustomLabels {

  @Autowired
  private RMAPITemplateFactory templateFactory;

  @Autowired
  private CustomLabelsService customLabelsService;
  @Autowired
  private ErrorHandler errorHandler;

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
  @Validate
  @HandleValidationErrors
  public void getEholdingsKbCredentialsCustomLabelsById(String credentialsId, Map<String, String> okapiHeaders,
                                                        Handler<AsyncResult<Response>> asyncResultHandler,
                                                        Context vertxContext) {
    customLabelsService.fetch(credentialsId, okapiHeaders)
      .thenAccept(customLabelsCollection -> asyncResultHandler.handle(succeededFuture(
        GetEholdingsKbCredentialsCustomLabelsByIdResponse.respond200WithApplicationVndApiJson(customLabelsCollection))))
      .exceptionally(handleException(asyncResultHandler));
  }

  @Override
  @Validate
  @HandleValidationErrors
  public void putEholdingsKbCredentialsCustomLabelsById(String credentialsId, CustomLabelsPutRequest putRequest,
                                                        Map<String, String> okapiHeaders,
                                                        Handler<AsyncResult<Response>> asyncResultHandler,
                                                        Context vertxContext) {
    customLabelsService.update(credentialsId, putRequest, okapiHeaders)
      .thenAccept(customLabelsCollection -> asyncResultHandler.handle(succeededFuture(
        PutEholdingsKbCredentialsCustomLabelsByIdResponse.respond200WithApplicationVndApiJson(customLabelsCollection))))
      .exceptionally(handleException(asyncResultHandler));
  }

  private Function<Throwable, Void> handleException(Handler<AsyncResult<Response>> asyncResultHandler) {
    return throwable -> {
      errorHandler.handle(asyncResultHandler, throwable);
      return null;
    };
  }
}
