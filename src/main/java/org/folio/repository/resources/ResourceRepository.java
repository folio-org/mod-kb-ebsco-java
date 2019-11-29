package org.folio.repository.resources;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface ResourceRepository {

  CompletableFuture<Void> save(String resourceId, String name, String tenantId);

  CompletableFuture<Void> delete(String resourceId, String tenantId);

  CompletableFuture<List<ResourceInfoInDB>> findByTagNameAndPackageId(List<String> tags, String packageId, int page, int count, String tenantId);
}
