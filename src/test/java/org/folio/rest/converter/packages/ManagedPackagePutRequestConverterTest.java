package org.folio.rest.converter.packages;

import static org.folio.rest.converter.packages.CommonPackagePutRequestConverter.HIDDEN_BY_CUSTOMER;
import static org.folio.util.PackagesTestUtil.getPackagePutRequest;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.folio.rest.jaxrs.model.Coverage;
import org.folio.rest.jaxrs.model.PackagePutDataAttributes;
import org.folio.rest.jaxrs.model.PackageVisibility;
import org.junit.jupiter.api.Test;

class ManagedPackagePutRequestConverterTest {

  private final ManagedPackagePutRequestConverter converter = new ManagedPackagePutRequestConverter();

  @Test
  void shouldCreateRequestToSelectPackage() {
    var packagePut = converter.convert(getPackagePutRequest(
      new PackagePutDataAttributes()
        .withIsSelected(true)));
    assertTrue(packagePut.getIsSelected());
  }

  @Test
  void shouldCreateRequestToHidePackage() {
    var packagePut = converter.convert(getPackagePutRequest(
      new PackagePutDataAttributes()
        .withIsSelected(true)
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
    var packagePut = converter.convert(getPackagePutRequest(
      new PackagePutDataAttributes()
        .withIsSelected(true)
        .withVisibility(List.of(new PackageVisibility()
          .withHidden(false)
          .withReason("some incoming reason")
          .withCategory(PackageVisibility.Category.PF)))
    ));
    var visibility = packagePut.getVisibilityDetails().getFirst();
    assertFalse(visibility.hidden());
    assertNull(visibility.reason());
  }

  @Test
  void shouldCreateRequestToAllowKbAddTitlesToPackage() {
    var packagePut = converter.convert(getPackagePutRequest(
      new PackagePutDataAttributes()
        .withIsSelected(true)
        .withAllowKbToAddTitles(true)));
    assertTrue(packagePut.getAllowEbscoToAddTitles());
  }

  @Test
  void shouldCreateRequestToAddCustomCoverage() {
    var packagePut = converter.convert(getPackagePutRequest(
      new PackagePutDataAttributes()
        .withIsSelected(true)
        .withCustomCoverage(new Coverage()
          .withBeginCoverage("2000-10-10")
          .withEndCoverage("2000-11-10"))));
    assertEquals("2000-10-10", packagePut.getCustomCoverage().getBeginCoverage());
    assertEquals("2000-11-10", packagePut.getCustomCoverage().getEndCoverage());
  }
}
