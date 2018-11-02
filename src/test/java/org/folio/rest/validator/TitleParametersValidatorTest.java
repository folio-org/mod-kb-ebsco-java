package org.folio.rest.validator;

import org.junit.Test;

import javax.validation.ValidationException;

public class TitleParametersValidatorTest {

  private final TitleParametersValidator validator = new TitleParametersValidator();

  @Test
  public void shouldNotThrowExceptionWhenParametersAreValid() {
    validator.validate("true", "book", "ebsco",
      null, null, null, null);
  }

  @Test(expected = ValidationException.class)
  public void shouldThrowExceptionWhenSelectedParameterIsInvalid() {
    validator.validate("doNotEnter", "book", "ebsco",
      null, null, null, null);
  }

  @Test(expected = ValidationException.class)
  public void shouldThrowExceptionWhenTypeParameterIsInvalid() {
    validator.validate("true", "doNotEnter", "ebsco",
      null, null, null, null);
  }

  @Test(expected = ValidationException.class)
  public void shouldThrowExceptionWhenThereAreConflictingParameters() {
    validator.validate("true", "book", "ebsco",
      null, "history", null, null);
  }

  @Test(expected = ValidationException.class)
  public void shouldThrowExceptionWhenSearchParameterIsEmpty() {
    validator.validate("true", "book", "",
      null, null, null, null);
  }
}
