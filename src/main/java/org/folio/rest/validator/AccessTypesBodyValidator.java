package org.folio.rest.validator;

import org.springframework.stereotype.Component;

import org.folio.rest.exception.InputValidationException;
import org.folio.rest.jaxrs.model.AccessTypeCollectionItem;
import org.folio.rest.jaxrs.model.AccessTypeDataAttributes;

@Component
public class AccessTypesBodyValidator {

  private static final String INVALID_REQUEST_BODY_TITLE = "Invalid request body";
  private static final String INVALID_REQUEST_BODY_DETAILS = "Json body must contain data.attributes";

  /**
   * @throws InputValidationException  if validation of attributes fails
   */
  public void validate(AccessTypeCollectionItem request) {
    if (request == null || request.getAttributes() == null) {
      throw new InputValidationException(INVALID_REQUEST_BODY_TITLE, INVALID_REQUEST_BODY_DETAILS);
    }
    AccessTypeDataAttributes attributes = request.getAttributes();
    ValidatorUtil.checkIsNotEmpty("name", attributes.getName());
    ValidatorUtil.checkMaxLength("name", attributes.getName(), 75);

    if (attributes.getDescription() != null) {
      ValidatorUtil.checkMaxLength("description", attributes.getDescription(), 150);
    }
  }
}
