package org.folio.repository.accesstypes;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.folio.repository.RecordType;
import org.folio.rest.model.filter.AccessTypeFilter;

public interface AccessTypeMappingsRepository {

  CompletableFuture<Optional<AccessTypeMapping>> findByRecord(String recordId, RecordType recordType,
                                                              UUID credentialsId,
                                                              String tenantId);

  CompletableFuture<Collection<AccessTypeMapping>> findByAccessTypeFilter(AccessTypeFilter accessTypeFilter,
                                                                          String tenantId);

  CompletableFuture<AccessTypeMapping> save(AccessTypeMapping accessTypeMapping, String tenantId);

  CompletableFuture<Void> deleteByRecord(String recordId, RecordType recordType, UUID credentialsId, String tenantId);

  CompletableFuture<Map<UUID, Integer>> countByRecordIdPrefix(String recordIdPrefix, RecordType recordType,
                                                              UUID credentialsId, String tenantId);
}
