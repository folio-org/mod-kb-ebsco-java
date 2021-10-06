package org.folio.rest.validator;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.folio.rest.impl.PackagesTestData;
import org.folio.rest.jaxrs.model.Coverage;
import org.folio.rest.jaxrs.model.PackagePutDataAttributes;
import org.folio.rest.jaxrs.model.VisibilityData;

public class PackagePutBodyValidatorTest {

  private PackagePutBodyValidator validator = new PackagePutBodyValidator();

  @Rule
  public ExpectedException expectedEx = ExpectedException.none();

  @Test
  public void shouldValidateWhenPackageIsSelected() {
    validator.validate(PackagesTestData.getPackagePutRequest(
      new PackagePutDataAttributes()
        .withIsSelected(true)
        .withCustomCoverage(new Coverage())
        .withAllowKbToAddTitles(true)
        .withVisibilityData(new VisibilityData()
          .withIsHidden(false)
          .withReason(""))));
  }

  @Test
  public void shouldValidateWhenPackageIsSelectedAndCoverageDateIsEmpty() {
    validator.validate(PackagesTestData.getPackagePutRequest(
      new PackagePutDataAttributes()
        .withIsSelected(true)
        .withCustomCoverage(new Coverage()
          .withBeginCoverage(""))));
  }
}
