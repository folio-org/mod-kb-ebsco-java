package org.folio.rest.impl;

import static org.folio.rest.util.RestConstants.OKAPI_TENANT_HEADER;

import java.util.Map;

import javax.ws.rs.core.Response;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import org.folio.rest.jaxrs.resource.LoadHoldings;
import org.folio.rest.service.holdings.HoldingsService;
import org.folio.rest.tools.utils.TenantTool;
import org.folio.rest.util.template.RMAPITemplate;
import org.folio.rest.util.template.RMAPITemplateFactory;
import org.folio.spring.SpringContextUtil;

public class LoadHoldingsImpl implements LoadHoldings {

  private static final Logger logger = LoggerFactory.getLogger(LoadHoldingsImpl.class);
  @Autowired
  private RMAPITemplateFactory templateFactory;
  @Autowired
  private HoldingsService holdingsService;


  public LoadHoldingsImpl() {
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
  }

  @Override
  public void postLoadHoldings(String contentType, Map<String, String> okapiHeaders,
                               Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    logger.info("Received signal to start scheduled loading of holdings");
    String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(OKAPI_TENANT_HEADER));
    RMAPITemplate template = templateFactory.createTemplate(okapiHeaders, asyncResultHandler);
    template.requestAction(context -> holdingsService.loadHoldings(context, tenantId));
    template.execute();
  }
}

