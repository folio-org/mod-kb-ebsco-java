package org.folio.service.loader;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import javax.ws.rs.NotFoundException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import org.folio.repository.RecordKey;
import org.folio.repository.tag.Tag;
import org.folio.repository.tag.TagRepository;
import org.folio.rest.jaxrs.model.Tags;
import org.folio.rest.tools.utils.TenantTool;
import org.folio.rmapi.result.Accessible;
import org.folio.rmapi.result.Tagable;
import org.folio.service.accesstypes.AccessTypesService;

@Component
public class RelatedEntitiesLoaderImpl implements RelatedEntitiesLoader {

  @Autowired
  private TagRepository tagRepository;
  @Autowired
  private Converter<List<Tag>, Tags> tagsConverter;
  @Autowired
  private AccessTypesService accessTypesService;

  @Override
  public CompletableFuture<Void> loadAccessType(Accessible accessible, RecordKey recordKey,
                                                Map<String, String> okapiHeaders) {
    CompletableFuture<Void> future = new CompletableFuture<>();
    accessTypesService.findByRecord(recordKey.getRecordId(), recordKey.getRecordType(), "credentialsId", okapiHeaders)
      .thenAccept(accessType -> {
        accessible.setAccessType(accessType);
        future.complete(null);
      })
      .exceptionally(throwable -> {
        Throwable cause = throwable.getCause();
        if (cause instanceof NotFoundException) {
          accessible.setAccessType(null);
          future.complete(null);
        } else {
          future.completeExceptionally(cause);
        }
        return null;
      });
    return future;
  }

  @Override
  public CompletableFuture<Void> loadTags(Tagable tagable, RecordKey recordKey, Map<String, String> okapiHeaders) {
    return tagRepository.findByRecord(TenantTool.tenantId(okapiHeaders), recordKey.getRecordId(), recordKey.getRecordType())
      .thenApply(tags -> {
        tagable.setTags(tagsConverter.convert(tags));
        return null;
      });
  }
}
