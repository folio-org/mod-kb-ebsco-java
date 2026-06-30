package org.folio.rest.validator.packages;

import jakarta.validation.ValidationException;
import java.util.Objects;
import org.folio.rest.exception.InputValidationException;
import org.folio.rest.jaxrs.model.Coverage;
import org.folio.rest.jaxrs.model.PackagePostRequest;
import org.folio.rest.validator.ValidatorUtil;
import org.springframework.stereotype.Component;

/**
 * Verifies that post data for packages are valid.
 */
@Component
public class PackagesPostBodyValidator {

  private static final String INVALID_POST_BODY = "Invalid request body";

  private final PackageCustomAttributesValidator customAttributesValidator;

  public PackagesPostBodyValidator(PackageCustomAttributesValidator customAttributesValidator) {
    this.customAttributesValidator = customAttributesValidator;
  }

  /**
   * Provides validation for the post data attributes.
   *
   * @throws ValidationException if validation fails
   */
  public void validate(PackagePostRequest entity) {
    if (Objects.isNull(entity) || Objects.isNull(entity.getData())
      || Objects.isNull(entity.getData().getAttributes())) {
      throw new InputValidationException(INVALID_POST_BODY, "");
    }
    var attributes = entity.getData().getAttributes();
    ValidatorUtil.checkIsNotBlank("name", attributes.getName());
    ValidatorUtil.checkMaxLength("name", attributes.getName(), 200);
    ValidatorUtil.checkIsNotNull("Content type", attributes.getContentType());
    checkBeginCoverageNotNull(attributes.getCustomCoverage());
    customAttributesValidator.validate(new PackageCustomAttributesValidator.PackageCustomAttributes(
      attributes.getCustomDescription(),
      attributes.getCustomDisplayName(),
      attributes.getUrl(),
      attributes.getCustomAltNames(),
      attributes.getCustomCoverage()));
  }

  private void checkBeginCoverageNotNull(Coverage coverage) {
    if (coverage != null) {
      ValidatorUtil.checkIsNotNull("beginCoverage", coverage.getBeginCoverage());
    }
  }
}
