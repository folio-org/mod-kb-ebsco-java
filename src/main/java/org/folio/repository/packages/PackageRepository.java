package org.folio.repository.packages;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.folio.holdingsiq.model.PackageByIdData;
import org.folio.holdingsiq.model.PackageId;

public interface PackageRepository {
  CompletableFuture<Void> savePackage(PackageByIdData packageData, String tenantId);

  CompletableFuture<Void> deletePackage(PackageId packageId, String tenantId);

  CompletableFuture<List<DbPackage>> getPackagesByTagName(List<String> tags, int page, int count, String tenantId);

  CompletableFuture<List<DbPackage>> getPackagesByTagNameAndProvider(List<String> tags, String providerId, int page, int count, String tenantId);

  CompletableFuture<List<DbPackage>> getPackagesByIds(List<PackageId> packageIds, String tenantId);
}
