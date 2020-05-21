package org.folio.repository.holdings.status.retry;

import java.util.concurrent.CompletableFuture;

public interface RetryStatusRepository {

  CompletableFuture<RetryStatus> findByCredentialsId(String credentialsId, String tenantId);

  CompletableFuture<Void> save(RetryStatus status, String credentialsId, String tenantId);

  CompletableFuture<Void> update(RetryStatus status, String credentialsId, String tenantId);

  CompletableFuture<Void> delete(String credentialsId, String tenantId);
}
