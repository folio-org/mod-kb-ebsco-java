package org.folio.repository.resources;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.folio.holdingsiq.model.Title;

public interface ResourceRepository {

  CompletableFuture<Void> saveResource(String resourceId, Title title, String tenantId);

  CompletableFuture<Void> deleteResource(String resourceId, String tenantId);

  CompletableFuture<List<ResourceInfoInDB>> getResourcesByTagNameAndPackageId(List<String> tags, String packageId, int page, int count, String tenantId);
}
