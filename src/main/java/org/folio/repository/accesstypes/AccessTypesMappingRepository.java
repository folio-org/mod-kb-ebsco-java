package org.folio.repository.accesstypes;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.folio.repository.RecordType;

public interface AccessTypesMappingRepository {

  CompletableFuture<AccessTypeMapping> save(AccessTypeMapping accessTypeMapping, String tenantId);

  CompletableFuture<Optional<AccessTypeMapping>> findByRecord(String recordId, RecordType recordType, String tenantId);

  CompletableFuture<Void> deleteByRecord(String recordId, RecordType recordType, String tenantId);
}
