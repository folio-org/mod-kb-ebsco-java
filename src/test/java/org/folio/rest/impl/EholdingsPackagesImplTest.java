package org.folio.rest.impl;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import org.folio.HttpStatus;
import org.folio.holdingsiq.model.PackageId;
import org.folio.holdingsiq.service.exception.ResourceNotFoundException;
import org.folio.okapi.common.XOkapiHeaders;
import org.folio.rest.exception.InputValidationException;
import org.folio.rest.jaxrs.model.KbCredentials;
import org.folio.rest.jaxrs.model.PackagePostBulkFetchRequest;
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
import org.folio.rest.model.filter.ResourceFilter;
import org.folio.rest.util.template.RmApiTemplate;
import org.folio.rest.util.template.RmApiTemplateContext;
import org.folio.rest.util.template.RmApiTemplateFactory;
import org.folio.rmapi.PackageServiceImpl;
import org.folio.service.kbcredentials.UserKbCredentialsService;
import org.folio.service.loader.FilteredEntitiesLoader;
import org.folio.service.packages.PackageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.objenesis.ObjenesisStd;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class EholdingsPackagesImplTest {

  private static final Map<String, String> OKAPI_HEADERS = Map.of(
    XOkapiHeaders.TENANT, "fs",
    XOkapiHeaders.URL, "http://localhost:8080");

  @Mock
  private PackageService packageService;
  @Mock
  private RmApiTemplateFactory templateFactory;
  @Mock
  private FilteredEntitiesLoader filteredEntitiesLoader;
  @Mock
  private UserKbCredentialsService userKbCredentialsService;
  @Mock
  private RmApiTemplate template;
  @Mock
  private Handler<AsyncResult<Response>> asyncResultHandler;
  @Mock
  private PackageServiceImpl packagesRmApiService;
  @Captor
  private ArgumentCaptor<AsyncResult<Response>> responseCaptor;

  private EholdingsPackagesImpl impl;
  private RmApiTemplateContext context;

  @BeforeEach
  void setUp() {
    impl = new ObjenesisStd().newInstance(EholdingsPackagesImpl.class);
    ReflectionTestUtils.setField(impl, "packageService", packageService);
    ReflectionTestUtils.setField(impl, "templateFactory", templateFactory);
    ReflectionTestUtils.setField(impl, "filteredEntitiesLoader", filteredEntitiesLoader);
    ReflectionTestUtils.setField(impl, "userKbCredentialsService", userKbCredentialsService);

    lenient().when(template.requestAction(any())).thenReturn(template);
    lenient().when(template.addErrorMapper(any(), any())).thenReturn(template);
    lenient().when(templateFactory.createTemplate(anyMap(), ArgumentMatchers.<Handler<AsyncResult<Response>>>any()))
      .thenReturn(template);

    context = RmApiTemplateContext.builder()
      .credentialsId(UUID.randomUUID().toString())
      .packagesService(packagesRmApiService)
      .build();
  }

  @SuppressWarnings("unchecked")
  private Function<RmApiTemplateContext, CompletableFuture<?>> captureRequestAction() {
    var captor = ArgumentCaptor.forClass(Function.class);
    verify(template).requestAction(captor.capture());
    return captor.getValue();
  }

  @Test
  void shouldFetchPackagesByTagFilterWhenTagsFilterIsGiven() {
    var filter = PackageRecordFilter.builder().filterTags(List.of("tag1")).sort("relevance")
      .page(1).count(25).build();
    when(filteredEntitiesLoader.fetchPackagesByTagFilter(any(), any())).thenReturn(completedFuture(null));

    invokeGetEholdingsPackages(filter);

    captureRequestAction().apply(context);
    verify(filteredEntitiesLoader).fetchPackagesByTagFilter(any(), eq(context));
    verify(packageService, never()).retrievePackages(any(), any(), any());
    verify(template).executeWithResult(org.folio.rest.jaxrs.model.PackageCollection.class);
  }

  @Test
  void shouldFetchPackagesByAccessTypeFilterWhenAccessTypeFilterIsGiven() {
    var filter = PackageRecordFilter.builder().filterAccessType(List.of("at1")).sort("relevance")
      .page(1).count(25).build();
    when(filteredEntitiesLoader.fetchPackagesByAccessTypeFilter(any(), any())).thenReturn(completedFuture(null));

    invokeGetEholdingsPackages(filter);

    captureRequestAction().apply(context);
    verify(filteredEntitiesLoader).fetchPackagesByAccessTypeFilter(any(), eq(context));
  }

  @Test
  void shouldResolveCustomProviderIdWhenFilterCustomIsTrue() {
    var filter = PackageRecordFilter.builder().filterCustom("true").sort("relevance").page(1).count(25).build();
    when(packageService.getCustomProviderIdAndRetrievePackages(filter, context)).thenReturn(completedFuture(null));

    invokeGetEholdingsPackages(filter);

    captureRequestAction().apply(context);
    verify(packageService).getCustomProviderIdAndRetrievePackages(filter, context);
    verify(packageService, never()).retrievePackages(any(), any(), any());
  }

  @Test
  void shouldRetrieveAllPackagesWhenFilterCustomIsNotSet() {
    var filter = PackageRecordFilter.builder().sort("relevance").page(1).count(25).build();
    when(packageService.retrievePackages(null, filter, context)).thenReturn(completedFuture(null));

    invokeGetEholdingsPackages(filter);

    captureRequestAction().apply(context);
    verify(packageService).retrievePackages(null, filter, context);
  }

  private void invokeGetEholdingsPackages(PackageRecordFilter filter) {
    impl.getEholdingsPackages(filter.getFilterCustom(), filter.getQuery(), filter.getQueryField(),
      filter.getQueryType(), filter.isHighlight(), filter.getFilterSelected(), filter.getFilterType(),
      filter.getFilterVisibility(), filter.getFilterFreeAccess(), filter.getFilterTags(),
      filter.getFilterAccessType(), filter.getSort(), filter.getPage(), filter.getCount(), OKAPI_HEADERS,
      asyncResultHandler, null);
  }

  @Test
  void shouldCreateCustomPackage() {
    var entity = new PackagePostRequest()
      .withData(new PackagePostData().withAttributes(new PackagePostDataAttributes()));
    when(packageService.createCustomPackage(entity, context)).thenReturn(completedFuture(null));

    impl.postEholdingsPackages("application/json", entity, OKAPI_HEADERS, asyncResultHandler, null);

    captureRequestAction().apply(context);
    verify(packageService).createCustomPackage(entity, context);
    verify(template).addErrorMapper(eq(NotFoundException.class), any());
    verify(template).executeWithResult(org.folio.rest.jaxrs.model.Package.class);
  }

  @Test
  void shouldRetrievePackageWithRelatedData() {
    when(packageService.retrievePackageWithRelatedData(any(), any(), any())).thenReturn(completedFuture(null));

    impl.getEholdingsPackagesByPackageId("19-3964", "provider,resources", OKAPI_HEADERS, asyncResultHandler, null);

    captureRequestAction().apply(context);
    verify(packageService).retrievePackageWithRelatedData(new PackageId(19, 3964),
      List.of("provider", "resources"), context);
    verify(template).executeWithResult(org.folio.rest.jaxrs.model.Package.class);
  }

  @Test
  void shouldUpdatePackage() {
    var entity = new PackagePutRequest()
      .withData(new PackagePutData().withAttributes(new PackagePutDataAttributes()));
    when(packageService.updatePackage(any(), any(), any())).thenReturn(completedFuture(null));

    impl.putEholdingsPackagesByPackageId("19-3964", "application/json", entity, OKAPI_HEADERS, asyncResultHandler,
      null);

    captureRequestAction().apply(context);
    verify(packageService).updatePackage(new PackageId(19, 3964), entity, context);
    verify(template).addErrorMapper(eq(NotFoundException.class), any());
    verify(template).addErrorMapper(eq(InputValidationException.class), any());
    verify(template).executeWithResult(org.folio.rest.jaxrs.model.Package.class);
  }

  @Test
  void shouldDeletePackage() {
    when(packageService.deletePackage(any(), any())).thenReturn(completedFuture(null));

    impl.deleteEholdingsPackagesByPackageId("19-3964", OKAPI_HEADERS, asyncResultHandler, null);

    captureRequestAction().apply(context);
    verify(packageService).deletePackage(new PackageId(19, 3964), context);
    verify(template).execute();
    verify(template, never()).executeWithResult(any());
  }

  @Test
  void shouldFetchResourcesByTagFilterWhenTagsFilterIsGiven() {
    var filter = ResourceFilter.builder().packageId("19-3964").filterTags(List.of("tag1"))
      .page(1).count(25).build();
    when(filteredEntitiesLoader.fetchResourcesByTagFilter(any(), any())).thenReturn(completedFuture(null));

    invokeGetResources(filter);

    captureRequestAction().apply(context);
    verify(filteredEntitiesLoader).fetchResourcesByTagFilter(any(), eq(context));
    verify(template).addErrorMapper(eq(ResourceNotFoundException.class), any());
    verify(template).executeWithResult(org.folio.rest.jaxrs.model.ResourceCollection.class);
  }

  @Test
  void shouldFetchResourcesByAccessTypeFilterWhenAccessTypeFilterIsGiven() {
    var filter = ResourceFilter.builder().packageId("19-3964").filterAccessType(List.of("at1"))
      .page(1).count(25).build();
    when(filteredEntitiesLoader.fetchResourcesByAccessTypeFilter(any(), any())).thenReturn(completedFuture(null));

    invokeGetResources(filter);

    captureRequestAction().apply(context);
    verify(filteredEntitiesLoader).fetchResourcesByAccessTypeFilter(any(), eq(context));
  }

  @Test
  void shouldRetrievePackageTitlesWhenNoTagOrAccessTypeFilterIsGiven() {
    var filter = ResourceFilter.builder().packageId("19-3964").filterName("test").sort("relevance")
      .page(1).count(25).build();
    Function<RmApiTemplateContext, CompletableFuture<?>> stubFunction = ctx -> completedFuture(null);
    when(packageService.retrievePackageTitles(filter)).thenReturn(stubFunction);

    invokeGetResources(filter);

    verify(template).requestAction(same(stubFunction));
  }

  private void invokeGetResources(ResourceFilter filter) {
    impl.getEholdingsPackagesResourcesByPackageId(filter.getPackageId(), filter.getFilterTags(),
      filter.getFilterAccessType(), filter.getFilterSelected(), filter.getFilterType(), filter.getFilterName(),
      filter.getFilterIsxn(), filter.getFilterSubject(), filter.getFilterPublisher(), filter.getSort(),
      filter.getPage(), filter.getCount(), OKAPI_HEADERS, asyncResultHandler, null);
  }

  @Test
  void shouldUpdateTagsForPackage() {
    var credentialsId = UUID.randomUUID();
    var attributes = new PackageTagsDataAttributes().withName("name");
    var entity = new PackageTagsPutRequest().withData(new PackageTagsPutData().withAttributes(attributes));
    when(userKbCredentialsService.findByUser(OKAPI_HEADERS))
      .thenReturn(completedFuture(new KbCredentials().withId(credentialsId.toString())));
    when(packageService.updateTagsForPackage(entity, credentialsId, "19-3964", "fs"))
      .thenReturn(completedFuture(attributes));

    impl.putEholdingsPackagesTagsByPackageId("19-3964", "application/json", entity, OKAPI_HEADERS,
      asyncResultHandler, null);

    verify(asyncResultHandler).handle(responseCaptor.capture());
    var response = responseCaptor.getValue().result();
    assertEquals(HttpStatus.SC_OK, response.getStatus());
    verify(packageService).updateTagsForPackage(entity, credentialsId, "19-3964", "fs");
  }

  @Test
  void shouldReturn422WhenTagsUpdateFails() {
    var credentialsId = UUID.randomUUID();
    var attributes = new PackageTagsDataAttributes().withName("name");
    var entity = new PackageTagsPutRequest().withData(new PackageTagsPutData().withAttributes(attributes));
    when(userKbCredentialsService.findByUser(OKAPI_HEADERS))
      .thenReturn(completedFuture(new KbCredentials().withId(credentialsId.toString())));
    when(packageService.updateTagsForPackage(any(), any(), any(), any()))
      .thenReturn(CompletableFuture.failedFuture(new InputValidationException("invalid", "invalid tags")));

    impl.putEholdingsPackagesTagsByPackageId("19-3964", "application/json", entity, OKAPI_HEADERS,
      asyncResultHandler, null);

    verify(asyncResultHandler).handle(responseCaptor.capture());
    var response = responseCaptor.getValue().result();
    assertEquals(HttpStatus.SC_UNPROCESSABLE_CONTENT, response.getStatus());
  }

  @Test
  void shouldRetrievePackagesBulk() {
    var entity = new PackagePostBulkFetchRequest().withPackages(java.util.Set.of("19-3964"));
    when(packagesRmApiService.retrievePackagesBulk(entity.getPackages())).thenReturn(completedFuture(null));

    impl.postEholdingsPackagesBulkFetch("application/json", entity, OKAPI_HEADERS, asyncResultHandler, null);

    captureRequestAction().apply(context);
    verify(packagesRmApiService).retrievePackagesBulk(entity.getPackages());
    verify(template).executeWithResult(org.folio.rest.jaxrs.model.PackageBulkFetchCollection.class);
  }
}
