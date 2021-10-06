package org.folio.rest.validator;

import org.springframework.stereotype.Component;

import org.folio.rest.exception.InputValidationException;
import org.folio.rest.jaxrs.model.PackagePutDataAttributes;
import org.folio.rest.jaxrs.model.PackagePutRequest;

@Component
public class PackagePutBodyValidator {

  private static final String INVALID_REQUEST_BODY_TITLE = "Invalid request body";
  private static final String INVALID_REQUEST_BODY_DETAILS = "Json body must contain data.attributes";
  private static final String PACKAGE_NOT_UPDATABLE_TITLE = "Package is not updatable";
  private static final String PACKAGE_NOT_UPDATABLE_DETAILS = "Non-custom packages must be selected";
  private static final int MAX_TOKEN_LENGTH = 500;

  public void validate(PackagePutRequest request) {
    if (request == null ||
      request.getData() == null ||
      request.getData().getAttributes() == null) {
      throw new InputValidationException(INVALID_REQUEST_BODY_TITLE, INVALID_REQUEST_BODY_DETAILS);
    }
    PackagePutDataAttributes attributes = request.getData().getAttributes();
    Boolean isSelected = attributes.getIsSelected();

    String beginCoverage = null;
    String endCoverage = null;
    if (attributes.getCustomCoverage() != null) {
      beginCoverage = attributes.getCustomCoverage().getBeginCoverage();
      endCoverage = attributes.getCustomCoverage().getEndCoverage();
    }

    String value = attributes.getPackageToken() != null ? attributes.getPackageToken().getValue() : null;

    if (isSelected == null || !isSelected) {
      throw new InputValidationException(PACKAGE_NOT_UPDATABLE_TITLE, PACKAGE_NOT_UPDATABLE_DETAILS);
    }
    ValidatorUtil.checkMaxLength("value", value, MAX_TOKEN_LENGTH);
    ValidatorUtil.checkDateValid("beginCoverage", beginCoverage);
    ValidatorUtil.checkDateValid("endCoverage", endCoverage);
  }
}
