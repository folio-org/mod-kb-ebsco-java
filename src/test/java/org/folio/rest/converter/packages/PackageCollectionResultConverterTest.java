package org.folio.rest.converter.packages;

import static org.folio.util.TestUtil.readJsonFile;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import org.folio.holdingsiq.model.Packages;
import org.folio.repository.accesstypes.DbAccessType;
import org.folio.rest.jaxrs.model.AccessType;
import org.folio.rest.jaxrs.model.PackageCollection;
import org.folio.rmapi.result.PackageCollectionResult;
import org.folio.spring.config.TestConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.ConversionService;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfig.class)
class PackageCollectionResultConverterTest {

  private static final String PACKAGES_FIXTURE = "responses/rmapi/packages/get-packages-by-provider-id.json";
  private static final String PACKAGE_FULL_ID = "19-3964";
  private static final String ACCESS_TYPE_NAME = "Subscribed";
  private static final String ACCESS_TYPE_DESCRIPTION = "Access Type description";

  @Autowired
  private ConversionService conversionService;

  @Test
  void shouldIncludeAccessTypeInPackageItemWhenAccessTypeIsMapped() {
    var packages = readJsonFile(PACKAGES_FIXTURE, Packages.class);
    var accessTypeId = UUID.randomUUID();
    var credentialsId = UUID.randomUUID();
    var dbAccessType = DbAccessType.builder()
      .id(accessTypeId)
      .credentialsId(credentialsId)
      .name(ACCESS_TYPE_NAME)
      .description(ACCESS_TYPE_DESCRIPTION)
      .build();
    Map<String, DbAccessType> accessTypes = Map.of(PACKAGE_FULL_ID, dbAccessType);
    var result = new PackageCollectionResult(packages, Collections.emptyList(), accessTypes);

    var packageCollection = conversionService.convert(result, PackageCollection.class);

    assertNotNull(packageCollection);
    var item = packageCollection.getData().getFirst();

    assertEquals(1, item.getIncluded().size());
    var includedAccessType = (AccessType) item.getIncluded().getFirst();
    assertEquals(accessTypeId.toString(), includedAccessType.getId());
    assertEquals(AccessType.Type.ACCESS_TYPES, includedAccessType.getType());

    var accessTypeRel = item.getRelationships().getAccessType();
    assertNotNull(accessTypeRel);
    assertEquals(accessTypeId.toString(), accessTypeRel.getData().getId());
    assertEquals("accessTypes", accessTypeRel.getData().getType());
    assertTrue(accessTypeRel.getMeta().getIncluded());
  }

  @Test
  void shouldNotIncludeAccessTypeWhenAccessTypesMapIsEmpty() {
    var packages = readJsonFile(PACKAGES_FIXTURE, Packages.class);
    var result = new PackageCollectionResult(packages, Collections.emptyList());

    var packageCollection = conversionService.convert(result, PackageCollection.class);

    assertNotNull(packageCollection);
    var item = packageCollection.getData().getFirst();

    assertTrue(item.getIncluded().isEmpty());
    assertNull(item.getRelationships().getAccessType());
  }
}
