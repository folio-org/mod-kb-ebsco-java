package org.folio.service.accesstypes;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.folio.repository.RecordKey;
import org.folio.rest.jaxrs.model.AccessType;
import org.folio.rest.jaxrs.model.AccessTypeCollection;
import org.folio.rest.jaxrs.model.AccessTypePostRequest;
import org.folio.rest.jaxrs.model.AccessTypePutRequest;

public interface AccessTypesService {

  CompletableFuture<AccessTypeCollection> findByUser(Map<String, String> okapiHeaders);

  CompletableFuture<AccessTypeCollection> findByCredentialsId(String credentialsId, Map<String, String> okapiHeaders);

  CompletableFuture<AccessType> findByUserAndId(String accessTypeId, Map<String, String> okapiHeaders);

  CompletableFuture<AccessType> findByCredentialsAndAccessTypeId(String credentialsId, String accessTypeId,
                                                                 boolean withMetadata,
                                                                 Map<String, String> okapiHeaders);

  CompletableFuture<AccessTypeCollection> findByNames(Collection<String> accessTypeNames, String credentialsId,
                                                      Map<String, String> okapiHeaders);

  CompletableFuture<AccessType> findByRecord(RecordKey recordKey, String credentialsId, Map<String, String> okapiHeaders);

  CompletableFuture<AccessType> save(String credentialsId, AccessTypePostRequest postRequest,
                                     Map<String, String> okapiHeaders);

  CompletableFuture<Void> update(String credentialsId, String accessTypeId, AccessTypePutRequest entity,
                                 Map<String, String> okapiHeaders);

  CompletableFuture<Void> delete(String credentialsId, String accessTypeId, Map<String, String> okapiHeaders);
}
