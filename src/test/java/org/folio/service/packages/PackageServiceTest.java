package org.folio.service.packages;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.folio.util.TestUtil.result;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.folio.holdingsiq.model.Configuration;
import org.folio.holdingsiq.model.PackageData;
import org.folio.holdingsiq.model.PackageId;
import org.folio.holdingsiq.model.PackagePost;
import org.folio.holdingsiq.model.PackagePut;
import org.folio.holdingsiq.model.Packages;
import org.folio.holdingsiq.model.RequestContext;
import org.folio.holdingsiq.model.Titles;
import org.folio.holdingsiq.service.exception.ResourceNotFoundException;
import org.folio.okapi.common.XOkapiHeaders;
import org.folio.properties.common.SearchProperties;
import org.folio.repository.RecordKey;
import org.folio.repository.RecordType;
import org.folio.rest.converter.packages.PackageRequestConvertionService;
import org.folio.rest.exception.InputValidationException;
import org.folio.rest.jaxrs.model.AccessType;
import org.folio.rest.jaxrs.model.PackagePostData;
import org.folio.rest.jaxrs.model.PackagePostDataAttributes;
import org.folio.rest.jaxrs.model.PackagePostRequest;
import org.folio.rest.jaxrs.model.PackagePutData;
import org.folio.rest.jaxrs.model.PackagePutDataAttributes;
import org.folio.rest.jaxrs.model.PackagePutRequest;
import org.folio.rest.jaxrs.model.PackageTagsDataAttributes;
import org.folio.rest.jaxrs.model.PackageTagsPutData;
import org.folio.rest.jaxrs.model.PackageTagsPutRequest;
import org.folio.rest.model.filter.PackageRecordFilter;
import org.folio.rest.util.template.RmApiTemplateContext;
import org.folio.rest.validator.packages.PackageValidationService;
import org.folio.rmapi.PackageServiceImpl;
import org.folio.rmapi.ProvidersServiceImpl;
import org.folio.rmapi.TitlesServiceImpl;
import org.folio.rmapi.result.PackageResult;
import org.folio.rmapi.result.TitleCollectionResult;
import org.folio.service.loader.RelatedEntitiesLoader;
import org.folio.util.TestFutureFailedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.convert.converter.Converter;

@ExtendWith(MockitoExtension.class)
class PackageServiceTest {

  private static final String CREDENTIALS_ID = UUID.randomUUID().toString();
  private static final String TENANT = "fs";

  @Mock
  private PackageRequestConvertionService requestConvertionService;
  @Mock
  private PackageValidationService validationService;
  @Mock
  private PackageAccessTypeService packageAccessTypeService;
  @Mock
  private PackageTagsService packageTagsService;
  @Mock
  private CustomProviderIdService customProviderIdService;
  @Mock
  private RelatedEntitiesLoader relatedEntitiesLoader;
  @Mock
  private Converter<Titles, TitleCollectionResult> titleCollectionConverter;
  @Mock
  private PackageServiceImpl packagesRmApiService;
  @Mock
  private ProvidersServiceImpl providersRmApiService;
  @Mock
  private TitlesServiceImpl titlesRmApiService;

  private PackageService packageService;
  private RmApiTemplateContext context;

  @BeforeEach
  void setUp() {
    var searchProperties = new SearchProperties("any", "any", "b");
    packageService = new PackageService(requestConvertionService, validationService, packageAccessTypeService,
      packageTagsService, customProviderIdService, relatedEntitiesLoader, titleCollectionConverter, searchProperties);

    var requestContext = new RequestContext(Map.of(
      XOkapiHeaders.TENANT, TENANT,
      XOkapiHeaders.URL, "http://localhost:8080"));
    context = RmApiTemplateContext.builder()
      .requestContext(requestContext)
      .configuration(Configuration.builder().build())
      .credentialsId(CREDENTIALS_ID)
      .packagesService(packagesRmApiService)
      .providersService(providersRmApiService)
      .titlesService(titlesRmApiService)
      .build();
  }

  @Test
  void shouldRetrieveGlobalPackagesWhenProviderIdIsNull() {
    var packages = Packages.builder().totalResults(0).build();
    when(packagesRmApiService.retrievePackages(any(), any())).thenReturn(completedFuture(packages));
    var filter = PackageRecordFilter.builder().filterTags(List.of("tag")).sort("relevance").page(1).count(10).build();

    var result = result(packageService.retrievePackages(null, filter, context));

    assertSame(packages, result);
    verify(packagesRmApiService).retrievePackages(any(), any());
    verify(packagesRmApiService, never()).retrievePackages(any(Integer.class), any(), any());
  }

  @Test
  void shouldRetrieveProviderScopedPackagesWhenProviderIdIsGiven() {
    var packages = Packages.builder().totalResults(0).build();
    when(packagesRmApiService.retrievePackages(eq(42), any(), any())).thenReturn(completedFuture(packages));
    var filter = PackageRecordFilter.builder().filterTags(List.of("tag")).sort("relevance").page(1).count(10).build();

    var result = result(packageService.retrievePackages(42, filter, context));

    assertSame(packages, result);
    verify(packagesRmApiService).retrievePackages(eq(42), any(), any());
  }

