package org.folio.rest.validator.resources;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import lombok.RequiredArgsConstructor;
import org.folio.properties.customlabels.CustomLabelsProperties;
import org.folio.rest.jaxrs.model.ResourcePutDataAttributes;
import org.folio.rest.validator.ValidatorUtil;
import org.springframework.stereotype.Component;

/**
 * Validates the attributes that are updatable for both managed and custom resources when the resource is selected.
 */
@Component
@RequiredArgsConstructor
public class ResourceCommonAttributesValidator {

  private static final int COVERAGE_STATEMENT_MAX_LENGTH = 250;

  private final CustomLabelsProperties customLabelsProperties;

  public void validate(ResourcePutDataAttributes attributes) {
    String cvgStmt = attributes.getCoverageStatement();
    if (isNotBlank(cvgStmt)) {
      ValidatorUtil.checkMaxLength("coverageStatement", cvgStmt, COVERAGE_STATEMENT_MAX_LENGTH);
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
  }
}
