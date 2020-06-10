package org.folio.service.holdings;

import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.core.Vertx;

import org.folio.service.holdings.message.ConfigurationMessage;
import org.folio.service.holdings.message.LoadHoldingsMessage;

@ProxyGen
public interface LoadServiceFacade {

  static LoadServiceFacade createProxy(Vertx vertx, String address) {
    return new LoadServiceFacadeVertxEBProxy(vertx, address);
  }

  void createSnapshot(ConfigurationMessage configuration);

  void loadHoldings(LoadHoldingsMessage configuration);
}
