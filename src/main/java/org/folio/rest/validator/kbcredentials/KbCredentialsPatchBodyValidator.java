package org.folio.rest.validator.kbcredentials;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import org.folio.rest.jaxrs.model.KbCredentialsDataAttributes;
import org.folio.rest.jaxrs.model.KbCredentialsPatchRequest;

@Component
public class KbCredentialsPatchBodyValidator extends KbCredentialsBodyAttributesValidator {

  public KbCredentialsPatchBodyValidator(@Value("${kb.ebsco.credentials.name.length.max:255}") int nameLengthMax) {
    super(nameLengthMax);
  }

  public void validate(KbCredentialsPatchRequest patchRequest) {
    KbCredentialsDataAttributes attributes = patchRequest.getData().getAttributes();
    if (attributes.getName() != null) {
      validateName(attributes);
    }
    if (attributes.getUrl() != null) {
      validateUrl(attributes);
    }
  }
}
