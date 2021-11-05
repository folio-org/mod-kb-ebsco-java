package org.folio.rest.validator;

import org.springframework.stereotype.Component;

import org.folio.rest.exception.InputValidationException;
import org.folio.rest.jaxrs.model.PackageTagsDataAttributes;
import org.folio.rest.jaxrs.model.PackageTagsPutRequest;

@Component
public class PackageTagsPutBodyValidator {
  private static final String INVALID_REQUEST_BODY_TITLE = "Invalid request body";
  private static final String INVALID_REQUEST_BODY_DETAILS = "Json body must contain data.attributes";

  public void validate(PackageTagsPutRequest request) {
    if (request == null ||
      request.getData() == null ||
      request.getData().getAttributes() == null) {
      throw new InputValidationException(INVALID_REQUEST_BODY_TITLE, INVALID_REQUEST_BODY_DETAILS);
    }
    PackageTagsDataAttributes attributes = request.getData().getAttributes();

    ValidatorUtil.checkIsNotBlank("name", attributes.getName());
    ValidatorUtil.checkMaxLength("name", attributes.getName(), 200);
    ValidatorUtil.checkIsNotNull("contentType", attributes.getContentType());
    ValidatorUtil.checkIsNotNull("tags", attributes.getTags());
  }
}
