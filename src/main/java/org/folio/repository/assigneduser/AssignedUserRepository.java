package org.folio.repository.assigneduser;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface AssignedUserRepository {

  CompletableFuture<Collection<DbAssignedUser>> findByCredentialsId(String credentialsId, String tenant);

  CompletableFuture<Optional<DbAssignedUser>> findByCredentialsIdAndUserId(String credentialsId, String userId,
                                                                           String tenant);
}
