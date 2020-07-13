package org.folio.repository.users;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface UsersRepository {

  CompletableFuture<Optional<DbUser>> findById(String id, String tenant);

  CompletableFuture<DbUser> save(DbUser user, String tenant);

  CompletableFuture<Boolean> update(DbUser user, String tenant);

}
