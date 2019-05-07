package org.folio.tag.repository.providers;

import org.folio.holdingsiq.model.VendorById;

import java.util.concurrent.CompletableFuture;

public interface ProviderRepository {

  CompletableFuture<Void> saveProvider(VendorById vendorById, String tenantId);

  CompletableFuture<Void> deleteProvider(String vendorId, String tenantId);
}
