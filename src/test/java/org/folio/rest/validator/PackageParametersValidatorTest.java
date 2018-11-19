package org.folio.rest.validator;

import javax.validation.ValidationException;
import org.junit.Test;

public class PackageParametersValidatorTest {

  private final PackageParametersValidator validator = new PackageParametersValidator();

  @Test
  public void shouldNotThrowExceptionWhenParametersAreValid() {
    validator.validate(null, null, null, "relevance");
  }

  @Test(expected = ValidationException.class)
  public void shouldNotThrowExceptionWhenFilterCustomIsInvalid() {
    validator.validate("false", null, null, "relevance");
  }

  @Test(expected = ValidationException.class)
  public void shouldNotThrowExceptionWhenFilterSelectedIsInvalid() {
    validator.validate("true", "notall", null, "relevance");
  }

  @Test(expected = ValidationException.class)
  public void shouldNotThrowExceptionWhenFilterTypeIsInvalid() {
    validator.validate("true", "true", "notall", "relevance");
  }

  @Test(expected = ValidationException.class)
  public void shouldNotThrowExceptionWhenSortIsInvalid() {
    validator.validate("true", "true", "all", "abc");
  }

}
