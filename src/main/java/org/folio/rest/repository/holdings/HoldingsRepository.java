package org.folio.rest.repository.holdings;

import java.util.concurrent.CompletableFuture;

import org.folio.holdingsiq.model.Holding;

public interface HoldingsRepository {

  CompletableFuture<Void> saveHolding(Holding holding, String tenantId);

}
