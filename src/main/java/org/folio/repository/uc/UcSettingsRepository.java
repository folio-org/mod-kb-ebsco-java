package org.folio.repository.uc;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface UcSettingsRepository {

  CompletableFuture<Optional<DbUcSettings>> findByCredentialsId(UUID credentialsId, String tenant);

  CompletableFuture<DbUcSettings> save(DbUcSettings ucSettings, String tenant);
}
