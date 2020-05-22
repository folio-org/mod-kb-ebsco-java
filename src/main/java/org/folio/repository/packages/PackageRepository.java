package org.folio.repository.packages;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.folio.holdingsiq.model.PackageId;

public interface PackageRepository {
  CompletableFuture<Void> save(DbPackage packageData, String tenantId);

  CompletableFuture<Void> delete(PackageId packageId, String credentialsId, String tenantId);

  CompletableFuture<List<DbPackage>> findByTagName(List<String> tags, int page, int count, String credentialsId,
                                                         String tenantId);

  CompletableFuture<List<DbPackage>> findByTagNameAndProvider(List<String> tags, String providerId, int page,
                                                                    int count, String credentialsId, String tenantId);

  CompletableFuture<List<DbPackage>> findByIds(List<PackageId> packageIds, String credentialsId,
                                                       String tenantId);
}
