package org.folio.rest.impl;

import java.util.Map;

import javax.ws.rs.core.Response;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

import org.folio.rest.jaxrs.resource.EholdingsResourcesResourceIdCostperuse;
import org.folio.service.uc.UCCostPerUseService;
import org.folio.spring.SpringContextUtil;

public class EholdingsCostperuseImpl implements EholdingsResourcesResourceIdCostperuse {

  private UCCostPerUseService costPerUseService;

  private EholdingsCostperuseImpl() {
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
  }

  @Override
  public void getEholdingsResourcesCostperuseByResourceId(String resourceId, String platform, int fiscalYear,
                                                          Map<String, String> okapiHeaders,
                                                          Handler<AsyncResult<Response>> asyncResultHandler,
                                                          Context vertxContext) {
    costPerUseService.getResourceCostPerUse(resourceId, platform, fiscalYear, okapiHeaders)
  }
}
