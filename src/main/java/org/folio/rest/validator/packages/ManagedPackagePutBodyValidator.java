package org.folio.rest.validator.packages;

import java.util.List;
import org.folio.rest.exception.InputValidationException;
import org.folio.rest.jaxrs.model.PackagePutDataAttributes;
import org.folio.rest.jaxrs.model.PackagePutRequest;
import org.folio.rest.jaxrs.model.PackageVisibility;
import org.folio.rest.validator.ValidatorUtil;
import org.springframework.stereotype.Component;

@Component
public class ManagedPackagePutBodyValidator {

  private static final String INVALID_REQUEST_BODY_TITLE = "Invalid request body";
  private static final String INVALID_REQUEST_BODY_DETAILS = "Json body must contain data.attributes";
  private static final int MAX_TOKEN_LENGTH = 500;

  private final PackageCustomAttributesValidator customAttributesValidator;

  public ManagedPackagePutBodyValidator(PackageCustomAttributesValidator customAttributesValidator) {
    this.customAttributesValidator = customAttributesValidator;
  }

  public void validate(PackagePutRequest request) {
    if (request == null || request.getData() == null || request.getData().getAttributes() == null) {
      throw new InputValidationException(INVALID_REQUEST_BODY_TITLE, INVALID_REQUEST_BODY_DETAILS);
    }
    PackagePutDataAttributes attributes = request.getData().getAttributes();

    String beginCoverage = attributes.getCustomCoverage() != null
      ? attributes.getCustomCoverage().getBeginCoverage() : null;
    String endCoverage = attributes.getCustomCoverage() != null
      ? attributes.getCustomCoverage().getEndCoverage() : null;
    String tokenValue = attributes.getPackageToken() != null
      ? attributes.getPackageToken().getValue() : null;

    validateNotSelected(attributes, attributes.getAllowKbToAddTitles(),
      attributes.getVisibility(), beginCoverage, endCoverage, tokenValue);
    ValidatorUtil.checkMaxLength("value", tokenValue, MAX_TOKEN_LENGTH);
    customAttributesValidator.validate(new PackageCustomAttributesValidator.PackageCustomAttributes(
      attributes.getCustomDescription(),
      attributes.getCustomDisplayName(),
      attributes.getUrl(),
      attributes.getCustomAltNames(),
      attributes.getCustomCoverage()));
  }

  private void validateNotSelected(PackagePutDataAttributes attributes, Boolean allowKbToAddTitles,
                                   List<PackageVisibility> visibility,
                                   String beginCoverage, String endCoverage, String value) {
    Boolean isSelected = attributes.getIsSelected();
    if (isSelected == null || !isSelected) {
      ValidatorUtil.checkFalseOrNull("allowKbToAddTitles", allowKbToAddTitles);
      ValidatorUtil.checkIsEmptyCollection("visibility", visibility);
      ValidatorUtil.checkIsEmpty("customCoverage.beginCoverage", beginCoverage);
      ValidatorUtil.checkIsEmpty("customCoverage.endCoverage", endCoverage);
      ValidatorUtil.checkIsNull("packageToken.value", value);
    }
  }
}
