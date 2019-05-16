package org.folio.tag.repository.packages;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.folio.holdingsiq.model.PackageByIdData;
import org.folio.holdingsiq.model.PackageId;

public interface PackageRepository {
  CompletableFuture<Void> savePackage(PackageByIdData packageData, String tenantId);

  CompletableFuture<Void> deletePackage(PackageId packageId, String tenantId);

  CompletableFuture<List<PackageId>> getPackageIdsByTagName(List<String> tags, int page, int count, String tenantId);

  CompletionStage<List<PackageId>> getPackageIdsByTagNameAndProvider(List<String> tags, String providerId, int page, int count, String tenant);
}
