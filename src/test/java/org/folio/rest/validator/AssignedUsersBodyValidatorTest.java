package org.folio.rest.validator;

import org.folio.rest.exception.InputValidationException;
import org.junit.Assert;
import org.junit.Test;

public class AssignedUsersBodyValidatorTest {

  private final AssignedUsersBodyValidator validator = new AssignedUsersBodyValidator();

  @Test
  public void shouldThrowExceptionWhenNoData() {
    Assert.assertThrows(InputValidationException.class, () -> validator.validate(null));
  }
}
