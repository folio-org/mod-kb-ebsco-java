package org.folio.rest.validator;

import java.util.List;
import java.util.Objects;

import javax.validation.ValidationException;

import org.apache.commons.lang.StringUtils;
import org.folio.rest.exception.InputValidationException;
import org.folio.rest.jaxrs.model.TitlePostIncluded;
import org.folio.rest.jaxrs.model.TitlePostRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Verifies that post data for titles are valid
 */
@Component
public class TitlesPostBodyValidator {

  private static final String INVALID_POST_BODY = "Invalid request body";

  private TitlesPostAttributesValidator attributesValidator;

  @Autowired
  public TitlesPostBodyValidator(TitlesPostAttributesValidator attributesValidator) {
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

    attributesValidator.validate(entity.getData().getAttributes());
  }
}
