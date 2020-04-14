package org.folio.repository.assigneduser;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public interface AssignedUserRepository {

  CompletableFuture<Collection<DbAssignedUser>> findByCredentialsId(String credentialsId, String tenant);

  CompletableFuture<DbAssignedUser> save(DbAssignedUser entity, String tenant);
}
