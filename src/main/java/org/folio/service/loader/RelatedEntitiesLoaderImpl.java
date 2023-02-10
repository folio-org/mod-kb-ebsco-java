package org.folio.service.loader;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import javax.ws.rs.NotFoundException;
import lombok.extern.log4j.Log4j2;
import org.folio.repository.RecordKey;
import org.folio.repository.tag.DbTag;
import org.folio.repository.tag.TagRepository;
import org.folio.rest.jaxrs.model.Tags;
import org.folio.rest.util.template.RmApiTemplateContext;
import org.folio.rmapi.result.Accessible;
import org.folio.rmapi.result.Tagable;
import org.folio.service.accesstypes.AccessTypesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Log4j2
@Component
public class RelatedEntitiesLoaderImpl implements RelatedEntitiesLoader {

  @Autowired
  private TagRepository tagRepository;
  @Autowired
  private Converter<List<DbTag>, Tags> tagsConverter;
  @Autowired
  private AccessTypesService accessTypesService;

  @Override
  public CompletableFuture<Void> loadAccessType(Accessible accessible, RecordKey recordKey,
                                                RmApiTemplateContext context) {
    log.debug("loadAccessType:: by [recordKey: {}]", recordKey);

    CompletableFuture<Void> future = new CompletableFuture<>();
    accessTypesService.findByRecord(recordKey, context.getCredentialsId(), context.getOkapiData().getHeaders())
      .whenComplete((accessType, throwable) -> {
        if (throwable != null && !(throwable.getCause() instanceof NotFoundException)) {
          future.completeExceptionally(throwable.getCause());
        } else {
          accessible.setAccessType(accessType);
          future.complete(null);
        }
      });
    return future;
  }

  @Override
  public CompletableFuture<Void> loadTags(Tagable tagable, RecordKey recordKey, RmApiTemplateContext context) {
    String tenant = context.getOkapiData().getTenant();
    log.debug("loadTags:: by [recordKey: {}, tenant: {}]", recordKey, tenant);

    return tagRepository.findByRecord(tenant, recordKey.getRecordId(),
        recordKey.getRecordType())
      .thenApply(tags -> {
        tagable.setTags(tagsConverter.convert(tags));
        return null;
      });
  }
}
