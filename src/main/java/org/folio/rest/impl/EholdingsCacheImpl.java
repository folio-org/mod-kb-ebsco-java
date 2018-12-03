package org.folio.rest.impl;

import java.util.Map;

import javax.ws.rs.core.Response;

import org.folio.config.cache.RMAPIConfigurationCache;
import org.folio.rest.jaxrs.resource.EholdingsCache;
import org.folio.rest.util.RestConstants;
import org.folio.rest.validator.HeaderValidator;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;

public class EholdingsCacheImpl implements EholdingsCache {
  private HeaderValidator headerValidator;

  public EholdingsCacheImpl() {
    this(new HeaderValidator());
  }

  public EholdingsCacheImpl(HeaderValidator headerValidator) {
    this.headerValidator = headerValidator;
  }

  @Override
  public void deleteEholdingsCache(Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    headerValidator.validate(okapiHeaders);
    RMAPIConfigurationCache.getInstance().invalidate(okapiHeaders.get(RestConstants.OKAPI_TENANT_HEADER));
    asyncResultHandler.handle(Future.succeededFuture(EholdingsCacheImpl.DeleteEholdingsCacheResponse.respond204()));
  }
}
