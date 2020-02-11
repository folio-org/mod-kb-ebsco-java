package org.folio.repository.accesstypes;

import java.util.concurrent.CompletableFuture;

import org.folio.repository.RecordType;

public interface AccessTypesMappingRepository {

  CompletableFuture<Boolean> save(AccessTypeInDb accessTypeMapping, String tenantId);

  CompletableFuture<AccessTypeInDb> findByRecordIdAndRecordType(String recordId, RecordType recordType, String tenantId);
}
