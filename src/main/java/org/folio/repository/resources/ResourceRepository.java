package org.folio.repository.resources;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface ResourceRepository {

  CompletableFuture<Void> save(DbResource resource, String tenantId);

  CompletableFuture<Void> delete(String resourceId, String credentialsId, String tenantId);

  CompletableFuture<List<DbResource>> findByTagNameAndPackageId(List<String> tags, String packageId, int page,
    int count, String credentialsId, String tenantId);
}
