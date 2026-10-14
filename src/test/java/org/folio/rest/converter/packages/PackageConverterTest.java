package org.folio.rest.converter.packages;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import org.folio.holdingsiq.model.CoverageDates;
import org.folio.holdingsiq.model.PackageData;
import org.folio.holdingsiq.model.SubjectAssociation;
import org.folio.rest.jaxrs.model.Package;
import org.folio.rest.jaxrs.model.PackageSubject;
import org.folio.rmapi.result.PackageResult;
import org.folio.spring.config.TestConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.ConversionService;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfig.class)
class PackageConverterTest {

  @Autowired
  private ConversionService conversionService;

  @Test
  void shouldKeepManagedAndExplicitCustomSubjectsAndDropCustomPlaceholders() {
    var packageData = packageDataBuilder()
      .subjectAssociations(List.of(
        new SubjectAssociation(1, 3, "Science", false, false, "Science", null),
        new SubjectAssociation(2, 3, "Biology", true, false, "Science -- Biology", 1),
        new SubjectAssociation(3, 3, "Genetics", true, true, "Science -- Biology -- Genetics", 2),
        new SubjectAssociation(4, 3, "Botany", true, true, "Science -- Botany", 1)))
      .build();

    var subjects = convert(packageData).getData().getAttributes().getSubjects();

    assertNotNull(subjects);
    assertEquals(List.of("Science", "Genetics", "Botany"), subjects.stream().map(PackageSubject::getName).toList());
  }

  @Test
  void shouldTreatNullIsCustomAsManaged() {
    var packageData = packageDataBuilder()
      .subjectAssociations(List.of(new SubjectAssociation(1, 3, "Science", null, null, null, null)))
      .build();

    var subjects = convert(packageData).getData().getAttributes().getSubjects();

    assertEquals(1, subjects.size());
  }

  @Test
  void shouldReturnNullSubjectsWhenNoSubjectAssociations() {
    var packageData = packageDataBuilder().build();

    assertNull(convert(packageData).getData().getAttributes().getSubjects());
  }

  private PackageData.PackageDataBuilder packageDataBuilder() {
    return PackageData.builder()
      .packageId(1)
      .vendorId(2)
      .contentType("EJournal")
      .customCoverage(CoverageDates.builder().beginCoverage("").endCoverage("").build());
  }

  private Package convert(PackageData packageData) {
    var result = conversionService.convert(new PackageResult(packageData), Package.class);
    assertNotNull(result);
    return result;
  }
}
