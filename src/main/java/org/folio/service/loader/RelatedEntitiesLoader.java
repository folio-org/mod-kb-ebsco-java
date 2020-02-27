package org.folio.service.loader;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.folio.repository.RecordKey;
import org.folio.rmapi.result.Accessible;
import org.folio.rmapi.result.Tagable;

public interface RelatedEntitiesLoader {

  CompletableFuture<Void> loadAccessType(Accessible accessible, RecordKey recordKey,
                                         Map<String, String> okapiHeaders);

  CompletableFuture<Void> loadTags(Tagable tagable, RecordKey recordKey, Map<String, String> okapiHeaders);
}
