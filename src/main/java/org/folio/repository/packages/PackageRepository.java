package org.folio.repository.packages;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.folio.holdingsiq.model.PackageId;
import org.folio.rest.model.filter.TagFilter;

public interface PackageRepository {

  CompletableFuture<Void> save(DbPackage packageData, String tenantId);

  CompletableFuture<Void> delete(PackageId packageId, UUID credentialsId, String tenantId);

  CompletableFuture<List<DbPackage>> findByTagFilter(TagFilter tagFilter, UUID credentialsId, String tenantId);

  CompletableFuture<List<DbPackage>> findByIds(List<PackageId> packageIds, UUID credentialsId, String tenantId);
}
