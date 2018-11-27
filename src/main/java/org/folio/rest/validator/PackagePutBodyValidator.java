package org.folio.rest.validator;

import org.folio.rest.exception.InputValidationException;
import org.folio.rest.jaxrs.model.PackageDataAttributes;
import org.folio.rest.jaxrs.model.PackagePutRequest;

public class PackagePutBodyValidator {

  private static final String INVALID_REQUEST_BODY_TITLE = "Invalid request body";
  private static final String INVALID_REQUEST_BODY_DETAILS = "Json body must contain data.attributes";
  private static final int MAX_TOKEN_LENGTH = 500;

  public void validate(PackagePutRequest request) {
    if (request == null ||
      request.getData() == null ||
      request.getData().getAttributes() == null) {
      throw new InputValidationException(INVALID_REQUEST_BODY_TITLE, INVALID_REQUEST_BODY_DETAILS);
    }
    PackageDataAttributes attributes = request.getData().getAttributes();
    Boolean isSelected = attributes.getIsSelected();
    Boolean allowKbToAddTitles = attributes.getAllowKbToAddTitles();

    Boolean isHidden = attributes.getVisibilityData() != null ? attributes.getVisibilityData().getIsHidden() : null;
    String beginCoverage = null;
    String endCoverage = null;
    if (attributes.getCustomCoverage() != null) {
      beginCoverage = attributes.getCustomCoverage().getBeginCoverage();
      endCoverage = attributes.getCustomCoverage().getEndCoverage();
    }

    String value = attributes.getPackageToken() != null ? attributes.getPackageToken().getValue() : null;

    if (isSelected == null || !isSelected) {
      ValidatorUtil.checkFalseOrNull("allowKbToAddTitles", allowKbToAddTitles);
      ValidatorUtil.checkFalseOrNull("visibilityData.isHidden", isHidden);
      ValidatorUtil.checkIsEmpty("customCoverage.beginCoverage", beginCoverage);
      ValidatorUtil.checkIsEmpty("customCoverage.endCoverage", endCoverage);
      ValidatorUtil.checkIsNull("packageToken.value", value);
    }
    ValidatorUtil.checkMaxLength("value", value, MAX_TOKEN_LENGTH);
    ValidatorUtil.checkDateValid("beginCoverage", beginCoverage);
    ValidatorUtil.checkDateValid("endCoverage", endCoverage);
  }
}
