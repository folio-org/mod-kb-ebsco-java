package org.folio.rest.converter;

import org.apache.commons.lang3.StringUtils;
import org.folio.config.RMAPIConfiguration;
import org.folio.rest.jaxrs.model.Attributes;
import org.folio.rest.jaxrs.model.Configuration;
import org.folio.rest.jaxrs.model.Data;

/**
 * Converts internal RMAPIConfiguration object into object that will be returned in response
 */
public class RMAPIConfigurationConverter {
  public Configuration convert(RMAPIConfiguration rmAPIConfig) {
    Configuration jsonConfig = new Configuration();
    jsonConfig.setData(new Data());
    jsonConfig.getData().setType("configurations");
    jsonConfig.getData().setAttributes(new Attributes());
    jsonConfig.getData().getAttributes().setApiKey(StringUtils.repeat("*", 40));
    jsonConfig.getData().getAttributes().setCustomerId(rmAPIConfig.getCustomerId());
    jsonConfig.getData().getAttributes().setRmapiBaseUrl(rmAPIConfig.getUrl());
    return jsonConfig;
  }
}
