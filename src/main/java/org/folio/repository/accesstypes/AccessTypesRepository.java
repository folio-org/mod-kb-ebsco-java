package org.folio.repository.accesstypes;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface AccessTypesRepository {

  /**
   * Returns all access types for given credentials and tenant.
   */
  CompletableFuture<List<DbAccessType>> findByCredentialsId(String credentialsId, String tenantId);

}
