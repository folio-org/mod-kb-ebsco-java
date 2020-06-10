package org.folio.repository.providers;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface ProviderRepository {

  CompletableFuture<Void> save(DbProvider provider, String tenantId);

  CompletableFuture<Void> delete(String vendorId, UUID credentialsId, String tenantId);

  CompletableFuture<List<Long>> findIdsByTagName(List<String> tags, int page, int count, UUID credentialsId,
                                                 String tenantId);
}
