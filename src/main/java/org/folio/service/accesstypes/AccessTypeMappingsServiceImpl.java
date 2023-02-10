package org.folio.service.accesstypes;

import static org.folio.common.FunctionUtils.nothing;
import static org.folio.db.RowSetUtils.toUUID;
import static org.folio.rest.util.TenantUtil.tenantId;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import lombok.extern.log4j.Log4j2;
import org.folio.repository.RecordType;
import org.folio.repository.accesstypes.AccessTypeMapping;
import org.folio.repository.accesstypes.AccessTypeMappingsRepository;
import org.folio.rest.jaxrs.model.AccessType;
import org.folio.rest.model.filter.AccessTypeFilter;
import org.springframework.stereotype.Component;

@Log4j2
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
    String tenantId = tenantId(okapiHeaders);
    log.debug("update:: by [recordId: {}, recordType: {}, tenant: {}]", recordId, recordType, tenantId);

    if (accessType == null) {
      log.info("update:: accessType == null, attempting to delete by record [recordId: {}]", recordId);
      return mappingRepository.deleteByRecord(recordId, recordType, toUUID(credentialsId), tenantId);
    }

    return mappingRepository.findByRecord(recordId, recordType, toUUID(credentialsId), tenantId)
      .thenCompose(dbMapping -> {
        UUID accessTypeId = UUID.fromString(accessType.getId());
        AccessTypeMapping mapping;
        if (dbMapping.isPresent()) {
          mapping = dbMapping.get().toBuilder().accessTypeId(accessTypeId).build();
        } else {
          mapping = createAccessTypeMapping(recordId, recordType, accessTypeId);
        }

        log.info("update:: Attempts to save by [mapping: {}, tenant: {}]", mapping, tenantId);
        return mappingRepository.save(mapping, tenantId).thenApply(nothing());
      });
  }

  @Override
  public CompletableFuture<Map<UUID, Integer>> countByRecordPrefix(String recordPrefix, RecordType recordType,
                                                                   String credentialsId,
                                                                   Map<String, String> okapiHeaders) {
    return mappingRepository.countByRecordIdPrefix(recordPrefix, recordType, toUUID(credentialsId),
      tenantId(okapiHeaders));
  }

  private AccessTypeMapping createAccessTypeMapping(String recordId, RecordType recordType, UUID accessTypeId) {
    return AccessTypeMapping.builder()
      .id(UUID.randomUUID())
      .accessTypeId(accessTypeId)
      .recordId(recordId)
      .recordType(recordType)
      .build();
  }
}
