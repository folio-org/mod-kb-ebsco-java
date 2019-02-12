package org.folio.rest.validator;

import java.util.List;
import java.util.Objects;

import javax.validation.ValidationException;

import org.folio.rest.exception.InputValidationException;
import org.folio.rest.jaxrs.model.Identifier;
import org.folio.rest.jaxrs.model.TitlePostDataAttributes;
import org.springframework.stereotype.Component;

@Component
public class TitlesPostAttributesValidator {

  private static final String TITLE_NAME = "Title Name";

  /**
   * Provides validation for the post data attributes
   *
   * @throws ValidationException if validation fails
   */
  public void validate(TitlePostDataAttributes attributes) {
    ValidatorUtil.checkIsNotNull(TITLE_NAME, attributes.getName());
    ValidatorUtil.checkIsNotEmpty(TITLE_NAME, attributes.getName());
    ValidatorUtil.checkMaxLength(TITLE_NAME, attributes.getName(), 400);

    ValidatorUtil.checkMaxLength("Publisher name", attributes.getPublisherName(), 250);
    ValidatorUtil.checkMaxLength("Edition", attributes.getEdition(), 250);
    ValidatorUtil.checkMaxLength("Description", attributes.getDescription(), 400);
    ValidatorUtil.checkIsNotNull("Publication Type", attributes.getPublicationType());

    checkIdentifiersList(attributes.getIdentifiers());
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
