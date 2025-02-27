package org.folio.rest.validator;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThrows;

import org.folio.rest.exception.InputValidationException;
import org.folio.rest.impl.PackagesTestData;
import org.folio.rest.jaxrs.model.ContentType;
import org.folio.rest.jaxrs.model.Coverage;
import org.folio.rest.jaxrs.model.PackagePutDataAttributes;
import org.junit.Test;

public class CustomPackagePutBodyValidatorTest {

  private final CustomPackagePutBodyValidator validator = new CustomPackagePutBodyValidator();

  @Test
  public void shouldThrowExceptionOnInvalidCoverageDate() {
    var request = PackagesTestData.getPackagePutRequest(
      new PackagePutDataAttributes()
        .withIsSelected(true)
        .withContentType(ContentType.MIXED_CONTENT)
        .withName("package name")
        .withCustomCoverage(new Coverage()
          .withBeginCoverage("abcd-10-ab")));
    var exception = assertThrows(InputValidationException.class, () -> validator.validate(request));
    assertThat(exception.getMessage(), containsString("beginCoverage"));
  }

  @Test
  public void shouldThrowExceptionOnEmptyName() {
    var request = PackagesTestData.getPackagePutRequest(
      new PackagePutDataAttributes()
        .withContentType(ContentType.MIXED_CONTENT)
        .withName(""));
    var exception = assertThrows(InputValidationException.class, () -> validator.validate(request));
    assertThat(exception.getMessage(), containsString("name"));
  }

  @Test
  public void shouldThrowExceptionOnNullContentType() {
    var request = PackagesTestData.getPackagePutRequest(
      new PackagePutDataAttributes()
        .withName("package name"));
    var exception = assertThrows(InputValidationException.class, () -> validator.validate(request));
    assertThat(exception.getMessage(), containsString("contentType"));
  }
}
