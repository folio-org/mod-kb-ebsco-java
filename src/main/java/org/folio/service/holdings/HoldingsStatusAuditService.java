package org.folio.service.holdings;

import java.util.concurrent.CompletableFuture;

public interface HoldingsStatusAuditService {

  CompletableFuture<Void> clearExpiredRecords(String credentialsId, String tenantId);
}
