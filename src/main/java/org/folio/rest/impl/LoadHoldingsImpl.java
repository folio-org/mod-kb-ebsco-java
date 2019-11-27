package org.folio.rest.impl;


import java.util.Map;

import javax.ws.rs.core.Response;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;

import org.folio.repository.holdings.status.HoldingsStatusRepository;
import org.folio.rest.jaxrs.model.HoldingsLoadingStatus;
import org.folio.rest.jaxrs.resource.LoadHoldings;
import org.folio.rest.tools.utils.TenantTool;
import org.folio.rest.util.ErrorUtil;
import org.folio.rest.util.template.RMAPITemplate;
import org.folio.rest.util.template.RMAPITemplateFactory;
import org.folio.service.holdings.HoldingsService;
import org.folio.service.holdings.HoldingsStatusAuditService;
import org.folio.service.holdings.exception.ProcessInProgressException;
import org.folio.spring.SpringContextUtil;

public class LoadHoldingsImpl implements LoadHoldings {

  private static final Logger logger = LoggerFactory.getLogger(LoadHoldingsImpl.class);
  @Autowired
  private RMAPITemplateFactory templateFactory;
  @Autowired
  private HoldingsService holdingsService;
  @Autowired
  private HoldingsStatusAuditService holdingsStatusAuditService;
  @Autowired
  private HoldingsStatusRepository holdingsStatusRepository;


  public LoadHoldingsImpl() {
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
  }

  @Override
  public void postLoadHoldings(String contentType, Map<String, String> okapiHeaders,
                               Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    logger.info("Received signal to start scheduled loading of holdings");
    RMAPITemplate template = templateFactory.createTemplate(okapiHeaders, asyncResultHandler);
    template.requestAction(context ->
        holdingsStatusAuditService.clearExpiredRecords(context.getOkapiData().getTenant()).thenCompose(o ->
          holdingsService.loadHoldings(context)
        )
    )
      .addErrorMapper(ProcessInProgressException.class,
        e -> PostLoadHoldingsResponse.respond409WithTextPlain(ErrorUtil.createError(e.getMessage())))
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

