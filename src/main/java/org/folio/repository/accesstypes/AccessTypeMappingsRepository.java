package org.folio.repository.accesstypes;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.folio.repository.RecordType;

public interface AccessTypeMappingsRepository {

  CompletableFuture<Optional<AccessTypeMapping>> findByRecord(String recordId, RecordType recordType, String tenantId);

  CompletableFuture<Collection<AccessTypeMapping>> findByAccessTypeId(String accessTypeId, String tenantId);

  CompletableFuture<Collection<AccessTypeMapping>> findByAccessTypeIds(Collection<String> accessTypeIds,
                                                                       RecordType recordType, int page, int count,
                                                                       String tenantId);

  CompletableFuture<AccessTypeMapping> save(AccessTypeMapping accessTypeMapping, String tenantId);

  CompletableFuture<Void> deleteByRecord(String recordId, RecordType recordType, String tenantId);

  CompletableFuture<Map<String, Integer>> countRecordsByAccessType(String tenantId);
}
