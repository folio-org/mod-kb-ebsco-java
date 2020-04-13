package org.folio.rest.converter.kbcredentials;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import org.folio.holdingsiq.model.Configuration;
import org.folio.rest.jaxrs.model.KbCredentials;

@Component
public class KbCredentialsConfigurationConverter implements Converter<KbCredentials, Configuration> {

  @Override
  public Configuration convert(KbCredentials source) {
    return Configuration.builder()
      .url(source.getAttributes().getUrl())
      .apiKey(source.getAttributes().getApiKey())
      .customerId(source.getAttributes().getCustomerId())
      .build();
  }
}
