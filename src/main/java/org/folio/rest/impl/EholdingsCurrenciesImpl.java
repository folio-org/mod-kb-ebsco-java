package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;
import static org.folio.rest.jaxrs.resource.EholdingsCurrencies.GetEholdingsCurrenciesResponse.respond200WithApplicationVndApiJson;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import java.util.Map;
import javax.ws.rs.core.Response;
import org.folio.rest.jaxrs.resource.EholdingsCurrencies;
import org.folio.rest.util.ErrorHandler;
import org.folio.service.currencies.CurrenciesService;
import org.folio.spring.SpringContextUtil;
import org.springframework.beans.factory.annotation.Autowired;

public class EholdingsCurrenciesImpl implements EholdingsCurrencies {

  @Autowired
  private CurrenciesService currenciesService;
  @Autowired
  private ErrorHandler errorHandler;

  public EholdingsCurrenciesImpl() {
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
  }

  @Override
  public void getEholdingsCurrencies(Map<String, String> okapiHeaders,
                                     Handler<AsyncResult<Response>> asyncResultHandler,
                                     Context vertxContext) {
    currenciesService.fetchCurrencyCollection(okapiHeaders)
      .thenAccept(currenciesCollection ->
        asyncResultHandler.handle(succeededFuture(respond200WithApplicationVndApiJson(currenciesCollection)))
      )
      .exceptionally(errorHandler.handle(asyncResultHandler));
  }
}
