package org.folio.rest.converter.kbcredentials;

import org.folio.holdingsiq.model.Configuration;
import org.folio.repository.kbcredentials.DbKbCredentials;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class DbKbCredentialsConfigurationConverter implements Converter<DbKbCredentials, Configuration> {

  @Override
  public Configuration convert(DbKbCredentials source) {
    return Configuration.builder()
      .url(source.getUrl())
      .apiKey(source.getApiKey())
      .customerId(source.getCustomerId())
      .build();
  }
}
