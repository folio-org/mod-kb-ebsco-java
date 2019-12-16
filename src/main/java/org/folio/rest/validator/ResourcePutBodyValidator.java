package org.folio.rest.validator;

import static org.folio.rest.validator.ValidationConstants.USER_DEFINED_FIELD_MAX_LENGTH;

import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Component;

import org.folio.rest.exception.InputValidationException;
import org.folio.rest.jaxrs.model.Coverage;
import org.folio.rest.jaxrs.model.EmbargoPeriod.EmbargoUnit;
import org.folio.rest.jaxrs.model.ResourcePutDataAttributes;
import org.folio.rest.jaxrs.model.ResourcePutRequest;

@Component
public class ResourcePutBodyValidator {

  private static final String INVALID_REQUEST_BODY_TITLE = "Invalid request body";
  private static final String INVALID_REQUEST_BODY_DETAILS = "Json body must contain data.attributes";
  private static final String INVALID_IS_SELECTED_TITLE = "Resource cannot be updated unless added to holdings";
  private static final String INVALID_IS_SELECTED_DETAILS = "Resource must be added to holdings to be able to update";
  private static final String IS_SELECTED_MUST_NOT_BE_EMPTY = "isSelected must not be empty";

  public void validate(ResourcePutRequest request, boolean isTitleCustom) {
    if (request == null ||
      request.getData() == null ||
      request.getData().getAttributes() == null) {
      throw new InputValidationException(INVALID_REQUEST_BODY_TITLE, INVALID_REQUEST_BODY_DETAILS);
    }

    ResourcePutDataAttributes attributes = request.getData().getAttributes();

    if (Objects.isNull(attributes.getIsSelected())) {
      throw new InputValidationException(INVALID_REQUEST_BODY_TITLE, IS_SELECTED_MUST_NOT_BE_EMPTY);
    }
    boolean isSelected = attributes.getIsSelected();
    String cvgStmt = attributes.getCoverageStatement();

    /*
     * Updates cannot be made to a resource unless it is selected
     */
    if (isSelected) {
      /*
       * Following fields can be updated only for a custom resource although UI sends complete payload
       * for both managed and custom resources
       */
      if (isTitleCustom) {
        String url = attributes.getUrl();
        if (!StringUtils.isBlank(url)) {
          ValidatorUtil.checkMaxLength("url", url, 600);
          ValidatorUtil.checkUrlFormat("url", url);
        }
      }

      /*
       * Following fields can be updated only for a managed resource although UI sends complete payload
       * for both managed and custom resources
       */
      if (!StringUtils.isBlank(cvgStmt)) {
        ValidatorUtil.checkMaxLength("coverageStatement", cvgStmt, 250);
      }
      attributes.getCustomCoverages().forEach(customCoverage -> {
        ValidatorUtil.checkDateValid("beginCoverage", customCoverage.getBeginCoverage());
        ValidatorUtil.checkDateValid("endCoverage", customCoverage.getEndCoverage());
      });

      ValidatorUtil.checkMaxLength("userDefinedField1", attributes.getUserDefinedField1(), USER_DEFINED_FIELD_MAX_LENGTH);
      ValidatorUtil.checkMaxLength("userDefinedField2", attributes.getUserDefinedField2(), USER_DEFINED_FIELD_MAX_LENGTH);
      ValidatorUtil.checkMaxLength("userDefinedField3", attributes.getUserDefinedField3(), USER_DEFINED_FIELD_MAX_LENGTH);
      ValidatorUtil.checkMaxLength("userDefinedField4", attributes.getUserDefinedField4(), USER_DEFINED_FIELD_MAX_LENGTH);
      ValidatorUtil.checkMaxLength("userDefinedField5", attributes.getUserDefinedField5(), USER_DEFINED_FIELD_MAX_LENGTH);
    } else {
      validateManagedResourceIfNotSelected(attributes, isTitleCustom, cvgStmt);
    }
  }

  private void validateManagedResourceIfNotSelected(ResourcePutDataAttributes attributes, boolean isTitleCustom, String cvgStmt) {
    Boolean isHidden = attributes.getVisibilityData() != null ? attributes.getVisibilityData().getIsHidden() : null;
    EmbargoUnit embargoUnit = attributes.getCustomEmbargoPeriod() != null ? attributes.getCustomEmbargoPeriod().getEmbargoUnit() : null;
    List<Coverage> customCoverages = attributes.getCustomCoverages();
    if (!isTitleCustom &&
      (!Strings.isEmpty(cvgStmt) || !Objects.isNull(embargoUnit) ||
      (!Objects.isNull(isHidden) && isHidden) ||
      (!Objects.isNull(customCoverages) && !customCoverages.isEmpty()) ||
        !Objects.isNull(attributes.getUserDefinedField1()) ||
        !Objects.isNull(attributes.getUserDefinedField2()) ||
        !Objects.isNull(attributes.getUserDefinedField3()) ||
        !Objects.isNull(attributes.getUserDefinedField4()) ||
        !Objects.isNull(attributes.getUserDefinedField5())
      )) {
      throw new InputValidationException(INVALID_IS_SELECTED_TITLE, INVALID_IS_SELECTED_DETAILS);
    }
  }

}

