package org.folio.rest.validator;

import org.folio.rest.exception.InputValidationException;
import org.folio.rest.jaxrs.model.ProviderTagsDataAttributes;
import org.folio.rest.jaxrs.model.ProviderTagsPutRequest;
import org.springframework.stereotype.Component;

@Component
public class ProviderTagsPutBodyValidator {

  private static final String INVALID_REQUEST_BODY_TITLE = "Invalid request body";
  private static final String INVALID_REQUEST_BODY_DETAILS = "Json body must contain data.attributes";

  public void validate(ProviderTagsPutRequest request, ProviderTagsDataAttributes attributes) {
    if (request == null || request.getData() == null || request.getData().getAttributes() == null) {
      throw new InputValidationException(INVALID_REQUEST_BODY_TITLE, INVALID_REQUEST_BODY_DETAILS);
    }

    ValidatorUtil.checkIsNotBlank("name", attributes.getName());
    ValidatorUtil.checkMaxLength("name", attributes.getName(), 200);
    ValidatorUtil.checkIsNotNull("tags", attributes.getTags());
  }
}
