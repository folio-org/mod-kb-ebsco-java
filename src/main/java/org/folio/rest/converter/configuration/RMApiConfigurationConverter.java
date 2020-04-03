package org.folio.rest.converter.configuration;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import org.folio.rest.jaxrs.model.Configuration;
import org.folio.rest.jaxrs.model.ConfigurationAttributes;
import org.folio.rest.jaxrs.model.ConfigurationData;
import org.folio.rest.util.RestConstants;

@Component
public class RMApiConfigurationConverter implements Converter<org.folio.holdingsiq.model.Configuration, Configuration> {

  private final String defaultUrl;

  public RMApiConfigurationConverter(@Value("${kb.ebsco.credentials.url.default}") String defaultUrl) {
    this.defaultUrl = defaultUrl;
  }

  @Override
  public Configuration convert(@NonNull org.folio.holdingsiq.model.Configuration rmAPIConfig) {
    Configuration jsonConfig = new Configuration();
    jsonConfig.setData(new ConfigurationData());
    jsonConfig.getData().setId("configuration");
    jsonConfig.getData().setType("configurations");
    jsonConfig.getData().setAttributes(new ConfigurationAttributes());
    if(rmAPIConfig.getApiKey() != null){
      jsonConfig.getData().getAttributes().setApiKey(StringUtils.repeat("*", 40));
    }
    jsonConfig.getData().getAttributes().setCustomerId(rmAPIConfig.getCustomerId());
    String baseUrl = rmAPIConfig.getUrl();
    baseUrl = baseUrl != null ? baseUrl : defaultUrl;
    jsonConfig.getData().getAttributes().setRmapiBaseUrl(baseUrl);
    jsonConfig.setJsonapi(RestConstants.JSONAPI);
    return jsonConfig;
  }
}
