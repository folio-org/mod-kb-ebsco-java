package org.folio.rest.validator;

import org.junit.Test;

import javax.validation.ValidationException;

public class TitleParametersValidatorTest {

  private final TitleParametersValidator validator = new TitleParametersValidator();

  @Test
  public void shouldNotThrowExceptionWhenParametersAreValid() {
    validator.validate("true", "book", "ebsco",
      null, null, null, "relevance");
  }

  @Test(expected = ValidationException.class)
  public void shouldThrowExceptionWhenSelectedParameterIsInvalid() {
    validator.validate("doNotEnter", "book", "ebsco",
      null, null, null, "relevance");
  }

  @Test(expected = ValidationException.class)
  public void shouldThrowExceptionWhenTypeParameterIsInvalid() {
    validator.validate("true", "doNotEnter", "ebsco",
      null, null, null, "relevance");
  }

  @Test(expected = ValidationException.class)
  public void shouldThrowExceptionWhenThereAreConflictingParameters() {
    validator.validate("true", "book", "ebsco",
      null, "history", null, "relevance");
  }
  
  /* One of filter[name], filter[isxn], filter[subject] or filter[publisher] is required */
  @Test(expected = ValidationException.class)
  public void shouldThrowExceptionWhenAtLeastOneRequiredFilterParametersIsNotProvided() {
    validator.validate("true", "book", null,
      null, null, null, null);
  }

  @Test(expected = ValidationException.class)
  public void shouldThrowExceptionWhenFilterNameParameterIsEmpty() {
    validator.validate("true", "book", "",
      null, null, null, "relevance");
  }
  
  @Test(expected = ValidationException.class)
  public void shouldThrowExceptionWhenFilterIsxnParameterIsEmpty() {
    validator.validate("true", "book", null,
      "", null, null, null);
  }
  
  @Test(expected = ValidationException.class)
  public void shouldThrowExceptionWhenFilterSubjectParameterIsEmpty() {
    validator.validate("true", "book", null,
      null, "", null, null);
  }
  
  @Test(expected = ValidationException.class)
  public void shouldThrowExceptionWhenFilterPublisherParameterIsEmpty() {
    validator.validate("true", "book", null,
      null, null, "", null);
  }
}
