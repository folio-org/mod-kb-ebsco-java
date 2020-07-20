package org.folio.repository.resources;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.folio.rest.model.filter.TagFilter;

public interface ResourceRepository {

  CompletableFuture<Void> save(DbResource resource, String tenantId);

  CompletableFuture<Void> delete(String resourceId, UUID credentialsId, String tenantId);

  CompletableFuture<List<DbResource>> findByTagFilter(TagFilter tagFilter, UUID credentialsId, String tenantId);
}
