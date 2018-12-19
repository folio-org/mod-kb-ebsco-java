package org.folio.rest.validator;

import org.folio.rest.exception.InputValidationException;
import org.folio.rest.jaxrs.model.ContentType;
import org.folio.rest.jaxrs.model.PackageDataAttributes;
import org.folio.rest.jaxrs.model.PackagePutRequest;

public class CustomPackagePutBodyValidator
{
  private static final String INVALID_REQUEST_BODY_TITLE = "Invalid request body";
  private static final String INVALID_REQUEST_BODY_DETAILS = "Json body must contain data.attributes";

  public void validate(PackagePutRequest request) {
    if (request == null ||
      request.getData() == null ||
      request.getData().getAttributes() == null) {
      throw new InputValidationException(INVALID_REQUEST_BODY_TITLE, INVALID_REQUEST_BODY_DETAILS);
    }
    PackageDataAttributes attributes = request.getData().getAttributes();
    String name = attributes.getName();
    ContentType contentType = attributes.getContentType();

    String beginCoverage = null;
    String endCoverage = null;
    if(attributes.getCustomCoverage() !=null){
      beginCoverage =attributes.getCustomCoverage().getBeginCoverage();
      endCoverage = attributes.getCustomCoverage().getEndCoverage();
    }

    ValidatorUtil.checkIsNotEmpty("name", name);
    ValidatorUtil.checkMaxLength("name", name, 200);
    ValidatorUtil.checkIsNotNull("contentType", contentType);
    ValidatorUtil.checkDateValid("beginCoverage", beginCoverage);
    ValidatorUtil.checkDateValid("endCoverage", endCoverage);
  }
}
