package org.folio.repository.accesstypes;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.folio.repository.RecordType;

public interface AccessTypesMappingRepository {

  CompletableFuture<Void> saveMapping(String accessTypeId, String recordId, RecordType recordType,
                                      Map<String, String> okapiHeaders);
}
