package org.folio.rest.validator;

import org.apache.commons.lang.StringUtils;

import java.util.Objects;
import java.util.List;
import javax.validation.ValidationException;
import org.folio.rest.exception.InputValidationException;
import org.folio.rest.jaxrs.model.Identifier;
import org.folio.rest.jaxrs.model.TitlePostIncluded;
import org.folio.rest.jaxrs.model.TitlePostRequest;
import org.springframework.stereotype.Component;

/**
 * Verifies that post data for titles are valid
 */
@Component
public class TitlesPostBodyValidator {

  private static final String INVALID_POST_BODY = "Invalid request body";
  private static final String TITLE_NAME = "Title Name";

  /**
   * Provides validation for the post data attributes
   *
   * @throws ValidationException if validation fails
   */
  public void validate(TitlePostRequest entity) {

    if (Objects.isNull(entity) || Objects.isNull(entity.getData()) || Objects.isNull(entity.getData().getAttributes())) {
      throw new InputValidationException(INVALID_POST_BODY, "");
    }

    ValidatorUtil.checkIsNotNull(TITLE_NAME, entity.getData().getAttributes().getName());
    ValidatorUtil.checkIsNotEmpty(TITLE_NAME, entity.getData().getAttributes().getName());
    ValidatorUtil.checkMaxLength(TITLE_NAME, entity.getData().getAttributes().getName(), 400);

    ValidatorUtil.checkMaxLength("Publisher name", entity.getData().getAttributes().getPublisherName(), 250);
    ValidatorUtil.checkMaxLength("Edition", entity.getData().getAttributes().getEdition(), 250);
    ValidatorUtil.checkMaxLength("Description", entity.getData().getAttributes().getDescription(), 1500);

    ValidatorUtil.checkIsNotNull("Publication Type", entity.getData().getAttributes().getPublicationType());

    List<TitlePostIncluded> included = entity.getIncluded();
    if (Objects.isNull(included) ||  included.isEmpty() || Objects.isNull(included.get(0))){
      throw new InputValidationException("Missing resource", "");
    }
    if (Objects.isNull(included.get(0).getAttributes()) ||
      Objects.isNull(included.get(0).getAttributes().getPackageId()) ||
      StringUtils.isEmpty(included.get(0).getAttributes().getPackageId())){
      throw new InputValidationException("Invalid package Id", "");
    }

    checkIdentifiersList(entity.getData().getAttributes().getIdentifiers());
  }

  private void checkIdentifiersList(
    List<Identifier> identifiers) {
    identifiers.forEach(titleIdentifier -> {
      if(Objects.isNull(titleIdentifier.getId()) || titleIdentifier.getId().length() > 20 ){
        throw new InputValidationException("Invalid Identifier id", "");
      }
    });

  }
}
