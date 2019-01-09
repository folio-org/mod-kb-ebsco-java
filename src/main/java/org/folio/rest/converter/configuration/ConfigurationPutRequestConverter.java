package org.folio.rest.converter.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import org.folio.config.RMAPIConfiguration;
import org.folio.rest.jaxrs.model.ConfigurationPutRequest;

@Component
public class ConfigurationPutRequestConverter implements Converter<ConfigurationPutRequest, RMAPIConfiguration> {

  private final String defaultUrl;

  public ConfigurationPutRequestConverter(@Value("${configuration.base.url.default}") String defaultUrl) {
    this.defaultUrl = defaultUrl;
  }

  @Override
  public RMAPIConfiguration convert(@NonNull ConfigurationPutRequest configuration) {
    String rmapiBaseUrl = configuration.getData().getAttributes().getRmapiBaseUrl();
    rmapiBaseUrl = rmapiBaseUrl != null ? rmapiBaseUrl : defaultUrl;

    RMAPIConfiguration.RMAPIConfigurationBuilder builder = RMAPIConfiguration.builder();
    builder.url(rmapiBaseUrl);
    builder.apiKey(configuration.getData().getAttributes().getApiKey());
    builder.customerId(configuration.getData().getAttributes().getCustomerId());
    return builder.build();
  }
}
