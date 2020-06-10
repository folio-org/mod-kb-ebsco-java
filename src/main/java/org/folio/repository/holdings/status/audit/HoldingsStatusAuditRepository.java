package org.folio.repository.holdings.status.audit;

import java.time.OffsetDateTime;
import java.util.concurrent.CompletableFuture;

public interface HoldingsStatusAuditRepository {

  CompletableFuture<Void> deleteBeforeTimestamp(OffsetDateTime timestamp, String credentialsId, String tenantId);
}
