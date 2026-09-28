package org.folio.service.packages;

import static org.folio.util.TestUtil.result;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.folio.holdingsiq.model.PackageId;
import org.folio.repository.RecordType;
import org.folio.repository.packages.DbPackage;
import org.folio.repository.packages.PackageRepository;
import org.folio.repository.tag.DbTag;
import org.folio.repository.tag.TagRepository;
import org.folio.rest.jaxrs.model.ContentType;
import org.folio.rest.jaxrs.model.PackageTagsDataAttributes;
import org.folio.rest.jaxrs.model.Tags;
import org.folio.rmapi.result.TitleResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PackageTagsServiceTest {

  private static final String TENANT = "fs";
  private static final String PACKAGE_ID = "19-3964";
  private static final UUID CREDENTIALS_ID = UUID.randomUUID();

  @Mock
  private PackageRepository packageRepository;
  @Mock
  private TagRepository tagRepository;
  @InjectMocks
  private PackageTagsService packageTagsService;

  @Test
  void shouldDoNothingWhenTagsAreNull() {
    var attributes = new PackageTagsDataAttributes().withName("name").withContentType(ContentType.UNKNOWN);

    var result = result(packageTagsService.updatePackageTags(PACKAGE_ID, CREDENTIALS_ID, attributes, TENANT));

    assertNull(result);
    verifyNoInteractions(packageRepository);
    verifyNoInteractions(tagRepository);
  }

  @Test
  void shouldSavePackageAndUpdateTagsWhenTagListIsNotEmpty() {
    when(packageRepository.save(any(), eq(TENANT))).thenReturn(CompletableFuture.completedFuture(null));
    when(tagRepository.updateRecordTags(eq(TENANT), eq(PACKAGE_ID), eq(RecordType.PACKAGE), any()))
      .thenReturn(CompletableFuture.completedFuture(true));
    var attributes = new PackageTagsDataAttributes()
      .withName("name")
      .withContentType(ContentType.UNKNOWN)
      .withTags(new Tags().withTagList(List.of("tag1", "tag2")));

    var result = result(packageTagsService.updatePackageTags(PACKAGE_ID, CREDENTIALS_ID, attributes, TENANT));

    assertNull(result);
    verify(packageRepository).save(any(DbPackage.class), eq(TENANT));
    verify(packageRepository, never()).delete(any(), any(), any());
    verify(tagRepository).updateRecordTags(TENANT, PACKAGE_ID, RecordType.PACKAGE, List.of("tag1", "tag2"));
  }

  @Test
  void shouldDeleteStoredPackageAndUpdateTagsWhenTagListIsEmpty() {
    when(packageRepository.delete(any(), any(), eq(TENANT))).thenReturn(CompletableFuture.completedFuture(null));
    when(tagRepository.updateRecordTags(eq(TENANT), eq(PACKAGE_ID), eq(RecordType.PACKAGE), any()))
      .thenReturn(CompletableFuture.completedFuture(true));
    var attributes = new PackageTagsDataAttributes()
      .withName("name")
      .withContentType(ContentType.UNKNOWN)
      .withTags(new Tags().withTagList(List.of()));

    var result = result(packageTagsService.updatePackageTags(PACKAGE_ID, CREDENTIALS_ID, attributes, TENANT));

    assertNull(result);
    verify(packageRepository).delete(new PackageId(19, 3964), CREDENTIALS_ID, TENANT);
    verify(packageRepository, never()).save(any(), any());
    verify(tagRepository).updateRecordTags(TENANT, PACKAGE_ID, RecordType.PACKAGE, List.of());
  }

  @Test
  void shouldDeletePackageAndItsTags() {
    var packageId = new PackageId(19, 3964);
    when(packageRepository.delete(packageId, CREDENTIALS_ID, TENANT))
      .thenReturn(CompletableFuture.completedFuture(null));
    when(tagRepository.deleteRecordTags(TENANT, PACKAGE_ID, RecordType.PACKAGE))
      .thenReturn(CompletableFuture.completedFuture(true));

    var result = result(packageTagsService.deletePackageTags(packageId, CREDENTIALS_ID, TENANT));

    assertNull(result);
    verify(packageRepository).delete(packageId, CREDENTIALS_ID, TENANT);
    verify(tagRepository).deleteRecordTags(TENANT, PACKAGE_ID, RecordType.PACKAGE);
  }

  @Test
  void shouldPopulateResourceTagsFromTagRepository() {
    var titleResult = new TitleResult(null, false);
    var resourceIdToTitle = Map.of("resource-1", titleResult);
    var tags = List.of(DbTag.builder().value("tag1").recordType(RecordType.RESOURCE).build());
    when(tagRepository.findPerRecord(TENANT, List.of("resource-1"), RecordType.RESOURCE))
      .thenReturn(CompletableFuture.completedFuture(Map.of("resource-1", tags)));

    result(packageTagsService.loadResourceTags(TENANT, resourceIdToTitle));

    assertEquals(tags, titleResult.getResourceTagList());
  }
}
