package org.folio.rest.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.serviceproxy.ServiceBinder;
import org.folio.rest.resource.interfaces.InitAPI;
import org.folio.service.holdings.HoldingConstants;
import org.folio.service.holdings.HoldingsService;
import org.folio.service.holdings.LoadServiceFacade;
import org.folio.spring.SpringContextUtil;
import org.folio.spring.config.ApplicationConfig;
import org.springframework.beans.factory.annotation.Autowired;

@SuppressWarnings({"java:S6813", "SpringJavaInjectionPointsAutowiringInspection"})
public class InitApiImpl implements InitAPI {
  @Autowired
  private HoldingsService holdingsService;
  @Autowired
  private LoadServiceFacade loadServiceFacade;

  @Override
  public void init(Vertx vertx, Context context, Handler<AsyncResult<Boolean>> handler) {
    vertx.executeBlocking(() -> {
      SpringContextUtil.init(vertx, context, ApplicationConfig.class);
      SpringContextUtil.autowireDependencies(this, context);
      new ServiceBinder(vertx)
        .setAddress(HoldingConstants.LOAD_FACADE_ADDRESS)
        .register(LoadServiceFacade.class, loadServiceFacade);
      new ServiceBinder(vertx)
        .setAddress(HoldingConstants.HOLDINGS_SERVICE_ADDRESS)
        .register(HoldingsService.class, holdingsService);
      return true;
    }).onComplete(handler);
  }
}
