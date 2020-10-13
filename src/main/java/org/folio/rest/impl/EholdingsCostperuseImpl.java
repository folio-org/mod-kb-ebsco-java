package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;

import java.util.Map;

import javax.ws.rs.core.Response;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import org.springframework.beans.factory.annotation.Autowired;

import org.folio.rest.annotations.Validate;
import org.folio.rest.aspect.HandleValidationErrors;
import org.folio.rest.jaxrs.resource.EholdingsResourcesResourceIdCostperuse;
import org.folio.rest.jaxrs.resource.EholdingsTitlesTitleIdCostperuse;
import org.folio.rest.util.ErrorHandler;
import org.folio.service.uc.UCCostPerUseService;
import org.folio.spring.SpringContextUtil;

public class EholdingsCostperuseImpl implements EholdingsResourcesResourceIdCostperuse, EholdingsTitlesTitleIdCostperuse {

  @Autowired
  private UCCostPerUseService costPerUseService;
  @Autowired
  private ErrorHandler costPerUseErrorHandler;

  public EholdingsCostperuseImpl() {
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
  }

  @Override
  @Validate
  @HandleValidationErrors
  public void getEholdingsResourcesCostperuseByResourceId(String resourceId, String platform, String fiscalYear,
                                                          Map<String, String> okapiHeaders,
                                                          Handler<AsyncResult<Response>> asyncResultHandler,
                                                          Context vertxContext) {
    costPerUseService.getResourceCostPerUse(resourceId, platform, fiscalYear, okapiHeaders)
      .thenAccept(costPerUse ->
        asyncResultHandler.handle(succeededFuture(
          GetEholdingsResourcesCostperuseByResourceIdResponse.respond200WithApplicationVndApiJson(costPerUse)))
      )
      .exceptionally(costPerUseErrorHandler.handle(asyncResultHandler));
  }

  @Override
  @Validate
  @HandleValidationErrors
  public void getEholdingsTitlesCostperuseByTitleId(String titleId, String platform, String fiscalYear,
                                                    Map<String, String> okapiHeaders,
                                                    Handler<AsyncResult<Response>> asyncResultHandler,
                                                    Context vertxContext) {
    costPerUseService.getTitleCostPerUse(titleId, platform, fiscalYear, okapiHeaders)
      .thenAccept(costPerUse ->
        asyncResultHandler.handle(succeededFuture(
          GetEholdingsTitlesCostperuseByTitleIdResponse.respond200WithApplicationVndApiJson(costPerUse)))
      )
      .exceptionally(costPerUseErrorHandler.handle(asyncResultHandler));
  }
}
