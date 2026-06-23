package org.folio.repository.holdings;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface HoldingsRepository {

  CompletableFuture<Void> saveAll(Set<DbHoldingInfo> holding, OffsetDateTime updatedAt, UUID credentialsId,
                                  String tenantId);

  CompletableFuture<Void> deleteBeforeTimestamp(OffsetDateTime timestamp, UUID credentialsId, String tenantId);

  CompletableFuture<Void> deleteAll(Set<HoldingsId> holdings, UUID credentialsId, String tenantId);
}
