package org.folio.rest.impl;

import static org.folio.repository.holdings.status.HoldingsLoadingStatusFactory.getStatusStarted;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import javax.ws.rs.core.Response;

import org.folio.repository.holdings.status.HoldingsStatusRepository;
import org.folio.rest.jaxrs.model.HoldingsLoadingStatus;
import org.folio.rest.jaxrs.resource.LoadHoldings;
import org.folio.rest.tools.utils.TenantTool;
import org.folio.rest.util.template.RMAPITemplate;
import org.folio.rest.util.template.RMAPITemplateFactory;
import org.folio.service.holdings.HoldingsService;
import org.folio.spring.SpringContextUtil;
import org.springframework.beans.factory.annotation.Autowired;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class LoadHoldingsImpl implements LoadHoldings {

  private static final Logger logger = LoggerFactory.getLogger(LoadHoldingsImpl.class);
  @Autowired
  private RMAPITemplateFactory templateFactory;
  @Autowired
  private HoldingsService holdingsService;
  @Autowired
  private HoldingsStatusRepository holdingsStatusRepository;


  public LoadHoldingsImpl() {
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
  }

  @Override
  public void postLoadHoldings(String contentType, Map<String, String> okapiHeaders,
                               Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    logger.info("Received signal to start scheduled loading of holdings");
    String tenantId = TenantTool.tenantId(okapiHeaders);
    holdingsStatusRepository.update(getStatusStarted(), tenantId);
    RMAPITemplate template = templateFactory.createTemplate(okapiHeaders, asyncResultHandler);
    template.requestAction(context -> {
      holdingsService.loadHoldings(context);
      return CompletableFuture.completedFuture(null);
    })
      .execute();
  }

  @Override
  public void getLoadHoldingsStatus(String contentType, Map<String, String> okapiHeaders,
                                    Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    logger.info("Getting holdings loading status");
    String tenantId = TenantTool.tenantId(okapiHeaders);
    RMAPITemplate template = templateFactory.createTemplate(okapiHeaders, asyncResultHandler);
    template
      .requestAction(context -> holdingsStatusRepository.get(tenantId))
      .executeWithResult(HoldingsLoadingStatus.class);
  }
}

