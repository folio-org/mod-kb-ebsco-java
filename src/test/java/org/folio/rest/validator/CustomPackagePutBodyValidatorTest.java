package org.folio.rest.validator;

import static org.hamcrest.Matchers.containsString;

import org.folio.rest.exception.InputValidationException;
import org.folio.rest.impl.PackagesTestData;
import org.folio.rest.jaxrs.model.ContentType;
import org.folio.rest.jaxrs.model.Coverage;
import org.folio.rest.jaxrs.model.PackagePutDataAttributes;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class CustomPackagePutBodyValidatorTest {

  @Rule
  public ExpectedException expectedEx = ExpectedException.none();

  private final CustomPackagePutBodyValidator validator = new CustomPackagePutBodyValidator();

  @Test
  public void shouldThrowExceptionOnInvalidCoverageDate() {
    expectedEx.expect(InputValidationException.class);
    expectedEx.expectMessage(containsString("beginCoverage"));
    validator.validate(PackagesTestData.getPackagePutRequest(
      new PackagePutDataAttributes()
        .withIsSelected(true)
        .withContentType(ContentType.MIXED_CONTENT)
        .withName("package name")
        .withCustomCoverage(new Coverage()
          .withBeginCoverage("abcd-10-ab"))));
  }

  @Test
  public void shouldThrowExceptionOnEmptyName() {
    expectedEx.expect(InputValidationException.class);
    expectedEx.expectMessage(containsString("name"));
    validator.validate(PackagesTestData.getPackagePutRequest(
      new PackagePutDataAttributes()
        .withContentType(ContentType.MIXED_CONTENT)
        .withName("")));
  }

  @Test
  public void shouldThrowExceptionOnNullContentType() {
    expectedEx.expect(InputValidationException.class);
    expectedEx.expectMessage(containsString("contentType"));
    validator.validate(PackagesTestData.getPackagePutRequest(
      new PackagePutDataAttributes()
        .withName("package name")));
  }
}
