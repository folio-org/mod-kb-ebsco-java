package org.folio.repository.providers;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.folio.holdingsiq.model.VendorById;
import org.folio.rest.jaxrs.model.Tags;

public interface ProviderRepository {

  CompletableFuture<Void> saveProvider(ProviderInfoInDb provider, Tags tags, String tenantId);

  CompletableFuture<Void> deleteProvider(String vendorId, String tenantId);

   CompletableFuture<List<Long>> getProviderIdsByTagName(List<String> tags, int page, int count, String tenantId);
}
