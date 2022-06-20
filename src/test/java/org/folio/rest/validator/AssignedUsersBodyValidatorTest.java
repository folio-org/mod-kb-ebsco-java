package org.folio.rest.validator;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.folio.rest.exception.InputValidationException;
import org.folio.rest.jaxrs.model.AssignedUserPostRequest;

public class AssignedUsersBodyValidatorTest {

  private static final int MAX_CHARACTERS = 200;
  private final AssignedUsersBodyValidator validator = new AssignedUsersBodyValidator();

  @Rule
  public ExpectedException expectedEx = ExpectedException.none();

  @Test(expected = InputValidationException.class)
  public void shouldThrowExceptionWhenNoData() {
    AssignedUserPostRequest request = new AssignedUserPostRequest().withData(null);
    validator.validate(request.getData());
  }
}
