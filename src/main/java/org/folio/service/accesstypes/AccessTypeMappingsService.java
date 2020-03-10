package org.folio.service.accesstypes;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.folio.repository.RecordType;
import org.folio.repository.accesstypes.AccessTypeMapping;
import org.folio.rest.jaxrs.model.AccessTypeCollectionItem;

public interface AccessTypeMappingsService {

  CompletableFuture<AccessTypeMapping> findByRecord(String recordId, RecordType recordType,
                                                    Map<String, String> okapiHeaders);

  CompletableFuture<Collection<AccessTypeMapping>> findByAccessTypeId(String accessTypeId, Map<String, String> okapiHeaders);

  CompletableFuture<Collection<AccessTypeMapping>> findByAccessTypeIds(Collection<String> accessTypeIds,
                                                                       RecordType recordType, int page, int count,
                                                                       Map<String, String> okapiHeaders);

  CompletableFuture<Void> update(AccessTypeCollectionItem accessType, String recordId, RecordType recordType,
                                 Map<String, String> okapiHeaders);

  CompletableFuture<Map<String, Integer>> countRecordsByAccessType(Map<String, String> okapiHeaders);
}
