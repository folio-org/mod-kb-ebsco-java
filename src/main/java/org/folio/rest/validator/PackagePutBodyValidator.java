package org.folio.rest.validator;

import java.util.List;
import org.folio.rest.exception.InputValidationException;
import org.folio.rest.jaxrs.model.PackagePutDataAttributes;
import org.folio.rest.jaxrs.model.PackagePutRequest;
import org.folio.rest.jaxrs.model.PackageVisibility;
import org.springframework.stereotype.Component;

@Component
public class PackagePutBodyValidator {

  private static final String INVALID_REQUEST_BODY_TITLE = "Invalid request body";
  private static final String INVALID_REQUEST_BODY_DETAILS = "Json body must contain data.attributes";
  private static final int MAX_TOKEN_LENGTH = 500;

  public void validate(PackagePutRequest request) {
    if (request == null || request.getData() == null || request.getData().getAttributes() == null) {
      throw new InputValidationException(INVALID_REQUEST_BODY_TITLE, INVALID_REQUEST_BODY_DETAILS);
    }
    PackagePutDataAttributes attributes = request.getData().getAttributes();
    Boolean allowKbToAddTitles = attributes.getAllowKbToAddTitles();

    String beginCoverage = null;
    String endCoverage = null;
    if (attributes.getCustomCoverage() != null) {
      beginCoverage = attributes.getCustomCoverage().getBeginCoverage();
      endCoverage = attributes.getCustomCoverage().getEndCoverage();
    }

    String value = attributes.getPackageToken() != null ? attributes.getPackageToken().getValue() : null;

    validateNotSelected(attributes, allowKbToAddTitles, attributes.getVisibility(), beginCoverage, endCoverage, value);
    ValidatorUtil.checkMaxLength("value", value, MAX_TOKEN_LENGTH);
    ValidatorUtil.checkDateValid("beginCoverage", beginCoverage);
    ValidatorUtil.checkDateValid("endCoverage", endCoverage);
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
