package org.folio.repository.holdings.status;

import java.util.concurrent.CompletableFuture;

import org.folio.rest.jaxrs.model.HoldingsLoadingStatus;

public interface HoldingsStatusRepository {

  CompletableFuture<HoldingsLoadingStatus> findByCredentialsId(String credentialsId, String tenantId);

  CompletableFuture<Void> save(HoldingsLoadingStatus status, String credentialsId, String tenantId);

  CompletableFuture<Void> update(HoldingsLoadingStatus status, String credentialsId, String tenantId);

  CompletableFuture<Void> delete(String credentialsId, String tenantId);

  CompletableFuture<HoldingsLoadingStatus> increaseImportedCount(int holdingsAmount, int pageAmount, String credentialsId, String tenantId);
}
