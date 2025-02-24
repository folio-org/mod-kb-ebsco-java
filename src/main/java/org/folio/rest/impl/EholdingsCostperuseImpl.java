package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import java.util.Map;
import javax.ws.rs.core.Response;
import org.folio.rest.annotations.Validate;
import org.folio.rest.aspect.HandleValidationErrors;
import org.folio.rest.jaxrs.model.Order;
import org.folio.rest.jaxrs.resource.EholdingsPackagesPackageIdCostperuse;
import org.folio.rest.jaxrs.resource.EholdingsPackagesPackageIdResourcesCostperuse;
import org.folio.rest.jaxrs.resource.EholdingsResourcesResourceIdCostperuse;
import org.folio.rest.jaxrs.resource.EholdingsTitlesTitleIdCostperuse;
import org.folio.rest.util.ErrorHandler;
import org.folio.service.uc.UcCostPerUseService;
import org.folio.spring.SpringContextUtil;
import org.springframework.beans.factory.annotation.Autowired;

@SuppressWarnings("java:S6813")
public class EholdingsCostperuseImpl
  implements EholdingsResourcesResourceIdCostperuse, EholdingsTitlesTitleIdCostperuse,
  EholdingsPackagesPackageIdCostperuse,
  EholdingsPackagesPackageIdResourcesCostperuse {

  @Autowired
  private UcCostPerUseService costPerUseService;
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

  @Override
  @Validate
  @HandleValidationErrors
  public void getEholdingsPackagesCostperuseByPackageId(String packageId, String platform, String fiscalYear,
                                                        Map<String, String> okapiHeaders,
                                                        Handler<AsyncResult<Response>> asyncResultHandler,
                                                        Context vertxContext) {
    costPerUseService.getPackageCostPerUse(packageId, platform, fiscalYear, okapiHeaders)
      .thenAccept(costPerUse ->
        asyncResultHandler.handle(succeededFuture(
          GetEholdingsPackagesCostperuseByPackageIdResponse.respond200WithApplicationVndApiJson(costPerUse)))
      )
      .exceptionally(costPerUseErrorHandler.handle(asyncResultHandler));
  }

  @Override
  @Validate
  @HandleValidationErrors
  public void getEholdingsPackagesResourcesCostperuseByPackageId(String packageId, String platform, String fiscalYear,
                                                                 Order order, String sort, int page, int count,
                                                                 Map<String, String> okapiHeaders,
                                                                 Handler<AsyncResult<Response>> asyncResultHandler,
                                                                 Context vertxContext) {
    costPerUseService.getPackageResourcesCostPerUse(packageId, platform, fiscalYear, sort, order, page, count,
        okapiHeaders)
      .thenAccept(costPerUseCollection ->
        asyncResultHandler.handle(succeededFuture(
          GetEholdingsPackagesResourcesCostperuseByPackageIdResponse
            .respond200WithApplicationVndApiJson(costPerUseCollection)))
      )
      .exceptionally(costPerUseErrorHandler.handle(asyncResultHandler));
  }
}
