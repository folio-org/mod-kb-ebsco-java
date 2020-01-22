package org.folio.repository.holdings.status;

import java.util.concurrent.CompletableFuture;

public interface TransactionIdRepository {
  CompletableFuture<Void> save(String transactionId, String tenantId);
  CompletableFuture<String> getLastTransactionId(String tenantId);
}
