package org.folio.repository.users;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface UsersRepository {

  CompletableFuture<Optional<DbUser>> findById(UUID id, String tenant);

  CompletableFuture<DbUser> save(DbUser user, String tenant);

  CompletableFuture<Boolean> update(DbUser user, String tenant);

}
