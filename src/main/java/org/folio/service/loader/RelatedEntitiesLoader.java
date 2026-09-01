package org.folio.service.loader;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.folio.repository.RecordKey;
import org.folio.repository.RecordType;
import org.folio.repository.accesstypes.DbAccessType;
import org.folio.rest.util.template.RmApiTemplateContext;
import org.folio.rmapi.result.Accessible;
import org.folio.rmapi.result.Tagable;

public interface RelatedEntitiesLoader {

  CompletableFuture<Void> loadAccessType(Accessible accessible, RecordKey recordKey, RmApiTemplateContext context);

  CompletableFuture<Void> loadTags(Tagable tagable, RecordKey recordKey, RmApiTemplateContext context);

  CompletableFuture<Map<String, DbAccessType>> loadAccessTypes(List<String> recordIds, RecordType recordType,
                                                               RmApiTemplateContext context);
}
