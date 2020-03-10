package org.folio.service.accesstypes;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.folio.repository.RecordType;
import org.folio.rest.jaxrs.model.AccessTypeCollection;
import org.folio.rest.jaxrs.model.AccessTypeCollectionItem;

public interface AccessTypesService {

  CompletableFuture<AccessTypeCollection> findAll(Map<String, String> okapiHeaders);

  CompletableFuture<AccessTypeCollection> findByNames(Collection<String> accessTypeNames, Map<String, String> okapiHeaders);

  CompletableFuture<AccessTypeCollectionItem> findById(String id, Map<String, String> okapiHeaders);

  CompletableFuture<AccessTypeCollectionItem> findByRecord(String recordId, RecordType recordType,
                                                           Map<String, String> okapiHeaders);

  CompletableFuture<AccessTypeCollectionItem> save(AccessTypeCollectionItem accessType, Map<String, String> okapiHeaders);

  CompletableFuture<Void> update(String id, AccessTypeCollectionItem accessType, Map<String, String> okapiHeaders);

  CompletableFuture<Void> deleteById(String id, Map<String, String> okapiHeaders);
}
