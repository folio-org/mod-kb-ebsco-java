package org.folio.rest.impl;

import java.util.Map;

import javax.ws.rs.core.Response;

import org.folio.config.cache.RMAPIConfigurationCache;
import org.folio.rest.jaxrs.resource.EholdingsCache;
import org.folio.rest.util.RestConstants;
import org.folio.rest.validator.HeaderValidator;
import org.folio.spring.SpringContextUtil;
import org.springframework.beans.factory.annotation.Autowired;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

public class EholdingsCacheImpl implements EholdingsCache {
  @Autowired
  private HeaderValidator headerValidator;
  @Autowired
  private RMAPIConfigurationCache rmapiConfigurationCache;

  @SuppressWarnings("squid:S1172")
  public EholdingsCacheImpl(Vertx vertx, String tenantId) {
    SpringContextUtil.autowireDependencies(this, vertx.getOrCreateContext());
  }

  @Override
  public void deleteEholdingsCache(Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    headerValidator.validate(okapiHeaders);
    rmapiConfigurationCache.invalidate(okapiHeaders.get(RestConstants.OKAPI_TENANT_HEADER));
    asyncResultHandler.handle(Future.succeededFuture(EholdingsCacheImpl.DeleteEholdingsCacheResponse.respond204()));
  }
}
