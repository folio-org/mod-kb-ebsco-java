package org.folio.rest.validator;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.folio.rest.exception.InputValidationException;
import org.junit.jupiter.api.Test;

class AssignedUsersBodyValidatorTest {

  private final AssignedUsersBodyValidator validator = new AssignedUsersBodyValidator();

  @Test
  void shouldThrowExceptionWhenNoData() {
    assertThrows(InputValidationException.class, () -> validator.validate(null));
  }
}
