package org.folio.repository.providers;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.folio.rest.model.filter.TagFilter;

public interface ProviderRepository {

  CompletableFuture<Void> save(DbProvider provider, String tenantId);

  CompletableFuture<Void> delete(String vendorId, UUID credentialsId, String tenantId);

  CompletableFuture<List<Long>> findIdsByTagFilter(TagFilter tagFilter, UUID credentialsId, String tenantId);
}
