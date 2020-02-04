package org.folio.service.accesstypes;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.folio.rest.jaxrs.model.AccessTypeCollection;
import org.folio.rest.jaxrs.model.AccessTypeCollectionItem;

public interface AccessTypesService {

  CompletableFuture<AccessTypeCollectionItem> save(AccessTypeCollectionItem accessType, Map<String, String> okapiHeaders);

  CompletableFuture<AccessTypeCollection> findAll(Map<String, String> okapiHeaders);

  CompletableFuture<AccessTypeCollectionItem> findById(String id, Map<String, String> okapiHeaders);

  CompletableFuture<Void> deleteById(String id, Map<String, String> okapiHeaders);

}
