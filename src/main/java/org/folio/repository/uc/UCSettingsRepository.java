package org.folio.repository.uc;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface UCSettingsRepository {

  CompletableFuture<Optional<DbUCSettings>> findByCredentialsId(UUID credentialsId, String tenant);
}
