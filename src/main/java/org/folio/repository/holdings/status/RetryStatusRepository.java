package org.folio.repository.holdings.status;

import java.util.concurrent.CompletableFuture;

public interface RetryStatusRepository {
  CompletableFuture<RetryStatus> get(String tenantId);

  CompletableFuture<Void> save(RetryStatus status, String tenantId);

  CompletableFuture<Void> update(RetryStatus retryStatus, String tenantId);

  CompletableFuture<Void> delete(String tenantId);
}
