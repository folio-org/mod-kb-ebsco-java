package org.folio.rest.validator;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.folio.rest.exception.InputValidationException;
import org.folio.rest.jaxrs.model.Coverage;
import org.folio.rest.jaxrs.model.PackagePutDataAttributes;
import org.folio.rest.jaxrs.model.PackageVisibility;
import org.folio.rest.jaxrs.model.Token;
import org.folio.util.PackagesTestUtil;
import org.junit.jupiter.api.Test;

class PackagePutBodyValidatorTest {

  private final PackagePutBodyValidator validator = new PackagePutBodyValidator();

  @Test
  void shouldValidateWhenPackageIsSelected() {
    var packagePutRequest = PackagesTestUtil.getPackagePutRequest(
      new PackagePutDataAttributes()
        .withIsSelected(true)
        .withCustomCoverage(new Coverage())
        .withAllowKbToAddTitles(true)
    );
    assertDoesNotThrow(() -> validator.validate(packagePutRequest));
  }

  @Test
  void shouldThrowExceptionWhenPackageIsNotSelectedAndIsHiddenIsTrue() {
    var request = PackagesTestUtil.getPackagePutRequest(
      new PackagePutDataAttributes()
        .withIsSelected(false)
        .withVisibility(List.of(new PackageVisibility().withCategory(PackageVisibility.Category.PF).withHidden(true)))
    );

    var exception = assertThrows(InputValidationException.class, () -> validator.validate(request));
    assertTrue(exception.getMessage().contains("visibility"));
  }

  @Test
  void shouldThrowExceptionWhenPackageIsNotSelectedAndCoverageIsNotEmpty() {
    var request = PackagesTestUtil.getPackagePutRequest(
      new PackagePutDataAttributes()
        .withIsSelected(false)
        .withCustomCoverage(new Coverage()
          .withBeginCoverage("2000-01-01")));

    var exception = assertThrows(InputValidationException.class, () -> validator.validate(request));
    assertTrue(exception.getMessage().contains("beginCoverage"));
  }

  @Test
  void shouldThrowExceptionWhenPackageIsNotSelectedAndAllowToAddTitlesTrue() {
    var request = PackagesTestUtil.getPackagePutRequest(
      new PackagePutDataAttributes()
        .withIsSelected(false)
        .withAllowKbToAddTitles(true));

    var exception = assertThrows(InputValidationException.class, () -> validator.validate(request));
    assertTrue(exception.getMessage().contains("allowKbToAddTitles"));
  }

  @Test
  void shouldThrowExceptionWhenPackageIsNotSelectedAndTokenIsNotEmpty() {
    var request = PackagesTestUtil.getPackagePutRequest(
      new PackagePutDataAttributes()
        .withIsSelected(false)
        .withPackageToken(new Token().withValue("tokenValue")));

    var exception = assertThrows(InputValidationException.class, () -> validator.validate(request));
    assertTrue(exception.getMessage().contains("value"));
  }

  @Test
  void shouldThrowExceptionWhenPackageIsSelectedAndTokenIsTooLong() {
    var request = PackagesTestUtil.getPackagePutRequest(
      new PackagePutDataAttributes()
        .withIsSelected(true)
        .withPackageToken(new Token().withValue(StringUtils.repeat("tokenvalue", 200))));

    var exception = assertThrows(InputValidationException.class, () -> validator.validate(request));
    assertTrue(exception.getMessage().contains("value"));
  }

  @Test
  void shouldThrowExceptionWhenPackageIsSelectedAndCoverageDateIsInvalid() {
    var request = PackagesTestUtil.getPackagePutRequest(
      new PackagePutDataAttributes()
        .withIsSelected(true)
        .withCustomCoverage(new Coverage()
          .withBeginCoverage("abcd-ab-ab")));

    var exception = assertThrows(InputValidationException.class, () -> validator.validate(request));
    assertTrue(exception.getMessage().contains("beginCoverage"));
  }

  @Test
  void shouldValidateWhenPackageIsSelectedAndCoverageDateIsEmpty() {
    var packagePutRequest = PackagesTestUtil.getPackagePutRequest(
      new PackagePutDataAttributes()
        .withIsSelected(true)
        .withCustomCoverage(new Coverage()
          .withBeginCoverage("")));
    assertDoesNotThrow(() -> validator.validate(packagePutRequest));
  }
}
