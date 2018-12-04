package org.folio.rest.validator;

import javax.validation.ValidationException;

import org.folio.rest.model.FilterQuery;
import org.junit.Before;
import org.junit.Test;

public class TitleParametersValidatorTest {

  private final TitleParametersValidator validator = new TitleParametersValidator();
  private FilterQuery.FilterQueryBuilder fqb;


  @Before
  public void setUp() {
    fqb = FilterQuery.builder();
  }

  @Test
  public void shouldNotThrowExceptionWhenParametersAreValid() {
    validator.validate(fqb.selected("true").type("book").name("ebsco").build(), "relevance");
  }

  @Test
  public void shouldNotThrowExceptionWhenNoFilterParametersAndAllowNullTrue() {
    validator.validate(fqb
      .selected(null).type(null)
      .name(null).isxn(null).subject(null)
      .publisher(null).build(), "relevance", true);
  }

  @Test(expected = ValidationException.class)
  public void shouldThrowExceptionWhenSelectedParameterIsInvalid() {
    validator.validate(fqb.selected("doNotEnter").type("book").name("ebsco").build(), "relevance");
  }

  @Test(expected = ValidationException.class)
  public void shouldThrowExceptionWhenTypeParameterIsInvalid() {
    validator.validate(fqb.selected("true").type("doNotEnter").name("ebsco").build(), "relevance");
  }

  @Test(expected = ValidationException.class)
  public void shouldThrowExceptionWhenThereAreConflictingParameters() {
    validator.validate(fqb.selected("true").type("book").name("ebsco").subject("history").build(), "relevance");
  }
  
  /* One of filter[name], filter[isxn], filter[subject] or filter[publisher] is required */
  @Test(expected = ValidationException.class)
  public void shouldThrowExceptionWhenAtLeastOneRequiredFilterParametersIsNotProvided() {
    validator.validate(fqb.selected("true").type("book").build(), null);
  }

  @Test(expected = ValidationException.class)
  public void shouldThrowExceptionWhenFilterNameParameterIsEmpty() {
    validator.validate(fqb.selected("true").type("book").name("").build(), "relevance");
  }

  @Test(expected = ValidationException.class)
  public void shouldThrowExceptionWhenFilterIsxnParameterIsEmpty() {
    validator.validate(fqb.selected("true").type("book").isxn("").build(), null);
  }

  @Test(expected = ValidationException.class)
  public void shouldThrowExceptionWhenFilterSubjectParameterIsEmpty() {
    validator.validate(fqb.selected("true").type("book").subject("").build(), null);
  }
  
  @Test(expected = ValidationException.class)
  public void shouldThrowExceptionWhenFilterPublisherParameterIsEmpty() {
    validator.validate(fqb.selected("true").type("book").publisher("").build(), null);
  }
}
