package org.folio.service.loader;

import java.util.concurrent.CompletableFuture;

import org.folio.repository.RecordKey;
import org.folio.rest.util.template.RMAPITemplateContext;
import org.folio.rmapi.result.Accessible;
import org.folio.rmapi.result.Tagable;

public interface RelatedEntitiesLoader {

  CompletableFuture<Void> loadAccessType(Accessible accessible, RecordKey recordKey, RMAPITemplateContext context);

  CompletableFuture<Void> loadTags(Tagable tagable, RecordKey recordKey, RMAPITemplateContext context);
}
