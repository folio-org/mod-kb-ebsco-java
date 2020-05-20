package org.folio.repository.providers;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface ProviderRepository {

  CompletableFuture<Void> save(ProviderInfoInDb provider, String tenantId);

  CompletableFuture<Void> delete(String vendorId, String credentialsId, String tenantId);

  CompletableFuture<List<Long>> findIdsByTagName(List<String> tags, int page, int count,
      String credentialsId, String tenantId);
}
