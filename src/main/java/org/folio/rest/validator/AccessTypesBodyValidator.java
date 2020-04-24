package org.folio.rest.validator;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import org.folio.rest.exception.InputValidationException;
import org.folio.rest.jaxrs.model.AccessType;
import org.folio.rest.jaxrs.model.AccessTypeDataAttributes;

@Component
public class AccessTypesBodyValidator {

  private static final String INVALID_REQUEST_BODY_TITLE = "Invalid request body";
  private static final String INVALID_REQUEST_BODY_DETAILS = "Json body must contain data.attributes";

  private final int maxNameLength;
  private final int maxDescriptionLength;

  public AccessTypesBodyValidator(@Value("${kb.ebsco.credentials.access.types.name.length.max:75}") int maxNameLength,
                                  @Value("${kb.ebsco.credentials.access.types.description.length.max:150}") int maxDescriptionLength) {
    this.maxNameLength = maxNameLength;
    this.maxDescriptionLength = maxDescriptionLength;
  }

  /**
   * @throws InputValidationException  if validation of attributes fails
   */
  public void validate(String credentialsId, AccessType request) {
    if (request == null || request.getAttributes() == null) {
      throw new InputValidationException(INVALID_REQUEST_BODY_TITLE, INVALID_REQUEST_BODY_DETAILS);
    }
    AccessTypeDataAttributes attributes = request.getAttributes();
    ValidatorUtil.checkIsNotEmpty("name", attributes.getName());
    ValidatorUtil.checkMaxLength("name", attributes.getName(), maxNameLength);

    if (attributes.getDescription() != null) {
      ValidatorUtil.checkMaxLength("description", attributes.getDescription(), maxDescriptionLength);
    }

    if (attributes.getCredentialsId() != null) {
      ValidatorUtil.checkIsEqual("credentialsId", credentialsId, attributes.getCredentialsId());
    }
  }
}