  @Test
  void shouldResolveCustomProviderIdBeforeRetrievingPackages() {
    var packages = Packages.builder().totalResults(0).build();
    when(customProviderIdService.getCustomProviderId(context)).thenReturn(completedFuture(111));
    when(packagesRmApiService.retrievePackages(eq(111), any(), any())).thenReturn(completedFuture(packages));
    var filter = PackageRecordFilter.builder().filterTags(List.of("tag")).sort("relevance").page(1).count(10).build();

    var result = result(packageService.getCustomProviderIdAndRetrievePackages(filter, context));

    assertSame(packages, result);
  }

  @Test
  void shouldCreateCustomPackageWithoutAccessType() {
    var entity = buildPostRequest(null);
    var packagePost = mock(PackagePost.class);
    var postedPackage = PackageData.builder().vendorId(19).packageId(3964).build();
    when(requestConvertionService.convertCustomPackagePostRequest(entity)).thenReturn(packagePost);
    when(customProviderIdService.getCustomProviderId(context)).thenReturn(completedFuture(19));
    when(packagesRmApiService.postPackage(packagePost, 19)).thenReturn(completedFuture(postedPackage));

    var result = result(packageService.createCustomPackage(entity, context));

    verify(validationService).validateCustomPackagePostRequest(entity);
    assertSame(postedPackage, result.getPackageData());
    verify(packageAccessTypeService, never()).fetchAccessType(any(), any());
    verify(packageAccessTypeService, never()).assignAccessType(any(), any(), any());
  }

  @Test
  void shouldCreateCustomPackageAndAssignAccessTypeWhenAccessTypeIdIsGiven() {
    var entity = buildPostRequest("access-type-id");
    var packagePost = mock(PackagePost.class);
    var postedPackage = PackageData.builder().vendorId(19).packageId(3964).build();
    var accessType = new AccessType();
    when(requestConvertionService.convertCustomPackagePostRequest(entity)).thenReturn(packagePost);
    when(customProviderIdService.getCustomProviderId(context)).thenReturn(completedFuture(19));
    when(packagesRmApiService.postPackage(packagePost, 19)).thenReturn(completedFuture(postedPackage));
    when(packageAccessTypeService.fetchAccessType("access-type-id", context)).thenReturn(completedFuture(accessType));
    when(packageAccessTypeService.assignAccessType(eq(accessType), any(), eq(context)))
      .thenAnswer(invocation -> completedFuture(invocation.getArgument(1)));

    var result = result(packageService.createCustomPackage(entity, context));

    assertSame(postedPackage, result.getPackageData());
    verify(packageAccessTypeService).assignAccessType(eq(accessType), any(), eq(context));
  }

  @Test
  void shouldRetrievePackageWithRelatedAccessTypeAndTags() {
    var packageId = new PackageId(19, 3964);
    var packageResult = new PackageResult(PackageData.builder().vendorId(19).packageId(3964).build());
    when(packagesRmApiService.retrievePackage(packageId, List.of("provider")))
      .thenReturn(completedFuture(packageResult));
    when(relatedEntitiesLoader.loadAccessType(eq(packageResult), any(), eq(context)))
      .thenReturn(completedFuture(null));
    when(relatedEntitiesLoader.loadTags(eq(packageResult), any(), eq(context))).thenReturn(completedFuture(null));

    var result = result(packageService.retrievePackageWithRelatedData(packageId, List.of("provider"), context));

    assertSame(packageResult, result);
    var expectedRecordKey = RecordKey.builder().recordId("19-3964").recordType(RecordType.PACKAGE).build();
    verify(relatedEntitiesLoader).loadAccessType(packageResult, expectedRecordKey, context);
    verify(relatedEntitiesLoader).loadTags(packageResult, expectedRecordKey, context);
  }

  @Test
  void shouldThrowWhenIsCustomDoesNotMatchOriginalPackage() {
    var packageId = new PackageId(19, 3964);
    var originalPackage = PackageData.builder().vendorId(19).packageId(3964).isCustom(true).build();
    var entity = buildPutRequest(false, null);
    when(packagesRmApiService.retrievePackage(3964)).thenReturn(completedFuture(originalPackage));

    var packageUpdateFuture = packageService.updatePackage(packageId, entity, context);
    var exception = assertThrows(TestFutureFailedException.class, () -> result(packageUpdateFuture));

    assertEquals(InputValidationException.class, exception.getCause().getClass());
    verify(packagesRmApiService, never()).updatePackage(any(Integer.class), any());
  }

