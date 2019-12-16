package org.folio.rest.validator;

import static org.folio.rest.validator.ValidationConstants.USER_DEFINED_FIELD_MAX_LENGTH;

import java.util.List;
import java.util.Objects;

import javax.validation.ValidationException;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.folio.rest.exception.InputValidationException;
import org.folio.rest.jaxrs.model.TitlePostDataAttributes;
import org.folio.rest.jaxrs.model.TitlePostIncluded;
import org.folio.rest.jaxrs.model.TitlePostRequest;

/**
 * Verifies that post data for titles are valid
 */
@Component
public class TitlesPostBodyValidator {

  private static final String INVALID_POST_BODY = "Invalid request body";

  private TitleCommonRequestAttributesValidator attributesValidator;

  @Autowired
  public TitlesPostBodyValidator(TitleCommonRequestAttributesValidator attributesValidator) {
    this.attributesValidator = attributesValidator;
  }

  /**
   * Provides validation for the post data attributes
   *
   * @throws ValidationException if validation fails
   */
  public void validate(TitlePostRequest entity) {

    if (Objects.isNull(entity) || Objects.isNull(entity.getData()) || Objects.isNull(entity.getData().getAttributes())) {
      throw new InputValidationException(INVALID_POST_BODY, "");
    }
    List<TitlePostIncluded> included = entity.getIncluded();
    if (Objects.isNull(included) ||  included.isEmpty() || Objects.isNull(included.get(0))){
      throw new InputValidationException("Missing resource", "");
    }
    if (Objects.isNull(included.get(0).getAttributes()) ||
      Objects.isNull(included.get(0).getAttributes().getPackageId()) ||
      StringUtils.isEmpty(included.get(0).getAttributes().getPackageId())){
      throw new InputValidationException("Invalid package Id", "");
    }

    TitlePostDataAttributes attributes = entity.getData().getAttributes();
    ValidatorUtil.checkMaxLength("userDefinedField1", attributes.getUserDefinedField1(), USER_DEFINED_FIELD_MAX_LENGTH);
    ValidatorUtil.checkMaxLength("userDefinedField2", attributes.getUserDefinedField2(), USER_DEFINED_FIELD_MAX_LENGTH);
    ValidatorUtil.checkMaxLength("userDefinedField3", attributes.getUserDefinedField3(), USER_DEFINED_FIELD_MAX_LENGTH);
    ValidatorUtil.checkMaxLength("userDefinedField4", attributes.getUserDefinedField4(), USER_DEFINED_FIELD_MAX_LENGTH);
    ValidatorUtil.checkMaxLength("userDefinedField5", attributes.getUserDefinedField5(), USER_DEFINED_FIELD_MAX_LENGTH);
    attributesValidator.validate(attributes);
  }
}
