package org.folio.rest.validator;

import java.util.Objects;
import javax.validation.ValidationException;

import org.springframework.stereotype.Component;

import org.folio.rest.exception.InputValidationException;
import org.folio.rest.jaxrs.model.AssignedUser;

@Component
public class AssignedUsersBodyValidator {

  private static final String INVALID_POST_BODY = "Invalid request body";

  /**
   * Provides validation for the data attributes
   *
   * @throws ValidationException if validation fails
   */
  public void validate(AssignedUser data) {
    if (Objects.isNull(data)) {
      throw new InputValidationException(INVALID_POST_BODY, "");
    }

  }
}
