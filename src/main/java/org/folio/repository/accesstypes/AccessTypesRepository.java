package org.folio.repository.accesstypes;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface AccessTypesRepository {

  CompletableFuture<List<DbAccessType>> findByCredentialsId(String credentialsId, String tenantId);

  CompletableFuture<DbAccessType> save(DbAccessType accessType, String tenantId);

  CompletableFuture<Integer> count(String credentialsId, String tenantId);
}
