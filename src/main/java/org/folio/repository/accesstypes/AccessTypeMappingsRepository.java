package org.folio.repository.accesstypes;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.folio.repository.RecordType;

public interface AccessTypeMappingsRepository {

  CompletableFuture<Collection<AccessTypeMapping>> findAll(String tenantId);

  CompletableFuture<Optional<AccessTypeMapping>> findByRecord(String recordId, RecordType recordType, String tenantId);

  CompletableFuture<Collection<AccessTypeMapping>> findByAccessTypeId(String accessTypeId, String tenantId);

  CompletableFuture<AccessTypeMapping> save(AccessTypeMapping accessTypeMapping, String tenantId);

  CompletableFuture<Void> deleteByRecord(String recordId, RecordType recordType, String tenantId);
}
