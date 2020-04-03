package org.folio.rest.validator;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import org.folio.rest.jaxrs.model.KbCredentials;
import org.folio.rest.jaxrs.model.KbCredentialsPostRequest;

@Component
public class KbCredentialsPostBodyValidator {

  private static final String ID_PARAMETER = "id";
  private static final String NAME_PARAMETER = "name";

  private final int nameLengthMax;

  public KbCredentialsPostBodyValidator(@Value("${kb.ebsco.credentials.name.length.max:255}") int nameLengthMax) {
    this.nameLengthMax = nameLengthMax;
  }

  public void validate(KbCredentialsPostRequest entity) {
    KbCredentials entityData = entity.getData();
    ValidatorUtil.checkIsNull(ID_PARAMETER, entityData.getId());
    ValidatorUtil.checkIsNotBlank(NAME_PARAMETER, entityData.getAttributes().getName());
    ValidatorUtil.checkMaxLength(NAME_PARAMETER, entityData.getAttributes().getName(), nameLengthMax);
  }
}
