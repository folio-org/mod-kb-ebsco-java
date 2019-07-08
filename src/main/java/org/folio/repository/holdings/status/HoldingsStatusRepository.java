package org.folio.repository.holdings.status;

import java.util.concurrent.CompletableFuture;

import org.folio.rest.jaxrs.model.HoldingsLoadingStatus;

public interface HoldingsStatusRepository {

  CompletableFuture<HoldingsLoadingStatus> get(String tenantId);

  CompletableFuture<Void> save(HoldingsLoadingStatus status, String tenantId);

  CompletableFuture<Void> update(HoldingsLoadingStatus status, String tenantId);

  CompletableFuture<Void> delete(String tenantId);
}
