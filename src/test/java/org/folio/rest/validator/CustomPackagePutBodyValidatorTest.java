package org.folio.rest.validator;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.folio.rest.exception.InputValidationException;
import org.folio.rest.jaxrs.model.ContentType;
import org.folio.rest.jaxrs.model.Coverage;
import org.folio.rest.jaxrs.model.PackagePutDataAttributes;
import org.folio.util.PackagesTestUtil;
import org.junit.jupiter.api.Test;

class CustomPackagePutBodyValidatorTest {

  private final CustomPackagePutBodyValidator validator = new CustomPackagePutBodyValidator();

  @Test
  void shouldThrowExceptionOnInvalidCoverageDate() {
    var request = PackagesTestUtil.getPackagePutRequest(
      new PackagePutDataAttributes()
        .withIsSelected(true)
        .withContentType(ContentType.MIXED_CONTENT)
        .withName("package name")
        .withCustomCoverage(new Coverage()
          .withBeginCoverage("abcd-10-ab")));
    var exception = assertThrows(InputValidationException.class, () -> validator.validate(request));
    assertTrue(exception.getMessage().contains("beginCoverage"));
  }

  @Test
  void shouldThrowExceptionOnEmptyName() {
    var request = PackagesTestUtil.getPackagePutRequest(
      new PackagePutDataAttributes()
        .withContentType(ContentType.MIXED_CONTENT)
        .withName(""));
    var exception = assertThrows(InputValidationException.class, () -> validator.validate(request));
    assertTrue(exception.getMessage().contains("name"));
  }

  @Test
  void shouldThrowExceptionOnNullContentType() {
    var request = PackagesTestUtil.getPackagePutRequest(
      new PackagePutDataAttributes()
        .withName("package name"));
    var exception = assertThrows(InputValidationException.class, () -> validator.validate(request));
    assertTrue(exception.getMessage().contains("contentType"));
  }
}
