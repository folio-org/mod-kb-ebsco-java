package org.folio.repository.holdings;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface HoldingsRepository {

  CompletableFuture<Void> saveHoldings(List<DbHolding> holding, String tenantId);

  CompletableFuture<Void> removeHoldings(String tenantId);

  CompletableFuture<List<DbHolding>> getHoldingsByIds(String tenantId, List<String> resourceIds);
}
