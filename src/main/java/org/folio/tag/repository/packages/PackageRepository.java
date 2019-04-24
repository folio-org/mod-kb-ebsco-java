package org.folio.tag.repository.packages;

import java.util.concurrent.CompletableFuture;

import org.folio.holdingsiq.model.PackageByIdData;

public interface PackageRepository {
  CompletableFuture<Void> savePackage(PackageByIdData packageData, String tenantId);
}
