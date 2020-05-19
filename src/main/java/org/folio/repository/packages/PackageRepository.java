package org.folio.repository.packages;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.folio.holdingsiq.model.PackageId;

public interface PackageRepository {
  CompletableFuture<Void> save(PackageInfoInDB packageData, String credentialsId, String tenantId);

  CompletableFuture<Void> delete(PackageId packageId, String credentialsId, String tenantId);

  CompletableFuture<List<PackageInfoInDB>> findByTagName(List<String> tags, int page, int count, String credentialsId,
                                                         String tenantId);

  CompletableFuture<List<PackageInfoInDB>> findByTagNameAndProvider(List<String> tags, String providerId, int page,
                                                                    int count, String credentialsId, String tenantId);

  CompletableFuture<List<PackageInfoInDB>> findByIds(List<PackageId> packageIds, String credentialsId,
                                                       String tenantId);
}
