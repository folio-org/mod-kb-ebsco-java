package org.folio.repository.uc;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface UCCredentialsRepository {

  CompletableFuture<Optional<DbUCCredentials>> find(String tenant);

  CompletableFuture<Void> save(DbUCCredentials credentials, String tenant);

  CompletableFuture<Void> delete(String tenant);
}
