package org.folio.repository.holdings.status;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

public interface HoldingsStatusAuditRepository {
  CompletableFuture<Void> deleteBeforeTimestamp(Instant timestamp, String tenantId);
}
