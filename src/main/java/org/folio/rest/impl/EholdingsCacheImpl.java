package org.folio.rest.impl;

import java.util.Map;

import javax.ws.rs.core.Response;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import org.folio.cache.VertxCache;
import org.folio.holdingsiq.model.Configuration;
import org.folio.okapi.common.XOkapiHeaders;
import org.folio.rest.jaxrs.resource.EholdingsCache;
import org.folio.rest.validator.HeaderValidator;
import org.folio.spring.SpringContextUtil;

public class EholdingsCacheImpl implements EholdingsCache {

  @Autowired
  private HeaderValidator headerValidator;
  @Autowired
  @Qualifier("rmApiConfigurationCache")
  private VertxCache<String, Configuration> rmapiConfigurationCache;

  public EholdingsCacheImpl() {
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
  }

  @Override
  public void deleteEholdingsCache(Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    headerValidator.validate(okapiHeaders);
    rmapiConfigurationCache.invalidate(okapiHeaders.get(XOkapiHeaders.TENANT));
    asyncResultHandler.handle(Future.succeededFuture(EholdingsCache.DeleteEholdingsCacheResponse.respond204()));
  }
}
