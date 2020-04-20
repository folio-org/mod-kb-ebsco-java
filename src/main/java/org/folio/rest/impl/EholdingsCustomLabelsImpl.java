package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import javax.ws.rs.core.Response;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import org.springframework.beans.factory.annotation.Autowired;

import org.folio.holdingsiq.model.RootProxyCustomLabels;
import org.folio.rest.annotations.Validate;
import org.folio.rest.aspect.HandleValidationErrors;
import org.folio.rest.converter.labels.CustomLabelPutRequestToRmApiConverter;
import org.folio.rest.jaxrs.model.CustomLabelPutRequest;
import org.folio.rest.jaxrs.model.CustomLabelsCollection;
import org.folio.rest.jaxrs.resource.EholdingsCustomLabels;
import org.folio.rest.jaxrs.resource.EholdingsKbCredentialsIdCustomLabels;
import org.folio.rest.util.ErrorHandler;
import org.folio.rest.util.template.RMAPITemplateContext;
import org.folio.rest.util.template.RMAPITemplateFactory;
import org.folio.rest.validator.CustomLabelsPutBodyValidator;
import org.folio.service.customlabels.CustomLabelsService;
import org.folio.spring.SpringContextUtil;

public class EholdingsCustomLabelsImpl implements EholdingsCustomLabels, EholdingsKbCredentialsIdCustomLabels {

  @Autowired
  private RMAPITemplateFactory templateFactory;
  @Autowired
  private CustomLabelPutRequestToRmApiConverter putRequestConverter;
  @Autowired
  private CustomLabelsPutBodyValidator putBodyValidator;

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
  @HandleValidationErrors
  public void putEholdingsCustomLabels(String contentType, CustomLabelPutRequest entity, Map<String, String> okapiHeaders,
                                       Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    putBodyValidator.validate(entity);
    templateFactory.createTemplate(okapiHeaders, asyncResultHandler)
      .requestAction(context -> {
        RootProxyCustomLabels rootProxyCustomLabels = putRequestConverter.convert(entity);
        return updateCustomLabels(context, rootProxyCustomLabels).thenApply(e -> entity);
      })
      .executeWithResult(CustomLabelsCollection.class);
  }

  @Override
  @Validate
  @HandleValidationErrors
  public void getEholdingsKbCredentialsCustomLabelsById(String credentialsId, Map<String, String> okapiHeaders,
                                                        Handler<AsyncResult<Response>> asyncResultHandler,
                                                        Context vertxContext) {
    customLabelsService.fetchCustomLabels(credentialsId, okapiHeaders)
      .thenAccept(customLabelsCollection -> asyncResultHandler.handle(succeededFuture(
        GetEholdingsKbCredentialsCustomLabelsByIdResponse.respond200WithApplicationVndApiJson(customLabelsCollection))))
      .exceptionally(handleException(asyncResultHandler));
  }

  private Function<Throwable, Void> handleException(Handler<AsyncResult<Response>> asyncResultHandler) {
    return throwable -> {
      errorHandler.handle(asyncResultHandler, throwable);
      return null;
    };
  }

  private CompletableFuture<RootProxyCustomLabels> updateCustomLabels(RMAPITemplateContext context,
                                                                      RootProxyCustomLabels source) {
    return context.getHoldingsService().retrieveRootProxyCustomLabels()
      .thenCompose(target -> updateCustomLabels(target, source, context));
  }

  private CompletableFuture<RootProxyCustomLabels> updateCustomLabels(RootProxyCustomLabels target,
                                                                      RootProxyCustomLabels source,
                                                                      RMAPITemplateContext context) {
    target.getLabelList().clear();
    target.getLabelList().addAll(source.getLabelList());
    return context.getHoldingsService().updateRootProxyCustomLabels(target);
  }
}
