package org.folio.repository.accesstypes;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.folio.repository.RecordType;

public interface AccessTypesRepository {

  CompletableFuture<List<DbAccessType>> findByCredentialsId(String credentialsId, String tenantId);

  CompletableFuture<Optional<DbAccessType>> findByCredentialsAndAccessTypeId(String credentialsId, String accessTypeId,
                                                                             String tenantId);

  CompletableFuture<List<DbAccessType>> findByCredentialsAndNames(String credentialsId, Collection<String> accessTypeNames,
                                                                  String tenantId);

  CompletableFuture<Optional<DbAccessType>> findByCredentialsAndRecord(String credentialsId, String recordId,
                                                                       RecordType recordType, String tenantId);

  CompletableFuture<DbAccessType> save(DbAccessType accessType, String tenantId);

  CompletableFuture<Integer> count(String credentialsId, String tenantId);

  CompletableFuture<Void> delete(String credentialsId, String accessTypeId, String tenantId);
}
