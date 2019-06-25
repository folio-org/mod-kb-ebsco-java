package org.folio.repository.holdings;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public interface HoldingsRepository {

  CompletableFuture<Void> saveAll(Set<DbHolding> holding, Instant updatedAt, String tenantId);

  CompletableFuture<Void> deleteByTimeStamp(Instant timeStamp, String tenantId);

  CompletableFuture<List<DbHolding>> findAllById(List<String> resourceIds, String tenantId);
}
