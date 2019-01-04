package org.folio.rest.validator;

import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.folio.rest.exception.InputValidationException;
import org.folio.rest.jaxrs.model.Coverage;
import org.folio.rest.jaxrs.model.EmbargoPeriod.EmbargoUnit;
import org.folio.rest.jaxrs.model.ResourceDataAttributes;
import org.folio.rest.jaxrs.model.ResourcePutRequest;
import org.springframework.stereotype.Component;

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

    ResourceDataAttributes attributes = request.getData().getAttributes();

    Boolean isSelected = attributes.getIsSelected();
    if (Objects.isNull(isSelected)) {
      throw new InputValidationException(INVALID_REQUEST_BODY_TITLE, IS_SELECTED_MUST_NOT_BE_EMPTY);
    }
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
        validateCustomResource(attributes);
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
    } else {
      validateManagedResourceIfNotSelected(attributes, isTitleCustom, cvgStmt);
    }
  }

  private void validateManagedResourceIfNotSelected(ResourceDataAttributes attributes, boolean isTitleCustom, String cvgStmt) {
    Boolean isHidden = attributes.getVisibilityData() != null ? attributes.getVisibilityData().getIsHidden() : null;
    EmbargoUnit embargoUnit = attributes.getCustomEmbargoPeriod() != null ? attributes.getCustomEmbargoPeriod().getEmbargoUnit() : null;
    List<Coverage> customCoverages = attributes.getCustomCoverages();
    if (!isTitleCustom && (!Objects.isNull(cvgStmt) || !Objects.isNull(embargoUnit) || (!Objects.isNull(isHidden) && isHidden) || (!Objects.isNull(customCoverages) && !customCoverages.isEmpty()))) {
      throw new InputValidationException(INVALID_IS_SELECTED_TITLE, INVALID_IS_SELECTED_DETAILS);
    }
  }

  private void validateCustomResource(ResourceDataAttributes attributes) {
    String name = attributes.getName();
    String pubType = attributes.getPublicationType() != null ? attributes.getPublicationType().value() : null;
    String pubName = attributes.getPublisherName();
    String edition = attributes.getEdition();
    String description = attributes.getDescription();
    String url = attributes.getUrl();

    ValidatorUtil.checkIsBlank("name", name);
    ValidatorUtil.checkMaxLength("name", name, 400);
    ValidatorUtil.checkIsBlank("publicationType", pubType);
    if (!StringUtils.isBlank(pubName)) {
      ValidatorUtil.checkMaxLength("publisherName", pubName, 250);
    }
    if (!StringUtils.isBlank(edition)) {
      ValidatorUtil.checkMaxLength("edition", edition, 250);
    }
    if (!StringUtils.isBlank(description)) {
      ValidatorUtil.checkMaxLength("description", description, 400);
    }
    if (!StringUtils.isBlank(url)) {
      ValidatorUtil.checkMaxLength("url", url, 600);
      ValidatorUtil.checkUrlFormat("url", url);
    }
    attributes.getIdentifiers().forEach(identifier -> ValidatorUtil.checkIdentifierValid("identifier", identifier));
  }
}

