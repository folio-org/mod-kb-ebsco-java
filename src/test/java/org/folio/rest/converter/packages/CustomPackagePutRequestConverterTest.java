package org.folio.rest.converter.packages;

import static org.folio.util.PackagesTestUtil.getPackagePutRequest;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.folio.rest.jaxrs.model.ContentType;
import org.folio.rest.jaxrs.model.Coverage;
import org.folio.rest.jaxrs.model.PackagePutDataAttributes;
import org.folio.rest.jaxrs.model.PackageVisibility;
import org.junit.jupiter.api.Test;

class CustomPackagePutRequestConverterTest {

  private final CustomPackagePutRequestConverter converter = new CustomPackagePutRequestConverter();

  @Test
  void shouldCreateRequestToChangeCustomPackageName() {
    var packagePut = converter.convert(getPackagePutRequest(
      new PackagePutDataAttributes()
        .withName("new package name")));
    assertEquals("new package name", packagePut.getPackageName());
  }

  @Test
  void shouldCreateRequestToChangeCustomPackageCoverageDates() {
    var packagePut = converter.convert(getPackagePutRequest(
      new PackagePutDataAttributes()
        .withCustomCoverage(new Coverage()
          .withBeginCoverage("2003-01-01")
          .withEndCoverage("2004-01-01"))));
    assertEquals("2003-01-01", packagePut.getCustomCoverage().getBeginCoverage());
    assertEquals("2004-01-01", packagePut.getCustomCoverage().getEndCoverage());
  }

  @Test
  void shouldCreateRequestToChangeCustomPackageCoverageDatesToEmpty() {
    var packagePut = converter.convert(getPackagePutRequest(
      new PackagePutDataAttributes()
        .withCustomCoverage(new Coverage()
          .withBeginCoverage("")
          .withEndCoverage(""))));
    assertEquals("", packagePut.getCustomCoverage().getBeginCoverage());
    assertEquals("", packagePut.getCustomCoverage().getEndCoverage());
  }

  @Test
  void shouldCreateRequestToChangeCustomPackageContentType() {
    var packagePut = converter.convert(getPackagePutRequest(
      new PackagePutDataAttributes()
        .withContentType(ContentType.STREAMING_MEDIA)));
    var aggregatedFullTextContentTypeCode = 8;
    assertEquals(aggregatedFullTextContentTypeCode, packagePut.getContentType());
  }

  @Test
  void shouldCreateRequestToChangeCustomPackageVisibility() {
    var packagePut =
      converter.convert(getPackagePutRequest(
        new PackagePutDataAttributes()
          .withVisibility(List.of(new PackageVisibility()
            .withHidden(true)
            .withCategory(PackageVisibility.Category.PF)))
      ));
    assertTrue(packagePut.getVisibilityDetails().getFirst().hidden());
  }
}
