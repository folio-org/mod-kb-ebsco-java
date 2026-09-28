package org.folio.rest.converter.packages;

import static org.folio.rest.converter.packages.CommonPackagePutRequestConverter.HIDDEN_BY_CUSTOMER;
import static org.folio.util.PackagesTestUtil.getPackagePutRequest;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.folio.rest.jaxrs.model.ContentType;
import org.folio.rest.jaxrs.model.Coverage;
import org.folio.rest.jaxrs.model.PackagePutDataAttributes;
import org.folio.rest.jaxrs.model.PackageVisibility;
import org.folio.service.sanitizer.Sanitizer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CustomPackagePutRequestConverterTest {

  @Mock
  private Sanitizer<String> htmlSanitizer;

  @InjectMocks
  private CustomPackagePutRequestConverter converter;

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
  void shouldSanitizeCustomDescription() {
    when(htmlSanitizer.sanitize("Some description")).thenReturn("sanitized description");

    var packagePut = converter.convert(getPackagePutRequest(
      new PackagePutDataAttributes()
        .withCustomDescription("Some description")));

    assertEquals("sanitized description", packagePut.getCustomDescription());
    verify(htmlSanitizer).sanitize("Some description");
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
    var visibility = packagePut.getVisibilityDetails().getFirst();
    assertTrue(visibility.hidden());
    assertEquals(HIDDEN_BY_CUSTOMER, visibility.reason());
  }

  @Test
  void shouldIgnoreIncomingReasonAndClearReasonWhenHiddenFalse() {
    var packagePut =
      converter.convert(getPackagePutRequest(
        new PackagePutDataAttributes()
          .withVisibility(List.of(new PackageVisibility()
            .withHidden(false)
            .withReason("some incoming reason")
            .withCategory(PackageVisibility.Category.PF)))
      ));
    var visibility = packagePut.getVisibilityDetails().getFirst();
    assertFalse(visibility.hidden());
    assertNull(visibility.reason());
  }
}
