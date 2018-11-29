package org.folio.rest.validator;

import static org.hamcrest.Matchers.containsString;

import org.apache.commons.lang3.StringUtils;
import org.folio.rest.exception.InputValidationException;
import org.folio.rest.impl.PackagesTestData;
import org.folio.rest.jaxrs.model.Coverage;
import org.folio.rest.jaxrs.model.PackageDataAttributes;
import org.folio.rest.jaxrs.model.Token;
import org.folio.rest.jaxrs.model.VisibilityData;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class PackagePutBodyValidatorTest {

  private PackagePutBodyValidator validator = new PackagePutBodyValidator();

  @Rule
  public ExpectedException expectedEx = ExpectedException.none();

  @Test
  public void shouldValidateWhenPackageIsSelected() {
    validator.validate(PackagesTestData.getPackagePutRequest(
      new PackageDataAttributes()
        .withIsSelected(true)
        .withCustomCoverage(new Coverage())
        .withAllowKbToAddTitles(true)
        .withVisibilityData(new VisibilityData()
          .withIsHidden(false)
          .withReason(""))));
  }

  @Test
  public void shouldThrowExceptionWhenPackageIsNotSelectedAndIsHiddenIsTrue() {
    expectedEx.expect(InputValidationException.class);
    expectedEx.expectMessage(containsString("isHidden"));
    validator.validate(PackagesTestData.getPackagePutRequest(
      new PackageDataAttributes()
      .withIsSelected(false)
      .withVisibilityData(new VisibilityData()
        .withIsHidden(true))));
  }

  @Test
  public void shouldThrowExceptionWhenPackageIsNotSelectedAndCoverageIsNotEmpty() {
    expectedEx.expect(InputValidationException.class);
    expectedEx.expectMessage(containsString("beginCoverage"));
    validator.validate(PackagesTestData.getPackagePutRequest(
      new PackageDataAttributes()
        .withIsSelected(false)
        .withCustomCoverage(new Coverage()
          .withBeginCoverage("2000-01-01"))));
  }

  @Test
  public void shouldThrowExceptionWhenPackageIsNotSelectedAndAllowToAddTitlesTrue() {
    expectedEx.expect(InputValidationException.class);
    expectedEx.expectMessage(containsString("allowKbToAddTitles"));
    validator.validate(PackagesTestData.getPackagePutRequest(
      new PackageDataAttributes()
        .withIsSelected(false)
        .withAllowKbToAddTitles(true)));
  }

  @Test
  public void shouldThrowExceptionWhenPackageIsNotSelectedAndTokenIsNotEmpty() {
    expectedEx.expect(InputValidationException.class);
    expectedEx.expectMessage(containsString("value"));
    validator.validate(PackagesTestData.getPackagePutRequest(
      new PackageDataAttributes()
        .withIsSelected(false)
        .withPackageToken(new Token().withValue("tokenValue"))));
  }

  @Test
  public void shouldThrowExceptionWhenPackageIsSelectedAndTokenIsTooLong() {
    expectedEx.expect(InputValidationException.class);
    expectedEx.expectMessage(containsString("value"));
    validator.validate(PackagesTestData.getPackagePutRequest(
      new PackageDataAttributes()
        .withIsSelected(true)
        .withPackageToken(new Token().withValue(StringUtils.repeat("tokenvalue",200)))));
  }

  @Test
  public void shouldThrowExceptionWhenPackageIsSelectedAndCoverageDateIsInvalid() {
    expectedEx.expect(InputValidationException.class);
    expectedEx.expectMessage(containsString("beginCoverage"));
    validator.validate(PackagesTestData.getPackagePutRequest(
      new PackageDataAttributes()
        .withIsSelected(true)
        .withCustomCoverage(new Coverage()
          .withBeginCoverage("abcd-ab-ab"))));
  }

  @Test
  public void shouldValidateWhenPackageIsSelectedAndCoverageDateIsEmpty() {
    validator.validate(PackagesTestData.getPackagePutRequest(
      new PackageDataAttributes()
        .withIsSelected(true)
        .withCustomCoverage(new Coverage()
          .withBeginCoverage(""))));
  }
}
