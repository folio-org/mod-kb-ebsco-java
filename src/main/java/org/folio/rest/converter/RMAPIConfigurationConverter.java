package org.folio.rest.converter;

import org.apache.commons.lang3.StringUtils;
import org.folio.config.RMAPIConfiguration;
import org.folio.rest.jaxrs.model.Attributes;
import org.folio.rest.jaxrs.model.Configuration;
import org.folio.rest.jaxrs.model.ConfigurationPutRequest;
import org.folio.rest.jaxrs.model.Data;

/**
 * Converts objects between REST API representation and internal representation
 */
public class RMAPIConfigurationConverter {
  private static final String DEFAULT_URL = "https://sandbox.ebsco.io";

  public Configuration convertToConfiguration(RMAPIConfiguration rmAPIConfig) {
    Configuration jsonConfig = new Configuration();
    jsonConfig.setData(new Data());
    jsonConfig.getData().setType("configurations");
    jsonConfig.getData().setAttributes(new Attributes());
    if(rmAPIConfig.getAPIKey() != null){
      jsonConfig.getData().getAttributes().setApiKey(StringUtils.repeat("*", 40));
    }
    jsonConfig.getData().getAttributes().setCustomerId(rmAPIConfig.getCustomerId());
    jsonConfig.getData().getAttributes().setRmapiBaseUrl(rmAPIConfig.getUrl());
    return jsonConfig;
  }

  public RMAPIConfiguration convertToRMAPIConfiguration(ConfigurationPutRequest configuration) {
    String rmapiBaseUrl = configuration.getData().getAttributes().getRmapiBaseUrl();
    rmapiBaseUrl = rmapiBaseUrl != null ? rmapiBaseUrl : DEFAULT_URL;

    RMAPIConfiguration rmapiConfiguration = new RMAPIConfiguration();
    rmapiConfiguration.setUrl(rmapiBaseUrl);
    rmapiConfiguration.setApiKey(configuration.getData().getAttributes().getApiKey());
    rmapiConfiguration.setCustomerId(configuration.getData().getAttributes().getCustomerId());
    return rmapiConfiguration;
  }
}
