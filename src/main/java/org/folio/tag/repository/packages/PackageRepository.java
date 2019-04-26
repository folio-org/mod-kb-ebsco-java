package org.folio.tag.repository.packages;

import java.util.concurrent.CompletableFuture;

import org.folio.holdingsiq.model.PackageByIdData;
import org.folio.holdingsiq.model.PackageId;

public interface PackageRepository {
  CompletableFuture<Void> savePackage(PackageByIdData packageData, String tenantId);

  CompletableFuture<Void> deletePackage(PackageId packageId, String tenantId);
}
