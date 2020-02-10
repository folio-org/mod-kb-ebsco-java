package org.folio.repository.accesstypes;

import java.util.concurrent.CompletableFuture;

public interface AccessTypesMappingRepository {

  CompletableFuture<Boolean> save(AccessTypeMapping accessTypeMapping, String tenantId);
}
