package org.folio.rest.converter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.folio.rest.impl.PackagesTestData;
import org.folio.rest.jaxrs.model.ContentType;
import org.folio.rest.jaxrs.model.Coverage;
import org.folio.rest.jaxrs.model.PackageDataAttributes;
import org.folio.rest.jaxrs.model.VisibilityData;
import org.folio.rmapi.model.PackagePut;
import org.folio.spring.config.ApplicationConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = ApplicationConfig.class)
public class PackagesConverterTest {
  @Autowired
  private PackagesConverter packagesConverter;

  @Test
  public void shouldCreateRequestToSelectPackage() {
    PackagePut packagePut = packagesConverter.convertToRMAPIPackagePutRequest(PackagesTestData.getPackagePutRequest(
      new PackageDataAttributes()
        .withIsSelected(true)));
    assertTrue(packagePut.getIsSelected());
  }

  @Test
  public void shouldCreateRequestToHidePackage() {
    PackagePut packagePut = packagesConverter.convertToRMAPIPackagePutRequest(PackagesTestData.getPackagePutRequest(
      new PackageDataAttributes()
        .withIsSelected(true)
        .withVisibilityData(new VisibilityData()
          .withIsHidden(true))));
    assertTrue(packagePut.getIsHidden());
  }

  @Test
  public void shouldCreateRequestToAllowKbAddTitlesToPackage() {
    PackagePut packagePut = packagesConverter.convertToRMAPIPackagePutRequest(PackagesTestData.getPackagePutRequest(
      new PackageDataAttributes()
        .withIsSelected(true)
        .withAllowKbToAddTitles(true)));
    assertTrue(packagePut.getAllowEbscoToAddTitles());
  }

  @Test
  public void shouldCreateRequestToAddCustomCoverage() {
    PackagePut packagePut = packagesConverter.convertToRMAPIPackagePutRequest(PackagesTestData.getPackagePutRequest(
      new PackageDataAttributes()
        .withIsSelected(true)
        .withCustomCoverage(new Coverage()
          .withBeginCoverage("2000-10-10")
          .withEndCoverage("2000-11-10"))));
    assertEquals("2000-10-10", packagePut.getCustomCoverage().getBeginCoverage());
    assertEquals("2000-11-10", packagePut.getCustomCoverage().getEndCoverage());
  }

  @Test
  public void shouldCreateRequestToChangeCustomPackageName() {
    PackagePut packagePut = packagesConverter.convertToRMAPICustomPackagePutRequest(PackagesTestData.getPackagePutRequest(
      new PackageDataAttributes()
        .withName("new package name")));
    assertEquals("new package name", packagePut.getPackageName());
  }

  @Test
  public void shouldCreateRequestToChangeCustomPackageCoverageDates() {
    PackagePut packagePut = packagesConverter.convertToRMAPICustomPackagePutRequest(PackagesTestData.getPackagePutRequest(
      new PackageDataAttributes()
        .withCustomCoverage(new Coverage()
          .withBeginCoverage("2003-01-01")
          .withEndCoverage("2004-01-01"))));
    assertEquals("2003-01-01", packagePut.getCustomCoverage().getBeginCoverage());
    assertEquals("2004-01-01", packagePut.getCustomCoverage().getEndCoverage());
  }


  @Test
  public void shouldCreateRequestToChangeCustomPackageCoverageDatesToEmpty() {
    PackagePut packagePut = packagesConverter.convertToRMAPICustomPackagePutRequest(PackagesTestData.getPackagePutRequest(
      new PackageDataAttributes()
        .withCustomCoverage(new Coverage()
          .withBeginCoverage("")
          .withEndCoverage(""))));
    assertEquals("", packagePut.getCustomCoverage().getBeginCoverage());
    assertEquals("", packagePut.getCustomCoverage().getEndCoverage());
  }

  @Test
  public void shouldCreateRequestToChangeCustomPackageContentType() {
    PackagePut packagePut = packagesConverter.convertToRMAPICustomPackagePutRequest(PackagesTestData.getPackagePutRequest(
      new PackageDataAttributes()
        .withContentType(ContentType.AGGREGATED_FULL_TEXT)));
    Integer aggregatedFullTextContentTypeCode = 1;
    assertEquals(aggregatedFullTextContentTypeCode, packagePut.getContentType());
  }

  @Test
  public void shouldCreateRequestToChangeCustomPackageVisibility() {
    PackagePut packagePut = packagesConverter.convertToRMAPICustomPackagePutRequest(PackagesTestData.getPackagePutRequest(
      new PackageDataAttributes()
        .withVisibilityData(new VisibilityData()
          .withIsHidden(true))));
    assertTrue(packagePut.getIsHidden());
  }
}
