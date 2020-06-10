package org.folio.service.accesstypes;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.folio.repository.RecordType;
import org.folio.repository.accesstypes.AccessTypeMapping;
import org.folio.rest.jaxrs.model.AccessType;
import org.folio.rest.model.filter.AccessTypeFilter;

public interface AccessTypeMappingsService {

  CompletableFuture<Collection<AccessTypeMapping>> findByAccessTypeFilter(AccessTypeFilter accessTypeFilter,
                                                                          Map<String, String> okapiHeaders);

  CompletableFuture<Void> update(AccessType accessType, String recordId, RecordType recordType,
                                 String credentialsId, Map<String, String> okapiHeaders);

  CompletableFuture<Map<UUID, Integer>> countByRecordPrefix(String recordPrefix, RecordType recordType,
                                                            String credentialsId, Map<String, String> okapiHeaders);
}
