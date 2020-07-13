package org.folio.repository.assigneduser;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface AssignedUserRepository {

  CompletableFuture<Collection<DbAssignedUser>> findByCredentialsId(UUID credentialsId, String tenant);

  CompletableFuture<Integer> count(UUID credentialsId, String tenant);

  CompletableFuture<DbAssignedUser> save(DbAssignedUser entity, String tenant);

  CompletableFuture<Void> delete(UUID credentialsId, UUID userId, String tenant);
}
