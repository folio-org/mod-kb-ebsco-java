package org.folio.rest.validator.kbcredentials;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import org.folio.rest.jaxrs.model.KbCredentialsPostRequest;

@Component
public class KbCredentialsPostBodyValidator extends KbCredentialsBodyAttributesValidator {


  public KbCredentialsPostBodyValidator(@Value("${kb.ebsco.credentials.name.length.max:255}") int nameLengthMax) {
    super(nameLengthMax);
  }

  public void validate(KbCredentialsPostRequest entity) {
    validateAttributes(entity.getData().getAttributes());
  }
}
