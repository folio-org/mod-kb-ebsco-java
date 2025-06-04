package org.folio.rest.validator;

import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.folio.properties.customlabels.CustomLabelsProperties;
import org.folio.rest.exception.InputValidationException;
import org.folio.rest.jaxrs.model.Coverage;
import org.folio.rest.jaxrs.model.EmbargoPeriod.EmbargoUnit;
import org.folio.rest.jaxrs.model.ResourcePutDataAttributes;
import org.folio.rest.jaxrs.model.ResourcePutRequest;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ResourcePutBodyValidator {

  private static final String INVALID_IS_SELECTED_TITLE = "Resource cannot be updated unless added to holdings";
  private static final String INVALID_IS_SELECTED_DETAILS = "Resource must be added to holdings to be able to update";

  private final CustomLabelsProperties customLabelsProperties;

  public void validate(ResourcePutRequest request, boolean isTitleCustom) {
    ResourcePutDataAttributes attributes = request.getData().getAttributes();

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
        if (isNotBlank(url)) {
          ValidatorUtil.checkMaxLength("url", url, 600);
          ValidatorUtil.checkUrlFormat("url", url);
        }
      }

      /*
       * Following fields can be updated only for a managed resource although UI sends complete payload
       * for both managed and custom resources
       */
      if (isNotBlank(cvgStmt)) {
        ValidatorUtil.checkMaxLength("coverageStatement", cvgStmt, 250);
      }
      attributes.getCustomCoverages().forEach(customCoverage -> {
        ValidatorUtil.checkDateValid("beginCoverage", customCoverage.getBeginCoverage());
        ValidatorUtil.checkDateValid("endCoverage", customCoverage.getEndCoverage());
      });

      int valueMaxLength = customLabelsProperties.valueMaxLength();
      ValidatorUtil.checkMaxLength("userDefinedField1", attributes.getUserDefinedField1(), valueMaxLength);
      ValidatorUtil.checkMaxLength("userDefinedField2", attributes.getUserDefinedField2(), valueMaxLength);
      ValidatorUtil.checkMaxLength("userDefinedField3", attributes.getUserDefinedField3(), valueMaxLength);
      ValidatorUtil.checkMaxLength("userDefinedField4", attributes.getUserDefinedField4(), valueMaxLength);
      ValidatorUtil.checkMaxLength("userDefinedField5", attributes.getUserDefinedField5(), valueMaxLength);
    } else {
      validateManagedResourceIfNotSelected(attributes, isTitleCustom, cvgStmt);
    }
  }

  private void validateManagedResourceIfNotSelected(ResourcePutDataAttributes attributes, boolean isTitleCustom,
                                                    String cvgStmt) {
    Boolean isHidden = attributes.getVisibilityData() != null ? attributes.getVisibilityData().getIsHidden() : null;
    EmbargoUnit embargoUnit =
      attributes.getCustomEmbargoPeriod() != null ? attributes.getCustomEmbargoPeriod().getEmbargoUnit() : null;
    List<Coverage> customCoverages = attributes.getCustomCoverages();
    if (!isTitleCustom && (isNotEmpty(cvgStmt) || nonNull(embargoUnit)
      || nonNull(isHidden) && Boolean.TRUE.equals(isHidden)
      || nonNull(customCoverages) && !customCoverages.isEmpty()
      || isNotEmpty(attributes.getUserDefinedField1())
      || isNotEmpty(attributes.getUserDefinedField2())
      || isNotEmpty(attributes.getUserDefinedField3())
      || isNotEmpty(attributes.getUserDefinedField4())
      || isNotEmpty(attributes.getUserDefinedField5()))) {
      throw new InputValidationException(INVALID_IS_SELECTED_TITLE, INVALID_IS_SELECTED_DETAILS);
    }
  }
}

