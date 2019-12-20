package org.folio.rest.validator;

import org.springframework.stereotype.Component;

import org.folio.rest.exception.InputValidationException;
import org.folio.rest.jaxrs.model.CustomLabelDataAttributes;
import org.folio.rest.jaxrs.model.CustomLabelPutRequest;

@Component
public class CustomLabelsPutBodyValidator {

  private static final String INVALID_REQUEST_BODY_TITLE = "Invalid request body";
  private static final String INVALID_REQUEST_BODY_DETAILS = "Json body must contain data.attributes";
  private static final String MUST_HAVE_EQUAL_IDS = "Ids should be equal!  %d != %d";
  private static final int CUSTOM_LABEL_NAME_MAX_LENGTH = 50;
  private static final String CUSTOM_LABEL_ID = "Custom Label id";

  public void validate(CustomLabelPutRequest request, int customLabelId) {
    if (request == null ||
      request.getData() == null ||
      request.getData().getAttributes() == null) {
      throw new InputValidationException(INVALID_REQUEST_BODY_TITLE, INVALID_REQUEST_BODY_DETAILS);
    }
    final CustomLabelDataAttributes attributes = request.getData().getAttributes();
    final Integer id = attributes.getId();
    ValidatorUtil.checkInRange(1, 5, id, CUSTOM_LABEL_ID);
    checkIsEqual(customLabelId, id);
    ValidatorUtil.checkMaxLength("Custom Label Name", attributes.getDisplayLabel(), CUSTOM_LABEL_NAME_MAX_LENGTH);
    ValidatorUtil.checkIsNotNull("Full Text Finder", attributes.getDisplayOnFullTextFinder());
    ValidatorUtil.checkIsNotNull("Publication Finder", attributes.getDisplayOnPublicationFinder());
  }

  private void checkIsEqual(int customLabelId, Integer id) {
    if(customLabelId != id) {
      throw new InputValidationException(
        String.format("Invalid %s", CUSTOM_LABEL_ID),
        String.format(MUST_HAVE_EQUAL_IDS, customLabelId, id));
    }
  }
}
