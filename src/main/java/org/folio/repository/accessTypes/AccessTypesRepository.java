package org.folio.repository.accessTypes;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.folio.rest.jaxrs.model.AccessTypeCollectionItem;

public interface AccessTypesRepository {
  /**
   * Returns all access types for given tenantId.
   */
  CompletableFuture<List<AccessTypeCollectionItem>> findAll(String tenantId);

  /**
   * Fetches an access type from the database
   *
   * @param id id of access type to get
   */
  CompletableFuture<AccessTypeCollectionItem> findById(String tenantId, String id);

  /**
   * Saves a new access type record to the database
   *
   * @param accessType - current AccessType  {@link AccessTypeCollectionItem} object to save
   * @return
   */
  CompletableFuture<AccessTypeCollectionItem> save(AccessTypeCollectionItem accessType, String tenantId);

  CompletableFuture<Long> count(String tenantId);

//  CompletableFuture<Void> delete(String resourceId, String tenantId);
}
