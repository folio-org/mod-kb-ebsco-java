package org.folio.rest.converter.packages;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import org.folio.holdingsiq.model.CoverageDates;
import org.folio.holdingsiq.model.PackageData;
import org.folio.holdingsiq.model.SubjectAssociation;
import org.folio.rest.jaxrs.model.PackageCollectionItem;
import org.folio.rest.jaxrs.model.PackageSubject;
import org.folio.spring.config.TestConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.ConversionService;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfig.class)
class PackageCollectionItemConverterTest {

  @Autowired
  private ConversionService conversionService;

  @Test
  void shouldKeepAllSubjects() {
    var packageData = packageDataBuilder()
      .subjectAssociations(List.of(
        new SubjectAssociation(1, 3, "Science", false, null, null, null),
        new SubjectAssociation(2, 3, "Biology", true, null, null, 1),
        new SubjectAssociation(3, 3, "Genetics", true, false, "Science -- Biology -- Genetics", 2)))
      .build();

    var subjects = convert(packageData).getAttributes().getSubjects();

    assertNotNull(subjects);
    assertEquals(List.of("Science", "Biology", "Genetics"), subjects.stream().map(PackageSubject::getName).toList());
    var genetics = subjects.get(2);
    assertEquals(3, genetics.getId());
    assertEquals(3, genetics.getSchemaId());
    assertEquals(2, genetics.getParentId());
    assertEquals(true, genetics.getIsCustom());
    assertEquals("Science -- Biology -- Genetics", genetics.getSubjectHierarchy());
  }

  @Test
  void shouldReturnNullSubjectsWhenNoSubjectAssociations() {
    var packageData = packageDataBuilder().build();

    assertNull(convert(packageData).getAttributes().getSubjects());
  }

  private PackageData.PackageDataBuilder packageDataBuilder() {
    return PackageData.builder()
      .packageId(1)
      .vendorId(2)
      .contentType("EJournal")
      .customCoverage(CoverageDates.builder().beginCoverage("").endCoverage("").build());
  }

  private PackageCollectionItem convert(PackageData packageData) {
    var item = conversionService.convert(packageData, PackageCollectionItem.class);
    assertNotNull(item);
    return item;
  }
}
