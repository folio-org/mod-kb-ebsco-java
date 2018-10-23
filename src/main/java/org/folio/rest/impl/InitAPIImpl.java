package org.folio.rest.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import org.folio.properties.PropertyConfiguration;
import org.folio.rest.resource.interfaces.InitAPI;

public class InitAPIImpl implements InitAPI{
  @Override
  public void init(Vertx vertx, Context context, Handler<AsyncResult<Boolean>> handler) {
    try {
      //call getInstance to initialize configuration
      PropertyConfiguration.getInstance();
      handler.handle(Future.succeededFuture(true));
    }
    catch (RuntimeException ex){
      handler.handle(Future.failedFuture(ex));
    }
  }
}
