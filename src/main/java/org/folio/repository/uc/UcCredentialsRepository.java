package org.folio.repository.uc;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface UcCredentialsRepository {

  CompletableFuture<Optional<DbUcCredentials>> find(String tenant);

  CompletableFuture<Void> save(DbUcCredentials credentials, String tenant);

  CompletableFuture<Void> delete(String tenant);
}
