package org.folio.repository.holdings.transaction;

import java.util.concurrent.CompletableFuture;

public interface TransactionIdRepository {
  CompletableFuture<Void> save(String credentialsId, String transactionId, String tenantId);
  CompletableFuture<String> getLastTransactionId(String credentialsId, String tenantId);
}
