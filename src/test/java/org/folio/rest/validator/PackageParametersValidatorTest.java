package org.folio.rest.validator;

import javax.validation.ValidationException;
import org.junit.Test;

public class PackageParametersValidatorTest {

  private final PackageParametersValidator validator = new PackageParametersValidator();

  @Test
  public void shouldNotThrowExceptionWhenParametersAreValid() {
    validator.validate(null, null, null, "relevance", "query");
  }

  @Test(expected = ValidationException.class)
  public void shouldThrowExceptionWhenFilterCustomIsInvalid() {
    validator.validate("false", null, null, "relevance", "query");
  }

  @Test(expected = ValidationException.class)
  public void shouldThrowExceptionWhenFilterSelectedIsInvalid() {
    validator.validate("true", "notall", null, "relevance", "query");
  }

  @Test(expected = ValidationException.class)
  public void shouldThrowExceptionWhenFilterTypeIsInvalid() {
    validator.validate("true", "true", "notall", "relevance", "query");
  }

  @Test(expected = ValidationException.class)
  public void shouldThrowExceptionWhenSortIsInvalid() {
    validator.validate("true", "true", "all", "abc", "query");
  }

  @Test(expected = ValidationException.class)
  public void shouldThrowExceptionWhenSearchQueryIsEmpty() {
    validator.validate("true", "true", "all", "abc", "");
  }

}
