package org.folio.rest.validator;

import org.folio.rest.exception.InputValidationException;
import org.folio.rest.jaxrs.model.AssignedUserPostRequest;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class AssignedUsersBodyValidatorTest {

  @Rule
  public ExpectedException expectedEx = ExpectedException.none();

  private final AssignedUsersBodyValidator validator = new AssignedUsersBodyValidator();

  @Test(expected = InputValidationException.class)
  public void shouldThrowExceptionWhenNoData() {
    AssignedUserPostRequest request = new AssignedUserPostRequest().withData(null);
    validator.validate(request.getData());
  }
}
