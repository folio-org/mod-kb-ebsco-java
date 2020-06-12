package org.folio.repository.accesstypes;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.folio.repository.RecordType;

public interface AccessTypesRepository {

  CompletableFuture<List<DbAccessType>> findByCredentialsId(UUID credentialsId, String tenantId);

  CompletableFuture<Optional<DbAccessType>> findByCredentialsAndAccessTypeId(UUID credentialsId, UUID accessTypeId,
                                                                             String tenantId);

  CompletableFuture<List<DbAccessType>> findByCredentialsAndNames(UUID credentialsId, Collection<String> accessTypeNames,
                                                                  String tenantId);

  CompletableFuture<Optional<DbAccessType>> findByCredentialsAndRecord(UUID credentialsId, String recordId,
                                                                       RecordType recordType, String tenantId);

  CompletableFuture<DbAccessType> save(DbAccessType accessType, String tenantId);

  CompletableFuture<Integer> count(UUID credentialsId, String tenantId);

  CompletableFuture<Void> delete(UUID credentialsId, UUID accessTypeId, String tenantId);
}
