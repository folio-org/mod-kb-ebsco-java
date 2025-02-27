package org.folio.rest.validator;

import static org.folio.rest.validator.ValidatorUtil.checkMaxLength;

import jakarta.validation.ValidationException;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.folio.properties.customlabels.CustomLabelsProperties;
import org.folio.rest.exception.InputValidationException;
import org.folio.rest.jaxrs.model.TitlePostDataAttributes;
import org.folio.rest.jaxrs.model.TitlePostIncluded;
import org.folio.rest.jaxrs.model.TitlePostRequest;
import org.springframework.stereotype.Component;

/**
 * Verifies that post data for titles are valid.
 */
@Component
@RequiredArgsConstructor
public class TitlesPostBodyValidator {

  private static final String INVALID_POST_BODY = "Invalid request body";

  private final TitleCommonRequestAttributesValidator attributesValidator;
  private final CustomLabelsProperties customLabelsProperties;

  /**
   * Provides validation for the post data attributes.
   *
   * @throws ValidationException if validation fails
   */
  public void validate(TitlePostRequest entity) {

    if (Objects.isNull(entity) || Objects.isNull(entity.getData())
      || Objects.isNull(entity.getData().getAttributes())) {
      throw new InputValidationException(INVALID_POST_BODY, "");
    }
    List<TitlePostIncluded> included = entity.getIncluded();
    if (Objects.isNull(included) || included.isEmpty() || Objects.isNull(included.getFirst())) {
      throw new InputValidationException("Missing resource", "");
    }
    if (Objects.isNull(included.getFirst().getAttributes())
      || Objects.isNull(included.getFirst().getAttributes().getPackageId())
      || StringUtils.isEmpty(included.getFirst().getAttributes().getPackageId())) {
      throw new InputValidationException("Invalid package Id", "");
    }

    TitlePostDataAttributes attributes = entity.getData().getAttributes();
    int valueMaxLength = customLabelsProperties.valueMaxLength();
    checkMaxLength("userDefinedField1", attributes.getUserDefinedField1(), valueMaxLength);
    checkMaxLength("userDefinedField2", attributes.getUserDefinedField2(), valueMaxLength);
    checkMaxLength("userDefinedField3", attributes.getUserDefinedField3(), valueMaxLength);
    checkMaxLength("userDefinedField4", attributes.getUserDefinedField4(), valueMaxLength);
    checkMaxLength("userDefinedField5", attributes.getUserDefinedField5(), valueMaxLength);
    attributesValidator.validate(attributes);
  }
}
