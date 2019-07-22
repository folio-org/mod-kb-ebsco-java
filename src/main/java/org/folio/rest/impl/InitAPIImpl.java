package org.folio.rest.impl;

import org.folio.rest.resource.interfaces.InitAPI;
import org.folio.service.holdings.HoldingConstants;
import org.folio.service.holdings.HoldingsService;
import org.folio.service.holdings.LoadServiceFacade;
import org.folio.spring.SpringContextUtil;
import org.folio.spring.config.ApplicationConfig;
import org.springframework.beans.factory.annotation.Autowired;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.serviceproxy.ServiceBinder;

public class InitAPIImpl implements InitAPI{
  @Autowired
  private HoldingsService holdingsService;
  @Autowired
  private LoadServiceFacade loadServiceFacade;
  @Override
  public void init(Vertx vertx, Context context, Handler<AsyncResult<Boolean>> handler) {
    vertx.executeBlocking(
      future -> {
        SpringContextUtil.init(vertx, context, ApplicationConfig.class);
        SpringContextUtil.autowireDependencies(this, context);
        new ServiceBinder(vertx)
          .setAddress(HoldingConstants.LOAD_FACADE_ADDRESS)
          .register(LoadServiceFacade.class, loadServiceFacade);
        new ServiceBinder(vertx)
          .setAddress(HoldingConstants.HOLDINGS_SERVICE_ADDRESS)
          .register(HoldingsService.class, holdingsService);
        future.complete();
      },
      result -> {
        if (result.succeeded()) {
          handler.handle(Future.succeededFuture(true));
        } else {
          handler.handle(Future.failedFuture(result.cause()));
        }
      });
  }
}
