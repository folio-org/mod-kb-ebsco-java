package org.folio.rest.converter.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import org.folio.holdingsiq.model.Configuration;
import org.folio.rest.jaxrs.model.ConfigurationPutRequest;

@Component
public class ConfigurationPutRequestConverter implements Converter<ConfigurationPutRequest, Configuration> {

  private final String defaultUrl;

  public ConfigurationPutRequestConverter(@Value("${kb.ebsco.credentials.url.default}") String defaultUrl) {
    this.defaultUrl = defaultUrl;
  }

  @Override
  public Configuration convert(@NonNull ConfigurationPutRequest configuration) {
    String rmapiBaseUrl = configuration.getData().getAttributes().getRmapiBaseUrl();
    rmapiBaseUrl = rmapiBaseUrl != null ? rmapiBaseUrl : defaultUrl;

    Configuration.ConfigurationBuilder builder = Configuration.builder();
    builder.url(rmapiBaseUrl);
    builder.apiKey(configuration.getData().getAttributes().getApiKey());
    builder.customerId(configuration.getData().getAttributes().getCustomerId());
    return builder.build();
  }
}
