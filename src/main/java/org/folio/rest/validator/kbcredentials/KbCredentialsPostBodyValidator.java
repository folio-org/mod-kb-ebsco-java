package org.folio.rest.validator.kbcredentials;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import org.folio.rest.jaxrs.model.KbCredentialsPostRequest;
import org.folio.rest.validator.ValidatorUtil;

@Component
public class KbCredentialsPostBodyValidator extends KbCredentialsBodyAttributesValidator {

  private static final String ID_PARAMETER = "id";

  public KbCredentialsPostBodyValidator(@Value("${kb.ebsco.credentials.name.length.max:255}") int nameLengthMax) {
    super(nameLengthMax);
  }

  public void validate(KbCredentialsPostRequest entity) {
    ValidatorUtil.checkIsNull(ID_PARAMETER, entity.getData().getId());
    validateAttributes(entity.getData().getAttributes());
  }
}
