package org.folio.rest.validator.packages;

import org.folio.rest.exception.InputValidationException;
import org.folio.rest.jaxrs.model.PackagePutRequest;
import org.folio.rest.validator.ValidatorUtil;
import org.springframework.stereotype.Component;

@Component
public class CustomPackagePutBodyValidator {

  private static final String INVALID_REQUEST_BODY_TITLE = "Invalid request body";
  private static final String INVALID_REQUEST_BODY_DETAILS = "Json body must contain data.attributes";

  private final PackageCustomAttributesValidator customAttributesValidator;

  public CustomPackagePutBodyValidator(PackageCustomAttributesValidator customAttributesValidator) {
    this.customAttributesValidator = customAttributesValidator;
  }

  public void validate(PackagePutRequest request) {
    if (request == null
        || request.getData() == null
        || request.getData().getAttributes() == null) {
      throw new InputValidationException(INVALID_REQUEST_BODY_TITLE, INVALID_REQUEST_BODY_DETAILS);
    }
    var attributes = request.getData().getAttributes();
    ValidatorUtil.checkIsNotBlank("name", attributes.getName());
    ValidatorUtil.checkMaxLength("name", attributes.getName(), 200);
    ValidatorUtil.checkIsNotNull("contentType", attributes.getContentType());
    customAttributesValidator.validate(new PackageCustomAttributesValidator.PackageCustomAttributes(
      attributes.getCustomDescription(),
      attributes.getCustomDisplayName(),
      attributes.getUrl(),
      attributes.getCustomAltNames(),
      attributes.getCustomCoverage()));
  }
}
