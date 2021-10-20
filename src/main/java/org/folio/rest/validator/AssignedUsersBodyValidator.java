package org.folio.rest.validator;

import java.util.Objects;
import javax.validation.ValidationException;

import org.springframework.stereotype.Component;

import org.folio.rest.exception.InputValidationException;
import org.folio.rest.jaxrs.model.AssignedUser;
import org.folio.rest.jaxrs.model.AssignedUserDataAttributes;

@Component
public class AssignedUsersBodyValidator {

  private static final String INVALID_POST_BODY = "Invalid request body";

  /**
   * Provides validation for the data attributes
   *
   * @throws ValidationException if validation fails
   */
  public void validate(AssignedUser data) {

    if (Objects.isNull(data) || Objects.isNull(data.getAttributes())) {
      throw new InputValidationException(INVALID_POST_BODY, "");
    }

    AssignedUserDataAttributes attributes = data.getAttributes();

    ValidatorUtil.checkIsNotBlank("userName", attributes.getUserName());
    ValidatorUtil.checkMaxLength("userName", attributes.getUserName(), 200);

    ValidatorUtil.checkIsNotBlank("lastName", attributes.getLastName());
    ValidatorUtil.checkMaxLength("lastName", attributes.getLastName(), 200);

    ValidatorUtil.checkIsNotBlank("patronGroup", attributes.getPatronGroup());
    ValidatorUtil.checkMaxLength("patronGroup", attributes.getPatronGroup(), 200);

    if (!Objects.isNull(attributes.getFirstName())) {
      ValidatorUtil.checkIsNotBlank("firstName", attributes.getFirstName());
      ValidatorUtil.checkMaxLength("firstName", attributes.getFirstName(), 200);
    }
  }
}
