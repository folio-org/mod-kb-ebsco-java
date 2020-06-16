package org.folio.rest.validator.kbcredentials;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import org.folio.rest.jaxrs.model.KbCredentials;
import org.folio.rest.jaxrs.model.KbCredentialsDataAttributes;
import org.folio.rest.validator.ValidatorUtil;

@Component
public class KbCredentialsPatchBodyValidator extends KbCredentialsBodyAttributesValidator {

  public KbCredentialsPatchBodyValidator(@Value("${kb.ebsco.credentials.name.length.max:255}") int nameLengthMax) {
    super(nameLengthMax);
  }

  public void validate(KbCredentials patchRequest) {
    KbCredentialsDataAttributes attributes = patchRequest.getAttributes();
    ValidatorUtil.checkIsNotAllBlank("attributes",
      attributes.getApiKey(), attributes.getCustomerId(), attributes.getName(), attributes.getUrl()
    );

    if (attributes.getName() != null) {
      validateName(attributes);
    }
    if (attributes.getApiKey() != null) {
      validateApiKey(attributes);
    }
    if (attributes.getCustomerId() != null) {
      validateCustomerId(attributes);
    }
    if (attributes.getUrl() != null) {
      validateUrl(attributes);
    }
  }
}
