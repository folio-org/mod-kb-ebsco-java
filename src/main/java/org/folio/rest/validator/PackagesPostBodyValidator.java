package org.folio.rest.validator;

import java.util.Objects;
import javax.validation.ValidationException;
import org.apache.commons.lang.StringUtils;
import org.folio.rest.exception.InputValidationException;
import org.folio.rest.jaxrs.model.Coverage;
import org.folio.rest.jaxrs.model.PackagePostRequest;
import org.springframework.stereotype.Component;

/**
 * Verifies that post data for packages are valid
 */
@Component
public class PackagesPostBodyValidator {

  private static final String INVALID_BEGIN_COVERAGE = "Begin Coverage has an invalid date.";
  private static final String INVALID_POST_BODY = "Invalid request body";

  /**
   * Provides validation for the post data attributes
   *
   * @throws ValidationException if validation fails
   */
  public void validate(PackagePostRequest entity) {

    if (Objects.isNull(entity) || Objects.isNull(entity.getData()) || Objects.isNull(entity.getData().getAttributes())) {
      throw new InputValidationException(INVALID_POST_BODY, "");
    }

    ValidatorUtil.checkIsNotBlank("name", entity.getData().getAttributes().getName());
    ValidatorUtil.checkMaxLength("name", entity.getData().getAttributes().getName(), 200);
    ValidatorUtil.checkIsNotNull("Content type", entity.getData().getAttributes().getContentType());

    Coverage customCoverage = entity.getData().getAttributes().getCustomCoverage();
    if (Objects.nonNull(customCoverage)) {

      String beginCoverage = customCoverage.getBeginCoverage();
      String endCoverage = customCoverage.getEndCoverage();

      ValidatorUtil.checkIsNotNull("beginCoverage", beginCoverage);
      ValidatorUtil.checkDateValid("beginCoverage", beginCoverage);

      if (beginCoverage.isEmpty() && !StringUtils.isEmpty(endCoverage)) {
        throw new InputValidationException(INVALID_BEGIN_COVERAGE, "");
      }

      if (StringUtils.isNotBlank(endCoverage)) {
        ValidatorUtil.checkDateValid("endCoverage", endCoverage);
        ValidatorUtil.checkDatesOrder(beginCoverage, endCoverage);
      }
    }
  }

}
