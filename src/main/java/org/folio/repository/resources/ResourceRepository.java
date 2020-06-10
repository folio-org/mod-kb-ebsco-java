package org.folio.repository.resources;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface ResourceRepository {

  CompletableFuture<Void> save(DbResource resource, String tenantId);

  CompletableFuture<Void> delete(String resourceId, UUID credentialsId, String tenantId);

  CompletableFuture<List<DbResource>> findByTagNameAndPackageId(List<String> tags, String packageId, int page,
                                                                int count, UUID credentialsId, String tenantId);
}
