package org.folio.rest.impl;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import javax.ws.rs.core.Response;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

import org.springframework.beans.factory.annotation.Autowired;

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
