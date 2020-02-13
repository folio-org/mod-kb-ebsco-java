package org.folio.repository.accesstypes;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.folio.rest.jaxrs.model.AccessTypeCollectionItem;

public interface AccessTypesRepository {

  /**
   * Returns all access types for given tenantId.
   */
  CompletableFuture<List<AccessTypeCollectionItem>> findAll(String tenantId);

  /**
   * Fetches an access type from the database
   * If access type with given id doesn't exist then returns NotFoundException as a cause.
   * @param id id of access type to get
   */
  CompletableFuture<Optional<AccessTypeCollectionItem>> findById(String id, String tenantId);

  /**
   * Saves a new access type record to the database
   * @param accessType - current AccessType  {@link AccessTypeCollectionItem} object to save
   */
  CompletableFuture<AccessTypeCollectionItem> save(AccessTypeCollectionItem accessType, String tenantId);

  /**
   * Updates access type with given id.
   * If access type with given id doesn't exist then returns NotFoundException as a cause.
   */
  CompletableFuture<Void> update(String id, AccessTypeCollectionItem accessType, String tenantId);

  CompletableFuture<Integer> count(String tenantId);

  /**
   * Delete access type with given id and tenantId
   *
   * @param id of access type to delete
   * @return {@link CompletableFuture} with the result of operation
   */
  CompletableFuture<Void> delete(String id, String tenantId);

}
