package org.folio.tag.repository.providers;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.folio.holdingsiq.model.VendorById;

public interface ProviderRepository {

  CompletableFuture<Void> saveProvider(VendorById vendorById, String tenantId);

  CompletableFuture<Void> deleteProvider(String vendorId, String tenantId);

   CompletableFuture<List<Long>> getProviderIdsByTagName(List<String> tags, int page, int count, String tenantId);
}
