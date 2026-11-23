package org.folio.rest.validator.resources;

import lombok.RequiredArgsConstructor;
import org.folio.rest.jaxrs.model.ResourcePutDataAttributes;
import org.folio.rest.jaxrs.model.ResourcePutRequest;
import org.folio.rest.validator.ValidatorUtil;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ManagedResourcePutBodyValidator {

  private final ResourceCommonAttributesValidator commonAttributesValidator;

  public void validate(ResourcePutRequest request) {
    var attributes = request.getData().getAttributes();

    // Updates cannot be made to a resource unless it is selected
    if (attributes.getIsSelected()) {
      commonAttributesValidator.validate(attributes);
    } else {
      validateNotSelected(attributes);
    }
  }

  private void validateNotSelected(ResourcePutDataAttributes attributes) {
    var embargoUnit = attributes.getCustomEmbargoPeriod() != null
                      ? attributes.getCustomEmbargoPeriod().getEmbargoUnit()
                      : null;
    ValidatorUtil.checkIsEmpty("coverageStatement", attributes.getCoverageStatement());
    ValidatorUtil.checkIsNull("customEmbargoPeriod.embargoUnit", embargoUnit);
    ValidatorUtil.checkIsEmptyCollection("visibility", attributes.getVisibility());
    ValidatorUtil.checkIsEmptyCollection("customCoverages", attributes.getCustomCoverages());
    ValidatorUtil.checkIsEmpty("userDefinedField1", attributes.getUserDefinedField1());
    ValidatorUtil.checkIsEmpty("userDefinedField2", attributes.getUserDefinedField2());
    ValidatorUtil.checkIsEmpty("userDefinedField3", attributes.getUserDefinedField3());
    ValidatorUtil.checkIsEmpty("userDefinedField4", attributes.getUserDefinedField4());
    ValidatorUtil.checkIsEmpty("userDefinedField5", attributes.getUserDefinedField5());
  }
}
