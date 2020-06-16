package org.folio.rest.converter.kbcredentials;

import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import org.folio.rest.jaxrs.model.KbCredentials;
import org.folio.rest.jaxrs.model.KbCredentialsDataAttributes;
import org.folio.rest.jaxrs.model.KbCredentialsPatchRequest;
import org.folio.rest.jaxrs.model.KbCredentialsPatchRequestData;
import org.folio.rest.jaxrs.model.KbCredentialsPatchRequestDataAttributes;

@Component
public class KbCredentialsPatchRequestConverter implements Converter<KbCredentialsPatchRequest, KbCredentials> {

  @Override
  public KbCredentials convert(@NonNull KbCredentialsPatchRequest source) {
    KbCredentialsPatchRequestData data = source.getData();
    KbCredentialsPatchRequestDataAttributes attributes = data.getAttributes();
    return new KbCredentials()
      .withId(data.getId())
      .withAttributes(new KbCredentialsDataAttributes()
        .withApiKey(attributes.getApiKey())
        .withCustomerId(attributes.getCustomerId())
        .withName(attributes.getName())
        .withUrl(attributes.getUrl())
      );
  }
}
