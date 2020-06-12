package org.folio.repository.holdings.status;

import java.util.UUID;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.folio.rest.jaxrs.model.HoldingsLoadingStatus;

public interface HoldingsStatusRepository {

  CompletableFuture<List<HoldingsLoadingStatus>> findAll(String tenantId);

  CompletableFuture<HoldingsLoadingStatus> findByCredentialsId(UUID credentialsId, String tenantId);

  CompletableFuture<Void> save(HoldingsLoadingStatus status, UUID credentialsId, String tenantId);

  CompletableFuture<Void> update(HoldingsLoadingStatus status, UUID credentialsId, String tenantId);

  CompletableFuture<Void> delete(UUID credentialsId, String tenantId);

  CompletableFuture<HoldingsLoadingStatus> increaseImportedCount(int holdingsAmount, int pageAmount, UUID credentialsId, String tenantId);
}
