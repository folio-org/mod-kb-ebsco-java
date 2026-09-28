package org.folio.service.packages;

import static java.util.concurrent.CompletableFuture.completedFuture;

import java.util.concurrent.CompletableFuture;
import org.folio.repository.RecordType;
import org.folio.rest.jaxrs.model.AccessType;
import org.folio.rest.util.template.RmApiTemplateContext;
import org.folio.rmapi.result.PackageResult;
import org.folio.service.accesstypes.AccessTypeMappingsService;
import org.folio.service.accesstypes.AccessTypesService;
import org.springframework.stereotype.Service;

/**
 * Encapsulates access type lookup and record-mapping updates for packages,
 * hiding {@link AccessTypesService} and {@link AccessTypeMappingsService} from {@link PackageService}.
 */
@Service
class PackageAccessTypeService {

  private final AccessTypesService accessTypesService;
  private final AccessTypeMappingsService accessTypeMappingsService;

  PackageAccessTypeService(AccessTypesService accessTypesService, AccessTypeMappingsService accessTypeMappingsService) {
    this.accessTypesService = accessTypesService;
    this.accessTypeMappingsService = accessTypeMappingsService;
  }

  CompletableFuture<AccessType> fetchAccessType(String accessTypeId, RmApiTemplateContext context) {
    if (accessTypeId == null) {
      return completedFuture(null);
    }
    return accessTypesService.findByCredentialsAndAccessTypeId(context.getCredentialsId(), accessTypeId, false,
      context.getRequestContext().getHeaders());
  }

  CompletableFuture<PackageResult> assignAccessType(AccessType accessType, PackageResult packageResult,
                                                    RmApiTemplateContext context) {
    String recordId = packageResult.getPackageData().getFullPackageId();
    return updateRecordMapping(accessType, recordId, context)
      .thenApply(v -> {
        packageResult.setAccessType(accessType);
        return packageResult;
      });
  }

  CompletableFuture<Void> updateRecordMapping(AccessType accessType, String recordId, RmApiTemplateContext context) {
    return accessTypeMappingsService.update(accessType, recordId, RecordType.PACKAGE, context.getCredentialsId(),
      context.getRequestContext().getHeaders());
  }
}
