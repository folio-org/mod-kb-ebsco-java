package org.folio.repository.accesstypes;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.folio.repository.RecordType;

public interface AccessTypesMappingRepository {

  CompletableFuture<List<AccessTypeMapping>> findAll(String tenantId);

  CompletableFuture<Optional<AccessTypeMapping>> findByRecord(String recordId, RecordType recordType, String tenantId);

  CompletableFuture<Boolean> saveMapping(String accessTypeId, String recordId, RecordType recordType, String tenantId);

  CompletableFuture<Boolean> saveMapping(AccessTypeMapping accessTypeMapping, String tenantId);
}
