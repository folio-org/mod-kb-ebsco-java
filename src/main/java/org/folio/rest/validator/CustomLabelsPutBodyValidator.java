package org.folio.rest.validator;

import org.springframework.stereotype.Component;

import org.folio.rest.exception.InputValidationException;
import org.folio.rest.jaxrs.model.CustomLabelDataAttributes;
import org.folio.rest.jaxrs.model.CustomLabelPutRequest;

@Component
public class CustomLabelsPutBodyValidator {

  private static final String INVALID_REQUEST_BODY_TITLE = "Invalid request body";
  private static final String INVALID_REQUEST_BODY_DETAILS = "Json body must contain data.attributes";
  private static final int CUSTOM_LABEL_NAME_MAX_LENGTH = 50;

  public void validate(CustomLabelPutRequest request, int customLabelId) {
    if (request == null ||
      request.getData() == null ||
      request.getData().getAttributes() == null) {
      throw new InputValidationException(INVALID_REQUEST_BODY_TITLE, INVALID_REQUEST_BODY_DETAILS);
    }
    final CustomLabelDataAttributes attributes = request.getData().getAttributes();
    final Integer id = attributes.getId();
    ValidatorUtil.checkInRange(1, 5, id, "Custom Label id");
    ValidatorUtil.checkIsEqual(customLabelId, id);
    ValidatorUtil.checkMaxLength("Custom Label Name", attributes.getDisplayLabel(), CUSTOM_LABEL_NAME_MAX_LENGTH);
    ValidatorUtil.checkIsNotNull("Full Text Finder", attributes.getDisplayOnFullTextFinder());
    ValidatorUtil.checkIsNotNull("Publication Finder", attributes.getDisplayOnPublicationFinder());
  }
}
