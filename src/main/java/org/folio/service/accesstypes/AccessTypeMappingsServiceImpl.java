package org.folio.service.accesstypes;

import static org.folio.rest.tools.utils.TenantTool.tenantId;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.springframework.stereotype.Component;

import org.folio.repository.RecordType;
import org.folio.repository.accesstypes.AccessTypeMapping;
import org.folio.repository.accesstypes.AccessTypeMappingsRepository;
import org.folio.rest.jaxrs.model.AccessType;
import org.folio.rest.model.filter.AccessTypeFilter;

@Component
public class AccessTypeMappingsServiceImpl implements AccessTypeMappingsService {

  private final AccessTypeMappingsRepository mappingRepository;

  public AccessTypeMappingsServiceImpl(AccessTypeMappingsRepository mappingRepository) {
    this.mappingRepository = mappingRepository;
  }

  @Override
  public CompletableFuture<Collection<AccessTypeMapping>> findByAccessTypeFilter(AccessTypeFilter accessTypeFilter,
                                                                                 Map<String, String> okapiHeaders) {
    return mappingRepository.findByAccessTypeFilter(accessTypeFilter, tenantId(okapiHeaders));
  }

  @Override
  public CompletableFuture<Void> update(AccessType accessType, String recordId, RecordType recordType,
                                        String credentialsId, Map<String, String> okapiHeaders) {
    if (accessType == null) {
      return mappingRepository.deleteByRecord(recordId, recordType, credentialsId, tenantId(okapiHeaders));
    }

    return mappingRepository.findByRecord(recordId, recordType, credentialsId, tenantId(okapiHeaders))
      .thenCompose(dbMapping -> {
        String accessTypeId = accessType.getId();
        AccessTypeMapping mapping;
        if (dbMapping.isPresent()) {
          mapping = dbMapping.get().toBuilder().accessTypeId(accessTypeId).build();
        } else {
          mapping = createAccessTypeMapping(recordId, recordType, accessTypeId);
        }
        return mappingRepository.save(mapping, tenantId(okapiHeaders)).thenApply(result -> null);
      });
  }

  @Override
  public CompletableFuture<Map<String, Integer>> countByRecordPrefix(String recordPrefix, RecordType recordType,
                                                                     String credentialsId,
                                                                     Map<String, String> okapiHeaders) {
    return mappingRepository.countByRecordIdPrefix(recordPrefix, recordType, credentialsId, tenantId(okapiHeaders));
  }

  private AccessTypeMapping createAccessTypeMapping(String recordId, RecordType recordType, String accessTypeId) {
    return AccessTypeMapping.builder()
      .id(UUID.randomUUID().toString())
      .accessTypeId(accessTypeId)
      .recordId(recordId)
      .recordType(recordType)
      .build();
  }
}
