package org.folio.repository.holdings.status.retry;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface RetryStatusRepository {

  CompletableFuture<RetryStatus> findByCredentialsId(UUID credentialsId, String tenantId);

  CompletableFuture<Void> save(RetryStatus status, UUID credentialsId, String tenantId);

  CompletableFuture<Void> update(RetryStatus status, UUID credentialsId, String tenantId);

  CompletableFuture<Void> delete(UUID credentialsId, String tenantId);
}
