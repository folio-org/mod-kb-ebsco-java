package org.folio.repository.holdings;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.folio.holdingsiq.model.Holding;

public interface HoldingsRepository {

  CompletableFuture<Void> saveHolding(List<Holding> holding, String tenantId);

  CompletableFuture<Void> removeHoldings(String tenantId);
}
