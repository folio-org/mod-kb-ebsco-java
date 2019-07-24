package org.folio.service.holdings;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.folio.repository.holdings.HoldingInfoInDB;
import org.folio.repository.resources.ResourceInfoInDB;
import org.folio.rest.util.template.RMAPITemplateContext;
import org.folio.service.holdings.message.LoadFailedMessage;
import org.folio.service.holdings.message.SnapshotCreatedMessage;
import org.folio.service.holdings.message.SnapshotFailedMessage;

import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Vertx;

@ProxyGen
@VertxGen
public interface HoldingsService {
  @GenIgnore
  static HoldingsService createProxy(Vertx vertx, String address) {
    return new HoldingsServiceVertxEBProxy(vertx, address);
  }

  @GenIgnore
  default void loadHoldings(RMAPITemplateContext context) {
    //Default implementation is necessary for automatically generated vertx proxy
    throw new UnsupportedOperationException();
  }

  @GenIgnore
  default CompletableFuture<List<HoldingInfoInDB>> getHoldingsByIds(List<ResourceInfoInDB> resourcesResult, String tenant) {
    throw new UnsupportedOperationException();
  }

  void saveHolding(HoldingsMessage holdings);

  void snapshotCreated(SnapshotCreatedMessage message);

  void snapshotFailed(SnapshotFailedMessage message);

  void loadingFailed(LoadFailedMessage message);
}
