package org.folio.rest.converter.packages;

import static org.folio.util.PackagesTestUtil.getPackagePutRequest;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.folio.rest.jaxrs.model.ContentType;
import org.folio.rest.jaxrs.model.Coverage;
import org.folio.rest.jaxrs.model.PackagePutDataAttributes;
import org.folio.rest.jaxrs.model.PackageVisibility;
import org.folio.spring.config.TestConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfig.class)
class PackageRequestConverterTest {
  @Autowired
  private PackageRequestConverter packagesConverter;

  @Test
  void shouldCreateRequestToSelectPackage() {
    var packagePut = packagesConverter.convertToRmApiPackagePutRequest(getPackagePutRequest(
      new PackagePutDataAttributes()
        .withIsSelected(true)));
    assertTrue(packagePut.getIsSelected());
  }

  @Test
  void shouldCreateRequestToHidePackage() {
    var packagePut = packagesConverter.convertToRmApiPackagePutRequest(getPackagePutRequest(
      new PackagePutDataAttributes()
        .withIsSelected(true)
        .withVisibility(List.of(new PackageVisibility()
          .withHidden(true)
          .withCategory(PackageVisibility.Category.PF)))
    ));
    assertTrue(packagePut.getVisibilityDetails().getFirst().hidden());
  }

  @Test
  void shouldCreateRequestToAllowKbAddTitlesToPackage() {
    var packagePut = packagesConverter.convertToRmApiPackagePutRequest(getPackagePutRequest(
      new PackagePutDataAttributes()
        .withIsSelected(true)
        .withAllowKbToAddTitles(true)));
    assertTrue(packagePut.getAllowEbscoToAddTitles());
  }

  @Test
  void shouldCreateRequestToAddCustomCoverage() {
    var packagePut = packagesConverter.convertToRmApiPackagePutRequest(getPackagePutRequest(
      new PackagePutDataAttributes()
        .withIsSelected(true)
        .withCustomCoverage(new Coverage()
          .withBeginCoverage("2000-10-10")
          .withEndCoverage("2000-11-10"))));
    assertEquals("2000-10-10", packagePut.getCustomCoverage().getBeginCoverage());
    assertEquals("2000-11-10", packagePut.getCustomCoverage().getEndCoverage());
  }

  @Test
  void shouldCreateRequestToChangeCustomPackageName() {
    var packagePut = packagesConverter.convertToRmApiCustomPackagePutRequest(getPackagePutRequest(
      new PackagePutDataAttributes()
        .withName("new package name")));
    assertEquals("new package name", packagePut.getPackageName());
  }

  @Test
  void shouldCreateRequestToChangeCustomPackageCoverageDates() {
    var packagePut = packagesConverter.convertToRmApiCustomPackagePutRequest(getPackagePutRequest(
      new PackagePutDataAttributes()
        .withCustomCoverage(new Coverage()
          .withBeginCoverage("2003-01-01")
          .withEndCoverage("2004-01-01"))));
    assertEquals("2003-01-01", packagePut.getCustomCoverage().getBeginCoverage());
    assertEquals("2004-01-01", packagePut.getCustomCoverage().getEndCoverage());
  }

  @Test
  void shouldCreateRequestToChangeCustomPackageCoverageDatesToEmpty() {
    var packagePut = packagesConverter.convertToRmApiCustomPackagePutRequest(getPackagePutRequest(
      new PackagePutDataAttributes()
        .withCustomCoverage(new Coverage()
          .withBeginCoverage("")
          .withEndCoverage(""))));
    assertEquals("", packagePut.getCustomCoverage().getBeginCoverage());
    assertEquals("", packagePut.getCustomCoverage().getEndCoverage());
  }

  @Test
  void shouldCreateRequestToChangeCustomPackageContentType() {
    var packagePut = packagesConverter.convertToRmApiCustomPackagePutRequest(getPackagePutRequest(
      new PackagePutDataAttributes()
        .withContentType(ContentType.STREAMING_MEDIA)));
    var aggregatedFullTextContentTypeCode = 8;
    assertEquals(aggregatedFullTextContentTypeCode, packagePut.getContentType());
  }

  @Test
  void shouldCreateRequestToChangeCustomPackageVisibility() {
    var packagePut =
      packagesConverter.convertToRmApiCustomPackagePutRequest(getPackagePutRequest(
        new PackagePutDataAttributes()
          .withVisibility(List.of(new PackageVisibility()
            .withHidden(true)
            .withCategory(PackageVisibility.Category.PF)))
      ));
    assertTrue(packagePut.getVisibilityDetails().getFirst().hidden());
  }
}
