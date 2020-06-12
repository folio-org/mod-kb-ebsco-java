package org.folio.repository.holdings.transaction;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface TransactionIdRepository {

  CompletableFuture<Void> save(UUID credentialsId, String transactionId, String tenantId);

  CompletableFuture<String> getLastTransactionId(UUID credentialsId, String tenantId);
}
