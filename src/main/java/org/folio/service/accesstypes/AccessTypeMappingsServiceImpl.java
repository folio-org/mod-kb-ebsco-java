package org.folio.service.accesstypes;

import static org.folio.rest.tools.utils.TenantTool.tenantId;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import javax.ws.rs.NotFoundException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.folio.repository.RecordType;
import org.folio.repository.accesstypes.AccessTypeMapping;
import org.folio.repository.accesstypes.AccessTypeMappingsRepository;
import org.folio.rest.jaxrs.model.AccessTypeCollectionItem;
import org.folio.rest.model.filter.AccessTypeFilter;

@Component
public class AccessTypeMappingsServiceImpl implements AccessTypeMappingsService {

  @Autowired
  private AccessTypeMappingsRepository mappingRepository;

  @Override
  public CompletableFuture<AccessTypeMapping> findByRecord(String recordId, RecordType recordType,
                                                           Map<String, String> okapiHeaders) {
    return mappingRepository.findByRecord(recordId, recordType, tenantId(okapiHeaders))
      .thenApply(mapping -> mapping.orElseThrow(() -> new NotFoundException(
        String.format("Access type mapping not found: recordId = %s, recordType = %s", recordId, recordType)))
      );
  }

  @Override
  public CompletableFuture<Collection<AccessTypeMapping>> findByAccessTypeId(String accessTypeId,
                                                                             Map<String, String> okapiHeaders) {
    return mappingRepository.findByAccessTypeId(accessTypeId, tenantId(okapiHeaders));
  }

  @Override
  public CompletableFuture<Collection<AccessTypeMapping>> findByAccessTypeFilter(AccessTypeFilter accessTypeFilter,
                                                                                 Map<String, String> okapiHeaders) {
    return mappingRepository.findByAccessTypeFilter(accessTypeFilter, tenantId(okapiHeaders));
  }

  @Override
  public CompletableFuture<Void> update(AccessTypeCollectionItem accessType, String recordId, RecordType recordType,
                                        Map<String, String> okapiHeaders) {
    if (accessType == null) {
      return mappingRepository.deleteByRecord(recordId, recordType, tenantId(okapiHeaders));
    }

    return mappingRepository.findByRecord(recordId, recordType, tenantId(okapiHeaders))
      .thenCompose(dbMapping -> {
        AccessTypeMapping mapping;
        if (dbMapping.isPresent()) {
          mapping = dbMapping.get().toBuilder().accessTypeId(accessType.getId()).build();
        } else {
          mapping = createAccessTypeMapping(accessType, recordId, recordType);
        }
        return mappingRepository.save(mapping, tenantId(okapiHeaders)).thenApply(result -> null);
      });
  }

  @Override
  public CompletableFuture<Map<String, Integer>> countRecordsByAccessType(Map<String, String> okapiHeaders) {
    return mappingRepository.countRecordsByAccessType(tenantId(okapiHeaders));
  }

  @Override
  public CompletableFuture<Map<String, Integer>> countRecordsByAccessTypeAndRecordPrefix(String recordIdPrefix,
                                                                                         RecordType recordType,
                                                                                         Map<String, String> okapiHeaders) {
    return mappingRepository.countRecordsByAccessTypeAndRecordIdPrefix(recordIdPrefix, recordType,
      tenantId(okapiHeaders));
  }

  private AccessTypeMapping createAccessTypeMapping(AccessTypeCollectionItem accessType, String recordId,
                                                    RecordType recordType) {
    return AccessTypeMapping.builder()
      .id(UUID.randomUUID().toString())
      .accessTypeId(accessType.getId())
      .recordId(recordId)
      .recordType(recordType).build();
  }
}
