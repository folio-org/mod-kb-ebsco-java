package org.folio.rest.validator;

import static org.hamcrest.Matchers.containsString;

import org.apache.commons.lang3.StringUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.folio.rest.exception.InputValidationException;
import org.folio.rest.impl.PackagesTestData;
import org.folio.rest.jaxrs.model.Coverage;
import org.folio.rest.jaxrs.model.PackagePutDataAttributes;
import org.folio.rest.jaxrs.model.Token;
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
  public void shouldThrowExceptionWhenPackageIsNotSelectedAndCoverageIsNotEmpty() {
    expectedEx.expect(InputValidationException.class);
    expectedEx.expectMessage(containsString("beginCoverage"));
    validator.validate(PackagesTestData.getPackagePutRequest(
      new PackagePutDataAttributes()
        .withIsSelected(false)
        .withCustomCoverage(new Coverage()
          .withBeginCoverage("2000-01-01"))));
  }

  @Test
  public void shouldThrowExceptionWhenPackageIsNotSelectedAndTokenIsNotEmpty() {
    expectedEx.expect(InputValidationException.class);
    expectedEx.expectMessage(containsString("value"));
    validator.validate(PackagesTestData.getPackagePutRequest(
      new PackagePutDataAttributes()
        .withIsSelected(false)
        .withPackageToken(new Token().withValue("tokenValue"))));
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
