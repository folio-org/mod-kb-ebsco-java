package org.folio.repository.packages;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.folio.holdingsiq.model.PackageId;

public interface PackageRepository {
  CompletableFuture<Void> save(PackageInfoInDB packageData, String tenantId);

  CompletableFuture<Void> delete(PackageId packageId, String tenantId);

  CompletableFuture<List<PackageInfoInDB>> findByTagName(List<String> tags, int page, int count, String tenantId);

  CompletableFuture<List<PackageInfoInDB>> findByTagNameAndProvider(List<String> tags, String providerId, int page, int count, String tenantId);

  CompletableFuture<List<PackageInfoInDB>> findAllById(List<PackageId> packageIds, String tenantId);
}
