package org.folio.rest.validator;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThrows;

import org.apache.commons.lang3.StringUtils;
import org.folio.rest.exception.InputValidationException;
import org.folio.rest.impl.PackagesTestData;
import org.folio.rest.jaxrs.model.Coverage;
import org.folio.rest.jaxrs.model.PackagePutDataAttributes;
import org.folio.rest.jaxrs.model.Token;
import org.folio.rest.jaxrs.model.VisibilityData;
import org.junit.Test;

public class PackagePutBodyValidatorTest {

  private final PackagePutBodyValidator validator = new PackagePutBodyValidator();

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
  public void shouldThrowExceptionWhenPackageIsNotSelectedAndIsHiddenIsTrue() {
    var request = PackagesTestData.getPackagePutRequest(
      new PackagePutDataAttributes()
        .withIsSelected(false)
        .withVisibilityData(new VisibilityData()
          .withIsHidden(true)));

    var exception = assertThrows(InputValidationException.class, () -> validator.validate(request));
    assertThat(exception.getMessage(), containsString("isHidden"));
  }

  @Test
  public void shouldThrowExceptionWhenPackageIsNotSelectedAndCoverageIsNotEmpty() {
    var request = PackagesTestData.getPackagePutRequest(
      new PackagePutDataAttributes()
        .withIsSelected(false)
        .withCustomCoverage(new Coverage()
          .withBeginCoverage("2000-01-01")));

    var exception = assertThrows(InputValidationException.class, () -> validator.validate(request));
    assertThat(exception.getMessage(), containsString("beginCoverage"));
  }

  @Test
  public void shouldThrowExceptionWhenPackageIsNotSelectedAndAllowToAddTitlesTrue() {
    var request = PackagesTestData.getPackagePutRequest(
      new PackagePutDataAttributes()
        .withIsSelected(false)
        .withAllowKbToAddTitles(true));

    var exception = assertThrows(InputValidationException.class, () -> validator.validate(request));
    assertThat(exception.getMessage(), containsString("allowKbToAddTitles"));
  }

  @Test
  public void shouldThrowExceptionWhenPackageIsNotSelectedAndTokenIsNotEmpty() {
    var request = PackagesTestData.getPackagePutRequest(
      new PackagePutDataAttributes()
        .withIsSelected(false)
        .withPackageToken(new Token().withValue("tokenValue")));

    var exception = assertThrows(InputValidationException.class, () -> validator.validate(request));
    assertThat(exception.getMessage(), containsString("value"));
  }

  @Test
  public void shouldThrowExceptionWhenPackageIsSelectedAndTokenIsTooLong() {
    var request = PackagesTestData.getPackagePutRequest(
      new PackagePutDataAttributes()
        .withIsSelected(true)
        .withPackageToken(new Token().withValue(StringUtils.repeat("tokenvalue", 200))));

    var exception = assertThrows(InputValidationException.class, () -> validator.validate(request));
    assertThat(exception.getMessage(), containsString("value"));
  }

  @Test
  public void shouldThrowExceptionWhenPackageIsSelectedAndCoverageDateIsInvalid() {
    var request = PackagesTestData.getPackagePutRequest(
      new PackagePutDataAttributes()
        .withIsSelected(true)
        .withCustomCoverage(new Coverage()
          .withBeginCoverage("abcd-ab-ab")));

    var exception = assertThrows(InputValidationException.class, () -> validator.validate(request));
    assertThat(exception.getMessage(), containsString("beginCoverage"));
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
