package org.folio.service.packages;

import static org.folio.util.TestUtil.result;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.folio.holdingsiq.model.PackageData;
import org.folio.holdingsiq.model.RequestContext;
import org.folio.okapi.common.XOkapiHeaders;
import org.folio.repository.RecordType;
import org.folio.rest.jaxrs.model.AccessType;
import org.folio.rest.util.template.RmApiTemplateContext;
import org.folio.rmapi.result.PackageResult;
import org.folio.service.accesstypes.AccessTypeMappingsService;
import org.folio.service.accesstypes.AccessTypesService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PackageAccessTypeServiceTest {

  private static final String CREDENTIALS_ID = "credentials-id";
  private static final String ACCESS_TYPE_ID = "access-type-id";

  @Mock
  private AccessTypesService accessTypesService;
  @Mock
  private AccessTypeMappingsService accessTypeMappingsService;
  @InjectMocks
  private PackageAccessTypeService packageAccessTypeService;
  private RmApiTemplateContext context;

  @BeforeEach
  void setUp() {
    var requestContext = new RequestContext(Map.of(
      XOkapiHeaders.TENANT, "fs",
      XOkapiHeaders.URL, "http://localhost:8080"));
    context = RmApiTemplateContext.builder()
      .requestContext(requestContext)
      .credentialsId(CREDENTIALS_ID)
      .build();
  }

  @Test
  void shouldReturnNullWhenAccessTypeIdIsNull() {
    var result = result(packageAccessTypeService.fetchAccessType(null, context));

    assertNull(result);
    verifyNoInteractions(accessTypesService);
  }

  @Test
  void shouldFetchAccessTypeByCredentialsAndAccessTypeId() {
    var accessType = new AccessType();
    when(accessTypesService.findByCredentialsAndAccessTypeId(CREDENTIALS_ID, ACCESS_TYPE_ID, false,
      context.getRequestContext().getHeaders())).thenReturn(CompletableFuture.completedFuture(accessType));

    var result = result(packageAccessTypeService.fetchAccessType(ACCESS_TYPE_ID, context));

    assertSame(accessType, result);
  }

  @Test
  void shouldAssignAccessTypeToPackageResultAndUpdateMapping() {
    when(accessTypeMappingsService.update(any(), any(), any(), any(), any()))
      .thenReturn(CompletableFuture.completedFuture(null));
    var accessType = new AccessType();
    var packageData = PackageData.builder().vendorId(19).packageId(3964).build();
    var packageResult = new PackageResult(packageData);

    var result = result(packageAccessTypeService.assignAccessType(accessType, packageResult, context));

    assertSame(packageResult, result);
    assertSame(accessType, result.getAccessType());
    verify(accessTypeMappingsService).update(accessType, "19-3964", RecordType.PACKAGE, CREDENTIALS_ID,
      context.getRequestContext().getHeaders());
  }

  @Test
  void shouldUpdateRecordMappingForGivenRecordId() {
    when(accessTypeMappingsService.update(any(), any(), any(), any(), any()))
      .thenReturn(CompletableFuture.completedFuture(null));

    result(packageAccessTypeService.updateRecordMapping(null, "19-3964", context));

    verify(accessTypeMappingsService).update(null, "19-3964", RecordType.PACKAGE, CREDENTIALS_ID,
      context.getRequestContext().getHeaders());
  }
}
