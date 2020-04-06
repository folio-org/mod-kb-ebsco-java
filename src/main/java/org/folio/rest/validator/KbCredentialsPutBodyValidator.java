package org.folio.rest.validator;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import org.folio.rest.jaxrs.model.KbCredentialsPutRequest;

@Component
public class KbCredentialsPutBodyValidator {

  private static final String NAME_PARAMETER = "name";

  private final int nameLengthMax;

  public KbCredentialsPutBodyValidator(@Value("${kb.ebsco.credentials.name.length.max:255}") int nameLengthMax) {
    this.nameLengthMax = nameLengthMax;
  }

  public void validate(KbCredentialsPutRequest entity) {
    String name = entity.getData().getAttributes().getName();
    ValidatorUtil.checkIsNotBlank(NAME_PARAMETER, name);
    ValidatorUtil.checkMaxLength(NAME_PARAMETER, name, nameLengthMax);
  }
}