  @Test
  void shouldUpdateCustomPackageAndAssignAccessType() {
    var packageId = new PackageId(19, 3964);
    var originalPackage = PackageData.builder().vendorId(19).packageId(3964).isCustom(true).build();
    var entity = buildPutRequest(true, "access-type-id");
    var packagePut = mock(PackagePut.class);
    var accessType = new AccessType();
    when(packagesRmApiService.retrievePackage(3964)).thenReturn(completedFuture(originalPackage));
    when(packageAccessTypeService.fetchAccessType("access-type-id", context)).thenReturn(completedFuture(accessType));
    when(requestConvertionService.convertCustomPackagePutRequest(entity)).thenReturn(packagePut);
    when(packagesRmApiService.updatePackage(3964, packagePut)).thenReturn(completedFuture(null));
    when(packageAccessTypeService.assignAccessType(eq(accessType), any(), eq(context)))
      .thenAnswer(invocation -> completedFuture(invocation.getArgument(1)));

    var result = result(packageService.updatePackage(packageId, entity, context));

    assertSame(originalPackage, result.getPackageData());
    verify(validationService).validateCustomPackagePutRequest(entity);
    verify(packageAccessTypeService).assignAccessType(eq(accessType), any(), eq(context));
  }

  @Test
  void shouldCleanUpLocalDataWhenPackageWasDeletedDuringUpdate() {
    var packageId = new PackageId(19, 3964);
    var originalPackage = PackageData.builder().vendorId(19).packageId(3964).isCustom(true).build();
    var entity = buildPutRequest(true, null);
    var packagePut = mock(PackagePut.class);
    var notFound = new ResourceNotFoundException("not found", 404, "Not Found", "", "query");
    var failedRetrieve = CompletableFuture.<PackageData>failedFuture(notFound);
    when(packagesRmApiService.retrievePackage(3964))
      .thenReturn(completedFuture(originalPackage))
      .thenReturn(failedRetrieve);
    when(requestConvertionService.convertCustomPackagePutRequest(entity)).thenReturn(packagePut);
    when(packagesRmApiService.updatePackage(3964, packagePut)).thenReturn(completedFuture(null));
    when(packageAccessTypeService.updateRecordMapping(null, "19-3964", context)).thenReturn(completedFuture(null));
    when(packageTagsService.deletePackageTags(eq(packageId), any(), eq(TENANT))).thenReturn(completedFuture(null));

    var packageUpdateFuture = packageService.updatePackage(packageId, entity, context);
    assertThrows(TestFutureFailedException.class, () -> result(packageUpdateFuture));

    verify(packageAccessTypeService).updateRecordMapping(null, "19-3964", context);
    verify(packageTagsService).deletePackageTags(eq(packageId), any(), eq(TENANT));
  }

  @Test
  void shouldThrowWhenDeletingNonCustomPackage() {
    var packageId = new PackageId(19, 3964);
    var packageData = PackageData.builder().vendorId(19).packageId(3964).isCustom(false).build();
    when(packagesRmApiService.retrievePackage(3964)).thenReturn(completedFuture(packageData));

    var packageDeletionFuture = packageService.deletePackage(packageId, context);
    var exception = assertThrows(TestFutureFailedException.class, () -> result(packageDeletionFuture));

    assertEquals(InputValidationException.class, exception.getCause().getClass());
    verify(packagesRmApiService, never()).deletePackage(any(Integer.class));
  }

  @Test
  void shouldDeleteCustomPackageAndAssignedResources() {
    var packageId = new PackageId(19, 3964);
    var packageData = PackageData.builder().vendorId(19).packageId(3964).isCustom(true).build();
    when(packagesRmApiService.retrievePackage(3964)).thenReturn(completedFuture(packageData));
    when(packagesRmApiService.deletePackage(3964)).thenReturn(completedFuture(null));
    when(packageAccessTypeService.updateRecordMapping(null, "19-3964", context)).thenReturn(completedFuture(null));
    when(packageTagsService.deletePackageTags(eq(packageId), any(), eq(TENANT))).thenReturn(completedFuture(null));

    result(packageService.deletePackage(packageId, context));

    verify(packagesRmApiService).deletePackage(3964);
    verify(packageAccessTypeService).updateRecordMapping(null, "19-3964", context);
    verify(packageTagsService).deletePackageTags(eq(packageId), any(), eq(TENANT));
  }

  @Test
  void shouldValidateAndDelegateTagsUpdateToPackageTagsService() {
    var credentialsId = UUID.randomUUID();
    var attributes = new PackageTagsDataAttributes().withName("name");
    var entity = new PackageTagsPutRequest().withData(new PackageTagsPutData().withAttributes(attributes));
    when(packageTagsService.updatePackageTags("19-3964", credentialsId, attributes, TENANT))
      .thenReturn(completedFuture(null));

    var result = result(packageService.updateTagsForPackage(entity, credentialsId, "19-3964", TENANT));

    assertSame(attributes, result);
    verify(validationService).validatePackageTagsPutRequest(entity);
  }

  private PackagePostRequest buildPostRequest(String accessTypeId) {
    return new PackagePostRequest()
      .withData(new PackagePostData()
        .withAttributes(new PackagePostDataAttributes().withAccessTypeId(accessTypeId)));
  }

  private PackagePutRequest buildPutRequest(boolean isCustom, String accessTypeId) {
    return new PackagePutRequest()
      .withData(new PackagePutData()
        .withAttributes(new PackagePutDataAttributes()
          .withIsCustom(isCustom)
          .withAccessTypeId(accessTypeId)));
  }
}
