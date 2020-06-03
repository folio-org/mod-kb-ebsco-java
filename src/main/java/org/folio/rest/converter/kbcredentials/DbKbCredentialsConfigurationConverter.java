package org.folio.rest.converter.kbcredentials;

import org.jetbrains.annotations.NotNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import org.folio.holdingsiq.model.Configuration;
import org.folio.repository.kbcredentials.DbKbCredentials;

@Component
public class DbKbCredentialsConfigurationConverter implements Converter<DbKbCredentials, Configuration> {

  @Override
  public Configuration convert(@NotNull DbKbCredentials source) {
    return Configuration.builder()
      .url(source.getUrl())
      .apiKey(source.getApiKey())
      .customerId(source.getCustomerId())
      .build();
  }
}
