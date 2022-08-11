package org.folio.rest.validator.kbcredentials;

import org.folio.rest.jaxrs.model.KbCredentialsPutRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class KbCredentialsPutBodyValidator extends KbCredentialsBodyAttributesValidator {

  public KbCredentialsPutBodyValidator(@Value("${kb.ebsco.credentials.name.length.max:255}") int nameLengthMax) {
    super(nameLengthMax);
  }

  public void validate(KbCredentialsPutRequest entity) {
    validateAttributes(entity.getData().getAttributes());
  }
}
