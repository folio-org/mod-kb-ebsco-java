package org.folio.repository.uc;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface UCCredentialsRepository {

  CompletableFuture<Optional<DbUCCredentials>> find(String tenant);
}
