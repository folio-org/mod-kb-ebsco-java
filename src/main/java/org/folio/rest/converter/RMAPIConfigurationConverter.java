package org.folio.rest.converter;

import org.apache.commons.lang3.StringUtils;
import org.folio.config.RMAPIConfiguration;
import org.folio.properties.PropertyConfiguration;
import org.folio.rest.jaxrs.model.ConfigurationAttributes;
import org.folio.rest.jaxrs.model.Configuration;
import org.folio.rest.jaxrs.model.ConfigurationPutRequest;
import org.folio.rest.jaxrs.model.ConfigurationData;
import org.folio.rest.util.RestConstants;
import org.springframework.stereotype.Component;

/**
 * Converts objects between REST API representation and internal representation
 */
@Component
public class RMAPIConfigurationConverter {
  private static final String DEFAULT_URL = PropertyConfiguration.getInstance().getConfiguration().getString("configuration.base.url.default");

  public Configuration convertToConfiguration(RMAPIConfiguration rmAPIConfig) {
    Configuration jsonConfig = new Configuration();
    jsonConfig.setData(new ConfigurationData());
    jsonConfig.getData().setId("configuration");
    jsonConfig.getData().setType("configurations");
    jsonConfig.getData().setAttributes(new ConfigurationAttributes());
    if(rmAPIConfig.getAPIKey() != null){
      jsonConfig.getData().getAttributes().setApiKey(StringUtils.repeat("*", 40));
    }
    jsonConfig.getData().getAttributes().setCustomerId(rmAPIConfig.getCustomerId());
    String baseUrl = rmAPIConfig.getUrl();
    baseUrl = baseUrl != null ? baseUrl : DEFAULT_URL;
    jsonConfig.getData().getAttributes().setRmapiBaseUrl(baseUrl);
    jsonConfig.setJsonapi(RestConstants.JSONAPI);
    return jsonConfig;
  }

  public RMAPIConfiguration convertToRMAPIConfiguration(ConfigurationPutRequest configuration) {
    String rmapiBaseUrl = configuration.getData().getAttributes().getRmapiBaseUrl();
    rmapiBaseUrl = rmapiBaseUrl != null ? rmapiBaseUrl : DEFAULT_URL;

    RMAPIConfiguration.RMAPIConfigurationBuilder builder = RMAPIConfiguration.builder();
    builder.url(rmapiBaseUrl);
    builder.apiKey(configuration.getData().getAttributes().getApiKey());
    builder.customerId(configuration.getData().getAttributes().getCustomerId());
    return builder.build();
  }
}
