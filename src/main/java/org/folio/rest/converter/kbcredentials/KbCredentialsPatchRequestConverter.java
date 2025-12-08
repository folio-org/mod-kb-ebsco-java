package org.folio.rest.converter.kbcredentials;

import org.folio.rest.jaxrs.model.KbCredentials;
import org.folio.rest.jaxrs.model.KbCredentialsDataAttributes;
import org.folio.rest.jaxrs.model.KbCredentialsPatchRequest;
import org.folio.rest.jaxrs.model.KbCredentialsPatchRequestData;
import org.folio.rest.jaxrs.model.KbCredentialsPatchRequestDataAttributes;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class KbCredentialsPatchRequestConverter implements Converter<KbCredentialsPatchRequest, KbCredentials> {

  @Override
  public KbCredentials convert(KbCredentialsPatchRequest source) {
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
